package zixing.bluetooth.unlocker;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

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