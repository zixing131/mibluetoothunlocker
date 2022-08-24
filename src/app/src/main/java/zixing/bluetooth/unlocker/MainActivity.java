package zixing.bluetooth.unlocker;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class MainActivity extends AppCompatActivity {

    private static void myLog(String msg) {
        try {
            Log.i("hookhelper",msg);
        }
        catch (Exception ex)
        {

        }
    }

    private List<String> mPermissionList = new ArrayList<>();

    // 动态申请权限
    private void initPermission(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            // Android 版本大于等于 Android12 时
            // 只包括蓝牙这部分的权限，其余的需要什么权限自己添加
            mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            mPermissionList.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            // Android 版本小于 Android12 及以下版本
            mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if(mPermissionList.size() > 0){
            ActivityCompat.requestPermissions(this,mPermissionList.toArray(new String[0]),1001);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 有权限没有通过
        boolean hasPermissionDismiss = false;
        if(1001 == requestCode){
            for(int i = 0; i < grantResults.length; i++){
                if(grantResults[i] == -1){
                    hasPermissionDismiss = true;
                    break;
                }
            }
        }
        if(hasPermissionDismiss){
            // 有权限未通过的处理
        } else {
            //权限全部通过的处理
        }
    }


    public static MainActivity self = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        self = this;
        initPermission();
        XSPUtils.initXSP(getApplicationContext());

        Button buttonpri=findViewById(R.id.buttonpri);

        buttonpri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(self);
                builder.setMessage("本应用非常重视用户隐私政策并严格遵守相关的法律规定。请您仔细阅读《隐私政策》后再继续使用。如果您继续使用我们的服务，表示您已经充分阅读和理解我们协议的全部内容。\n本app尊重并保护所有使用服务用户的个人隐私权。为了给您提供更准确、更优质的服务，本应用会按照本隐私权政策的规定使用和披露您的个人信息。除本隐私权政策另有规定外，在未征得您事先许可的情况下，本应用不会将这些信息对外披露或向第三方提供。本应用会不时更新本隐私权政策。 您在同意本应用服务使用协议之时，即视为您已经同意本隐私权政策全部内容。\n" +
                        "1. 适用范围\n" +
                        "(a) 在您使用本软件时，软件所收集到的信息，包括手机蓝牙mac地址，位置信息\n" +
                        "(b) 在您使用本软件时，软件所收集到的附近设备的信息\n" +
                        "2. 信息使用\n" +
                        "(a)本应用不会向任何无关第三方提供、出售、出租、分享或交易您的个人登录信息。如果我们存储发生维修或升级，我们会事先发出推送消息来通知您，请您提前允许本应用消息通知。\n" +
                        "(b) 本应用亦不允许任何第三方以任何手段收集、编辑、出售或者无偿传播您的个人信息。任何本应用平台用户如从事上述活动，一经发现，本应用有权立即终止与该用户的服务协议。\n" +
                        "(c) 为服务用户的目的，本应用可能通过使用您的个人信息，向您提供您感兴趣的信息，包括但不限于向您发出产品和服务信息，或者与本应用合作伙伴共享信息以便他们向您发送有关其产品和服务的信息（后者需要您的事先同意）。\n"+
                        "3. 信息披露\n" +
                        "在如下情况下，本应用将依据您的个人意愿或法律的规定全部或部分的披露您的个人信息：\n" +
                        "(a) 未经您事先同意，我们不会向第三方披露；\n" +
                        "(b)为提供您所要求的产品和服务，而必须和第三方分享您的个人信息；\n" +
                        "(c) 根据法律的有关规定，或者行政或司法机构的要求，向第三方或者行政、司法机构披露；\n" +
                        "(d) 如您出现违反中国有关法律、法规或者本应用服务协议或相关规则的情况，需要向第三方披露；\n" +
                        "(e) 如您是适格的知识产权投诉人并已提起投诉，应被投诉人要求，向被投诉人披露，以便双方处理可能的权利纠纷；"+
                        "4.本隐私政策的更改\n" +
                        "(a)如果决定更改隐私政策，我们会在本政策中、本公司网站中以及我们认为适当的位置发布这些更改，以便您了解我们如何收集、使用您的个人信息，哪些人可以访问这些信息，以及在什么情况下我们会透露这些信息。\n" +
                        "(b)本应用保留随时修改本政策的权利，因此请经常查看。如对本政策作出重大更改，本应用会通过网站通知的形式告知。\n" +
                        "方披露自己的个人信息，如联络方式或者邮政地址。请您妥善保护自己的个人信息，仅在必要的情形下向他人提供。如您发现自己的个人信息泄密，尤其是本应用用户名及密码发生泄露，请您立即联络本应用客服，以便本应用采取相应措施。\n" +
                        "感谢您花时间了解我们的隐私政策！我们将尽全力保护您的个人信息和合法权益，再次感谢您的信任！");
                builder.setCancelable(false);
                builder.setTitle("隐私协议");
                builder.setPositiveButton("同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                    }
                });
                builder.setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        self.finish();
                    }
                });
                builder.create().show();
            }
        });


        Button button=findViewById(R.id.button);
        EditText editText=findViewById(R.id.editText);
        try {
            String rssi = XSPUtils.getString("rssi", "-50",0);
            editText.setText(rssi);
        }catch (Exception ex)
        {

        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    /*
                    if(BluetoothHelper.CanUnlockByBluetooth(MainActivity.self.getApplicationContext(),"80:CF:A2:EA:A8:9B",  this.getClass().getClassLoader(),0) == true)
                    {
                        Toast.makeText(MainActivity.this,"成功！",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        Toast.makeText(MainActivity.this,"失败！",Toast.LENGTH_SHORT).show();
                    }

                     */

                    String text = editText.getText().toString();
                    int xinhao =  Integer.parseInt(text);
                    if(xinhao>=-128&&xinhao<=0)
                    {
                        if(XSPUtils.setString("rssi", text))
                        {
                            Toast.makeText(MainActivity.this,"保存成功！",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(MainActivity.this,"保存失败！",Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(MainActivity.this,"请输入正确的数值(-128到0)",Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception ex)
                {
                    myLog("发生错误："+ex.toString());
                    Toast.makeText(MainActivity.this,"请启用插件并重启后再设置！",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}