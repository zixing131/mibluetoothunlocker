package zixing.bluetooth.unlocker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import io.github.libxposed.service.XposedService;
import zixing.bluetooth.unlocker.UnlockerApp;

public class ConfigUtil {

    public static final String BASE_MODE = "basemode";
    public static final String REMOTE_GROUP = "config";
    private static final String[] SYNC_KEYS = {"mac", "devices", "rssi", "showtips"};

    public static java.util.List<TrustedDevice> getDevices(int type) {
        return TrustedDevice.decode(getString("devices", null, type), getString("mac", "", type));
    }

    public static java.util.List<TrustedDevice> getDevicesForCheck(int type, String nativeAddress) {
        return TrustedDevice.resolve(getString("devices", null, type), getString("mac", "", type), nativeAddress);
    }

    public static synchronized boolean enableBasicMode() {
        SharedPreferences local = SPUtils.getInstance().sp;
        if (local == null || !local.edit().putString("devices", "base").putString("mac", BASE_MODE).commit()) return false;
        XposedService service = UnlockerApp.getService();
        if (service == null) return true;
        try {
            return service.getRemotePreferences(REMOTE_GROUP).edit().putString("devices", "base")
                    .putString("mac", BASE_MODE).commit();
        } catch (Exception ex) { myLog("sync basic mode failed: " + ex); return false; }
    }

    public static synchronized boolean saveDevices(java.util.List<TrustedDevice> devices) {
        SharedPreferences local = SPUtils.getInstance().sp;
        if (local == null) return false;
        String encoded = TrustedDevice.encode(devices);
        String primary = devices.isEmpty() ? "" : devices.get(0).address;
        boolean saved = local.edit().putString("devices", encoded).putString("mac", primary).commit();
        if (!saved) return false;
        XposedService service = UnlockerApp.getService();
        if (service != null) {
            try {
                return service.getRemotePreferences(REMOTE_GROUP).edit()
                        .putString("devices", encoded).putString("mac", primary).commit();
            } catch (Exception ex) {
                myLog("sync devices failed: " + ex);
                return false;
            }
        }
        return true;
    }

    public static synchronized boolean putDevice(String address, String mode) {
        TrustedDevice added = new TrustedDevice(address, mode);
        java.util.List<TrustedDevice> devices = getDevices(0);
        devices.removeIf(device -> device.address.equals(added.address));
        devices.add(added);
        return saveDevices(devices);
    }

    public static synchronized boolean removeDevice(String address) {
        java.util.List<TrustedDevice> devices = getDevices(0);
        if (!devices.removeIf(device -> device.address.equals(TrustedDevice.normalizeAddress(address)))) return true;
        return saveDevices(devices);
    }

    public interface RemoteConfigReader {
        String getString(String key, String def);
    }

    public static volatile RemoteConfigReader remoteReader;

    private static void myLog(String msg) {
        try {
            Log.i("hookhelper", msg);
        } catch (Exception ignored) {
        }
    }

    public static int execRootCmdSilent(String cmd) {
        int result = -1;
        DataOutputStream dos = null;
        try {
            Process p = Runtime.getRuntime().exec("su");
            dos = new DataOutputStream(p.getOutputStream());
            dos.writeBytes(cmd + "\n");
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            p.waitFor();
            result = p.exitValue();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static boolean setString(String data, String value) {
        boolean ok = SPUtils.setString(data, value);
        if (!ok) return false;
        XposedService service = UnlockerApp.getService();
        if (service != null) {
            try {
                return service.getRemotePreferences(REMOTE_GROUP).edit().putString(data, value).commit();
            } catch (Exception ex) {
                myLog("sync remote pref failed: " + ex);
                return false;
            }
        }
        return ok;
    }

    public static void initXSP(Context context) {
        try {
            if (!SPUtils.isEnableModule) {
                return;
            }

            String mac = "";
            String rssi = "";
            String bkdatapath = "/data/data/zixing.bluetooth.unlocker/shared_prefs/unlocker.xml";

            try {
                File file1 = new File(bkdatapath);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dombuild = factory.newDocumentBuilder();
                Document dom = dombuild.parse(file1);
                NodeList bookList = dom.getElementsByTagName("map");
                for (int i = 0; i < bookList.getLength(); i++) {
                    Element bookElement = (Element) bookList.item(i);
                    String name = bookElement.getTagName();
                    if ("map".equals(name)) {
                        NodeList strings = dom.getElementsByTagName("string");
                        for (int j = 0; j < strings.getLength(); j++) {
                            Element item2 = (Element) strings.item(j);
                            String name2 = item2.getAttribute("name");
                            String value = item2.getTextContent();
                            if ("mac".equals(name2)) {
                                mac = value;
                            } else if ("rssi".equals(name2)) {
                                rssi = value;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            if (mac != null && !mac.isEmpty()) {
                setString("mac", mac);
                new File(bkdatapath).delete();
                Toast.makeText(context, "已导入旧版配置！", Toast.LENGTH_SHORT).show();
            }
            if (rssi != null && !rssi.isEmpty() && !"-50".equals(rssi)) {
                setString("rssi", rssi);
            }
        } catch (Exception ex) {
            myLog("----------initXSP error------------" + ex);
        }
    }

    public static void syncWithRemote(XposedService service) {
        try {
            if (service == null || SPUtils.getInstance().sp == null) {
                return;
            }
            SharedPreferences remote = service.getRemotePreferences(REMOTE_GROUP);
            SharedPreferences local = SPUtils.getInstance().sp;
            SharedPreferences.Editor remoteEdit = remote.edit();
            SharedPreferences.Editor localEdit = local.edit();
            boolean localChanged = false;
            for (String key : SYNC_KEYS) {
                String localVal = local.getString(key, null);
                String remoteVal = remote.getString(key, null);
                if (localVal != null) {
                    remoteEdit.putString(key, localVal);
                } else if (remoteVal != null) {
                    localEdit.putString(key, remoteVal);
                    localChanged = true;
                }
            }
            remoteEdit.commit();
            if (localChanged) {
                localEdit.commit();
            }
        } catch (Exception ex) {
            myLog("syncWithRemote error: " + ex);
        }
    }

    // type1是setting，2是系统界面，0是软件本体
    public static String getString(String key, String data, int i) {
        try {
            if (i == 0) {
                return SPUtils.getString(key, data);
            }
            if (remoteReader != null) {
                return remoteReader.getString(key, data);
            }
            return data;
        } catch (Exception ex) {
            myLog(ex.toString());
            ex.printStackTrace();
            return data;
        }
    }
}
