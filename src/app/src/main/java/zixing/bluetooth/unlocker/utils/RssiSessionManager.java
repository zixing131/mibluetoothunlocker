package zixing.bluetooth.unlocker.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** All methods and events run on one scheduler. Each check owns short-lived GATT clients. */
public final class RssiSessionManager {
    public interface Scheduler {
        void post(Runnable task, long delayMs);
        void cancel(Runnable task);
        long nowMs();
    }
    public interface Link {
        boolean readRssi();
        void close();
    }
    public interface Events {
        void connected();
        void failed();
        void rssi(int value, boolean success);
    }
    public interface Transport { Link connect(String address, Events events); }
    public interface Listener {
        void measured(String address, int rssi);
        void finished(String matchingAddress);
    }

    private final Scheduler scheduler;
    private final Transport transport;
    private final ArrayDeque<String> pending = new ArrayDeque<>();
    private final Map<String, Session> active = new LinkedHashMap<>();
    private Listener listener;
    private int threshold;
    private int requiredSamples;
    private int allowedConcurrent;
    private int staggerMs;
    private final Runnable expand = this::expandConcurrency;
    private final Runnable deadline = () -> finish(null);
    private static final int MAX_CONCURRENT = 3;

    public RssiSessionManager(Scheduler scheduler, Transport transport) {
        this.scheduler = scheduler;
        this.transport = transport;
    }

    public boolean isRunning() { return listener != null; }

    public void check(List<String> addresses, int threshold, Listener listener) {
        start(addresses, threshold, listener, 1, 0);
    }

    /** Two consecutive connection measurements; hedge other devices without opening all at once. */
    public void checkForUnlock(List<String> addresses, int threshold, Listener listener) {
        start(addresses, threshold, listener, 2, 350);
    }

    private void start(List<String> addresses, int threshold, Listener listener, int samples, int stagger) {
        cancel();
        requiredSamples = samples;
        staggerMs = stagger;
        allowedConcurrent = stagger == 0 ? MAX_CONCURRENT : 1;
        this.threshold = threshold;
        this.listener = listener;
        for (String address : addresses) {
            String normalized = TrustedDevice.normalizeAddress(address);
            if (!normalized.isEmpty() && !pending.contains(normalized)) pending.add(normalized);
        }
        scheduler.post(deadline, 12000);
        pump();
        if (this.listener != null && staggerMs > 0 && !pending.isEmpty()) scheduler.post(expand, staggerMs);
    }

    public void cancel() {
        listener = null;
        scheduler.cancel(deadline);
        scheduler.cancel(expand);
        pending.clear();
        List<Session> old = new ArrayList<>(active.values());
        active.clear();
        for (Session session : old) session.close();
    }

    private void finish(String address) {
        Listener completed = listener;
        cancel();
        if (completed != null) completed.finished(address);
    }

    private void expandConcurrency() {
        if (listener == null) return;
        allowedConcurrent = Math.min(MAX_CONCURRENT, allowedConcurrent + 1);
        pump();
        if (listener != null && allowedConcurrent < MAX_CONCURRENT && !pending.isEmpty()) scheduler.post(expand, staggerMs);
    }

    private void pump() {
        while (listener != null && active.size() < allowedConcurrent && !pending.isEmpty()) {
            Session session = new Session(pending.remove());
            active.put(session.address, session);
            session.connect();
        }
        if (listener != null && active.isEmpty() && pending.isEmpty()) finish(null);
    }

    private final class Session {
        final String address;
        Link link;
        int generation;
        int attempts;
        boolean connected;
        boolean readPending;
        int confirmedSamples;
        long readStartedAt;
        final Runnable nextRead = this::read;
        final Runnable timeout = this::failed;
        final Runnable retry = this::connect;

        Session(String address) { this.address = address; }
        boolean current(int version) {
            return listener != null && active.get(address) == this && version == generation;
        }

        void connect() {
            if (active.get(address) != this) return;
            int version = ++generation;
            ++attempts;
            connected = false;
            readPending = false;
            confirmedSamples = 0;
            scheduler.post(timeout, 3500);
            try {
                link = transport.connect(address, new Events() {
                    public void connected() {
                        scheduler.post(() -> {
                            if (!current(version) || connected) return;
                            connected = true;
                            read();
                        }, 0);
                    }
                    public void failed() {
                        scheduler.post(() -> { if (current(version)) Session.this.failed(); }, 0);
                    }
                    public void rssi(int value, boolean success) {
                        long receivedAt = scheduler.nowMs();
                        scheduler.post(() -> {
                            if (!current(version) || !readPending || receivedAt < readStartedAt) return;
                            readPending = false;
                            if (scheduler.nowMs() - receivedAt > 750 || scheduler.nowMs() - readStartedAt > 750) {
                                Session.this.failed();
                                return;
                            }
                            if (!success || !TrustedDevice.validRssi(value)) {
                                Session.this.failed();
                                return;
                            }
                            listener.measured(address, value);
                            if (!current(version)) return;
                            if (value >= threshold) {
                                if (++confirmedSamples >= requiredSamples) finish(address);
                                else scheduler.post(nextRead, 150);
                            } else {
                                active.remove(address);
                                close();
                                pump();
                            }
                        }, 0);
                    }
                });
                if (link == null) failed();
            } catch (RuntimeException ex) { failed(); }
        }

        void read() {
            if (active.get(address) != this || !connected || readPending) return;
            readPending = true;
            readStartedAt = scheduler.nowMs();
            try { if (link == null || !link.readRssi()) failed(); }
            catch (RuntimeException ex) { failed(); }
        }

        void failed() {
            if (active.get(address) != this) return;
            close(); // Invalidate queued callbacks before starting a new attempt.
            if (attempts < 2) {
                scheduler.post(retry, 500);
            } else {
                active.remove(address);
                pump();
            }
        }

        void close() {
            ++generation;
            scheduler.cancel(timeout);
            scheduler.cancel(retry);
            scheduler.cancel(nextRead);
            readPending = false;
            Link old = link;
            link = null;
            if (old != null) {
                try { old.close(); } catch (RuntimeException ignored) { }
            }
        }
    }
}
