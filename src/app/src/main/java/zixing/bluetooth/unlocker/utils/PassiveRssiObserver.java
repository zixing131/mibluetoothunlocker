package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Opportunistic only: never starts a radio scan. Registration runs off the SystemUI thread. */
public final class PassiveRssiObserver {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Handler worker;
    private final PassiveSignalCache cache = new PassiveSignalCache();
    private List<String> targets = new ArrayList<>();
    private String configuration = "";
    private volatile long generation;
    // Scanner fields are owned exclusively by worker.
    private BluetoothLeScanner scanner;
    private ScanCallback callback;
    private Runnable pendingStart;
    private long nextStartAt;

    public PassiveRssiObserver() {
        HandlerThread thread = new HandlerThread("unlocker-passive-scan", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        worker = new Handler(thread.getLooper());
    }

    public void setTargets(List<String> requested, String configSignature) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String address : requested) {
            String normalized = TrustedDevice.normalizeAddress(address);
            if (!normalized.isEmpty()) unique.add(normalized);
        }
        List<String> next = new ArrayList<>(unique);
        if (targets.equals(next) && configuration.equals(configSignature)) return;
        targets = next;
        configuration = configSignature;
        cache.setTargets(next);
        long epoch = ++generation;
        worker.post(() -> {
            if (epoch != generation) return;
            stopOwnedScan();
            if (next.isEmpty()) return;
            pendingStart = () -> { if (epoch == generation) start(next, epoch); };
            worker.postDelayed(pendingStart, Math.max(0, nextStartAt - SystemClock.elapsedRealtime()));
        });
    }

    public List<String> prioritize(List<String> addresses) {
        return cache.prioritize(addresses, SystemClock.elapsedRealtimeNanos());
    }
    public void invalidate(String address) { cache.invalidate(address); }
    public void stop() {
        // Invalidate callbacks before posting stopScan (registration may still be in progress).
        targets = new ArrayList<>();
        configuration = "";
        cache.setTargets(targets);
        long epoch = ++generation;
        worker.post(() -> { if (epoch == generation) stopOwnedScan(); });
    }

    @SuppressLint("MissingPermission")
    private void start(List<String> addresses, long epoch) {
        pendingStart = null;
        nextStartAt = SystemClock.elapsedRealtime() + 6000;
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) return;
            scanner = adapter.getBluetoothLeScanner();
            if (scanner == null) return;
            List<ScanFilter> filters = new ArrayList<>();
            for (String address : addresses) filters.add(new ScanFilter.Builder().setDeviceAddress(address).build());
            callback = new ScanCallback() {
                @Override public void onScanResult(int type, ScanResult result) { receive(result, epoch); }
                @Override public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult result : results) receive(result, epoch);
                }
                @Override public void onScanFailed(int code) {
                    if (epoch != generation) return;
                    android.util.Log.w("hookhelper", "Passive RSSI unavailable, scan code=" + code);
                    main.post(() -> { if (epoch == generation) cache.clear(); });
                    worker.post(() -> {
                        if (epoch != generation) return;
                        nextStartAt = SystemClock.elapsedRealtime() + 30000;
                        stopOwnedScan();
                    });
                }
            };
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_OPPORTUNISTIC)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setReportDelay(0).build();
            scanner.startScan(filters, settings, callback);
            if (epoch != generation) stopOwnedScan();
        } catch (RuntimeException ex) {
            android.util.Log.w("hookhelper", "Passive RSSI unavailable: " + ex.getClass().getSimpleName());
            nextStartAt = SystemClock.elapsedRealtime() + 30000;
            stopOwnedScan(); // Permission/OEM restrictions fall back to the existing connection check.
        }
    }

    private void receive(ScanResult result, long epoch) {
        if (epoch != generation || result == null || result.getDevice() == null) return;
        main.post(() -> {
            if (epoch != generation) return;
            cache.record(result.getDevice().getAddress(), result.getRssi(), result.getTimestampNanos(),
                    SystemClock.elapsedRealtimeNanos());
        });
    }

    @SuppressLint("MissingPermission")
    private void stopOwnedScan() {
        if (pendingStart != null) worker.removeCallbacks(pendingStart);
        pendingStart = null;
        try { if (scanner != null && callback != null) scanner.stopScan(callback); }
        catch (RuntimeException ignored) { }
        scanner = null;
        callback = null;
    }
}
