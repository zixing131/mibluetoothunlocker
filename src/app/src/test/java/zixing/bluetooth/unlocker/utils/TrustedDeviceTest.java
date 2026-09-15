package zixing.bluetooth.unlocker.utils;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class TrustedDeviceTest {
    private static final String A = "AA:BB:CC:DD:EE:01";
    private static final String B = "AA:BB:CC:DD:EE:02";
    @Test public void migratesLegacyMacWithoutLosingAddress() {
        List<TrustedDevice> result = TrustedDevice.decode(null, " aa:bb:cc:dd:ee:01 ");
        assertEquals(1, result.size());
        assertEquals(A, result.get(0).address);
        assertEquals(TrustedDevice.PROXIMITY, result.get(0).mode);
    }
    @Test public void explicitEmptyListDoesNotResurrectLegacyDevice() {
        assertTrue(TrustedDevice.decode(TrustedDevice.encode(Collections.emptyList()), A).isEmpty());
        assertTrue(TrustedDevice.decode(null, "basemode").isEmpty());
    }
    @Test public void roundTripsDifferentModes() {
        List<TrustedDevice> result = TrustedDevice.decode(TrustedDevice.encode(Arrays.asList(
                new TrustedDevice(A, TrustedDevice.PROXIMITY), new TrustedDevice(B, TrustedDevice.CONNECTED))), "");
        assertEquals(2, result.size());
        assertEquals(TrustedDevice.CONNECTED, result.get(1).mode);
        assertEquals(B, result.get(1).address);
    }
    @Test public void removalNeverReactivatesSystemAddress() {
        assertTrue(TrustedDevice.resolve("v1\n", "basemode", A).isEmpty());
        assertTrue(TrustedDevice.resolve("v1\n", "", A).isEmpty());
        assertEquals(A, TrustedDevice.resolve(null, "basemode", A).get(0).address);
        assertTrue(TrustedDevice.resolve("broken", "basemode", A).isEmpty());
    }
    @Test public void malformedAndUnknownConfigFailsClosed() {
        assertTrue(TrustedDevice.decode("broken", A).isEmpty());
        assertTrue(TrustedDevice.decode("v2\n" + A + ",connected", A).isEmpty());
        assertTrue(TrustedDevice.decode("v1\n" + A + ",unknown\ninvalid,rssi\n", A).isEmpty());
    }
    @Test public void duplicateAddressesCannotCreateExtraConnections() {
        assertEquals(1, TrustedDevice.decode("v1\n" + A + ",rssi\n" + A.toLowerCase() + ",connected\n", "").size());
    }
    @Test public void missingRssiIsNeverProximityEvidence() {
        for (int value : new int[]{Integer.MIN_VALUE, -128, 0, 3, 127}) assertFalse(TrustedDevice.validRssi(value));
        assertTrue(TrustedDevice.validRssi(-127));
        assertTrue(TrustedDevice.validRssi(-1));
    }
}
