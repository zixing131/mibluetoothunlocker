package zixing.bluetooth.unlocker.Xp;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import zixing.bluetooth.unlocker.utils.BluetoothHelper;
import zixing.bluetooth.unlocker.utils.XSPUtils;


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
            final  Class BluetoothDeviceClass = XposedHelpers.findClass("android.bluetooth.BluetoothDevice", loadPackageParam.classLoader);
            final Class MiuiLockPatternUtilClass = XposedHelpers.findClass("android.security.MiuiLockPatternUtils", loadPackageParam.classLoader);


            final String[] macrep = {XSPUtils.getString("mac", "", 1)};

            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context1 = (Context) param.args[0];

                    if(macrep[0] !=null && !macrep[0].isEmpty())
                    {
                        if(XSPUtils.BASE_MODE.equals(macrep[0]))
                        {
                            Constructor c0=MiuiLockPatternUtilClass.getDeclaredConstructor(new Class[]{Context.class});
                            c0.setAccessible(true);
                            Object utilclass= c0.newInstance(new Object[]{ context1});
                            XposedHelpers.callMethod(utilclass,"setBluetoothUnlockEnabled",false);
                            XposedHelpers.callMethod(utilclass,"setBluetoothAddressToUnlock","");
                            XposedHelpers.callMethod(utilclass,"setBluetoothNameToUnlock","");
                            XposedHelpers.callMethod(utilclass,"setBluetoothKeyToUnlock","");

                            XSPUtils.clearSettingString();
                            macrep[0] ="";

                        }else{
                            Constructor c0=MiuiLockPatternUtilClass.getDeclaredConstructor(new Class[]{Context.class});
                            c0.setAccessible(true);
                            Object utilclass= c0.newInstance(new Object[]{ context1});
                            XposedHelpers.callMethod(utilclass,"setBluetoothUnlockEnabled",true);
                            XposedHelpers.callMethod(utilclass,"setBluetoothAddressToUnlock", macrep[0]);
                            XposedHelpers.callMethod(utilclass,"setBluetoothNameToUnlock","mibluetoothunlocker");
                            XposedHelpers.callMethod(utilclass,"setBluetoothKeyToUnlock","mibluetoothunlocker");
                        }
                    }
                }
            });


            XposedHelpers.findAndHookMethod(MiuiLockPatternUtilClass,"getBluetoothAddressToUnlock",new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                             if(macrep[0] !=null && !macrep[0].isEmpty())
                             {
                                 if(XSPUtils.BASE_MODE.equals(macrep[0]))
                                 {
                                     param.setResult(null);
                                 }else{
                                 param.setResult(macrep[0]);
                             }
                             }
                        }
                    });


            //myLog("--------------"+MiuiLockPatternUtilClass.toString()+"------------");


            XposedHelpers.findAndHookMethod("com.android.settings.MiuiSecurityBluetoothMatchDeviceFragment", loadPackageParam.classLoader,"switchToTapConfirmingLayout",new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {

                    try {
                        //myLog("--------------??????hook switchToTapConfirmingLayout MiuiLockPatternUtilClass------------");

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

                        //myLog("--------------??????hook switchToTapConfirmingLayout------------");

                    }
                    catch(Exception ex)
                    {
                        myLog("-------------- ???????????? ??? "+ex.toString());
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
                    mUnlockListener  = mUnlockListenerField.get(param.thisObject);

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
                                    param.args[0] = (byte)(1);
                                    BluetoothHelper.CanUnlockByBluetoothOldDirect(context, XposedHelpers.callMethod(mLockPatternUtils,"getBluetoothAddressToUnlock").toString() ,loadPackageParam.classLoader,1);
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
            final Class MiuiLockPatternUtilClass = XposedHelpers.findClass("android.security.MiuiLockPatternUtils", loadPackageParam.classLoader);


            String macrep = XSPUtils.getString("mac","",2);

            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context1 = (Context) param.args[0];

                    if(macrep!=null && !macrep.isEmpty())
                    {
                        if(XSPUtils.BASE_MODE.equals(macrep))
                        {
                            Constructor c0=MiuiLockPatternUtilClass.getDeclaredConstructor(new Class[]{Context.class});
                            c0.setAccessible(true);
                            Object utilclass= c0.newInstance(new Object[]{ context1});
                            XposedHelpers.callMethod(utilclass,"setBluetoothUnlockEnabled",false);
                            XposedHelpers.callMethod(utilclass,"setBluetoothAddressToUnlock","");
                            XposedHelpers.callMethod(utilclass,"setBluetoothNameToUnlock","");
                            XposedHelpers.callMethod(utilclass,"setBluetoothKeyToUnlock","");
                            XSPUtils.clearSystemUIString();

                        }else{
                            Constructor c0=MiuiLockPatternUtilClass.getDeclaredConstructor(new Class[]{Context.class});
                            c0.setAccessible(true);
                            Object utilclass= c0.newInstance(new Object[]{ context1});
                            XposedHelpers.callMethod(utilclass,"setBluetoothUnlockEnabled",true);
                            XposedHelpers.callMethod(utilclass,"setBluetoothAddressToUnlock",macrep);
                            XposedHelpers.callMethod(utilclass,"setBluetoothNameToUnlock","mibluetoothunlocker");
                            XposedHelpers.callMethod(utilclass,"setBluetoothKeyToUnlock","mibluetoothunlocker");
                        }
                    }
                }
            });

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
        // ??????1?????????????????? ?????? ?????????????????????????????? ????????? ???????????????
        Runnable mt = new Runnable() {
            // ??????2?????????run???????????????????????????
            @Override
            public void run() {
                if(context!=null && mLockPatternUtils!=null && classLoader!=null)
                {
                    BluetoothHelper.CanUnlockByBluetoothOldDirect( context,XposedHelpers.callMethod(mLockPatternUtils,"getBluetoothAddressToUnlock").toString() ,classLoader,2)                             ;
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
    static Object mUnlockListener = null;
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

    public static void SetBluetoothStatus(byte b){
        try {
            if(mUnlockListener!=null && context!=null)
            {
                new Handler(context.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        XposedHelpers.callMethod(mUnlockListener,"onUnlocked",b);
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
