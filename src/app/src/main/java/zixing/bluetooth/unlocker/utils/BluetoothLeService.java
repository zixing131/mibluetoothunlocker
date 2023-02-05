package zixing.bluetooth.unlocker.utils;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import zixing.bluetooth.unlocker.interfaces.IBluetoothLeConnection;

/* loaded from: classes.dex */
public class BluetoothLeService extends Service {
    public static final int ACTION_ADDED = 1;
    public static final String ACTION_BACKGROUND_MODE_CHANGED = "no.nordicsemi.android.mcp.ACTION_BACKGROUND_MODE_CHANGED";
    public static final String ACTION_CONNECTIONS_CHANGED = "no.nordicsemi.android.mcp.ACTION_CONNECTIONS_CHANGED";
    public static final int ACTION_REMOVED = 0;
    public static final String ACTION_SERVER_CONFIGURATION_CHANGED = "no.nordicsemi.android.mcp.ACTION_SERVER_CONFIGURATION_CHANGED";
    public static final String ACTION_SERVER_FAILED_TO_START = "no.nordicsemi.android.mcp.ACTION_SERVER_FAILED_TO_START";
    public static final String ACTION_STOP_NRF_CONNECT = "no.nordicsemi.android.mcp.ACTION_STOP_NRF_CONNECT";
    private static final int ACTION_STOP_REQ_ID = 22;
    public static final String EXTRA_ACTION = "no.nordicsemi.android.mcp.EXTRA_ACTION";
    public static final String EXTRA_BACKGROUND_MODE = "no.nordicsemi.android.mcp.EXTRA_BACKGROUND_MODE";
    public static final String EXTRA_POSITION = "no.nordicsemi.android.mcp.EXTRA_POSITION";
    private static final int NOTIFICATION_ID = 21;
    private static final String TAG = "BluetoothLeService";
    private boolean mBackgroundMode;
    private Map<Device, IBluetoothLeConnection> mConnections;

