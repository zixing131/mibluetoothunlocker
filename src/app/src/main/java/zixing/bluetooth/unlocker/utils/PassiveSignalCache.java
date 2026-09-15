package zixing.bluetooth.unlocker.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Advertising RSSI is a scheduling hint, never authorization to unlock. Single-thread owned. */
public final class PassiveSignalCache {
    private static final long WINDOW_NS = 1_500_000_000L;
    private static final long FRESH_NS = 500_000_000L;
    private static final long MIN_SPAN_NS = 100_000_000L;
    private static final int MAX_SAMPLES = 5;
    private final Set<String> targets = new HashSet<>();
    private final Map<String, ArrayDeque<Sample>> samples = new HashMap<>();
    private static final class Sample {
        final int rssi;
        final long timestamp;
        Sample(int rssi, long timestamp) { this.rssi = rssi; this.timestamp = timestamp; }
    }

    public void setTargets(List<String> addresses) {
        targets.clear();
        for (String address : addresses) {
            String normalized = TrustedDevice.normalizeAddress(address);
            if (!normalized.isEmpty()) targets.add(normalized);
        }
        samples.clear();
    }

    public void clear() { samples.clear(); }
    public void invalidate(String address) { samples.remove(TrustedDevice.normalizeAddress(address)); }

    public void record(String address, int rssi, long observedNs, long nowNs) {
        String normalized = targets.contains(address) ? address : TrustedDevice.normalizeAddress(address);
        if (!targets.contains(normalized) || !TrustedDevice.validRssi(rssi)
                || observedNs <= 0 || observedNs > nowNs || nowNs - observedNs > FRESH_NS) return;
        ArrayDeque<Sample> history = samples.computeIfAbsent(normalized, key -> new ArrayDeque<>());
        if (!history.isEmpty() && observedNs <= history.getLast().timestamp) return;
        history.addLast(new Sample(rssi, observedNs));
        while (history.size() > MAX_SAMPLES || nowNs - history.getFirst().timestamp > WINDOW_NS) history.removeFirst();
    }

    public Integer median(String address, long nowNs) {
        ArrayDeque<Sample> history = samples.get(TrustedDevice.normalizeAddress(address));
        if (history == null || history.isEmpty()) return null;
        while (!history.isEmpty() && nowNs - history.getFirst().timestamp > WINDOW_NS) history.removeFirst();
        if (history.size() < 3 || history.getLast().timestamp > nowNs
                || nowNs - history.getLast().timestamp > FRESH_NS
                || history.getLast().timestamp - history.getFirst().timestamp < MIN_SPAN_NS) return null;
        int[] values = new int[history.size()];
        int i = 0;
        for (Sample sample : history) values[i++] = sample.rssi;
        Arrays.sort(values);
        return values[values.length / 2];
    }

    /** Preserve every candidate, including silent/out-of-range advertisers and original tie order. */
    public List<String> prioritize(List<String> addresses, long nowNs) {
        List<String> result = new ArrayList<>(addresses);
        Map<String, Integer> ranks = new HashMap<>();
        for (String address : result) {
            Integer value = median(address, nowNs);
            ranks.put(address, value == null ? Integer.MIN_VALUE : value);
        }
        result.sort((a, b) -> Integer.compare(ranks.get(b), ranks.get(a)));
        return result;
    }
}
