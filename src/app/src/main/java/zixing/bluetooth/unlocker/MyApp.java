package zixing.bluetooth.unlocker;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Trace;
import android.text.TextUtils;

import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

import java.util.HashMap;
import java.util.Map;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Context appContext = this;
        // 友盟SDK调试Log开关，App上线前请关闭Log开关
        UMConfigure.setLogEnabled(false);
        // 友盟SDK初始化，需要设置友盟Appkey（需从友盟官网注册申请），及应用发布渠道
        // 友盟SDK初始化说明文档：https://developer.umeng.com/docs/66632/detail/101814
        UMConfigure.preInit(getApplicationContext(),"63de90f8ba6a5259c4f96b9d","Umeng");
        // 友盟统计SDK页面采集模式，建议使用自动页面采集
        // 友盟统计SDK集成说明：https://developer.umeng.com/docs/66632/detail/101848
        UMConfigure.init(appContext, "63de90f8ba6a5259c4f96b9d", "Umeng", UMConfigure.DEVICE_TYPE_PHONE,
                null);
        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO);
    }
}
