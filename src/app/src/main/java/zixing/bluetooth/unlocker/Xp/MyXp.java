package zixing.bluetooth.unlocker.Xp;

import android.app.Application;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;
import zixing.bluetooth.unlocker.utils.BluetoothHelper;
import zixing.bluetooth.unlocker.utils.ConfigUtil;
import zixing.bluetooth.unlocker.utils.ReflectUtil;

public class MyXp extends XposedModule {

    private static final String TAG = "hookhelper";
    public static volatile MyXp instance;
    private String processName;
    private final java.util.Set<Class<?>> listenerClasses = new java.util.HashSet<>();

    public MyXp() {
    }

    public static void myLog(String msg) {
        Log.i(TAG, msg);
        MyXp module = instance;
        if (module != null) {
            module.log(Log.INFO, TAG, msg);
        }
    }

    @Override
    public void onModuleLoaded(@NonNull XposedModuleInterface.ModuleLoadedParam param) {
        instance = this;
        processName = param.getProcessName();
        bindRemoteConfig();
        myLog("onModuleLoaded process=" + processName
                + " framework=" + getFrameworkName()
                + " api=" + getApiVersion());
    }

    private void bindRemoteConfig() {
        ConfigUtil.remoteReader = (key, def) -> getRemotePreferences(ConfigUtil.REMOTE_GROUP).getString(key, def);
    }

    @Override
    public void onPackageReady(@NonNull XposedModuleInterface.PackageReadyParam param) {
        String packageName = param.getPackageName();
        ClassLoader classLoader = param.getClassLoader();
        if ("com.android.settings".equals(packageName)) {
            hookSettings(classLoader);
        } else if ("com.android.systemui".equals(packageName)) {
            hookSystemUi(classLoader);
        }
    }

    @Override
    public boolean onHotReloading(@NonNull XposedModuleInterface.HotReloadingParam param) {
        // 本模块依赖已构造的 SystemUI/Settings 实例，热更新后无法可靠找回这些对象。
        BluetoothHelper.releaseGatt();
        BluetoothHelper.stopPassive();
        myLog("skip hot reload, reboot required. process=" + processName);
        return false;
    }

    @Override
    public void onHotReloaded(@NonNull XposedModuleInterface.HotReloadedParam param) {
        instance = this;
        processName = param.getProcessName();
        bindRemoteConfig();
        for (XposedInterface.HookHandle handle : param.getOldHookHandles()) {
            handle.unhook();
        }
        myLog("onHotReloaded process=" + processName);
        Context app = findCurrentApplication();
        ClassLoader classLoader = app != null ? app.getClassLoader() : null;
        if (classLoader == null) {
            myLog("onHotReloaded missing app classloader");
            return;
        }
        if ("com.android.settings".equals(processName)) {
            hookSettings(classLoader);
        } else if ("com.android.systemui".equals(processName)) {
            hookSystemUi(classLoader);
        }
        if (app != null) {
            applyBluetoothUnlockConfig(app);
        }
    }

