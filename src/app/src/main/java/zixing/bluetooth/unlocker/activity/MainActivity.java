package zixing.bluetooth.unlocker.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import zixing.bluetooth.unlocker.R;
import zixing.bluetooth.unlocker.Xp.MyXp;
import zixing.bluetooth.unlocker.adapter.DerviceAdapter;
import zixing.bluetooth.unlocker.adapter.DerviceAdapter$MyViewHolder_ViewBinding;
import zixing.bluetooth.unlocker.bean.DeviceBean;
import zixing.bluetooth.unlocker.utils.BluetoothUtils;
import zixing.bluetooth.unlocker.utils.XSPUtils;

/**
 * Author:紫星
 * Date:2022/12/03
 * Time:15:15
 * QQ:1311817771
 * E-mail:1311817771@qq.com
 */

public class MainActivity extends BaseActivity  {
    @BindView(R.id.editText)
    TextInputEditText editText;
    @BindView(R.id.btnSave)
    Button btnSave;

    private List<String> mPermissionList = new ArrayList<>();
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 有权限没有通过
        boolean hasPermissionDismiss = false;
        if (1001 == requestCode) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        if (hasPermissionDismiss) {
            // 有权限未通过的处理
        } else {
            //权限全部通过的处理
        }
    }

    private void ShowHelp() {
        String helpstr = "手环解锁工具\n启用后勾选设置[com.android.setting]和系统界面[com.android.systemui]\n然后重启手机即可，注意：使用本插件可能会降低系统安全性！\n信号阈值为负数，越小越灵敏(越容易被解锁)\n注意：更新软件后，可能要重启设备才会生效";
        AlertDialog.Builder builder = new AlertDialog.Builder(self);
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
        initPermission();
        self = this;
        XSPUtils.initXSP(getApplicationContext());
        View view = this.findViewById(R.id.itemdevicemain);
        Adapter = new DerviceAdapter.MyViewHolder(view);
        if(!BluetoothUtils.getInstance().isEnabled())
        {
            BluetoothUtils.getInstance().enable();
        }
        initView();
    }

    @OnClick({R.id.btnSave,R.id.fabmain})
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.btnSave:
                saveConfig();
                break;
            case  R.id.fabmain:
                long nowtime = System.currentTimeMillis();
                if(nowtime - lstClickTime< 5000)
                {
                    Tt("请不要频繁点击刷新！");
                    return;
                }
                lstClickTime=nowtime;
                Tt("开始刷新...");
                readConfig();
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
                openActList();
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("请输入自定义的mac地址 (如 12:B4:8E:66:99:AA ) ").setView(inputServer)

                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                });

        builder.setCancelable(false).setPositiveButton("确定", new DialogInterface.OnClickListener() {
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
                                XSPUtils.setString("mac",mac.toUpperCase());
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
        String helpstr = "确认后将清除软件绑定的解锁设备，请使用系统自带的蓝牙解锁设备绑定功能！";
        AlertDialog.Builder builder = new AlertDialog.Builder(self);
        builder.setMessage(helpstr);
        builder.setCancelable(false);
        builder.setTitle("是否启用基础模式");
        builder.setPositiveButton("确认",(dialog, which) -> {
            XSPUtils.setString("mac",XSPUtils.BASE_MODE);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //权限列表?
            mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            mPermissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            //权限列表?
            mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            mPermissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (mPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(this, mPermissionList.toArray(new String[0]), 1001);
        }
    }

    private void saveConfig() {
        String text = editText.getText().toString();
        if (text.isEmpty()) return;
        try {
            int xinhao = Integer.parseInt(text);
            if (xinhao < -128 || xinhao > 0) {
                Tt("请输入正确的数值(-128到0)");
                return;
            }
        } catch (Exception ex) {
            Tt("请输入正确的数值(-128到0)");
            return;
        }
        Tt(XSPUtils.setString("rssi", text) ? "保存成功！" : "保存失败！");
    }

    private void PrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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


    private void openActList() {
        Intent intent = new Intent(MainActivity.this, DeviceActivity.class);
        startActivity(intent);
    }

    static BluetoothGatt bluetoothGattInstance = null;
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
            XSPUtils.execRootCmdSilent("reboot");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    public void readConfig() {
        try {
            String text = XSPUtils.getString("rssi", "-50", 0);
            XSPUtils.execRootCmdSilent("");
            editText.setText(text);

            mac = XSPUtils.getString("mac", "", 0);
            if(XSPUtils.BASE_MODE.equals(mac))
            {
                readBaseMode();
                return;
            }
            if (!IsEmptyOrNull(mac)) {

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    initPermission();
                }
                BluetoothAdapter mDefaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mDefaultBluetoothAdapter.isEnabled() == false) {
                    BluetoothUtils.getInstance().enable();
                }
                BluetoothDevice device = mDefaultBluetoothAdapter.getRemoteDevice(mac);
                DeviceBean bean=new DeviceBean();
                bean.setAddress(device.getAddress());
                bean.setName(device.getName());
                bean.setStatus(device.getBondState() == BluetoothDevice.BOND_BONDED);
                DeviceBean data= bean;
                if (data == null) return;
                Adapter.txtAddress.setText(IsEmptyOrNull(data.getName())?"Unknown":data.getName());
                Adapter.txtMac.setText(IsEmptyOrNull (data.getAddress())?"Unknown":data.getAddress());
                Adapter.txtRssi.setText("");
                Adapter.txtTime.setText("");
                Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(-120));
                Adapter.txtDesc.setVisibility(data.isStatus()?View.VISIBLE:View.GONE);
                boolean rdRemoteRssi=false;
                if(bluetoothGattInstance==null)
                {
                    initbluetoothGattInstance (mac);
                }
                Thread.yield();
                Thread.sleep(100);
                Thread.yield();
                rdRemoteRssi = bluetoothGattInstance.readRemoteRssi();

                if(!rdRemoteRssi)
                {
                    myLog("第一次读取rssi失败，重试一次 ");
                    bluetoothGattInstance.close();
                    Thread.yield();
                    Thread.sleep(100);
                    Thread.yield();
                    initbluetoothGattInstance(mac);
                    Thread.yield();
                    Thread.sleep(100);
                    Thread.yield();
                    rdRemoteRssi = bluetoothGattInstance.readRemoteRssi();
                    if(!rdRemoteRssi)
                    {
                        Tt("读取绑定设备的信号强度失败！");
                        myLog("二次读取rssi失败 ");
                    }
                }
            }else{
                readBaseMode();
            }

        } catch (Exception e) {
            e.printStackTrace();
            readNoDevice();
        }
    }

    @SuppressLint("MissingPermission")
    private  void initbluetoothGattInstance(String mac)
    {
            BluetoothAdapter mDefaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = mDefaultBluetoothAdapter.getRemoteDevice(mac);
            bluetoothGattInstance = device.connectGatt(this.getApplicationContext(), true, new BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    myLog("信号强度："+rssi);
                    super.onReadRemoteRssi(gatt, rssi, status);
                    self.runOnUiThread(()->{
                        Adapter.txtRssi.setText(rssi+"dB");
                        Adapter.txtTime.setText(String.format("%.2f",  BluetoothUtils.getInstance().getDistance(rssi))+"m");
                        Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(rssi));
                    });
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

    private void readNoDevice()
    {
        Adapter.txtAddress.setText("未读取到设备");
        Adapter.txtMac.setText("Unknown");
        Adapter.txtRssi.setText("");
        Adapter.txtTime.setText("");
        Adapter.imageSignal.setImageResource(DerviceAdapter.getRssiIcon(0));
        Adapter.txtDesc.setVisibility(View.GONE);
    }

}