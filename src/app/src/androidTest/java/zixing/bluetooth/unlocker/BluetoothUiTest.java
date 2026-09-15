package zixing.bluetooth.unlocker;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import zixing.bluetooth.unlocker.activity.DeviceActivity;
import zixing.bluetooth.unlocker.activity.MainActivity;
import zixing.bluetooth.unlocker.utils.ConfigUtil;
import zixing.bluetooth.unlocker.utils.SPUtils;
import zixing.bluetooth.unlocker.utils.TrustedDevice;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BluetoothUiTest {
    @Test public void activitiesOpenWithoutBluetoothPermissionOrAutomaticScan() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        Activity main = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.runOnMainSync(() -> {
            assertNotNull(main.findViewById(R.id.itemdevicemain));
            assertFalse(((TextView) main.findViewById(R.id.txtAddress)).getText().toString().isEmpty());
        });
        Activity devices = instrumentation.startActivitySync(new Intent(context, DeviceActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        instrumentation.runOnMainSync(() -> {
            assertEquals(View.GONE, devices.findViewById(R.id.progress).getVisibility());
            assertNotNull(devices.findViewById(R.id.recyclerview));
            devices.recreate();
        });
        instrumentation.waitForIdleSync();
        // Finish the recreated activity using its own navigation flow.
        instrumentation.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        instrumentation.runOnMainSync(main::finish);
    }

    @Test public void deviceRemovalAndBasicModeRemainDistinctOnAndroidPreferences() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SPUtils.getInstance().init(context);
        // Use isolated preferences; never change the installed module's user configuration.
        SharedPreferences original = SPUtils.getInstance().sp;
        SharedPreferences isolated = context.getSharedPreferences("device_config_test", Context.MODE_PRIVATE);
        isolated.edit().clear().commit();
        SPUtils.getInstance().sp = isolated;
        try {
            assertNull(UnlockerApp.getService());
            assertTrue(ConfigUtil.putDevice("AA:BB:CC:DD:EE:01", TrustedDevice.PROXIMITY));
            assertTrue(ConfigUtil.putDevice("AA:BB:CC:DD:EE:02", TrustedDevice.CONNECTED));
            assertEquals(2, ConfigUtil.getDevices(0).size());
            assertTrue(ConfigUtil.removeDevice("AA:BB:CC:DD:EE:01"));
            assertEquals("AA:BB:CC:DD:EE:02", ConfigUtil.getDevices(0).get(0).address);
            assertTrue(ConfigUtil.removeDevice("AA:BB:CC:DD:EE:02"));
            assertTrue(ConfigUtil.getDevicesForCheck(0, "AA:BB:CC:DD:EE:01").isEmpty());
            assertTrue(ConfigUtil.enableBasicMode());
            assertEquals(1, ConfigUtil.getDevicesForCheck(0, "AA:BB:CC:DD:EE:01").size());
        } finally {
            SPUtils.getInstance().sp = original;
            isolated.edit().clear().commit();
        }
    }
}
