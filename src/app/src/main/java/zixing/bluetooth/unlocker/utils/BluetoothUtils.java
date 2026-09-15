package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import zixing.bluetooth.unlocker.bean.DeviceBean;

@SuppressLint("MissingPermission")
public final class BluetoothUtils {
    private static BluetoothUtils instance;
    private Context context;
    private BluetoothInterface listener;
    private DeviceMetadata metadata;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, DeviceBean> devices = new LinkedHashMap<>();
    private BluetoothLeScanner scanner;
    private boolean scanning;
    private boolean ownsDiscovery;
    private boolean registered;
    private boolean destroyed;
    private final Runnable finishScan = this::cancelDiscovery;
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int type, ScanResult result) {
            handler.post(() -> { if (scanning) update(result.getDevice(), result.getRssi(),
                    result.getScanRecord() == null ? null : result.getScanRecord().getDeviceName()); });
        }
        @Override public void onScanFailed(int code) {
            handler.post(() -> {
                if (!scanning) return;
                report("低功耗蓝牙搜索失败（" + code + "），请稍后重试");
                cancelDiscovery();
            });
        }
    };

    public static BluetoothUtils getInstance() {
        if (instance == null) instance = new BluetoothUtils();
        return instance;
    }
    public ArrayList<DeviceBean> getDeviceBeans() { return new ArrayList<>(devices.values()); }
    public void setBluetoothListener(BluetoothInterface listener) { this.listener = listener; }

    public void initBluetooth(Context context) {
        this.context = context.getApplicationContext();
        destroyed = false;
        metadata = new DeviceMetadata(context);
        if (registered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        if (android.os.Build.VERSION.SDK_INT >= 30) filter.addAction(BluetoothDevice.ACTION_ALIAS_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        ContextCompat.registerReceiver(this.context, receiver, filter, ContextCompat.RECEIVER_EXPORTED);
        registered = true;
    }

    public void loadKnownDevices() {
        // Managing saved targets must also work with Bluetooth off or permission denied.
        for (TrustedDevice trusted : ConfigUtil.getDevices(0)) {
            if (!devices.containsKey(trusted.address)) {
                DeviceBean bean = new DeviceBean();
                bean.setAddress(trusted.address);
                bean.setName(metadata.name(trusted.address));
                devices.put(trusted.address, bean);
            }
        }
        for (DeviceBean bean : devices.values()) bean.setName(metadata.name(bean.getAddress()));
        if (listener != null) listener.updateBluetoothDervice(null);
        if (context == null || !BluetoothPermissions.canConnect(context)) return;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) return;
        try {
            for (TrustedDevice trusted : ConfigUtil.getDevices(0)) update(adapter.getRemoteDevice(trusted.address), null);
            if (adapter.isEnabled()) for (BluetoothDevice device : adapter.getBondedDevices()) update(device, null);
        } catch (SecurityException ex) { report("请授予附近设备权限"); }
    }

    private void update(BluetoothDevice device, Integer rssi) {
        update(device, rssi, null);
    }

    private void update(BluetoothDevice device, Integer rssi, String observedName) {
        if (destroyed || device == null) return;
        try {
            String address = TrustedDevice.normalizeAddress(device.getAddress());
            if (address.isEmpty()) return;
            DeviceBean bean = devices.get(address);
            if (bean == null) { bean = new DeviceBean(); bean.setAddress(address); }
            bean.setName(metadata.resolve(device, observedName));
            bean.setBondState(device.getBondState());
            if (rssi != null) {
                bean.setRssi(rssi);
                bean.setDistance(getDistance(rssi));
            }
            devices.put(address, bean);
            if (listener != null) listener.updateBluetoothDervice(null);
        } catch (SecurityException ex) { report("请授予附近设备权限"); }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context ignored, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (scanning) update(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE),
                        (int) intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE),
                        intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if (scanning && ownsDiscovery) {
                    ownsDiscovery = false;
                    startLeScan();
                }
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (!isEnabled()) cancelDiscovery();
                else loadKnownDevices();
            } else {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                update(device, null, intent.getStringExtra(BluetoothDevice.EXTRA_NAME));
                if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int previous = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    if (state == BluetoothDevice.BOND_BONDED) report("配对成功，可添加为解锁设备");
                    else if (state == BluetoothDevice.BOND_NONE && previous == BluetoothDevice.BOND_BONDING) report("配对未完成，请重试");
                    else if (state == BluetoothDevice.BOND_NONE && previous == BluetoothDevice.BOND_BONDED) {
                        boolean saved = device != null && ConfigUtil.removeDevice(device.getAddress());
                        report(saved ? "已取消配对并移出解锁列表" : "取消配对后保存配置失败，请移除解锁设备");
                    }
                }
            }
        }
    };

    public boolean isEnabled() {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled();
        } catch (SecurityException ex) { return false; }
    }
    public boolean isDiscovering() { return scanning; }

    public boolean startDiscovery() {
        if (context == null || !BluetoothPermissions.canScan(context)) { report("请授予蓝牙和位置权限后搜索"); return false; }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !isEnabled()) { report("请先开启蓝牙"); return false; }
        cancelDiscovery();
        devices.clear();
        loadKnownDevices();
        try {
            if (adapter.isDiscovering()) { report("系统正在搜索，请稍后再试"); return false; }
            scanning = true;
            // Classic discovery first, then a bounded BLE scan; never scan in the background.
            ownsDiscovery = adapter.startDiscovery();
            if (!ownsDiscovery) startLeScan();
            handler.postDelayed(finishScan, 20000);
            return scanning;
        } catch (SecurityException ex) { report("蓝牙权限不足"); cancelDiscovery(); return false; }
    }

    private void startLeScan() {
        if (!scanning) return;
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            scanner = adapter == null ? null : adapter.getBluetoothLeScanner();
            if (scanner == null) { cancelDiscovery(); return; }
            scanner.startScan(scanCallback);
            handler.removeCallbacks(finishScan);
            handler.postDelayed(finishScan, 6000);
        } catch (RuntimeException ex) { report("无法启动低功耗蓝牙搜索"); cancelDiscovery(); }
    }

    public void cancelDiscovery() {
        boolean wasScanning = scanning;
        scanning = false;
        handler.removeCallbacks(finishScan);
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (ownsDiscovery && adapter != null) adapter.cancelDiscovery();
        } catch (RuntimeException ignored) { }
        ownsDiscovery = false;
        try { if (scanner != null) scanner.stopScan(scanCallback); }
        catch (RuntimeException ignored) { }
        scanner = null;
        if (wasScanning && listener != null) listener.onBluetoothFinish();
    }

    public boolean pair(String address) {
        if (context == null || !BluetoothPermissions.canConnect(context)) { report("请先授予附近设备权限"); return false; }
        try {
            cancelDiscovery();
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null || !adapter.isEnabled()) { report("请先开启蓝牙"); return false; }
            BluetoothDevice device = adapter.getRemoteDevice(address);
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) { report("设备已配对"); return true; }
            for (DeviceBean bean : devices.values()) {
                if (bean.getBondState() == BluetoothDevice.BOND_BONDING) { report("请先完成当前设备的配对"); return false; }
            }
            boolean started = device.createBond();
            report(started ? "请在系统配对窗口中确认" : "无法发起配对，请在系统蓝牙设置中重试");
            return started;
        } catch (RuntimeException ex) { report("无法发起配对，请在系统蓝牙设置中重试"); return false; }
    }

    public boolean removeBond(String address) {
        if (context == null || !BluetoothPermissions.canConnect(context)) return false;
        try {
            cancelDiscovery();
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) return false;
            return Boolean.TRUE.equals(ReflectUtil.callMethod(adapter.getRemoteDevice(address), "removeBond"));
        } catch (Exception | LinkageError ex) { return false; }
    }

    public void onDestroy() {
        cancelDiscovery();
        destroyed = true;
        listener = null;
        if (registered) context.unregisterReceiver(receiver);
        registered = false;
        instance = null;
    }
    private void report(String message) { if (listener != null) listener.onBluetoothMessage(message); }
    public interface BluetoothInterface {
        void addBluetoothDervice(DeviceBean bean);
        void updateBluetoothDervice(DeviceBean bean);
        void onBluetoothFinish();
        void onBluetoothMessage(String message);
    }
    public double getDistance(int rssi) {
        return TrustedDevice.validRssi(rssi) ? Math.pow(10.0, (-rssi - 50.0) / 25.0) : Double.NaN;
    }
}
