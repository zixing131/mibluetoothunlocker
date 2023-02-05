package zixing.bluetooth.unlocker.utils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

/* loaded from: classes.dex */
public class ParserUtils {
    public static final int FORMAT_SINT12 = 82;
    public static final int FORMAT_SINT16_BIG_ENDIAN = 114;
    public static final int FORMAT_SINT24 = 35;
    public static final int FORMAT_SINT32_BIG_ENDIAN = 116;
    public static final int FORMAT_SINT48 = 38;
    public static final int FORMAT_UINT12 = 66;
    public static final int FORMAT_UINT16_BIG_ENDIAN = 98;
    public static final int FORMAT_UINT24 = 19;
    public static final int FORMAT_UINT32_BIG_ENDIAN = 100;
    public static final int FORMAT_UINT48 = 22;
    private static final char[] HEX_ARRAY = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String advDataToHex(byte[] bArr, boolean z) {
        int i;
        if (bArr == null || bArr.length == 0) {
            return "";
        }
        int i2 = 0;
        int i3 = 0;
        while (i2 < bArr.length && (i = bArr[i2] & FlagsParser.UNKNOWN_FLAGS) != 0) {
            int i4 = i + 1;
            i3 += i4;
            i2 += i4;
        }
        return bytesToHex(bArr, 0, i3, z);
    }

    public static String advertisingSidToString(int i) {
        return i == 255 ? "Not present" : String.valueOf(i);
    }

    public static String advertisingTypeToString(boolean z) {
        return z ? "Legacy" : "Bluetooth 5 Advertising Extension";
    }

    public static String bondingStateToString(int i) {
        return i != 11 ? i != 12 ? "NOT BONDED" : "BONDED" : "BONDING...";
    }

    public static String bytesToAddress(byte[] bArr, int i) {
        if (bArr == null || bArr.length < i + 6) {
            return "";
        }
        char[] cArr = new char[17];
        for (int i2 = 0; i2 < 6; i2++) {
            int i3 = bArr[(i + 5) - i2] & FlagsParser.UNKNOWN_FLAGS;
            int i4 = i2 * 3;
            char[] cArr2 = HEX_ARRAY;
            cArr[i4] = cArr2[i3 >>> 4];
            cArr[i4 + 1] = cArr2[i3 & 15];
            if (i2 < 5) {
                cArr[i4 + 2] = ':';
            }
        }
        return new String(cArr);
    }

    public static String bytesToAddressBigEndian(byte[] bArr, int i) {
        if (bArr == null || bArr.length < i + 6) {
            return "";
        }
        char[] cArr = new char[17];
        for (int i2 = 0; i2 < 6; i2++) {
            int i3 = bArr[i + i2] & FlagsParser.UNKNOWN_FLAGS;
            int i4 = i2 * 3;
            char[] cArr2 = HEX_ARRAY;
            cArr[i4] = cArr2[i3 >>> 4];
            cArr[i4 + 1] = cArr2[i3 & 15];
            if (i2 < 5) {
                cArr[i4 + 2] = ':';
            }
        }
        return new String(cArr);
    }

    public static String bytesToHex(byte[] bArr, boolean z) {
        return bArr == null ? "" : bytesToHex(bArr, 0, bArr.length, z);
    }

    public static String bytesToMeshNetworkID(byte[] bArr, int i) {
        if (bArr == null || bArr.length < i + 8) {
            return "";
        }
        char[] cArr = new char[23];
        for (int i2 = 0; i2 < 8; i2++) {
            int i3 = bArr[i + i2] & FlagsParser.UNKNOWN_FLAGS;
            int i4 = i2 * 3;
            char[] cArr2 = HEX_ARRAY;
            cArr[i4] = cArr2[i3 >>> 4];
            cArr[i4 + 1] = cArr2[i3 & 15];
            if (i2 < 7) {
                cArr[i4 + 2] = ':';
            }
        }
        return new String(cArr);
    }

    public static UUID bytesToUUID(byte[] bArr, int i, int i2) {
        if (bArr == null || bArr.length < i + 16 || i2 != 16) {
            return null;
        }
        long j = 0;
        long j2 = 0;
        for (int i3 = 0; i3 < 8; i3++) {
            j2 += (255 & bArr[i + i3]) << (56 - (i3 * 8));
        }
        for (int i4 = 0; i4 < 8; i4++) {
            j += (bArr[(i + i4) + 8] & 255) << (56 - (i4 * 8));
        }
        return new UUID(j2, j);
    }

