package zixing.bluetooth.unlocker.Xp;

import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import zixing.bluetooth.unlocker.BluetoothHelper;
import zixing.bluetooth.unlocker.XSPUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;


public class MyXp implements IXposedHookLoadPackage {

    private static void myLog(String msg)
    {
        Log.i("hookhelper",msg);
        XposedBridge.log(msg);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        if ("com.android.settings".equals(loadPackageParam.packageName)) {

            final Class MiuiSecurityBluetoothMatchDeviceFragmentClass =  XposedHelpers.findClass("com.android.settings.MiuiSecurityBluetoothMatchDeviceFragment", loadPackageParam.classLoader);
            final Class MiuiLockPatternUtilClass = XposedHelpers.findClass("android.security.MiuiLockPatternUtils", loadPackageParam.classLoader);
            final  Class BluetoothDeviceClass = XposedHelpers.findClass("android.bluetooth.BluetoothDevice", loadPackageParam.classLoader);

            //myLog("--------------"+MiuiLockPatternUtilClass.toString()+"------------");


            XposedHelpers.findAndHookMethod("com.android.settings.MiuiSecurityBluetoothMatchDeviceFragment", loadPackageParam.classLoader,"switchToTapConfirmingLayout",new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                    try {
                        //myLog("--------------开始hook switchToTapConfirmingLayout MiuiLockPatternUtilClass------------");

                        Field   mLockPatternUtilsField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField ("mLockPatternUtils");
                        mLockPatternUtilsField.setAccessible(true);
                            mLockPatternUtils  = mLockPatternUtilsField.get(methodHookParam.thisObject);

                        Field  mDeviceField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField ("mDevice");
                        mDeviceField.setAccessible(true);
                        Object mDevice=mDeviceField.get(methodHookParam.thisObject);

                        Field  mDeviceTypeField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField ("mDeviceType");
                        mDeviceTypeField.setAccessible(true);
                        Object mDeviceType=mDeviceTypeField.get(methodHookParam.thisObject);

                        Field  mDeviceMajorClassField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField ("mDeviceMajorClass");
                        mDeviceMajorClassField.setAccessible(true);
                        Object mDeviceMajorClass=mDeviceMajorClassField.get(methodHookParam.thisObject);

                        Field  mDeviceMinorClassField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField("mDeviceMinorClass");
                        mDeviceMinorClassField.setAccessible(true);
                        Object mDeviceMinorClass=mDeviceMinorClassField.get(methodHookParam.thisObject);


                        XposedHelpers.callMethod(mLockPatternUtils,"setBluetoothUnlockEnabled",true);


                        XposedHelpers.callMethod(mLockPatternUtils,"setBluetoothAddressToUnlock",XposedHelpers.callMethod(mDevice,"getAddress").toString());

                        XposedHelpers.callMethod(mLockPatternUtils,"setBluetoothNameToUnlock",XposedHelpers.callMethod(mDevice,"getName").toString());

                        XposedHelpers.callMethod(methodHookParam.thisObject,"saveDevice",XposedHelpers.callMethod(methodHookParam.thisObject,"getContext"),XposedHelpers.callMethod(mDevice,"getAddress").toString(),mDeviceType,mDeviceMajorClass, mDeviceMinorClass, true);


                        XposedHelpers.callMethod(methodHookParam.thisObject,"switchToSucceedLayout");

                        //myLog("--------------结束hook switchToTapConfirmingLayout------------");

                    }
                    catch(Exception ex)
                    {
                        myLog("-------------- 发生错误 ： "+ex.toString());
                    }
                    return null;
                }
            });

