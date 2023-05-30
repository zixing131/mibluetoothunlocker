package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import zixing.bluetooth.unlocker.BuildConfig;
import zixing.bluetooth.unlocker.Xp.MyXp;
import zixing.bluetooth.unlocker.activity.MainActivity;

public class ConfigUtil {

    public static final String BASE_MODE="basemode";
    private static ContentResolver resolver;

    private static void myLog(String msg) {
        try {
            Log.i("hookhelper",msg);
            if(MainActivity.self==null)
            {
                XposedBridge.log(msg);
            }
        }
        catch (Exception ex)
        {

        }
    }

    // cmd="chmod 777 /dev/ttyACM"
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
        return SPUtils.setString(data,value);
    }

    public static void initXSP(Context applicationContext) {

    }

    private static XSharedPreferences getPref(String path) {
        XSharedPreferences pref = new XSharedPreferences(BuildConfig.APPLICATION_ID, path);
        return pref.getFile().canRead() ? pref : null;
    }

    //type1是setting，2是系统界面，0是软件本体
    public static String getString(String key, String data, int i) {
        try{
        if(i==0)
        {
            return SPUtils.getString(key,data);
        }

            return getPref("config").getString(key,data);
        }
        catch (Exception ex)
        {
            myLog(ex.toString());
            ex.printStackTrace();
            return data;
        }
    }
}