    public static String dataStatusToString(int i) {
        return i != 0 ? "Truncated" : "Complete";
    }

    public static byte[] decodeUuid(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ByteBuffer wrap = ByteBuffer.wrap(new byte[16]);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        wrap.putLong(uuid.getLeastSignificantBits());
        wrap.putLong(uuid.getMostSignificantBits());
        return wrap.array();
    }

    public static UUID decodeUuid128(byte[] bArr, int i) {
        return new UUID((decodeUuid32(bArr, i + 12) << 32) + (decodeUuid32(bArr, i + 8) & 4294967295L), (decodeUuid32(bArr, i + 4) << 32) + (decodeUuid32(bArr, i + 0) & 4294967295L));
    }

    public static int decodeUuid16(byte[] bArr, int i) {
        return (((bArr[i + 1] & FlagsParser.UNKNOWN_FLAGS) << 8) | (bArr[i] & FlagsParser.UNKNOWN_FLAGS)) & 65535;
    }

    public static int decodeUuid32(byte[] bArr, int i) {
        int i2 = bArr[i] & FlagsParser.UNKNOWN_FLAGS;
        int i3 = bArr[i + 1] & FlagsParser.UNKNOWN_FLAGS;
        return ((bArr[i + 3] & FlagsParser.UNKNOWN_FLAGS) << 24) | ((bArr[i + 2] & FlagsParser.UNKNOWN_FLAGS) << 16) | (i3 << 8) | i2;
    }

    public static String deviceTypeTyString(int i) {
        return i != 1 ? i != 2 ? i != 3 ? "UNKNOWN" : "CLASSIC and LE" : "LE only" : "CLASSIC";
    }

    public static float floatOrThrow(Float f) {
        f.getClass();
        return f.floatValue();
    }

