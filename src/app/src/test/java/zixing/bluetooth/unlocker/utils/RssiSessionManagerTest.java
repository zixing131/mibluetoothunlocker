package zixing.bluetooth.unlocker.utils;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class RssiSessionManagerTest {
    private static final String A = "AA:BB:CC:DD:EE:01";
    private static final String B = "AA:BB:CC:DD:EE:02";
    private static final String C = "AA:BB:CC:DD:EE:03";
    private static final String D = "AA:BB:CC:DD:EE:04";
    private static class Clock implements RssiSessionManager.Scheduler {
        long now, sequence;
        static class Job { Runnable task; long at, order; Job(Runnable t, long a, long o) { task=t; at=a; order=o; } }
        List<Job> queue = new ArrayList<>();
        public void post(Runnable task, long delay) { queue.add(new Job(task, now + delay, sequence++)); }
        public void cancel(Runnable task) { queue.removeIf(job -> job.task == task); }
        public long nowMs() { return now; }
        void advance(long time) {
            long end = now + time;
            int guard = 1000;
            while (--guard > 0) {
                Job next = queue.stream().min(Comparator.comparingLong((Job j) -> j.at).thenComparingLong(j -> j.order)).orElse(null);
                if (next == null || next.at > end) break;
                now = next.at; queue.remove(next); next.task.run();
            }
            if (guard == 0) fail("Unbounded retries");
            now = end;
        }
    }
    private static class Connection implements RssiSessionManager.Link {
        final String address;
        final RssiSessionManager.Events events;
        int reads, closes;
        boolean acceptRead = true;
        Connection(String a, RssiSessionManager.Events e) { address=a; events=e; }
        public boolean readRssi() { reads++; return acceptRead; }
        public void close() { closes++; }
    }
    private static class Fixture implements RssiSessionManager.Listener {
        Clock clock = new Clock();
        List<Connection> connections = new ArrayList<>();
        List<String> matches = new ArrayList<>();
        int samples;
        RssiSessionManager manager = new RssiSessionManager(clock, (a, e) -> {
            Connection connection = new Connection(a, e); connections.add(connection); return connection;
        });
        public void measured(String address, int rssi) { samples++; }
        public void finished(String matched) { matches.add(matched); }
        void start(String... addresses) { manager.check(Arrays.asList(addresses), -50, this); }
        void sample(int i, int value, boolean ok) {
            Connection c = connections.get(i); c.events.connected(); clock.advance(0);
            c.events.rssi(value, ok); clock.advance(0);
        }
    }
    @Test public void waitsForConnectionBeforeReading() {
        Fixture f = new Fixture(); f.start(A);
        f.clock.advance(1000); assertEquals(0, f.connections.get(0).reads);
        f.connections.get(0).events.connected(); f.clock.advance(0);
        assertEquals(1, f.connections.get(0).reads);
    }
    @Test public void secondDeviceCanUnlockWhenFirstIsOffline() {
        Fixture f = new Fixture(); f.start(A, B); f.sample(1, -40, true);
        assertEquals(Collections.singletonList(B), f.matches);
        assertFalse(f.manager.isRunning());
        for (Connection c : f.connections) assertEquals(1, c.closes);
        assertTrue(f.clock.queue.isEmpty());
    }
    @Test public void weakDeviceDoesNotPreventAnotherDeviceFromUnlocking() {
        Fixture f = new Fixture(); f.start(A, B); f.sample(0, -80, true);
        assertTrue(f.matches.isEmpty()); f.sample(1, -50, true);
        assertEquals(Collections.singletonList(B), f.matches);
    }
    @Test public void invalidOrFailedReadCannotUnlock() {
        for (int value : new int[]{0, 3, 127, -128}) {
            Fixture f = new Fixture(); f.start(A); f.sample(0, value, true); f.clock.advance(20000);
            assertEquals(0, f.samples); assertEquals(Collections.singletonList(null), f.matches);
        }
        Fixture f = new Fixture(); f.start(A); f.sample(0, -20, false); f.clock.advance(20000);
        assertEquals(0, f.samples); assertEquals(Collections.singletonList(null), f.matches);
    }
    @Test public void timeoutRetriesOnceWithBackoffThenClosesEverything() {
        Fixture f = new Fixture(); f.start(A); f.clock.advance(3500);
        assertEquals(1, f.connections.size()); assertEquals(1, f.connections.get(0).closes);
        f.clock.advance(499); assertEquals(1, f.connections.size());
        f.clock.advance(1); assertEquals(2, f.connections.size());
        f.clock.advance(20000); assertEquals(2, f.connections.size());
        for (Connection c : f.connections) assertEquals(1, c.closes);
        assertEquals(Collections.singletonList(null), f.matches);
    }
    @Test public void staleSuccessCannotUnlockAfterCancelOrReplacement() {
        Fixture f = new Fixture(); f.start(A); Connection old = f.connections.get(0);
        old.events.connected(); f.clock.advance(0);
        f.manager.cancel(); f.start(B); old.events.rssi(-20, true); f.clock.advance(0);
        assertTrue(f.matches.isEmpty()); assertEquals(0, f.samples);
        f.sample(1, -40, true); assertEquals(Collections.singletonList(B), f.matches);
    }
    @Test public void disconnectedClientIsClosedAndItsCallbacksIgnored() {
        Fixture f = new Fixture(); f.start(A); Connection old = f.connections.get(0);
        old.events.connected(); f.clock.advance(0);
        old.events.failed(); f.clock.advance(0); old.events.rssi(-20, true); f.clock.advance(0);
        assertEquals(1, old.closes); assertTrue(f.matches.isEmpty());
        f.clock.advance(500); f.sample(1, -40, true); assertEquals(Collections.singletonList(A), f.matches);
    }
    @Test public void concurrencyIsBoundedAndQueueAdvances() {
        Fixture f = new Fixture(); f.start(A, B, C, D);
        assertEquals(3, f.connections.size()); f.sample(0, -90, true);
        assertEquals(4, f.connections.size()); assertEquals(D, f.connections.get(3).address);
        f.sample(3, -40, true); assertEquals(Collections.singletonList(D), f.matches);
    }
    @Test public void duplicateCallbacksAndAddressesDoNotReadOrUnlockTwice() {
        Fixture f = new Fixture(); f.start(A, A); assertEquals(1, f.connections.size());
        Connection c = f.connections.get(0); c.events.connected(); c.events.connected(); f.clock.advance(0);
        assertEquals(1, c.reads); c.events.rssi(-30, true); c.events.rssi(-30, true); f.clock.advance(0);
        assertEquals(1, f.samples); assertEquals(1, f.matches.size());
    }
    @Test public void rejectedReadIsRetriedWithoutBusyLoop() {
        Fixture f = new Fixture(); f.start(A); f.connections.get(0).acceptRead = false;
        f.connections.get(0).events.connected(); f.clock.advance(0);
        assertEquals(1, f.connections.get(0).closes); assertEquals(1, f.connections.size());
        f.clock.advance(500); f.sample(1, -45, true); assertEquals(Collections.singletonList(A), f.matches);
    }
    @Test public void cancellationDuringBackoffPreventsReconnect() {
        Fixture f = new Fixture(); f.start(A); f.connections.get(0).events.failed(); f.clock.advance(0);
        f.manager.cancel(); f.clock.advance(20000);
        assertEquals(1, f.connections.size()); assertTrue(f.matches.isEmpty());
    }
    @Test public void emptyOrInvalidListFinishesWithoutConnecting() {
        Fixture f = new Fixture(); f.start("invalid", "");
        assertTrue(f.connections.isEmpty()); assertEquals(Collections.singletonList(null), f.matches);
        assertTrue(f.clock.queue.isEmpty());
    }
    @Test public void wholeCheckHasDeadlineEvenWithManyDevices() {
        Fixture f = new Fixture(); f.start(A, B, C, D, "AA:BB:CC:DD:EE:05", "AA:BB:CC:DD:EE:06");
        f.clock.advance(12000); assertFalse(f.manager.isRunning());
        for (Connection c : f.connections) assertEquals(1, c.closes);
        assertEquals(Collections.singletonList(null), f.matches);
    }
    @Test public void passivePriorityPlusFastConfirmationAvoidsOtherConnections() {
        Fixture f = new Fixture();
        f.manager.checkForUnlock(Arrays.asList(B,A,C),-50,f);
        assertEquals(1,f.connections.size());
        f.sample(0,-40,true);
        assertTrue(f.matches.isEmpty());
        f.clock.advance(150);
        assertEquals(2,f.connections.get(0).reads);
        f.connections.get(0).events.rssi(-42,true); f.clock.advance(0);
        assertEquals(Collections.singletonList(B),f.matches);
        f.clock.advance(1000);
        assertEquals(1,f.connections.size());
    }
    @Test public void oneRssiSpikeCannotUnlock() {
        Fixture f = new Fixture(); f.manager.checkForUnlock(Arrays.asList(A),-50,f);
        f.sample(0,-30,true); f.clock.advance(150);
        f.connections.get(0).events.rssi(-80,true); f.clock.advance(0);
        assertEquals(Collections.singletonList(null),f.matches);
    }
    @Test public void anOfflinePreferredDeviceDoesNotBlockTheNextOne() {
        Fixture f = new Fixture(); f.manager.checkForUnlock(Arrays.asList(A,B,C),-50,f);
        f.clock.advance(349); assertEquals(1,f.connections.size());
        f.clock.advance(1); assertEquals(2,f.connections.size());
        f.sample(1,-40,true); f.clock.advance(150);
        f.connections.get(1).events.rssi(-40,true); f.clock.advance(0);
        assertEquals(Collections.singletonList(B),f.matches);
        assertEquals(2,f.connections.size());
    }
    @Test public void duplicateFirstReadCannotCountAsTwoMeasurements() {
        Fixture f = new Fixture(); f.manager.checkForUnlock(Arrays.asList(A),-50,f);
        f.sample(0,-40,true); f.connections.get(0).events.rssi(-40,true); f.clock.advance(0);
        assertTrue(f.matches.isEmpty()); assertEquals(1,f.samples);
        f.clock.advance(150); assertEquals(2,f.connections.get(0).reads);
        f.connections.get(0).events.rssi(-40,true); f.clock.advance(0);
        assertEquals(Collections.singletonList(A),f.matches);
    }
    @Test public void cancelPreventsConfirmationReadAndHedgedConnections() {
        Fixture f = new Fixture(); f.manager.checkForUnlock(Arrays.asList(A,B,C),-50,f);
        f.sample(0,-40,true); f.manager.cancel(); f.clock.advance(20000);
        assertEquals(1,f.connections.size()); assertEquals(1,f.connections.get(0).reads);
        assertTrue(f.matches.isEmpty());
    }
    @Test public void callbackQueuedBeforeLongUiStallIsNotFreshEvidence() {
        Fixture f = new Fixture(); f.manager.checkForUnlock(Arrays.asList(A),-50,f);
        f.connections.get(0).events.connected(); f.clock.advance(0);
        f.connections.get(0).events.rssi(-40,true);
        // Dispatch the queued callback late, without altering its arrival timestamp.
        Clock.Job result = f.clock.queue.stream().filter(j -> j.at == 0).findFirst().get();
        f.clock.queue.remove(result); f.clock.now = 800; result.task.run();
        assertEquals(0,f.samples); assertTrue(f.matches.isEmpty());
        assertEquals(1,f.connections.get(0).closes);
    }

    @Test public void slowRssiResponseCannotBeTreatedAsRealtime() {
        Fixture f = new Fixture(); f.manager.checkForUnlock(Arrays.asList(A),-50,f);
        f.connections.get(0).events.connected(); f.clock.advance(0);
        f.clock.advance(800); f.connections.get(0).events.rssi(-40,true); f.clock.advance(0);
        assertEquals(0,f.samples); assertTrue(f.matches.isEmpty());
        assertEquals(1,f.connections.get(0).closes);
    }
    @Test public void strongAdvertisingHintCannotAuthorizeWeakConnectionRssi() {
        PassiveSignalCache cache = new PassiveSignalCache(); cache.setTargets(Arrays.asList(A,B));
        for (long t : new long[]{1_000_000_000L,1_100_000_000L,1_200_000_000L}) cache.record(A,-10,t,t);
        Fixture f = new Fixture();
        f.manager.checkForUnlock(cache.prioritize(Arrays.asList(B,A),1_200_000_000L),-50,f);
        assertTrue(f.matches.isEmpty()); assertEquals(A,f.connections.get(0).address);
        f.sample(0,-90,true); f.clock.advance(20000);
        assertEquals(Collections.singletonList(null),f.matches);
    }

}
