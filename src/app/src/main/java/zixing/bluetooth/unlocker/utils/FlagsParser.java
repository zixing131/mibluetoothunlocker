package zixing.bluetooth.unlocker.utils;

/* loaded from: classes.dex */
public class FlagsParser {
    private static final byte BR_EDR_NOT_SUPPORTED = 4;
    private static final byte LE_GENERAL_DISCOVERABLE_MODE = 2;
    private static final byte LE_LIMITED_DISCOVERABLE_MODE = 1;
    private static final byte SIMULTANEOUS_LE_AND_BR_EDR_CAPABLE_CONTROLLER = 8;
    private static final byte SIMULTANEOUS_LE_AND_BR_EDR_CAPABLE_HOST = 16;
    public static final byte UNKNOWN_FLAGS = -1;

    public static void parse(DataUnion dataUnion, byte b) {
        if (b == -1) {
            dataUnion.addData("Flags", "GeneralDiscoverable, [Device specific]");
            return;
        }
        StringBuilder sb = new StringBuilder();
        if ((b & LE_LIMITED_DISCOVERABLE_MODE) > 0) {
            sb.append("LimitedDiscoverable, ");
        }
        if ((b & LE_GENERAL_DISCOVERABLE_MODE) > 0) {
            sb.append("GeneralDiscoverable, ");
        }
        if ((b & BR_EDR_NOT_SUPPORTED) > 0) {
            sb.append("BrEdrNotSupported, ");
        }
        if ((b & SIMULTANEOUS_LE_AND_BR_EDR_CAPABLE_CONTROLLER) > 0) {
            sb.append("LeAndBrErdCapable (Controller), ");
        }
        if ((b & SIMULTANEOUS_LE_AND_BR_EDR_CAPABLE_HOST) > 0) {
            sb.append("LeAndBrErdCapable (Host), ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 2);
            dataUnion.addData("Flags", sb.toString());
            return;
        }
        dataUnion.addData("Flags", "No flags");
    }
}