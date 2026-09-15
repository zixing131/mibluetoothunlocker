package zixing.bluetooth.unlocker.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Versioned, platform-independent trusted-device configuration. Unknown modes fail closed. */
public final class TrustedDevice {
    private static final java.util.regex.Pattern MAC_PATTERN = java.util.regex.Pattern.compile("([0-9A-F]{2}:){5}[0-9A-F]{2}");
    public static final String PROXIMITY = "rssi";
    public static final String CONNECTED = "connected";
    public final String address;
    public final String mode;

    public TrustedDevice(String address, String mode) {
        this.address = normalizeAddress(address);
        if (this.address.isEmpty() || !(PROXIMITY.equals(mode) || CONNECTED.equals(mode))) {
            throw new IllegalArgumentException("Invalid trusted device");
        }
        this.mode = mode;
    }

    public static String normalizeAddress(String value) {
        if (value == null) return "";
        String address = value.trim().toUpperCase(Locale.ROOT);
        return MAC_PATTERN.matcher(address).matches() ? address : "";
    }

    public static List<TrustedDevice> decode(String stored, String legacyMac) {
        Map<String, TrustedDevice> result = new LinkedHashMap<>();
        if (stored == null) {
            String address = normalizeAddress(legacyMac);
            if (!address.isEmpty()) result.put(address, new TrustedDevice(address, PROXIMITY));
        } else if (stored.startsWith("v1\n")) {
            for (String line : stored.substring(3).split("\n")) {
                String[] parts = line.split(",", -1);
                if (parts.length != 2) continue;
                try {
                    TrustedDevice device = new TrustedDevice(parts[0], parts[1]);
                    result.putIfAbsent(device.address, device);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return new ArrayList<>(result.values());
    }

    public static String encode(List<TrustedDevice> devices) {
        StringBuilder result = new StringBuilder("v1\n");
        Map<String, TrustedDevice> unique = new LinkedHashMap<>();
        for (TrustedDevice device : devices) unique.put(device.address, device);
        for (TrustedDevice device : unique.values()) {
            result.append(device.address).append(',').append(device.mode).append('\n');
        }
        return result.toString();
    }

    public static List<TrustedDevice> resolve(String stored, String legacyMac, String nativeAddress) {
        if ((stored == null || "base".equals(stored)) && "basemode".equals(legacyMac)) {
            return decode(null, nativeAddress);
        }
        return decode(stored, legacyMac);
    }

    public static boolean validRssi(int rssi) {
        // 0 / 127 are commonly used for missing measurements; never trust them.
        return rssi >= -127 && rssi < 0;
    }
}
