package zixing.bluetooth.unlocker.utils;

/** Shared display rules, independent of radio state and Android UI. */
public final class DeviceListPolicy {
    private DeviceListPolicy() { }

    public static String usableName(String name) {
        if (name == null) return "";
        String value = name.trim();
        if (value.isEmpty() || value.equalsIgnoreCase("unknown") || value.equalsIgnoreCase("null")
                || value.equalsIgnoreCase("unknown device") || value.equals("未命名设备")
                || value.equals("未知设备")) return "";
        return value;
    }

    public static String firstName(String... names) {
        for (String name : names) {
            String value = usableName(name);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    public static boolean visible(boolean configuredOnly, boolean configured, boolean hideUnnamed,
                                  String name, boolean commonOnly, boolean common) {
        if (configuredOnly) return configured;
        return (!hideUnnamed || !usableName(name).isEmpty()) && (!commonOnly || common);
    }
}