    private Context findCurrentApplication() {
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object application = activityThread.getMethod("currentApplication").invoke(null);
            if (application instanceof Context) {
                return (Context) application;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void applyBluetoothUnlockConfig(Context app) {
        // Remove only the synthetic native registration written by older module versions.
        // Custom devices are checked by this module; native BLE must not connect to them too.
        try {
            Class<?> util = ReflectUtil.findClass("android.security.MiuiLockPatternUtils", app.getClassLoader());
            Object settings = ReflectUtil.newInstance(util, app);
            if ("mibluetoothunlocker".equals(ReflectUtil.callMethod(settings, "getBluetoothNameToUnlock"))) {
                ReflectUtil.callMethod(settings, "setBluetoothUnlockEnabled", false);
                ReflectUtil.callMethod(settings, "setBluetoothAddressToUnlock", "");
                ReflectUtil.callMethod(settings, "setBluetoothNameToUnlock", "");
                ReflectUtil.callMethod(settings, "setBluetoothKeyToUnlock", "");
            }
        } catch (Exception | LinkageError ex) {
            myLog("Native config migration unavailable: " + ex.getClass().getSimpleName());
        }
    }

    private boolean contextReady;
    private void initializeContext(Context app, boolean systemUi) {
        if (app == null || contextReady) return;
        contextReady = true;
        context = app.getApplicationContext() == null ? app : app.getApplicationContext();
        applyBluetoothUnlockConfig(context);
        if (!systemUi) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable check = MyXp::CheckPhoneUnlock;
        ContextCompat.registerReceiver(context, new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_SCREEN_OFF.equals(action) || Intent.ACTION_USER_PRESENT.equals(action)) {
                    handler.removeCallbacks(check);
                    BluetoothHelper.releaseGatt();
                    BluetoothHelper.stopPassive();
                    return;
                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) != BluetoothAdapter.STATE_ON) {
                        handler.removeCallbacks(check);
                        BluetoothHelper.releaseGatt();
                        BluetoothHelper.stopPassive();
                        return;
                    }
                    BluetoothHelper.preparePassive(ctx);
                } else if (!Intent.ACTION_SCREEN_ON.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device == null) return;
                    if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)
                            || BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                        BluetoothHelper.invalidatePassive(device.getAddress());
                    }
                    boolean connectedModeTarget = false;
                    for (zixing.bluetooth.unlocker.utils.TrustedDevice trusted : ConfigUtil.getDevices(2)) {
                        if (zixing.bluetooth.unlocker.utils.TrustedDevice.CONNECTED.equals(trusted.mode)
                                && trusted.address.equals(device.getAddress())) connectedModeTarget = true;
                    }
                    // Our own GATT connection also emits ACL events. Never cancel/restart it here.
                    if (!connectedModeTarget) return;
                }
                if (Intent.ACTION_SCREEN_ON.equals(action)) BluetoothHelper.preparePassive(ctx);
                handler.removeCallbacks(check);
                handler.postDelayed(check, 300);
            }
        }, filter, ContextCompat.RECEIVER_EXPORTED);
        BluetoothHelper.preparePassive(context);
    }

    private void hookMethod(Executable executable, XposedInterface.Hooker hooker) {
        hook(executable).intercept(hooker);
    }

    private void hookAllMethods(Class<?> clazz, String name, XposedInterface.Hooker hooker) {
        if (clazz == null) {
            return;
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (name.equals(method.getName())) {
                hookMethod(method, hooker);
            }
        }
    }

    private void hookAllConstructors(Class<?> clazz, XposedInterface.Hooker hooker) {
        if (clazz == null) {
            return;
        }
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            hookMethod(constructor, hooker);
        }
    }

    private void hookSettings(ClassLoader classLoader) {
        try {
        final Class<?> MiuiSecurityBluetoothMatchDeviceFragmentClass = ReflectUtil.findClass(
                "com.android.settings.MiuiSecurityBluetoothMatchDeviceFragment", classLoader);
        final Class<?> MiuiLockPatternUtilClass = ReflectUtil.findClass(
                "android.security.MiuiLockPatternUtils", classLoader);

        hookMethod(ReflectUtil.findMethod(Application.class, "attach", Context.class), chain -> {
            Object result = chain.proceed();
            initializeContext((Context) chain.getArg(0), false);
            return result;
        });
        initializeContext(findCurrentApplication(), false);

        myLog("--------------" + MiuiLockPatternUtilClass + "------------");

        hookMethod(ReflectUtil.findMethod(MiuiSecurityBluetoothMatchDeviceFragmentClass, "switchToTapConfirmingLayout"), chain -> {
            try {
                myLog("--------------开始hook switchToTapConfirmingLayout MiuiLockPatternUtilClass------------");

                Field mLockPatternUtilsField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField("mLockPatternUtils");
                mLockPatternUtilsField.setAccessible(true);
                mLockPatternUtils = mLockPatternUtilsField.get(chain.getThisObject());

                Field mDeviceField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField("mDevice");
                mDeviceField.setAccessible(true);
                Object mDevice = mDeviceField.get(chain.getThisObject());

                Field mDeviceTypeField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField("mDeviceType");
                mDeviceTypeField.setAccessible(true);
                Object mDeviceType = mDeviceTypeField.get(chain.getThisObject());

                Field mDeviceMajorClassField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField("mDeviceMajorClass");
                mDeviceMajorClassField.setAccessible(true);
                Object mDeviceMajorClass = mDeviceMajorClassField.get(chain.getThisObject());

                Field mDeviceMinorClassField = MiuiSecurityBluetoothMatchDeviceFragmentClass.getDeclaredField("mDeviceMinorClass");
                mDeviceMinorClassField.setAccessible(true);
                Object mDeviceMinorClass = mDeviceMinorClassField.get(chain.getThisObject());

                ReflectUtil.callMethod(mLockPatternUtils, "setBluetoothUnlockEnabled", true);
                ReflectUtil.callMethod(mLockPatternUtils, "setBluetoothAddressToUnlock",
                        ReflectUtil.callMethod(mDevice, "getAddress").toString());
                ReflectUtil.callMethod(mLockPatternUtils, "setBluetoothNameToUnlock",
                        Objects.toString(ReflectUtil.callMethod(mDevice, "getName"), "未命名设备"));
                ReflectUtil.callMethod(chain.getThisObject(), "saveDevice",
                        ReflectUtil.callMethod(chain.getThisObject(), "getContext"),
                        ReflectUtil.callMethod(mDevice, "getAddress").toString(),
                        mDeviceType, mDeviceMajorClass, mDeviceMinorClass, true);
                ReflectUtil.callMethod(chain.getThisObject(), "switchToSucceedLayout");
                myLog("--------------结束hook switchToTapConfirmingLayout------------");
            } catch (Exception | LinkageError ex) {
                myLog("-------------- 发生错误 ： " + ex);
            }
            return null;
        });

        Class<?> MiuiSecurityBluetoothDeviceInfoFragment = ReflectUtil.findClassIfExists(
                "com.android.settings.MiuiSecurityBluetoothDeviceInfoFragment",
                classLoader
        );
        if (MiuiSecurityBluetoothDeviceInfoFragment == null) {
            return;
        }
        Method onCreate;
        try {
            onCreate = ReflectUtil.findMethod(MiuiSecurityBluetoothDeviceInfoFragment, "onCreate", android.os.Bundle.class);
        } catch (NoSuchMethodError e) {
            onCreate = null;
        }
        XposedInterface.Hooker onCreateHooker = chain -> {
            myLog("-------------- before hook MiuiSecurityBluetoothDeviceInfoFragment.onCreate ------------");
            Object result = chain.proceed();
            myLog("-------------- after hook MiuiSecurityBluetoothDeviceInfoFragment.onCreate ------------");
            try {
                Field mUnlockListenerField = MiuiSecurityBluetoothDeviceInfoFragment.getDeclaredField("mUnlockListener");
                mUnlockListenerField.setAccessible(true);
                mUnlockListener = mUnlockListenerField.get(chain.getThisObject());

                Field mLockPatternUtilsField = MiuiSecurityBluetoothDeviceInfoFragment.getDeclaredField("mLockPatternUtils");
                mLockPatternUtilsField.setAccessible(true);
                mLockPatternUtils = mLockPatternUtilsField.get(chain.getThisObject());

                Class<?> clazzActivity = mUnlockListener.getClass();
                if (listenerClasses.add(clazzActivity)) hookAllMethods(clazzActivity, "onUnlocked", unlockedChain -> {
                    myLog("-------------- before hook com.android.settings.MiuiSecurityBluetoothDeviceInfoFragment$1 ------------");
                    Object[] args = unlockedChain.getArgs().toArray();
                    if (args.length == 1 && "0".equals(String.valueOf(args[0]))) {
                        args[0] = (byte) 1;
                        BluetoothHelper.CanUnlockByBluetoothOldDirect(context,
                                Objects.toString(ReflectUtil.callMethod(mLockPatternUtils, "getBluetoothAddressToUnlock"), ""),
                                classLoader, 1);
                        myLog("-------------- after hook com.android.settings.MiuiSecurityBluetoothDeviceInfoFragment$1 ------------");
                        return unlockedChain.proceed(args);
                    }
                    Object unlockedResult = unlockedChain.proceed();
                    myLog("-------------- after hook com.android.settings.MiuiSecurityBluetoothDeviceInfoFragment$1 ------------");
                    return unlockedResult;
                });
            } catch (Exception | LinkageError ex) {
                myLog("-------------- onCreate hook error ： " + ex);
            }
            return result;
        };
        if (onCreate != null) {
            hookMethod(onCreate, onCreateHooker);
        } else {
            hookAllMethods(MiuiSecurityBluetoothDeviceInfoFragment, "onCreate", onCreateHooker);
        }
        } catch (ReflectUtil.ClassNotFoundError ex) {
            myLog("hookSettings missing class: " + ex);
        } catch (Exception | LinkageError ex) {
            myLog("hookSettings error: " + ex);
        }
    }

    private void hookSystemUi(ClassLoader classLoader) {
        try {
            myLog("com.android.systemui enter");
            MyXp.classLoader = classLoader;
            hookMethod(ReflectUtil.findMethod(Application.class, "attach", Context.class), chain -> {
                Object result = chain.proceed();
                initializeContext((Context) chain.getArg(0), true);
                return result;
            });
            initializeContext(findCurrentApplication(), true);

            try {
            try {
                final Class<?> MiuiKeyguardUtilsClass = ReflectUtil.findClass(
                        "com.android.keyguard.utils.MiuiKeyguardUtils", classLoader);
                systemuiR = ReflectUtil.findClass("com.android.systemui.R$string", classLoader);
                miui_keyguard_ble_unlock_succeed_msg = ReflectUtil.getStaticIntField(systemuiR, "miui_keyguard_ble_unlock_succeed_msg");

                hookMethod(ReflectUtil.findMethod(MiuiKeyguardUtilsClass, "handleBleUnlockSucceed", Context.class), chain -> {
                    if ("1".equals(ConfigUtil.getString("showtips", "1", 2))) {
                        Context ctx = (Context) chain.getArg(0);
                        String unlockstring = ctx.getResources().getString(miui_keyguard_ble_unlock_succeed_msg);
                        Toast.makeText(ctx, unlockstring, Toast.LENGTH_SHORT).show();
                    }
                    return null;
                });
            } catch (ReflectUtil.ClassNotFoundError ex) {
                Method tryUnlockByBle = ReflectUtil.findMethod(
                        ReflectUtil.findClass("com.android.keyguard.MiuiBleUnlockHelper", classLoader),
                        "tryUnlockByBle");
                Method toastMakeText = ReflectUtil.findMethod(Toast.class, "makeText", Context.class, int.class, int.class);
                Method toastShow = ReflectUtil.findMethod(Toast.class, "show");
                hookMethod(tryUnlockByBle, chain -> {
                    XposedInterface.HookHandle makeTextHandle = hook(toastMakeText).intercept(makeTextChain -> {
                        String resName = ((Context) makeTextChain.getArg(0)).getResources()
                                .getResourceName((Integer) makeTextChain.getArg(1));
                        if (Objects.equals(resName, "com.android.systemui:string/miui_keyguard_ble_unlock_succeed_msg")) {
                            XposedInterface.HookHandle showHandle = hook(toastShow).intercept(showChain -> {
                                if (!("1".equals(ConfigUtil.getString("showtips", "1", 2)))) {
                                    return null;
                                }
                                return showChain.proceed();
                            });
                            try {
                                return makeTextChain.proceed();
                            } finally {
                                showHandle.unhook();
                            }
                        }
                        return makeTextChain.proceed();
                    });
                    try {
                        return chain.proceed();
                    } finally {
                        makeTextHandle.unhook();
                    }
                });
            }

            } catch (Exception | LinkageError ex) {
                myLog("Optional unlock toast hook unavailable: " + ex.getClass().getSimpleName());
            }

            final Class<?> BluetoothControllerImplClass = ReflectUtil.findClassIfExists(
                    "com.android.systemui.statusbar.policy.BluetoothControllerImpl", classLoader);
            hookAllConstructors(BluetoothControllerImplClass, chain -> {
                myLog("-------------- before hook BluetoothControllerImplClass ------------");
                Object result = chain.proceed();
                myLog("-------------- after hook BluetoothControllerImplClass ------------");
                BluetoothHelper.BluetoothControllerImplInstance = chain.getThisObject();
                return result;
            });

            Class<?> MiuiBleUnlockHelper = ReflectUtil.findClassIfExists(
                    "com.android.keyguard.MiuiBleUnlockHelper",
                    classLoader
            );
            hookAllMethods(MiuiBleUnlockHelper, "tryUnlockByBle", chain -> {
                Object result = chain.proceed();
                // The native bouncer callback invokes this method when the credential UI opens.
                if (!deliveringBluetoothResult && isBouncerShowing()) CheckPhoneUnlock();
                return result;
            });
            myLog("-------------- hook MiuiBleUnlockHelper ------------");
            hookAllConstructors(MiuiBleUnlockHelper, chain -> {
                myLog("-------------- before hook MiuiBleUnlockHelper ------------");
                Object result = chain.proceed();
                myLog("-------------- after hook MiuiBleUnlockHelper ------------");
                try {
                    systemBleHelper = chain.getThisObject();
                    bouncerVisibleField = ReflectUtil.getFieldContainingName(MiuiBleUnlockHelper, "bouncerVisible");
                    Field mBleListenerField = ReflectUtil.getFieldContainingName(MiuiBleUnlockHelper, "bleListener");
                    mBleListener = mBleListenerField.get(chain.getThisObject());

                    initializeContext(findCurrentApplication(), true);
                    MyXp.classLoader = classLoader;
                    Field mLockPatternUtilsField = ReflectUtil.getFieldContainingName(MiuiBleUnlockHelper, "lockPatternUtils");
                    mLockPatternUtils = mLockPatternUtilsField.get(chain.getThisObject());

                    try {
                        Object monitorCallback = ReflectUtil.findField(MiuiBleUnlockHelper, "mUpdateMonitorCallback")
                                .get(chain.getThisObject());
                        if (monitorCallback != null && listenerClasses.add(monitorCallback.getClass())) {
                            hookAllMethods(monitorCallback.getClass(), "onKeyguardBouncerStateChanged", bouncerChain -> {
                                if (bouncerChain.getArgs().size() == 1 && Boolean.FALSE.equals(bouncerChain.getArg(0))) {
                                    BluetoothHelper.releaseGatt();
                                }
                                return bouncerChain.proceed();
                            });
                        }
                    } catch (Exception | LinkageError ex) {
                        myLog("Optional bouncer lifecycle hook unavailable: " + ex.getClass().getSimpleName());
                    }

                    Class<?> clazzActivity = mBleListener.getClass();
                    if (listenerClasses.add(clazzActivity)) hookAllMethods(clazzActivity, "onUnlocked", unlockedChain -> {
                        myLog("-------------- before hook mBleListener.onUnlocked ------------");
                        if (unlockedChain.getArgs().size() == 1
                                && "0".equals(String.valueOf(unlockedChain.getArg(0)))) {
                            CheckPhoneUnlock(true);
                        }
                        Object unlockedResult = unlockedChain.proceed();
                        myLog("-------------- after hook mBleListener.onUnlocked ------------");
                        return unlockedResult;
                    });
                } catch (Exception | LinkageError ex) {
                    myLog("-------------- MiuiBleUnlockHelper hook error ： " + ex);
                }
                return result;
            });

        } catch (ReflectUtil.ClassNotFoundError ex) {
            myLog(ex.toString());
        } catch (Exception | LinkageError ex) {
            myLog(ex.toString());
        }
        myLog("com.android.systemui leave");
    }

    public static Class<?> systemuiR = null;
    static int miui_keyguard_ble_unlock_succeed_msg;

    public static void CheckPhoneUnlock() { CheckPhoneUnlock(false); }

    private static void CheckPhoneUnlock(boolean nativeEvent) {
        // Basic mode follows the native feature's own enabled state/callbacks.
        if (!nativeEvent && ConfigUtil.getDevices(2).isEmpty()) return;
        if (context == null || mBleListener == null || classLoader == null || !isBouncerShowing()) return;
        try {
            String legacy = mLockPatternUtils == null ? "" : Objects.toString(
                    ReflectUtil.callMethod(mLockPatternUtils, "getBluetoothAddressToUnlock"), "");
            BluetoothHelper.CanUnlockByBluetoothOldDirect(context, legacy, classLoader, 2);
        } catch (Exception | LinkageError ex) {
            myLog("Unlock check unavailable: " + ex.getClass().getSimpleName());
        }
    }

    static ClassLoader classLoader = null;
    static Object mLockPatternUtils = null;
    static Context context = null;
    static Object mUnlockListener = null;
    public static Object mBleListener = null;
    private static Object systemBleHelper;
    private static Field bouncerVisibleField;
    private static boolean deliveringBluetoothResult;

    private static boolean isBouncerShowing() {
        try {
            return systemBleHelper != null && bouncerVisibleField != null
                    && bouncerVisibleField.getBoolean(systemBleHelper);
        } catch (IllegalAccessException | IllegalArgumentException ex) { return false; }
    }

    public static void UnlockPhone() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            new Handler(Looper.getMainLooper()).post(MyXp::UnlockPhone);
            return;
        }
        try {
            if (mBleListener != null && context != null && isBouncerShowing()
                    && BluetoothHelper.canCheckLockscreen(context)) {
                deliveringBluetoothResult = true;
                try { ReflectUtil.callMethod(mBleListener, "onUnlocked", (byte) 2); }
                finally { deliveringBluetoothResult = false; }
            }
        } catch (Exception | LinkageError ex) { myLog("Unlock callback unavailable: " + ex); }
    }

    public static void SetBluetoothStatus(byte b) {
        try {
            if (mUnlockListener != null && context != null) {
                new Handler(context.getMainLooper()).post(() -> {
                    try { ReflectUtil.callMethod(mUnlockListener, "onUnlocked", b); }
                    catch (Exception | LinkageError ex) { myLog("Settings callback unavailable: " + ex); }
                });
            } else {
                myLog("---------------NULL unlockMethod--------------");
            }
        } catch (Exception e) {
            myLog(e.toString());
        }
    }
}
