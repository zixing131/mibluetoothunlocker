package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

public class BluetoothServiceHelper {
    public void NewService()
    {
        private final ServiceConnection mServiceConnection = new ServiceConnection() { // from class: no.nordicsemi.android.mcp.connection.DeviceDetailsFragment2.1
            /* JADX WARN: Code restructure failed: missing block: B:57:0x01f2, code lost:
                if (r9 != 5) goto L51;
             */
            /* JADX WARN: Removed duplicated region for block: B:65:0x0228  */
            /* JADX WARN: Removed duplicated region for block: B:68:0x023f  */
            @Override // android.content.ServiceConnection
            @SuppressLint({"RestrictedApi"})
        /*
            Code decompiled incorrectly, please refer to instructions dump.
        */
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                IBluetoothLeService iBluetoothLeService = (IBluetoothLeService) iBinder;
                IBluetoothLeConnection connection = iBluetoothLeService.getConnection(DeviceDetailsFragment2.this.mDevice);
                if (connection == null) {
                    Log.w(DeviceDetailsFragment2.TAG, "Fragment bound before activity! Creating connection in the fragment.");
                    connection = iBluetoothLeService.createConnection(DeviceDetailsFragment2.this.mDevice, true);
                }
                DeviceDetailsFragment2.this.mConnection = connection;
                if (!connection.isConnectionAttemptDone()) {
                    connection.setAutoConnect(DeviceDetailsFragment2.this.mAutoConnect);
                    connection.connect();
                } else {
                    DeviceDetailsFragment2.this.mAutoConnect = connection.hasAutoConnect();
                }
                if (DeviceDetailsFragment2.this.mLogPane != null) {
                    if (Boolean.TRUE.equals((Boolean) connection.get(DeviceDetailsFragment2.KEY_LOG_OPEN))) {
                        DeviceDetailsFragment2.this.mLogPane.openPane(false);
                    } else {
                        DeviceDetailsFragment2.this.mLogPane.closePane(false);
                    }
                }
                Integer num = (Integer) connection.get(DeviceDetailsFragment2.KEY_LOG_SCROLL_POSITION);
                if (num != null) {
                    DeviceDetailsFragment2.this.mLogScrollPosition = num.intValue();
                }
                int i = PreferenceManager.getDefaultSharedPreferences(DeviceDetailsFragment2.this.requireContext()).getInt(DeviceDetailsFragment2.PREFS_MIN_LEVEL, 2);
                DeviceDetailsFragment2.this.mLogLevel.setText((CharSequence) DeviceDetailsFragment2.this.mLogLevel.getAdapter().getItem(i), false);
                DeviceDetailsFragment2.this.logLevelSelected(i);
                DeviceDetailsFragment2 deviceDetailsFragment2 = DeviceDetailsFragment2.this;
                DeviceDetailsFragment2 deviceDetailsFragment22 = DeviceDetailsFragment2.this;
                ServicesAdapter2 servicesAdapter2 = deviceDetailsFragment2.mAdapter = new ServicesAdapter2(deviceDetailsFragment22.mDatabaseHelper, deviceDetailsFragment22, connection, true);
                servicesAdapter2.setConnected(connection.isConnected());
                servicesAdapter2.setEnabled(!DeviceDetailsFragment2.this.isActionModeEnabled());
                DeviceDetailsFragment2 deviceDetailsFragment23 = DeviceDetailsFragment2.this;
                DeviceDetailsFragment2 deviceDetailsFragment24 = DeviceDetailsFragment2.this;
                ServicesAdapter2 servicesAdapter22 = deviceDetailsFragment23.mServerAdapter = new ServicesAdapter2(deviceDetailsFragment24.mDatabaseHelper, deviceDetailsFragment24, connection, false);
                servicesAdapter22.setConnected(connection.isConnectedToServer());
                servicesAdapter22.setEnabled(!DeviceDetailsFragment2.this.isActionModeEnabled());
                Integer num2 = (Integer) connection.get(DeviceDetailsFragment2.KEY_ADAPTER_TYPE);
                DeviceDetailsFragment2.this.mModeTabLayout.addTab(DeviceDetailsFragment2.this.mModeTabLayout.newTab().m4363t(C2955R.string.tab_client).m4364s(0), num2 == null || num2.intValue() == 0);
                DeviceDetailsFragment2.this.mModeTabLayout.addTab(DeviceDetailsFragment2.this.mModeTabLayout.newTab().m4363t(C2955R.string.tab_server).m4364s(1), num2 != null && num2.intValue() == 1);
                if (connection.containsKey(DeviceDetailsFragment2.KEY_ADAPTER_STATE)) {
                    DeviceDetailsFragment2.this.mServicesView.getLayoutManager().mo6547d1((Parcelable) connection.get(DeviceDetailsFragment2.KEY_ADAPTER_STATE));
                }
                DeviceDetailsFragment2.this.mActionMore.setVisibility((!connection.isConnected() || connection.isDfuInProgress() || DeviceDetailsFragment2.this.isActionModeEnabled()) ? 4 : 0);
                DeviceDetailsFragment2.this.mOperationInProgress.setVisibility(connection.isOperationInProgress() ? 0 : 4);
                if (connection.isDfuInProgress()) {
                    DeviceDetailsFragment2.this.setDfuProgressVisible(true);
                    DeviceDetailsFragment2.this.mUploadSpeedSeries.addAll(connection.getDfuSpeedValues());
                    DeviceDetailsFragment2.this.mUploadAvgSpeedSeries.addAll(connection.getDfuAvgSpeedValues());
                    DeviceDetailsFragment2.this.mProgressGraph.setRangeBoundaries(0, Integer.valueOf(DeviceDetailsFragment2.this.normalizeDfuMaxValue(connection.getDfuMaxAvgSpeed())), BoundaryMode.FIXED);
                }
                int connectionState = connection.getConnectionState();
                if (connectionState != 0) {
                    if (connectionState == 1) {
                        DeviceDetailsFragment2.this.updateConnectionState(C2955R.string.status_connecting, new Object[0]);
                    } else if (connectionState == 2) {
                        DeviceDetailsFragment2.this.updateConnectionState(C2955R.string.status_connected, new Object[0]);
                    } else if (connectionState == 3 || connectionState == 4) {
                        DeviceDetailsFragment2.this.updateConnectionState(C2955R.string.status_disconnecting, new Object[0]);
                    }
                    if (DeviceDetailsFragment2.this.mOnServiceConnectedCallback != null) {
                        DeviceDetailsFragment2.this.mOnServiceConnectedCallback.onServiceConnected(connection);
                        DeviceDetailsFragment2.this.mOnServiceConnectedCallback = null;
                    }
                    if (DeviceDetailsFragment2.this.actionModeEnabled) {
                        DeviceDetailsFragment2.this.onViewSelected();
                    }
                    DeviceDetailsFragment2.this.onServiceConnected(connection);
                    DeviceDetailsFragment2.this.requireActivity().invalidateOptionsMenu();
                }
                DeviceDetailsFragment2.this.updateConnectionState(C2955R.string.status_disconnected, new Object[0]);
                if (DeviceDetailsFragment2.this.mOnServiceConnectedCallback != null) {
                }
                if (DeviceDetailsFragment2.this.actionModeEnabled) {
                }
                DeviceDetailsFragment2.this.onServiceConnected(connection);
                DeviceDetailsFragment2.this.requireActivity().invalidateOptionsMenu();
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName componentName) {
                DeviceDetailsFragment2 deviceDetailsFragment2 = DeviceDetailsFragment2.this;
                deviceDetailsFragment2.mConnection = null;
                if (!deviceDetailsFragment2.requireActivity().isFinishing()) {
                    DeviceDetailsFragment2.this.requireActivity().finish();
                }
            }
        };

    }
}