            Class<?> MiuiSecurityBluetoothDeviceInfoFragment = XposedHelpers.findClassIfExists(
                    "com.android.settings.MiuiSecurityBluetoothDeviceInfoFragment",
                    loadPackageParam.classLoader
            );
            XposedBridge.hookAllMethods(MiuiSecurityBluetoothDeviceInfoFragment,"onCreate",new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    //myLog("-------------- before hook MiuiSecurityBluetoothDeviceInfoFragment.onCreate ------------");

                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    //myLog("-------------- after hook MiuiSecurityBluetoothDeviceInfoFragment.onCreate ------------");

                    context = (Context)( XposedHelpers.callMethod(param.thisObject,"getContext"));
                    XSPUtils.initXSP(context);

                    Field   mUnlockListenerField = MiuiSecurityBluetoothDeviceInfoFragment.getDeclaredField ("mUnlockListener");
                    mUnlockListenerField.setAccessible(true);
                    Object   mUnlockListener  = mUnlockListenerField.get(param.thisObject);

                    Field   mLockPatternUtilsField = MiuiSecurityBluetoothDeviceInfoFragment.getDeclaredField ("mLockPatternUtils");
                    mLockPatternUtilsField.setAccessible(true);
                     mLockPatternUtils  = mLockPatternUtilsField.get(param.thisObject);


                    Class clazzActivity=  mUnlockListener .getClass();

                    XposedBridge.hookAllMethods(clazzActivity,"onUnlocked",new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            //myLog("-------------- before hook com.android.settings.MiuiSecurityBluetoothDeviceInfoFragment$1 ------------");
                            if(param.args.length==1)
                            {
                                if("0".equals(param.args[0].toString()))
                                {
                                    if(BluetoothHelper.CanUnlockByBluetoothOld(context, XposedHelpers.callMethod(mLockPatternUtils,"getBluetoothAddressToUnlock").toString() ,loadPackageParam.classLoader,1) == true)
                                    {
                                        param.args[0] = (byte)(2);
                                    }
                                    else {
                                        param.args[0] = (byte)(1);
                                        myLog("-------------- 不符合解锁条件 ------------");
                                    }
                                }
                            }
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            //myLog("-------------- after hook com.android.settings.MiuiSecurityBluetoothDeviceInfoFragment$1 ------------");
                        }
                    });
             }
            });
            Class<?> OnUnlockStateChangeListenerClazz = XposedHelpers.findClassIfExists(
                    "miui.bluetooth.ble.MiBleUnlockProfile$OnUnlockStateChangeListener",
                    loadPackageParam.classLoader
            );
        }
        else if("com.android.systemui".equals(loadPackageParam.packageName))
        {

            final Class BluetoothControllerImplClass =  XposedHelpers.findClass("com.android.systemui.statusbar.policy.BluetoothControllerImpl",  loadPackageParam.classLoader);

            XposedBridge.hookAllConstructors(BluetoothControllerImplClass, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                //myLog("-------------- before hook BluetoothControllerImplClass ------------");
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                //myLog("-------------- after hook BluetoothControllerImplClass ------------");
                BluetoothHelper.BluetoothControllerImplInstance = param.thisObject;
            }
        });

            Class<?> MiuiBleUnlockHelper = XposedHelpers.findClassIfExists(
                    "com.android.keyguard.MiuiBleUnlockHelper",
                    loadPackageParam.classLoader
            );

            //myLog("-------------- hook MiuiBleUnlockHelper ------------");
            XposedBridge.hookAllConstructors(MiuiBleUnlockHelper, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    super.beforeHookedMethod(param);
                    //myLog("-------------- before hook MiuiBleUnlockHelper ------------");
                }
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // myLog("-------------- after hook MiuiBleUnlockHelper ------------");

                    context  = (Context)(param.args[0]);
                    XSPUtils.initXSP(context);

                    Field  mBleListenerField = MiuiBleUnlockHelper.getDeclaredField ("mBleListener");
                    mBleListenerField.setAccessible(true);
                    mBleListener  = mBleListenerField.get(param.thisObject);

                    classLoader  = loadPackageParam.classLoader;
                    Field   mLockPatternUtilsField = MiuiBleUnlockHelper.getDeclaredField ("mLockPatternUtils");
                    mLockPatternUtilsField.setAccessible(true);
                    mLockPatternUtils  = mLockPatternUtilsField.get(param.thisObject);


                    Class clazzActivity=  mBleListener.getClass();


                    XposedBridge.hookAllMethods(clazzActivity,"onUnlocked",new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            //myLog("-------------- before hook mBleListener.onUnlocked ------------");
                            if(param.args.length==1)
                            {
                                if("0".equals(param.args[0].toString()))
                                {
                                    CheckPhoneUnlock();
                                }
                            }
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // myLog("-------------- after hook mBleListener.onUnlocked ------------");
                        }
                    });
                }
            });
        }
    }

    public static void CheckPhoneUnlock()
    {
        // 步骤1：通过匿名类 直接 创建线程辅助对象，即 实例化 线程辅助类
        Runnable mt = new Runnable() {
            // 步骤2：复写run（），定义线程行为
            @Override
            public void run() {
                if(context!=null && mLockPatternUtils!=null && classLoader!=null)
                {
                    if(BluetoothHelper.CanUnlockByBluetoothOld( context,XposedHelpers.callMethod(mLockPatternUtils,"getBluetoothAddressToUnlock").toString() ,classLoader,2) == true)
                    {
                        UnlockPhone();
                    }
                    else {
                        myLog("-------------- 不符合解锁条件 ------------");
                    }
                }  else{
                    myLog("---------------NULL context--------------"+context+mLockPatternUtils+classLoader);
                }
            }
        };
        Thread mt1 = new Thread(mt, "unlockthread");
        mt1.start();
        //myLog("--------------mt1 unlockthread ------------");
    }
    static ClassLoader classLoader = null;
    static  Object   mLockPatternUtils=null;
    static Context context = null;
    public static Object mBleListener = null;
    public static void UnlockPhone(){
         try {
            if(mBleListener!=null && context!=null)
            {
                new Handler(context.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        XposedHelpers.callMethod(mBleListener,"onUnlocked",((byte)(2)));
                    }
                });
            }
            else{
                myLog("---------------NULL unlockMethod--------------");
            }
        } catch (Exception e) {
             myLog(e.toString());
        }
    }
}
