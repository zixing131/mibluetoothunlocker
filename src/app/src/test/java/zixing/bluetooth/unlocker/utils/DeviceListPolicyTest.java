package zixing.bluetooth.unlocker.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class DeviceListPolicyTest {
    @Test public void managementNeverLeaksUnconfiguredDiscoveries() {
        assertFalse(DeviceListPolicy.visible(true, false, false, "Nearby headphones", false, true));
        assertTrue(DeviceListPolicy.visible(true, true, true, null, true, false));
    }
    @Test public void addEntryShowsAllUnlessUserFilters() {
        assertTrue(DeviceListPolicy.visible(false, false, false, null, false, false));
        assertTrue(DeviceListPolicy.visible(false, true, false, "Watch", false, true));
    }
    @Test public void filtersCombineAndCanHideUncommonPairedDevices() {
        assertFalse(DeviceListPolicy.visible(false, false, true, "unknown", false, true));
        assertFalse(DeviceListPolicy.visible(false, true, false, "Watch", true, false));
        assertTrue(DeviceListPolicy.visible(false, false, true, "Headphones", true, true));
    }
    @Test public void missingNamesFallBackWithoutDiscardingLastKnownName() {
        assertEquals("My watch", DeviceListPolicy.firstName(null, "unknown", "My watch"));
        assertEquals("Broadcast name", DeviceListPolicy.firstName("", "Broadcast name", "Cached name"));
        assertEquals("My label", DeviceListPolicy.firstName(" My label ", "System alias"));
        assertEquals("", DeviceListPolicy.firstName(null, "Unknown Device", "  ", "null", "未命名设备"));
    }
}
