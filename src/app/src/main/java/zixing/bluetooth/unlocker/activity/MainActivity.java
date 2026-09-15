package zixing.bluetooth.unlocker.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import zixing.bluetooth.unlocker.R;
import zixing.bluetooth.unlocker.UnlockerApp;
import zixing.bluetooth.unlocker.adapter.DerviceAdapter;
import zixing.bluetooth.unlocker.utils.BluetoothUtils;
import zixing.bluetooth.unlocker.utils.ConfigUtil;
import zixing.bluetooth.unlocker.utils.SPUtils;
import zixing.bluetooth.unlocker.utils.TrustedDevice;
import zixing.bluetooth.unlocker.utils.BluetoothPermissions;
import zixing.bluetooth.unlocker.utils.BluetoothHelper;
import zixing.bluetooth.unlocker.utils.RssiSessionManager;
import zixing.bluetooth.unlocker.utils.AppDialogs;
import zixing.bluetooth.unlocker.utils.DeviceMetadata;

/**
 * Author:紫星
 * Date:2022/12/03
 * Time:15:15
 * QQ:1311817771
 * E-mail:1311817771@qq.com
 */

public class MainActivity extends BaseActivity  {

    public  static boolean isXpEnable(){
        return UnlockerApp.isModuleEnabled();
    }

    @BindView(R.id.editText)
    TextInputEditText editText;
    @BindView(R.id.btnSave)
    Button btnSave;


    public static MainActivity self = null;

