package zixing.bluetooth.unlocker.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

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

    @SuppressLint("WrongConstant")
    public static void initXSP(Context context) {
        try {
            if(MainActivity.self==null)
            {
                return;
            }
            if(SPUtils.isEnableModule==false)
            {
                return;
            }

            String mac = "";
            String rssi="";
            String bkdatapath = "/data/data/zixing.bluetooth.unlocker/shared_prefs/unlocker.xml";

            try{
                File file1=new File(bkdatapath);
                //创建DOM解析工厂
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                //创建DON解析器
                DocumentBuilder dombuild = factory.newDocumentBuilder();
                //开始解析XML文档并且得到整个文档的对象模型
                Document dom = dombuild.parse(file1);
                //得到根节点下所有标签为<book>的子节点
                NodeList bookList = dom.getElementsByTagName("map");
                //遍历book节点
                for (int i = 0; i < bookList.getLength(); i++) {
                    //得到本次Book元素节点
                    Element bookElement = (Element) bookList.item(i);

                    String name =  bookElement.getTagName();
                    if("map".equals(name))
                    {
                        NodeList strings = dom.getElementsByTagName("string");
                        for (int j = 0; j < strings.getLength(); j++) {

                            Element item2 = (Element) strings.item(j);
                            String name2 =  item2.getAttribute("name");
                            String value =  item2.getTextContent();
                            if("mac".equals(name2))
                            {
                                mac = value;
                            }else if("rssi".equals(name2))
                            {
                                rssi = value;
                            }
                        }
                    }
                }

            }catch (Exception ex)
            {
            }
            if(mac!=null && !mac.isEmpty())
            {
                setString("mac",mac);
                new File(bkdatapath).delete();
                Toast.makeText(context,"已导入旧版配置！",Toast.LENGTH_SHORT).show();
            }
            if(rssi!=null && !rssi.isEmpty() && !"-50".equals(rssi))
            {
                setString("rssi",rssi);
            }

        } catch (Exception ex)
        {
            myLog("----------initXSP error------------"+ex.toString());
        }
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
