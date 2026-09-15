package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserManager;

import java.util.ArrayList;
import java.util.List;

import zixing.bluetooth.unlocker.Xp.MyXp;

public final class BluetoothHelper {
    public static Object BluetoothControllerImplInstance;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static RssiSessionManager unlockSessions;
    private static PassiveRssiObserver passiveObserver;
    private static long lastCheck = -2000;
    private static String checkingConfig = "";

    public static RssiSessionManager createRssiManager(Context context) {
        Context app = context.getApplicationContext();
        Context connectionContext = app == null ? context : app;
        return new RssiSessionManager(new RssiSessionManager.Scheduler() {
            public void post(Runnable task, long delay) { MAIN.postDelayed(task, delay); }
            public void cancel(Runnable task) { MAIN.removeCallbacks(task); }
            public long nowMs() { return SystemClock.elapsedRealtime(); }
        }, (address, events) -> connect(connectionContext, address, events));
    }

    @SuppressLint("MissingPermission")
    private static RssiSessionManager.Link connect(Context context, String address,
                                                    RssiSessionManager.Events events) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) return null;
        BluetoothDevice device = adapter.getRemoteDevice(address);
        // A classic audio-only device is not a GATT server. Use the explicit connected mode.
        if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) return null;
        BluetoothGatt gatt = device.connectGatt(context, false, new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt g, int status, int state) {
                if (status != BluetoothGatt.GATT_SUCCESS || state == BluetoothProfile.STATE_DISCONNECTED) {
                    events.failed();
                } else if (state == BluetoothProfile.STATE_CONNECTED) {
                    events.connected();
                }
            }
            @Override
            public void onReadRemoteRssi(BluetoothGatt g, int rssi, int status) {
                events.rssi(rssi, status == BluetoothGatt.GATT_SUCCESS);
            }
        }, BluetoothDevice.TRANSPORT_LE);
        if (gatt == null) return null;
        return new RssiSessionManager.Link() {
            public boolean readRssi() { return gatt.readRemoteRssi(); }
            // Close only our client. Never toggle the adapter or disconnect another app's profiles.
            public void close() { gatt.close(); }
        };
    }

    public static boolean canCheckLockscreen(Context context) {
        KeyguardManager keyguard = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        UserManager users = (UserManager) context.getSystemService(Context.USER_SERVICE);
        return keyguard != null && power != null && users != null && users.isUserUnlocked()
                && power.isInteractive() && keyguard.isKeyguardLocked();
    }

    @SuppressLint("MissingPermission")
    public static boolean isBondedAndConnected(BluetoothDevice device) {
        if (device == null) return false;
        try {
            if (device.getBondState() != BluetoothDevice.BOND_BONDED) return false;
            if (BluetoothControllerImplInstance != null) {
                Object connected = ReflectUtil.callMethod(BluetoothControllerImplInstance, "getConnectedDevices");
                if (connected instanceof Iterable<?>) {
                    for (Object cached : (Iterable<?>) connected) {
                        BluetoothDevice candidate = (BluetoothDevice) ReflectUtil.callMethod(cached, "getDevice");
                        if (candidate != null && device.getAddress().equals(candidate.getAddress())
                                && Boolean.TRUE.equals(ReflectUtil.callMethod(cached, "isConnected"))) return true;
                    }
                }
            }
        } catch (Exception | LinkageError ignored) { }
        try {
            // Available inside the hooked system process; app-side failure is displayed as unknown.
            return device.getBondState() == BluetoothDevice.BOND_BONDED
                    && Boolean.TRUE.equals(ReflectUtil.callMethod(device, "isConnected"));
        } catch (Exception | LinkageError ignored) { return false; }
    }

    // type 1: Settings preview; type 2: lock screen. Coalesce repeated native callbacks.
    public static void CanUnlockByBluetoothOldDirect(Context context, String legacyAddress,
                                                      ClassLoader loader, int type) {
        if (context == null) return;
        MAIN.post(() -> check(context, legacyAddress, type));
    }

    @SuppressLint("MissingPermission")
    private static void check(Context context, String legacyAddress, int type) {
        try {
            if (type == 2 && !canCheckLockscreen(context)) { releaseGatt(); stopPassive(); return; }
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) { releaseGatt(); stopPassive(); return; }
            List<TrustedDevice> devices = ConfigUtil.getDevicesForCheck(type, legacyAddress);
            String signature = TrustedDevice.encode(devices) + ConfigUtil.getString("rssi", "-50", type);
            if (unlockSessions == null) unlockSessions = createRssiManager(context);
            List<String> addresses = new ArrayList<>();
            for (TrustedDevice device : devices) {
                if (TrustedDevice.CONNECTED.equals(device.mode)) {
                    if (isBondedAndConnected(adapter.getRemoteDevice(device.address))) {
                        unlockSessions.cancel();
                        deliver(type, context, device.address, signature, legacyAddress);
                        return;
                    }
                } else addresses.add(device.address);
            }
            if (unlockSessions.isRunning() && signature.equals(checkingConfig)) return;
            if (!signature.equals(checkingConfig)) unlockSessions.cancel();
            else if (SystemClock.elapsedRealtime() - lastCheck < 1500) return;
            checkingConfig = signature;
            lastCheck = SystemClock.elapsedRealtime();
            int threshold;
            try { threshold = Integer.parseInt(ConfigUtil.getString("rssi", "-50", type)); }
            catch (NumberFormatException ex) { return; }
            if (threshold < -127 || threshold >= 0) return;
            if (type == 2) {
                preparePassive(context);
                if (passiveObserver != null) {
                    addresses = passiveObserver.prioritize(addresses);
                }
            }
            unlockSessions.checkForUnlock(addresses, threshold, new RssiSessionManager.Listener() {
                public void measured(String address, int rssi) { }
                public void finished(String matched) {
                    if (matched != null) deliver(type, context, matched, signature, legacyAddress);
                    else if (type == 1) MyXp.SetBluetoothStatus((byte) 1);
                }
            });
        } catch (Exception | LinkageError ex) {
            releaseGatt();
            MyXp.myLog("Bluetooth check failed: " + ex.getClass().getSimpleName());
        }
    }

    private static void deliver(int type, Context context, String address, String signature, String legacy) {
        List<TrustedDevice> current = ConfigUtil.getDevicesForCheck(type, legacy);
        if (!signature.equals(TrustedDevice.encode(current) + ConfigUtil.getString("rssi", "-50", type))) return;
        if (type == 1) MyXp.SetBluetoothStatus((byte) 2);
        else if (type == 2 && canCheckLockscreen(context)) {
            MyXp.UnlockPhone();
            stopPassive();
        }
    }

    /** Prime advertising hints during an interactive lock screen; no periodic polling or radio scan. */
    public static void preparePassive(Context context) {
        if (Looper.myLooper() != Looper.getMainLooper()) { MAIN.post(() -> preparePassive(context)); return; }
        if (!canCheckLockscreen(context)) { stopPassive(); return; }
        List<String> addresses = new ArrayList<>();
        List<TrustedDevice> devices = ConfigUtil.getDevices(2);
        for (TrustedDevice device : devices) {
            if (TrustedDevice.PROXIMITY.equals(device.mode)) addresses.add(device.address);
        }
        if (addresses.isEmpty()) { stopPassive(); return; }
        if (passiveObserver == null) passiveObserver = new PassiveRssiObserver();
        passiveObserver.setTargets(addresses, TrustedDevice.encode(devices) + ConfigUtil.getString("rssi", "-50", 2));
    }

    public static void invalidatePassive(String address) {
        if (Looper.myLooper() != Looper.getMainLooper()) { MAIN.post(() -> invalidatePassive(address)); return; }
        if (passiveObserver != null) passiveObserver.invalidate(address);
    }

    public static void stopPassive() {
        if (Looper.myLooper() != Looper.getMainLooper()) { MAIN.post(BluetoothHelper::stopPassive); return; }
        if (passiveObserver != null) passiveObserver.stop();
    }

    public static void releaseGatt() {
        if (Looper.myLooper() != Looper.getMainLooper()) { MAIN.post(BluetoothHelper::releaseGatt); return; }
        if (unlockSessions != null) unlockSessions.cancel();
        lastCheck = -2000;
    }
}
