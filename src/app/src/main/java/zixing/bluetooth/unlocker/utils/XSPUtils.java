package zixing.bluetooth.unlocker.utils;


import android.annotation.SuppressLint;

import de.robv.android.xposed.XSharedPreferences;

public class XSPUtils {
    
    public static XSharedPreferences intance;

    /**
     * 初始化，在handleLoadPackage里面使用
     */
    
    public static XSharedPreferences getInstance() {
        if (intance == null){
            intance = new XSharedPreferences("zixing.bluetooth.unlocker","config");
            intance.makeWorldReadable();
        }else {
            intance.reload();
        }
        return intance;
    }
    
    public static boolean getBoolean(String key,boolean def){
        return getInstance().getBoolean(key,def);
    }
    
    public static String getString(String key,String def){
        return getInstance().getString(key,def);
    }
    
    public static int getInt(String key,int def){
        return getInstance().getInt(key,def);
    }
    
    public static float getFloat(String key,float def){
        return getInstance().getFloat(key,def);
    }
    
    public static long getLong(String key,long def){
        return getInstance().getLong(key,def);
    }
}
