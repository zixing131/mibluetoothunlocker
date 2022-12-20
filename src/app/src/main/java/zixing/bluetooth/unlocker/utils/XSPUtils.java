package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
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

import de.robv.android.xposed.XposedBridge;
import zixing.bluetooth.unlocker.activity.MainActivity;

public class XSPUtils {

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

    public static Context catchContext;
    public static Object lockobj = new Object();
    public static SharedPreferences xsp;

    /**
     * 初始化，在handleLoadPackage里面使用
     */
    @SuppressLint("WrongConstant")
    public static void initXSP(Context context) {
        try {
            catchContext = context;
            if(MainActivity.self==null)
            {
                return;
            }
            Context useCount =  context.createPackageContext("zixing.bluetooth.unlocker",
                    Context.CONTEXT_RESTRICTED);

            myLog("----------initXSP zixing.bluetooth.unlocker------------"+useCount);
            xsp  = useCount.getSharedPreferences("unlocker", Context.MODE_ENABLE_WRITE_AHEAD_LOGGING);

            myLog("----------initXSP  xsp------------"+xsp);
        } catch (Exception ex)
        {
            myLog("----------initXSP error------------"+ex.toString());
        }
    }

    //type1是setting，2是系统界面，0是软件本体


    public static String getString(String key,String def,int type){

        if(type==0)
        {
            if(xsp==null)
            {
                initXSP(catchContext);
            }if(xsp!=null)
        {
            return xsp.getString(key,def);
        }
            myLog("xsp 为空1！");
            return  def;

        }
        else if(type==1)
        {
            try{
                if("rssi".equals(key))
                {
                    synchronized(lockobj) {
                        File file = new File("/sdcard/Android/data/com.android.settings/config.ini");
                        FileInputStream fis = new FileInputStream(file);
                        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                        String data=  br.readLine().trim();
                        br.close();
                        fis.close();
                        return data;
                    }
                }
                else if("mac".equals(key)) {
                    synchronized (lockobj) {
                        File file = new File("/sdcard/Android/data/com.android.settings/config_mac.ini");
                        FileInputStream fis = new FileInputStream(file);
                        BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                        String data = br.readLine().trim();
                        br.close();
                        fis.close();
                        return data;
                    }
                }
            }catch (Exception ex)
            {
                myLog("读取com.android.settings/config.ini失败"+ex);
            }

        }
        else if(type==2)
        {
            try{
                if("rssi".equals(key))
                {
                synchronized(lockobj) {
                    File file = new File("/sdcard/Android/data/com.android.systemui/config.ini");
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    String data = br.readLine().trim();
                    br.close();
                    fis.close();
                    return data;
                }
                } else if("mac".equals(key)) {
                    synchronized(lockobj) {
                    File file = new File("/sdcard/Android/data/com.android.systemui/config_mac.ini");
                    FileInputStream fis = new FileInputStream(file);
                    BufferedReader br = new BufferedReader(new InputStreamReader(fis));
                    String data = br.readLine().trim();
                    br.close();
                    fis.close();
                    return data;
                    }
                }
            }catch (Exception ex)
            {
                myLog("读取com.android.systemui/config.ini失败"+ex);
            }
        }
        return "";

        /*
        synchronized(catchContext){
        File file = new File("/data/data/zixing.bluetooth.unlocker/shared_prefs/unlocker.xml");

        FileInputStream fs = null;

        try {
            XmlPullParser xp = Xml.newPullParser();
            fs = new FileInputStream(file);
            
            xp.setInput(fs, "utf-8");

            int type = xp.getEventType();

            StringBuilder sb = new StringBuilder();

            while (type != XmlPullParser.END_DOCUMENT) {

                if (type == XmlPullParser.START_TAG) {

                    String ac = xp.getName();
                    if("string".equals(ac))
                    {
                        String nx = xp.nextText();

                        return nx;
                    }
                }
                type = xp.next();
            }
        }catch (Exception ex)
        {
            myLog("出错："+ex.toString());
        }
        finally {
            if(fs!=null)
            {
                try {
                    fs.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return def;
        }
        */

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

    public static final String BASE_MODE="basemode";

    public static void clearSettingString()
    {
        try{
                synchronized (lockobj) {
                    File file = new File("/sdcard/Android/data/com.android.settings/config_mac.ini");
                    FileOutputStream fis = new FileOutputStream(file);
                    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fis));
                    br.write("");
                    br.close();
                    fis.close();
            }
        }catch (Exception ex)
        {
            myLog("写入com.android.settings/config.ini失败"+ex);
        }
    }
    public static void clearSystemUIString()
    {
        try{
            synchronized(lockobj) {
                File file = new File("/sdcard/Android/data/com.android.systemui/config_mac.ini");
                FileOutputStream fis = new FileOutputStream(file);
                BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fis));
                br.write("");
                br.close();
                fis.close();
            }
        }catch (Exception ex)
        {
            myLog("写入com.android.settings/config.ini失败"+ex);
        }
    }

    public static boolean setString(String key,String def){
        try
        {
            if(xsp==null)
            {
                initXSP(catchContext);
            }
            if(xsp!=null)
            {
                xsp.edit().putString(key,def).commit();
            }

            if("rssi".equals(key))
            {
                synchronized(catchContext) {

                    synchronized(catchContext) {
                        File folder = new File("/sdcard/Android/data/com.android.settings/");
                        if(folder.exists()==false)
                        {
                            execRootCmdSilent("mkdir /sdcard/Android/data/com.android.settings/");
                        }
                        execRootCmdSilent("echo \""+def+"\" > /sdcard/Android/data/com.android.settings/config.ini");
                    }
                }

                synchronized(catchContext) {
                    File folder = new File("/sdcard/Android/data/com.android.systemui/");
                    if(folder.exists()==false)
                    {
                        execRootCmdSilent("mkdir /sdcard/Android/data/com.android.systemui/");
                    }
                    execRootCmdSilent("echo \""+def+"\" > /sdcard/Android/data/com.android.systemui/config.ini");

                }

            }
            else if("mac".equals(key))
            {
                synchronized(catchContext) {

                    synchronized(catchContext) {
                        File folder = new File("/sdcard/Android/data/com.android.settings/");
                        if(folder.exists()==false)
                        {
                            execRootCmdSilent("mkdir /sdcard/Android/data/com.android.settings/");
                        }
                        execRootCmdSilent("echo \""+def+"\" > /sdcard/Android/data/com.android.settings/config_mac.ini");
                    }
                }

                synchronized(catchContext) {
                    File folder = new File("/sdcard/Android/data/com.android.systemui/");
                    if(folder.exists()==false)
                    {
                        execRootCmdSilent("mkdir /sdcard/Android/data/com.android.systemui/");
                    }
                    execRootCmdSilent("echo \""+def+"\" > /sdcard/Android/data/com.android.systemui/config_mac.ini");

                }
            }
            return true;
        }
       catch (Exception ex)
       {
           myLog("io错误："+ex.toString());
           return false;
       }
    }
}