    private List<Device> mDevices;
    private boolean mFinishing;
    private Handler mHandler;
    private final ServiceBinder mBinder = new ServiceBinder();
    private final BroadcastReceiver mBluetoothStateBroadcastReceiver = new BroadcastReceiver() { // from class: no.nordicsemi.android.mcp.ble.BluetoothLeService.1
        private String state2String(int i4) {
            switch (i4) {
                case 10:
                    return "OFF";
                case 11:
                    return "TURNING ON";
                case 12:
                    return "ON";
                case 13:
                    return "TURNING OFF";
                default:
                    return "UNKNOWN (" + i4 + ")";
            }
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothLeService.this.isBleSupported()) {
                return;
            }
            int intExtra = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 10);
            String str = "[Broadcast] Action received: android.bluetooth.adapter.action.STATE_CHANGED, state changed to " + state2String(intExtra);
            for (IBluetoothLeConnection iBluetoothLeConnection : BluetoothLeService.this.mConnections.values()) {
                iBluetoothLeConnection.saveLogAndFlash(0, str);
            }
            if (intExtra != 10) {
                if (intExtra == 12) {
                    BluetoothLeService.this.updateServer();
                    return;
                } else if (intExtra != 13) {
                    return;
                }
            }
            for (IBluetoothLeConnection iBluetoothLeConnection2 : BluetoothLeService.this.mConnections.values()) {
                iBluetoothLeConnection2.onBluetoothOff();
            }
            BluetoothLeService.this.stopServer();
        }
    };
    private final BroadcastReceiver mConnectionStateBroadcastReceiver = new BroadcastReceiver() { // from class: no.nordicsemi.android.mcp.ble.BluetoothLeService.2
        @SuppressLint("MissingPermission")
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean equals = "android.bluetooth.device.action.ACL_CONNECTED".equals(action);
            BluetoothDevice bluetoothDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            if (bluetoothDevice == null) {
                return;
            }
            for (IBluetoothLeConnection iBluetoothLeConnection : BluetoothLeService.this.mConnections.values()) {
                Device device = iBluetoothLeConnection.getDevice();
                if (device.getBluetoothDevice().equals(bluetoothDevice)) {
                    iBluetoothLeConnection.saveLogAndFlash(0, "[Broadcast] Action received: " + action);
                    if (!equals || BluetoothLeService.this.isServerStarted() || iBluetoothLeConnection.isConnected() || iBluetoothLeConnection.isDfuInProgress()) {
                        return;
                    }
                    iBluetoothLeConnection.connect();
                    return;
                } else if (iBluetoothLeConnection.isDfuInProgress() && device.equals(bluetoothDevice)) {
                    return;
                }
            }
            if ((Build.VERSION.SDK_INT >= 31 || bluetoothDevice.getType() == 1) {
                return;
            }
            boolean z3 = PreferenceManager.getDefaultSharedPreferences(BluetoothLeService.this).getBoolean(SettingsFragment.SETTINGS_SHOW_INCOMING_CONNECTION, true);
            if (BluetoothLeService.this.isServerStarted() || !z3 || !equals || bluetoothDevice.getBondState() == 11) {
                return;
            }
            if (BluetoothLeService.this.mBackgroundMode) {
                NotificationHelper.showIncomingConnectionNotification(context, bluetoothDevice);
            } else {
                BluetoothLeService.this.onNewConnection(bluetoothDevice);
            }
        }
    };
    private final BroadcastReceiver mRefreshServerConfigurationBroadcastReceiver = new BroadcastReceiver() { // from class: no.nordicsemi.android.mcp.ble.BluetoothLeService.3
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
            if (defaultAdapter == null || !defaultAdapter.isEnabled()) {
                return;
            }
            BluetoothLeService.this.updateServer();
        }
    };
    private final BroadcastReceiver mBackgroundModeBroadcastReceiver = new BroadcastReceiver() { // from class: no.nordicsemi.android.mcp.ble.BluetoothLeService.4
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean booleanExtra = intent.getBooleanExtra(BluetoothLeService.EXTRA_BACKGROUND_MODE, false);
            BluetoothLeService.this.mBackgroundMode = booleanExtra;
            if (BluetoothLeService.this.mServerController != null) {
                BluetoothLeService.this.mServerController.setBackgroundMode(booleanExtra);
            }
            if (booleanExtra) {
                BluetoothLeService.this.startForeground();
            } else {
                BluetoothLeService.this.stopForeground();
            }
        }
    };

    /* loaded from: classes.dex */
    public class ServiceBinder extends Binder implements IBluetoothLeService, IBluetoothLeAdvertiser {
        private int mCurrentPosition;

        public ServiceBinder() {
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public IBluetoothLeConnection createConnection(Device device, boolean z3) {
            BluetoothLeService bluetoothLeService = BluetoothLeService.this;
            BluetoothLeConnection bluetoothLeConnection = new BluetoothLeConnection(bluetoothLeService, bluetoothLeService.mHandler, BluetoothLeService.this.mDatabase, device, BluetoothLeService.this);
            bluetoothLeConnection.setAsCentral(z3);
            bluetoothLeConnection.setServerController(BluetoothLeService.this.mServerController);
            BluetoothLeService.this.mConnections.put(device, bluetoothLeConnection);
            if (BluetoothLeService.this.mServerController != null) {
                BluetoothLeService.this.mServerController.addNewConnection(device.getBluetoothDevice(), bluetoothLeConnection);
            }
            BluetoothLeService.this.mDevices.add(device);
            BluetoothLeService.this.updateForeground();
            Intent intent = new Intent(BluetoothLeService.ACTION_CONNECTIONS_CHANGED);
            intent.putExtra(BluetoothLeService.EXTRA_ACTION, 1);
            i0.a.b(BluetoothLeService.this).d(intent);
            return bluetoothLeConnection;
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public void disconnectAndClose(IBluetoothLeConnection iBluetoothLeConnection, boolean z3) {
            BluetoothLeConnection bluetoothLeConnection = (BluetoothLeConnection) iBluetoothLeConnection;
            int indexOf = BluetoothLeService.this.mDevices.indexOf(bluetoothLeConnection.getDevice());
            BluetoothLeService.this.mDevices.remove(bluetoothLeConnection.getDevice());
            bluetoothLeConnection.disconnectAndClose(z3);
            Intent intent = new Intent(BluetoothLeService.ACTION_CONNECTIONS_CHANGED);
            intent.putExtra(BluetoothLeService.EXTRA_ACTION, 0);
            intent.putExtra(BluetoothLeService.EXTRA_POSITION, indexOf);
            i0.a.b(BluetoothLeService.this).d(intent);
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public void disconnectAndCloseAll(boolean z3) {
            BluetoothLeService.this.mFinishing = true;
            for (IBluetoothLeConnection iBluetoothLeConnection : BluetoothLeService.this.mConnections.values()) {
                ((BluetoothLeConnection) iBluetoothLeConnection).disconnectAndClose(z3);
            }
            BluetoothLeService.this.mDevices.clear();
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser
        public int getAdvertisingTxPower(long j4) {
            return BluetoothLeService.this.mAdvertiserImpl.getAdvertisingTxPower(j4);
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public List<Device> getConnectedDevices() {
            return BluetoothLeService.this.mDevices;
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public IBluetoothLeConnection getConnection(Device device) {
            return (IBluetoothLeConnection) BluetoothLeService.this.mConnections.get(device);
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public int getSelectedTabPosition() {
            return this.mCurrentPosition;
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser
        public boolean isAdvertisementActive(long j4) {
            return BluetoothLeService.this.mAdvertiserImpl.isAdvertisementActive(j4);
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser
        public boolean isAdvertising() {
            return BluetoothLeService.this.mAdvertiserImpl != null && BluetoothLeService.this.mAdvertiserImpl.isAdvertising();
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public void setSelectedTabPosition(int i4) {
            this.mCurrentPosition = i4;
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser
        public void startAdvertisement(long j4, int i4, int i5, AdvertisingStatusCallback advertisingStatusCallback) {
            BluetoothLeService.this.mAdvertiserImpl.startAdvertisement(j4, i4, i5, advertisingStatusCallback);
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser
        public void stopAdvertisement(long j4) {
            BluetoothLeService.this.mAdvertiserImpl.stopAdvertisement(j4);
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser
        public void stopAllAdvertisements() {
            BluetoothLeService.this.mAdvertiserImpl.stopAllAdvertisements();
        }

        @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeService
        public void stopServerIfNoConnections() {
            if (BluetoothLeService.this.mConnections == null || BluetoothLeService.this.mConnections.isEmpty()) {
                BluetoothLeService.this.stopServer();
                BluetoothLeService.this.stopSelf();
            }
        }
    }

    private boolean isBleEnabled() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        return defaultAdapter != null && defaultAdapter.isEnabled();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isBleSupported() {
        return getPackageManager().hasSystemFeature("android.hardware.bluetooth_le");
    }

    private boolean isServerEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(this).getLong(ServerFragment.PREFS_SERVER_CONFIGURATION, 0L) > 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public boolean isServerStarted() {
        ServiceServerController serviceServerController = this.mServerController;
        return serviceServerController != null && serviceServerController.isStarted();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void startForeground() {
        Map<Device, IBluetoothLeConnection> map;
        if (!this.mBackgroundMode || (map = this.mConnections) == null) {
            return;
        }
        int size = map.size();
        boolean isAdvertising = this.mBinder.isAdvertising();
        int i4 = size > 0 ? isAdvertising ? R.plurals.notif_message_plus_advertising : R.plurals.notif_message : isAdvertising ? R.string.notif_message_advertising_only : R.string.notif_message_nothing;
        Intent intent = new Intent(this, DeviceListActivity.class);
        intent.addFlags(268435456);
        int i5 = 134217728;
        int i6 = Build.VERSION.SDK_INT;
        if (i6 >= 23) {
            i5 = 201326592;
        }
        j.d j4 = new j.d(this, NotificationHelper.CHANNEL_FOREGROUND_SERVICE).l(getString(R.string.notif_title)).h(getResources().getColor(R.color.nordicBlue)).u(R.drawable.ic_stat_notify_nrf_connect).r(-2).g(ServiceContract.Service.SERVICE_CONTENT_DIRECTORY).p(true).q(true).t(false).j(PendingIntent.getActivity(this, 0, intent, i5));
        j4.b(new j.a.C0015a(R.drawable.ic_action_disable_server, getString(R.string.notif_action_shutdown), PendingIntent.getBroadcast(this, 22, new Intent(ACTION_STOP_NRF_CONNECT), i5)).a());
        String quantityString = size > 0 ? getResources().getQuantityString(i4, size, Integer.valueOf(size)) : getString(i4);
        if (i6 >= 24) {
            j4.w(quantityString);
            j4.k(getString(R.string.notif_text));
        } else {
            j4.k(quantityString);
            j4.i(getString(R.string.notif_text));
        }
        startForeground(21, j4.c());
        if (isServerStarted()) {
            return;
        }
        NotificationHelper.registerNewConnectionListener(this, this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopForeground() {
        stopForeground(true);
        NotificationHelper.unregisterNewConnectionListener(this, this);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void stopServer() {
        if (isServerStarted()) {
            this.mServerController.close();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateForeground() {
        if (!this.mFinishing) {
            startForeground();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateServer() {
        long j4 = PreferenceManager.getDefaultSharedPreferences(this).getLong(ServerFragment.PREFS_SERVER_CONFIGURATION, 0L);
        if (j4 <= 0) {
            if (isServerStarted()) {
                Log.d(TAG, "Stopping server...");
                for (IBluetoothLeConnection iBluetoothLeConnection : this.mConnections.values()) {
                    this.mServerController.cancelConnection(iBluetoothLeConnection.getDevice().getBluetoothDevice());
                }
                stopServer();
                i0.a.b(this).d(new Intent(ServiceConstants.ACTION_GATT_SERVER_CONFIGURATION_CHANGED));
                return;
            }
            Log.d(TAG, "Server not enabled");
            return;
        }
        Log.d(TAG, "Starting server...");
        String serverConfiguration = this.mDatabase.getServerConfiguration(j4);
        if (serverConfiguration == null) {
            Log.w(TAG, "Server configuration not found (id = " + j4 + ")");
            Log.d(TAG, "Stopping server...");
            stopServer();
            return;
        }
        try {
            ServerConfiguration serverConfiguration2 = (ServerConfiguration) new Persister(new Format(new HyphenStyle())).read((Class<? extends Object>) ServerConfiguration.class, serverConfiguration);
            if (this.mServerController == null) {
                this.mServerController = new ServiceServerController(this, this.mDatabase, this.mBinder, new ServiceServerController.ControllerCallback() { // from class: no.nordicsemi.android.mcp.ble.BluetoothLeService.5
                    @Override // no.nordicsemi.android.mcp.ble.server.ServiceServerController.ControllerCallback
                    public void onDatabaseChanged() {
                        for (IBluetoothLeConnection iBluetoothLeConnection2 : BluetoothLeService.this.mConnections.values()) {
                            ((BluetoothLeConnection) iBluetoothLeConnection2).setServerController(BluetoothLeService.this.mServerController);
                            BluetoothLeService.this.mServerController.updateConnection(iBluetoothLeConnection2.getDevice().getBluetoothDevice(), iBluetoothLeConnection2);
                        }
                        i0.a.b(BluetoothLeService.this).d(new Intent(ServiceConstants.ACTION_GATT_SERVER_CONFIGURATION_CHANGED));
                    }

                    @Override // no.nordicsemi.android.mcp.ble.server.ServiceServerController.ControllerCallback
                    public void onError(int i4) {
                        Log.w(BluetoothLeService.TAG, "Starting server failed, error: " + i4);
                        BluetoothLeService.this.stopServer();
                        BluetoothLeService.this.mServerController = null;
                        for (IBluetoothLeConnection iBluetoothLeConnection2 : BluetoothLeService.this.mConnections.values()) {
                            ((BluetoothLeConnection) iBluetoothLeConnection2).setServerController(null);
                        }
                        i0.a.b(BluetoothLeService.this).d(new Intent(BluetoothLeService.ACTION_SERVER_FAILED_TO_START));
                    }
                });
            }
            this.mServerController.setServerConfiguration(serverConfiguration2);
            for (IBluetoothLeConnection iBluetoothLeConnection2 : this.mConnections.values()) {
                this.mServerController.addExistingConnection(iBluetoothLeConnection2.getDevice().getBluetoothDevice(), iBluetoothLeConnection2);
            }
            this.mServerController.start(this);
        } catch (Exception e4) {
            Log.e(TAG, "Starting server failed", e4);
            stopServer();
            this.mServerController = null;
            Iterator<IBluetoothLeConnection> it = this.mConnections.values().iterator();
            while (it.hasNext()) {
                ((BluetoothLeConnection) it.next()).setServerController(null);
            }
            i0.a.b(this).d(new Intent(ACTION_SERVER_FAILED_TO_START));
        }
    }

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return this.mBinder;
    }

    @Override // no.nordicsemi.android.mcp.ble.BluetoothLeConnection.OnConnectionClosedListener
    public void onConnectionClosed(IBluetoothLeConnection iBluetoothLeConnection) {
        Map<Device, IBluetoothLeConnection> map = this.mConnections;
        if (map == null) {
            return;
        }
        map.remove(iBluetoothLeConnection.getDevice());
        ServiceServerController serviceServerController = this.mServerController;
        if (serviceServerController != null) {
            serviceServerController.removeConnection(iBluetoothLeConnection.getDevice().getBluetoothDevice());
        }
        updateForeground();
        if (!this.mFinishing || !this.mConnections.isEmpty()) {
            return;
        }
        stopServer();
        stopSelf();
    }

    @Override // android.app.Service
    public void onCreate() {
        super.onCreate();
        this.mHandler = new Handler();
        this.mConnections = new ConcurrentHashMap();
        this.mDevices = new ArrayList();
        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        this.mDatabase = databaseHelper;
        int i4 = Build.VERSION.SDK_INT;
        if (i4 >= 26) {
            this.mAdvertiserImpl = new OreoBluetoothLeAdvertiserImpl(this, databaseHelper, this.mHandler, new IBluetoothLeAdvertiser.AdvertisingStartedObserver() { // from class: no.nordicsemi.android.mcp.ble.j
                @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser.AdvertisingStartedObserver
                public final void onAdvertisingStarted() {
                    BluetoothLeService.this.updateForeground();
                }
            }, new IBluetoothLeAdvertiser.AdvertisingStoppedObserver() { // from class: no.nordicsemi.android.mcp.ble.k
                @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser.AdvertisingStoppedObserver
                public final void onAdvertisingStopped() {
                    BluetoothLeService.this.updateForeground();
                }
            });
        } else if (i4 >= 21) {
            this.mAdvertiserImpl = new LollipopBluetoothLeAdvertiserImpl(this, databaseHelper, this.mHandler, new IBluetoothLeAdvertiser.AdvertisingStartedObserver() { // from class: no.nordicsemi.android.mcp.ble.j
                @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser.AdvertisingStartedObserver
                public final void onAdvertisingStarted() {
                    BluetoothLeService.this.updateForeground();
                }
            }, new IBluetoothLeAdvertiser.AdvertisingStoppedObserver() { // from class: no.nordicsemi.android.mcp.ble.k
                @Override // no.nordicsemi.android.mcp.ble.IBluetoothLeAdvertiser.AdvertisingStoppedObserver
                public final void onAdvertisingStopped() {
                    BluetoothLeService.this.updateForeground();
                }
            });
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECT_REQUESTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        registerReceiver(this.mConnectionStateBroadcastReceiver, intentFilter);
        registerReceiver(this.mBluetoothStateBroadcastReceiver, new IntentFilter("android.bluetooth.adapter.action.STATE_CHANGED"));
        i0.a.b(this).c(this.mRefreshServerConfigurationBroadcastReceiver, new IntentFilter(ACTION_SERVER_CONFIGURATION_CHANGED));
        i0.a.b(this).c(this.mBackgroundModeBroadcastReceiver, new IntentFilter(ACTION_BACKGROUND_MODE_CHANGED));
    }

    @Override // android.app.Service
    public void onDestroy() {
        stopForeground();
        super.onDestroy();
        unregisterReceiver(this.mConnectionStateBroadcastReceiver);
        unregisterReceiver(this.mBluetoothStateBroadcastReceiver);
        i0.a.b(this).e(this.mRefreshServerConfigurationBroadcastReceiver);
        i0.a.b(this).e(this.mBackgroundModeBroadcastReceiver);
        IBluetoothLeAdvertiser iBluetoothLeAdvertiser = this.mAdvertiserImpl;
        if (iBluetoothLeAdvertiser != null) {
            iBluetoothLeAdvertiser.stopAllAdvertisements();
        }
        this.mAdvertiserImpl = null;
        this.mServerController = null;
        this.mDatabase = null;
        this.mConnections = null;
    }

    @Override // no.nordicsemi.android.mcp.util.NotificationHelper.NewConnectionListener
    public void onNewConnection(BluetoothDevice bluetoothDevice) {
        this.mBinder.createConnection(new Device(bluetoothDevice), false).saveLogAndFlash(0, "[Broadcast] Action received: android.bluetooth.device.action.ACL_CONNECTED");
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i4, int i5) {
        if (isBleEnabled() && isServerEnabled() && !isServerStarted()) {
            updateServer();
        }
        if (!this.mConnections.isEmpty() && this.mDevices.isEmpty()) {
            this.mDevices.addAll(this.mConnections.keySet());
        }
        this.mBackgroundMode = intent.getBooleanExtra(EXTRA_BACKGROUND_MODE, false);
        updateForeground();
        return 2;
    }

    @Override // android.app.Service
    public void onTaskRemoved(Intent intent) {
        super.onTaskRemoved(intent);
        this.mBinder.disconnectAndCloseAll(true);
        if (Build.VERSION.SDK_INT >= 21) {
            this.mBinder.stopAllAdvertisements();
        }
        stopServer();
        stopSelf();
    }
}