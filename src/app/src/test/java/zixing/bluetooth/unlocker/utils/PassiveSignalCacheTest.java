package zixing.bluetooth.unlocker.utils;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class PassiveSignalCacheTest {
    private static final String A = "AA:BB:CC:DD:EE:01", B = "AA:BB:CC:DD:EE:02", C = "AA:BB:CC:DD:EE:03";
    private static final long SECOND = 1_000_000_000L;
    private PassiveSignalCache cache() {
        PassiveSignalCache cache = new PassiveSignalCache();
        cache.setTargets(Arrays.asList(A, B, C));
        return cache;
    }
    private void sample(PassiveSignalCache c, String address, int rssi, long time) { c.record(address, rssi, time, time); }
    private void burst(PassiveSignalCache c, String address, int rssi) {
        sample(c, address, rssi, SECOND);
        sample(c, address, rssi, SECOND + 100_000_000L);
        sample(c, address, rssi, SECOND + 200_000_000L);
    }
    @Test public void medianSuppressesAnIsolatedAdvertisingSpike() {
        PassiveSignalCache c = cache();
        sample(c,A,-90,SECOND); sample(c,A,-10,SECOND+100_000_000L); sample(c,A,-91,SECOND+200_000_000L);
        assertEquals(Integer.valueOf(-90), c.median(A, SECOND+200_000_000L));
    }
    @Test public void staleOrBatchedResultsCannotBecomeFreshOnDelivery() {
        PassiveSignalCache c = cache();
        for (long t : new long[]{SECOND,SECOND+100_000_000L,SECOND+200_000_000L}) c.record(A,-30,t,3*SECOND);
        assertNull(c.median(A,3*SECOND));
        burst(c,A,-30); assertNull(c.median(A,SECOND+701_000_000L));
    }
    @Test public void duplicateAndOutOfOrderTimestampsDoNotFormConsensus() {
        PassiveSignalCache c = cache();
        sample(c,A,-40,SECOND); sample(c,A,-40,SECOND); sample(c,A,-40,SECOND-1);
        assertNull(c.median(A,SECOND));
    }
    @Test public void futureInvalidAndUnconfiguredResultsAreRejected() {
        PassiveSignalCache c = cache();
        for (int rssi : new int[]{0,127,3,-128}) sample(c,A,rssi,SECOND);
        c.record(A,-20,2*SECOND,SECOND);
        burst(c,"AA:BB:CC:DD:EE:99",-20);
        assertNull(c.median(A,2*SECOND)); assertNull(c.median("AA:BB:CC:DD:EE:99",2*SECOND));
    }
    @Test public void samplesNeedCountAndTimeSpan() {
        PassiveSignalCache c = cache();
        sample(c,A,-30,SECOND); sample(c,A,-30,SECOND+1); sample(c,A,-30,SECOND+2);
        assertNull(c.median(A,SECOND+2));
    }
    @Test public void rankingRetainsSilentCandidatesAndStableTies() {
        PassiveSignalCache c = cache(); burst(c,B,-45);
        assertEquals(Arrays.asList(B,A,C),c.prioritize(Arrays.asList(A,B,C),SECOND+200_000_000L));
        assertEquals(Arrays.asList(A,B,C),c.prioritize(Arrays.asList(A,B,C),3*SECOND));
    }
    @Test public void configurationAndDisconnectClearEvidence() {
        PassiveSignalCache c = cache(); burst(c,A,-40); c.invalidate(A);
        assertNull(c.median(A,SECOND+200_000_000L));
        burst(c,B,-40); c.setTargets(Arrays.asList(A));
        assertNull(c.median(B,SECOND+200_000_000L));
        burst(c,A,-40); c.clear(); assertNull(c.median(A,SECOND+200_000_000L));
    }
}