    public static int getExponent(byte[] bArr, int i, int i2) {
        if (getTypeLen(i) + i2 <= bArr.length) {
            if (i == 50) {
                return unsignedToSigned(unsignedByteToInt(bArr[i2 + 1]) >> 4, 4);
            }
            if (i == 52) {
                return bArr[i2 + 3];
            }
            return 0;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public static int getIntValue(byte[] bArr, int i, int i2) {
        if (getTypeLen(i) + i2 <= bArr.length) {
            if (i == 66) {
                return unsignedBytesToInt((byte) (bArr[i2] & 15), bArr[i2 + 1]);
            }
            if (i == 82) {
                return unsignedToSigned(unsignedBytesToInt((byte) (bArr[i2] & 15), bArr[i2 + 1]), 12);
            }
            if (i == 98) {
                return unsignedBytesToInt(bArr[i2 + 1], bArr[i2]);
            }
            if (i != 100) {
                switch (i) {
                    case 17:
                        return unsignedByteToInt(bArr[i2]);
                    case 18:
                        return unsignedBytesToInt(bArr[i2], bArr[i2 + 1]);
                    case 19:
                        return unsignedBytesToInt(bArr[i2], bArr[i2 + 1], bArr[i2 + 2]);
                    case 20:
                        return unsignedBytesToInt(bArr[i2], bArr[i2 + 1], bArr[i2 + 2], bArr[i2 + 3]);
                    default:
                        switch (i) {
                            case 33:
                                return unsignedToSigned(unsignedByteToInt(bArr[i2]), 8);
                            case 34:
                                return unsignedToSigned(unsignedBytesToInt(bArr[i2], bArr[i2 + 1]), 16);
                            case 35:
                                return unsignedToSigned(unsignedBytesToInt(bArr[i2], bArr[i2 + 1], bArr[i2 + 2]), 24);
                            case 36:
                                return unsignedBytesToInt(bArr[i2], bArr[i2 + 1], bArr[i2 + 2], bArr[i2 + 3]);
                            default:
                                Log.d("ParserUtils", "Format type " + i + " not supported");
                                return 0;
                        }
                }
            }
            return unsignedBytesToInt(bArr[i2 + 3], bArr[i2 + 2], bArr[i2 + 1], bArr[i2]);
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public static long getLongValue(byte[] bArr, int i, int i2) {
        if (getTypeLen(i) + i2 <= bArr.length) {
            if (i == 20) {
                return unsignedBytesToInt(bArr[i2], bArr[i2 + 1], bArr[i2 + 2], bArr[i2 + 3]);
            }
            if (i == 22) {
                return unsignedBytesToLong(bArr[i2], bArr[i2 + 1], bArr[i2 + 2], bArr[i2 + 3], bArr[i2 + 4], bArr[i2 + 5]);
            }
            if (i == 38) {
                return unsignedToSigned(unsignedBytesToLong(bArr[i2], bArr[i2 + 1], bArr[i2 + 2], bArr[i2 + 3], bArr[i2 + 4], bArr[i2 + 5]), 48);
            }
            if (i != 100) {
                Log.d("ParserUtils", "Format type " + i + " not supported");
                return 0L;
            }
            return unsignedBytesToInt(bArr[i2 + 3], bArr[i2 + 2], bArr[i2 + 1], bArr[i2]);
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public static int getMantissa(byte[] bArr, int i, int i2) {
        if (getTypeLen(i) + i2 <= bArr.length) {
            if (i == 50) {
                return unsignedToSigned(unsignedByteToInt(bArr[i2]) + ((unsignedByteToInt(bArr[i2 + 1]) & 15) << 8), 12);
            }
            if (i == 52) {
                return unsignedToSigned(unsignedByteToInt(bArr[i2]) + (unsignedByteToInt(bArr[i2 + 1]) << 8) + (unsignedByteToInt(bArr[i2 + 2]) << 16), 24);
            }
            return 0;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public static String getProperties(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        int properties = bluetoothGattCharacteristic.getProperties();
        StringBuilder sb = new StringBuilder();
        if ((properties & 1) > 0) {
            sb.append("B ");
        }
        if ((properties & 128) > 0) {
            sb.append("E ");
        }
        if ((properties & 32) > 0) {
            sb.append("I ");
        }
        if ((properties & 16) > 0) {
            sb.append("N ");
        }
        if ((properties & 2) > 0) {
            sb.append("R ");
        }
        if ((properties & 64) > 0) {
            sb.append("SW ");
        }
        if ((properties & 8) > 0) {
            sb.append("W ");
        }
        if ((properties & 4) > 0) {
            sb.append("WNR ");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            sb.insert(0, "[");
            sb.append("]");
        }
        return sb.toString();
    }

    public static int getTypeLen(int i) {
        return i & 15;
    }

    public static int intOrThrow(Integer num) {
        num.getClass();
        return num.intValue();
    }

    private static int intToSignedBits(int i, int i2) {
        if (i < 0) {
            int i3 = 1 << (i2 - 1);
            return (i & (i3 - 1)) + i3;
        }
        return i;
    }

    private static long longToSignedBits(long j, int i) {
        if (j < 0) {
            int i2 = 1 << (i - 1);
            return (j & (i2 - 1)) + i2;
        }
        return j;
    }

    public static String periodicAdvertisingIntervalToString(int i) {
        if (i == 0) {
            return "Not present";
        }
        StringBuilder sb = new StringBuilder();
        double d = i;
        Double.isNaN(d);
        sb.append(d * 1.25d);
        sb.append("ms");
        return sb.toString();
    }

    public static String phyToString(int i) {
        return i != 1 ? i != 2 ? i != 3 ? "Unused" : "LE Coded" : "LE 2M" : "LE 1M";
    }

    public static int setByteArrayValue(byte[] bArr, int i, String str) {
        if (str == null) {
            return i;
        }
        for (int i2 = 0; i2 < str.length(); i2 += 2) {
            bArr[(i2 / 2) + i] = (byte) ((Character.digit(str.charAt(i2), 16) << 4) + Character.digit(str.charAt(i2 + 1), 16));
        }
        return i + (str.length() / 2);
    }

    public static int setValue(byte[] bArr, int i, int i2, int i3) {
        int typeLen = getTypeLen(i3) + i;
        if (typeLen > bArr.length) {
            return i;
        }
        if (i3 != 98) {
            if (i3 != 100) {
                if (i3 == 114) {
                    i2 = intToSignedBits(i2, 16);
                } else if (i3 != 116) {
                    switch (i3) {
                        case 17:
                            bArr[i] = (byte) (i2 & 255);
                            break;
                        case 18:
                            bArr[i] = (byte) (i2 & 255);
                            bArr[i + 1] = (byte) ((i2 >> 8) & 255);
                            break;
                        case 19:
                            int i4 = i + 1;
                            bArr[i] = (byte) (i2 & 255);
                            bArr[i4] = (byte) ((i2 >> 8) & 255);
                            bArr[i4 + 1] = (byte) ((i2 >> 16) & 255);
                            break;
                        case 20:
                            int i5 = i + 1;
                            bArr[i] = (byte) (i2 & 255);
                            int i6 = i5 + 1;
                            bArr[i5] = (byte) ((i2 >> 8) & 255);
                            bArr[i6] = (byte) ((i2 >> 16) & 255);
                            bArr[i6 + 1] = (byte) ((i2 >> 24) & 255);
                            break;
                        default:
                            switch (i3) {
                                case 33:
                                    i2 = intToSignedBits(i2, 8);
                                    bArr[i] = (byte) (i2 & 255);
                                    break;
                                case 34:
                                    i2 = intToSignedBits(i2, 16);
                                    bArr[i] = (byte) (i2 & 255);
                                    bArr[i + 1] = (byte) ((i2 >> 8) & 255);
                                    break;
                                case 35:
                                    i2 = intToSignedBits(i2, 24);
                                    int i42 = i + 1;
                                    bArr[i] = (byte) (i2 & 255);
                                    bArr[i42] = (byte) ((i2 >> 8) & 255);
                                    bArr[i42 + 1] = (byte) ((i2 >> 16) & 255);
                                    break;
                                case 36:
                                    i2 = intToSignedBits(i2, 32);
                                    int i52 = i + 1;
                                    bArr[i] = (byte) (i2 & 255);
                                    int i62 = i52 + 1;
                                    bArr[i52] = (byte) ((i2 >> 8) & 255);
                                    bArr[i62] = (byte) ((i2 >> 16) & 255);
                                    bArr[i62 + 1] = (byte) ((i2 >> 24) & 255);
                                    break;
                                default:
                                    Log.d("ParserUtils", "Format type " + i3 + " not supported");
                                    return i;
                            }
                    }
                    return typeLen;
                } else {
                    i2 = intToSignedBits(i2, 32);
                }
            }
            int i7 = i + 1;
            bArr[i] = (byte) ((i2 >> 24) & 255);
            int i8 = i7 + 1;
            bArr[i7] = (byte) ((i2 >> 16) & 255);
            bArr[i8] = (byte) ((i2 >> 8) & 255);
            bArr[i8 + 1] = (byte) (i2 & 255);
            return typeLen;
        }
        bArr[i] = (byte) ((i2 >> 8) & 255);
        bArr[i + 1] = (byte) (i2 & 255);
        return typeLen;
    }

    public static String txPowerToString(int i) {
        if (i == 127) {
            return "Not present";
        }
        return i + " dBm";
    }

    private static int unsignedByteToInt(byte b) {
        return b & FlagsParser.UNKNOWN_FLAGS;
    }

    private static long unsignedByteToLong(byte b) {
        return b & 255;
    }

    private static int unsignedBytesToInt(byte b, byte b2) {
        return unsignedByteToInt(b) + (unsignedByteToInt(b2) << 8);
    }

    private static long unsignedBytesToLong(byte b, byte b2, byte b3, byte b4, byte b5, byte b6) {
        return unsignedByteToLong(b) + (unsignedByteToLong(b2) << 8) + (unsignedByteToLong(b3) << 16) + (unsignedByteToLong(b4) << 24) + (unsignedByteToLong(b5) << 32) + (unsignedByteToLong(b6) << 40);
    }

    private static int unsignedToSigned(int i, int i2) {
        int i3 = 1 << (i2 - 1);
        return (i & i3) != 0 ? (i3 - (i & (i3 - 1))) * (-1) : i;
    }

    private static long unsignedToSigned(long j, int i) {
        long j2 = 1 << (i - 1);
        return (j & j2) != 0 ? (j2 - (j & (j2 - 1))) * (-1) : j;
    }

    public static String bytesToHex(byte[] bArr, int i, int i2, boolean z) {
        if (bArr == null || bArr.length <= i || i2 <= 0) {
            return "";
        }
        int min = Math.min(i2, bArr.length - i);
        char[] cArr = new char[min * 2];
        for (int i3 = 0; i3 < min; i3++) {
            int i4 = bArr[i + i3] & FlagsParser.UNKNOWN_FLAGS;
            int i5 = i3 * 2;
            char[] cArr2 = HEX_ARRAY;
            cArr[i5] = cArr2[i4 >>> 4];
            cArr[i5 + 1] = cArr2[i4 & 15];
        }
        if (!z) {
            return new String(cArr);
        }
        return "0x" + new String(cArr);
    }

    private static int unsignedBytesToInt(byte b, byte b2, byte b3) {
        return unsignedByteToInt(b) + (unsignedByteToInt(b2) << 8) + (unsignedByteToInt(b3) << 16);
    }

    private static int unsignedBytesToInt(byte b, byte b2, byte b3, byte b4) {
        return unsignedByteToInt(b) + (unsignedByteToInt(b2) << 8) + (unsignedByteToInt(b3) << 16) + (unsignedByteToInt(b4) << 24);
    }

    public static byte[] decodeUuid(String str) {
        if (str == null || !str.matches("^([a-zA-Z0-9]{4}){1,2}")) {
            return null;
        }
        int length = str.length() / 2;
        byte[] bArr = new byte[length];
        for (int i = 0; i < str.length(); i += 2) {
            bArr[(length - 1) - (i / 2)] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return bArr;
    }

    public static int setValue(byte[] bArr, int i, long j, int i2) {
        int typeLen = getTypeLen(i2) + i;
        if (typeLen > bArr.length) {
            return i;
        }
        if (i2 != 20) {
            if (i2 != 36) {
                if (i2 != 100) {
                    if (i2 != 116) {
                        Log.d("ParserUtils", "Format type " + i2 + " not supported as long");
                        return i;
                    }
                    j = longToSignedBits(j, 32);
                }
                int i3 = i + 1;
                bArr[i] = (byte) ((j >> 24) & 255);
                int i4 = i3 + 1;
                bArr[i3] = (byte) ((j >> 16) & 255);
                bArr[i4] = (byte) ((j >> 8) & 255);
                bArr[i4 + 1] = (byte) (j & 255);
                return typeLen;
            }
            j = longToSignedBits(j, 32);
        }
        int i5 = i + 1;
        bArr[i] = (byte) (j & 255);
        int i6 = i5 + 1;
        bArr[i5] = (byte) ((j >> 8) & 255);
        bArr[i6] = (byte) ((j >> 16) & 255);
        bArr[i6 + 1] = (byte) ((j >> 24) & 255);
        return typeLen;
    }

    public static int setValue(byte[] bArr, int i, int i2, int i3, int i4) {
        int typeLen = getTypeLen(i4) + i;
        if (typeLen > bArr.length) {
            return i;
        }
        if (i4 == 50) {
            int intToSignedBits = intToSignedBits(i2, 12);
            int intToSignedBits2 = intToSignedBits(i3, 4);
            int i5 = i + 1;
            bArr[i] = (byte) (intToSignedBits & 255);
            bArr[i5] = (byte) ((intToSignedBits >> 8) & 15);
            bArr[i5] = (byte) (bArr[i5] + ((byte) ((intToSignedBits2 & 15) << 4)));
        } else if (i4 != 52) {
            return i;
        } else {
            int intToSignedBits3 = intToSignedBits(i2, 24);
            int intToSignedBits4 = intToSignedBits(i3, 8);
            int i6 = i + 1;
            bArr[i] = (byte) (intToSignedBits3 & 255);
            int i7 = i6 + 1;
            bArr[i6] = (byte) ((intToSignedBits3 >> 8) & 255);
            int i8 = i7 + 1;
            bArr[i7] = (byte) ((intToSignedBits3 >> 16) & 255);
            bArr[i8] = (byte) (bArr[i8] + ((byte) (intToSignedBits4 & 255)));
        }
        return typeLen;
    }

    public static int setValue(byte[] bArr, int i, String str) {
        if (str == null) {
            return i;
        }
        byte[] bytes = str.getBytes();
        System.arraycopy(bytes, 0, bArr, i, bytes.length);
        return i + bytes.length;
    }
}