package zixing.bluetooth.unlocker.utils;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

/** Local display metadata, never used as an unlock identity. */
public final class DeviceMetadata {
    private final SharedPreferences preferences;

    public DeviceMetadata(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("device_display", Context.MODE_PRIVATE);
    }

    public String name(String address) {
        return DeviceListPolicy.firstName(preferences.getString("label_" + address, ""),
                preferences.getString("name_" + address, ""));
    }

    public String resolve(BluetoothDevice device, String observedName) {
        String address = TrustedDevice.normalizeAddress(device.getAddress());
        String alias = "", systemName = "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) alias = device.getAlias();
            systemName = device.getName();
        } catch (SecurityException ignored) { }
        String discovered = DeviceListPolicy.firstName(alias, observedName, systemName);
        if (!discovered.isEmpty() && !discovered.equals(preferences.getString("name_" + address, ""))) {
            preferences.edit().putString("name_" + address, discovered).apply();
        }
        return name(address);
    }

    public void setLabel(String address, String label) {
        preferences.edit().putString("label_" + address, DeviceListPolicy.usableName(label)).apply();
    }

    public boolean isCommon(String address, boolean configuredOrBonded) {
        return preferences.getBoolean("common_" + address, configuredOrBonded);
    }

    public void setCommon(String address, boolean common) {
        preferences.edit().putBoolean("common_" + address, common).apply();
    }
}