    private static void myLog(String msg) {
        try {
            Log.i("hookhelper", msg);
        } catch (Exception ex) {

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.right_menu, menu);
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 1001 && Adapter != null) readConfig();
    }

    private void ShowHelp() {
        String helpstr = "多设备蓝牙解锁\n选择设备可逐个配对并添加，任一设备满足条件即可解锁。\n手环选距离模式；眼镜、车载音响选连接模式（须已配对且实际连接，不检查距离）。\n启用后勾选设置[com.android.settings]和系统界面[com.android.systemui]\n然后重启手机即可，注意：使用本插件可能会降低系统安全性！\n信号阈值为负数，越小越灵敏(越容易被解锁)\n注意：更新软件后，可能要重启设备才会生效";
        AlertDialog.Builder builder = AppDialogs.builder(self);
        builder.setMessage(helpstr);
        builder.setCancelable(false);
        builder.setTitle("帮助关于");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }


    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }
    DerviceAdapter.MyViewHolder Adapter=null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        self = this;
        SPUtils.getInstance().init(this.getApplicationContext());
        ConfigUtil.initXSP(this.getApplicationContext());
        View view = this.findViewById(R.id.itemdevicemain);
        Adapter = new DerviceAdapter.MyViewHolder(view);
        preview = BluetoothHelper.createRssiManager(this);
        this.findViewById(R.id.itemdevicemain).setOnClickListener(v -> openActList(true));
        initView();

        com.google.android.material.switchmaterial.SwitchMaterial mSwitch = findViewById(R.id.switchtips);

        boolean ischecked = "1".equals(ConfigUtil.getString("showtips","1",0));

        mSwitch.setChecked(ischecked);

        // 添加监听
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!ConfigUtil.setString("showtips", isChecked ? "1" : "0")) {
                    Tt("保存或同步失败，请重试");
                } else Tt(isChecked ? "已开启蓝牙解锁提示" : "已关闭蓝牙解锁提示");
            }
        });

    }

    @OnClick({R.id.btnSave,R.id.fabmain})
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btnSave:
                saveConfig();
                break;
            case  R.id.fabmain:
                long nowtime = System.currentTimeMillis();
                if(nowtime - lstClickTime< 1000)
                {
                    return;
                }
                lstClickTime=nowtime;
                refreshSignals();
                break;
        }
    }

    private void ExitProgram() {
        self.finish();
    }

    @Override
    public void initToolbar() {
        super.initToolbar();
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        txtCenterTitle.setText(R.string.app_name);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_device_menu:
                openActList(false);
                break;
            case R.id.help_menu:
                ShowHelp();
                break;
            case R.id.private_menu:
                PrivacyPolicy();
                break;
            case R.id.clear_device:
                ClearDevice();
                break;
            case R.id.custome_device_menu:
                CustomeDevice();
                break;
            case R.id.exit_menu:
                ExitProgram();
                break;
            case R.id.reboot_menu:
                reboot();
                break;
            case android.R.id.home:
                return true;
            default:
                break;
        }
        return false;
    }
    private boolean stringIsMac(String val) {
        String trueMacAddress = "([A-Fa-f0-9]{2}:){5}[A-Fa-f0-9]{2}";
        // 这是真正的MAV地址；正则表达式；

        if (val.matches(trueMacAddress)) {
            return true;

        } else {
            return false;
        }
    }

    //自定义mac地址
    private void CustomeDevice() {

        final EditText inputServer = new EditText(this);

        inputServer.setFilters(new InputFilter[]{new InputFilter.LengthFilter(17)});

        AlertDialog.Builder builder = AppDialogs.builder(this);

        if(!TextUtils.isEmpty(mac))
        {
            inputServer.setText(mac);
        }

        inputServer.setHint("12:B4:8E:66:99:AA");
        inputServer.setSingleLine(true);
        android.widget.LinearLayout field = new android.widget.LinearLayout(this);
        int padding = AppDialogs.dp(this, 24);
        field.setPadding(padding, AppDialogs.dp(this, 8), padding, 0);
        field.addView(inputServer, new android.widget.LinearLayout.LayoutParams(-1, -2));
        builder.setTitle("手动添加设备").setMessage("输入设备的蓝牙 MAC 地址").setView(field)

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });

        builder.setCancelable(false).setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


            }

        });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button btnPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                btnPositive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String mac = inputServer.getText().toString();

                        if(mac!=null && !mac.isEmpty())
                        {
                            if(stringIsMac(mac))
                            {
                                if (!ConfigUtil.putDevice(mac, TrustedDevice.PROXIMITY)) {
                                    Tt("保存或同步失败，请重试");
                                    return;
                                }
                                MainActivity.self.readConfig();
                                dialog.dismiss();
                            }
                            else{
                                Toast.makeText(MainActivity.this,"请输入正确的mac地址！",Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(MainActivity.this,"输入为空！",Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        dialog.show();
    }

    private void ClearDevice()
    {
        String helpstr = "清除全部自定义解锁设备并切换到基础模式。系统配对关系会保留。";
        AlertDialog.Builder builder = AppDialogs.builder(self);
        builder.setMessage(helpstr);
        builder.setCancelable(false);
        builder.setTitle("是否启用基础模式");
        builder.setPositiveButton("确认",(dialog, which) -> {
            if (!ConfigUtil.enableBasicMode()) { Tt("保存或同步失败，请重试"); return; }
            MainActivity.self.readConfig();
            dialog.dismiss();
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create().show();
    }

    private void initView() {
        readConfig();
    }

    // 动态申请权限
    private void initPermission() {
        if (!BluetoothPermissions.canConnect(this)) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 1001);
        }
    }

    private void saveConfig() {
        if(SPUtils.isEnableModule==false)
        {
            Toast.makeText(this.getApplicationContext(),"请启用模块后再进行操作！",Toast.LENGTH_SHORT).show();
            return;
        }
        String text = editText.getText().toString();
        if (text.isEmpty()) {
            return;
        }
        try {
            int xinhao = Integer.parseInt(text);
            if (xinhao < -127 || xinhao >= 0) {
                Tt("请输入正确的数值(-127到-1)");
                return;
            }
        } catch (Exception ex) {
            Tt("请输入正确的数值(-127到-1)");
            return;
        }
        Tt(ConfigUtil.setString("rssi", text) ? "保存成功！" : "保存失败！");
    }

    private void PrivacyPolicy() {
        AlertDialog.Builder builder = AppDialogs.builder(this);
        builder.setMessage("本应用非常重视用户隐私政策并严格遵守相关的法律规定。请您仔细阅读《隐私政策》后再继续使用。如果您继续使用我们的服务，表示您已经充分阅读和理解我们协议的全部内容。\n本app尊重并保护所有使用服务用户的个人隐私权。为了给您提供更准确、更优质的服务，本应用会按照本隐私权政策的规定使用和披露您的个人信息。除本隐私权政策另有规定外，在未征得您事先许可的情况下，本应用不会将这些信息对外披露或向第三方提供。本应用会不时更新本隐私权政策。 您在同意本应用服务使用协议之时，即视为您已经同意本隐私权政策全部内容。\n" +
                "1. 适用范围\n" +
                "(a) 在您使用本软件时，软件所收集到的信息，包括手机蓝牙mac地址，位置信息\n" +
                "(b) 在您使用本软件时，软件所收集到的附近设备的信息\n" +
                "2. 信息使用\n" +
                "(a)本应用不会向任何无关第三方提供、出售、出租、分享或交易您的个人登录信息。如果我们存储发生维修或升级，我们会事先发出推送消息来通知您，请您提前允许本应用消息通知。\n" +
                "(b) 本应用亦不允许任何第三方以任何手段收集、编辑、出售或者无偿传播您的个人信息。任何本应用平台用户如从事上述活动，一经发现，本应用有权立即终止与该用户的服务协议。\n" +
                "(c) 为服务用户的目的，本应用可能通过使用您的个人信息，向您提供您感兴趣的信息，包括但不限于向您发出产品和服务信息，或者与本应用合作伙伴共享信息以便他们向您发送有关其产品和服务的信息（后者需要您的事先同意）。\n" +
                "3. 信息披露\n" +
                "在如下情况下，本应用将依据您的个人意愿或法律的规定全部或部分的披露您的个人信息：\n" +
                "(a) 未经您事先同意，我们不会向第三方披露；\n" +
                "(b)为提供您所要求的产品和服务，而必须和第三方分享您的个人信息；\n" +
                "(c) 根据法律的有关规定，或者行政或司法机构的要求，向第三方或者行政、司法机构披露；\n" +
                "(d) 如您出现违反中国有关法律、法规或者本应用服务协议或相关规则的情况，需要向第三方披露；\n" +
                "(e) 如您是适格的知识产权投诉人并已提起投诉，应被投诉人要求，向被投诉人披露，以便双方处理可能的权利纠纷；" +
                "4.本隐私政策的更改\n" +
                "(a)如果决定更改隐私政策，我们会在本政策中、本公司网站中以及我们认为适当的位置发布这些更改，以便您了解我们如何收集、使用您的个人信息，哪些人可以访问这些信息，以及在什么情况下我们会透露这些信息。\n" +
                "(b)本应用保留随时修改本政策的权利，因此请经常查看。如对本政策作出重大更改，本应用会通过网站通知的形式告知。\n" +
                "方披露自己的个人信息，如联络方式或者邮政地址。请您妥善保护自己的个人信息，仅在必要的情形下向他人提供。如您发现自己的个人信息泄密，尤其是本应用用户名及密码发生泄露，请您立即联络本应用客服，以便本应用采取相应措施。\n" +
                "感谢您花时间了解我们的隐私政策！我们将尽全力保护您的个人信息和合法权益，再次感谢您的信任！");
        builder.setCancelable(false);
        builder.setTitle("隐私协议");
        builder.setPositiveButton("同意", (dialog, which) -> dialog.dismiss());
        builder.setNegativeButton("拒绝", (dialog, which) -> {
            dialog.dismiss();
            System.exit(0);
        });
        builder.create().show();
    }


    private void openActList(boolean configuredOnly) {
        Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
        intent.putExtra(DeviceActivity.EXTRA_CONFIGURED_ONLY, configuredOnly);
        startActivity(intent);
    }

    String mac = "";

    private boolean IsEmptyOrNull(String data)
    {
        if(data==null)
        {
            return true;
        }
        return data.isEmpty();
    }

    private static void reboot() {
        try {

            String helpstr = "确认后将重新启动手机，请确保软件拥有root权限！";
            AlertDialog.Builder builder = AppDialogs.builder(self);
            builder.setMessage(helpstr);
            builder.setCancelable(false);
            builder.setTitle("是否重启手机");
            builder.setPositiveButton("确认",(dialog, which) -> {
                ConfigUtil.execRootCmdSilent("reboot");
                dialog.dismiss();
            });
            builder.setNegativeButton("取消", (dialog, which) -> {
                dialog.dismiss();
            });
            builder.create().show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private RssiSessionManager preview;
    private boolean resumed;

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (Adapter != null) readConfig();
    }
    @Override protected void onStop() {
        resumed = false;
        if (preview != null) preview.cancel();
        super.onStop();
    }
    @Override protected void onDestroy() {
        if (preview != null) preview.cancel();
        if (self == this) self = null;
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    public void readConfig() {
        if (Adapter == null) return;
        if (preview != null) preview.cancel();
        editText.setText(ConfigUtil.getString("rssi", "-50", 0));
        if (!isXpEnable()) { notXp(); return; }
        List<TrustedDevice> devices = ConfigUtil.getDevices(0);
        if (devices.isEmpty()) {
            if (ConfigUtil.BASE_MODE.equals(ConfigUtil.getString("mac", "", 0))) readBaseMode();
            else readNoDevice();
            return;
        }
        mac = devices.get(0).address;
        Adapter.txtAddress.setText("已配置 " + devices.size() + " 个解锁设备");
        StringBuilder details = new StringBuilder();
        DeviceMetadata metadata = new DeviceMetadata(this);
        for (TrustedDevice device : devices) {
            if (details.length() > 0) details.append("\n");
            String name = metadata.name(device.address);
            details.append(name.isEmpty() ? device.address : name).append(TrustedDevice.CONNECTED.equals(device.mode) ? " · 连接模式" : " · 距离模式");
        }
        Adapter.txtMac.setText(details.toString());
        Adapter.txtRssi.setText("尚未测量");
        Adapter.txtTime.setText("");
        Adapter.txtDesc.setText("点击管理设备");
        Adapter.txtDesc.setVisibility(View.VISIBLE);
        Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(Integer.MIN_VALUE));
    }

    @SuppressLint("MissingPermission")
    private void refreshSignals() {
        readConfig();
        if (!resumed || !isXpEnable() || preview == null) return;
        if (!BluetoothPermissions.canConnect(this)) { initPermission(); return; }
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth == null) { Tt("此设备不支持蓝牙"); return; }
        if (!bluetooth.isEnabled()) { startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return; }
        List<String> addresses = new ArrayList<>();
        List<TrustedDevice> devices = ConfigUtil.getDevices(0);
        for (TrustedDevice device : devices) {
            if (TrustedDevice.PROXIMITY.equals(device.mode)) addresses.add(device.address);
        }
        if (addresses.isEmpty()) {
            Adapter.txtRssi.setText("连接模式不测量距离");
            return;
        }
        Adapter.txtRssi.setText("正在测量…");
        // Threshold 0 means collect every device's measurement without early success.
        final int[] strongest = {Integer.MIN_VALUE};
        final String signature = TrustedDevice.encode(devices);
        preview.check(addresses, 0, new RssiSessionManager.Listener() {
            private boolean valid() { return resumed && signature.equals(TrustedDevice.encode(ConfigUtil.getDevices(0))); }
            public void measured(String address, int rssi) {
                if (!valid() || rssi <= strongest[0]) return;
                strongest[0] = rssi;
                Adapter.txtRssi.setText("最强信号 " + rssi + " dBm");
                Adapter.txtTime.setText(String.format(java.util.Locale.getDefault(), "约 %.2f m", BluetoothUtils.getInstance().getDistance(rssi)));
                Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(rssi));
            }
            public void finished(String matched) {
                if (valid() && strongest[0] == Integer.MIN_VALUE) Adapter.txtRssi.setText("无有效信号；设备可能离线或不支持 BLE");
            }
        });
    }

    private long lstClickTime=0;

    private void readBaseMode()
    {
        Adapter.txtAddress.setText("当前处于基础模式");
        Adapter.txtMac.setText("请前往系统设置设置解锁设备");
        Adapter.txtRssi.setText("");
        Adapter.txtTime.setText("");
        Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(0));
        Adapter.txtDesc.setVisibility(View.GONE);
    }
    private void notXp()
    {
        Adapter.txtAddress.setText("当前模块未启用");
        Adapter.txtMac.setText("请前往 LSPosed 中启用模块");
        Adapter.txtRssi.setText("");
        Adapter.txtTime.setText("");
        Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(0));
        Adapter.txtDesc.setVisibility(View.GONE);
    }

    private void readNoDevice()
    {
        Adapter.txtAddress.setText("尚未添加解锁设备");
        Adapter.txtMac.setText("从右上角添加配对设备，或手动输入地址");
        Adapter.txtRssi.setText("");
        Adapter.txtTime.setText("");
        Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(0));
        Adapter.txtDesc.setVisibility(View.GONE);
    }

}
