package zixing.bluetooth.unlocker.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.annotation.SuppressLint;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.Manifest;

import butterknife.BindView;
import butterknife.OnClick;
import zixing.bluetooth.unlocker.R;
import zixing.bluetooth.unlocker.adapter.DerviceAdapter;
import zixing.bluetooth.unlocker.adapter.base.BaseRecyclerViewAdapter;
import zixing.bluetooth.unlocker.bean.DeviceBean;
import zixing.bluetooth.unlocker.utils.ArrUtils;
import zixing.bluetooth.unlocker.utils.BluetoothUtils;
import zixing.bluetooth.unlocker.utils.ConfigUtil;
import zixing.bluetooth.unlocker.utils.SPUtils;

public class DeviceActivity extends BaseActivity implements BluetoothUtils.BluetoothInterface{

    @BindView(R.id.recyclerview) RecyclerView mRecyclerView;
    @BindView(R.id.progress) ProgressBar progress;
    @BindView(R.id.fab) FloatingActionButton fab;
    private DerviceAdapter adapter;
    private String sortMode="ASC";

    @Override
    public int getLayoutId() {
        return R.layout.activity_add_device;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        if(!BluetoothUtils.getInstance().isEnabled())
        {
            BluetoothUtils.getInstance().enable();
        }
    }

    @Override
    public void initToolbar() {
        super.initToolbar();
        txtCenterTitle.setText("选择解锁设备");
    }

    @Override
    public void addBluetoothDervice(DeviceBean deviceBeans) {
        adapter.add(deviceBeans);//添加设备
    }

    @Override
    public void updateBluetoothDervice(DeviceBean deviceBeans) {
        if (deviceBeans == null) {
            adapter.setList(BluetoothUtils.getInstance().getDeviceBeans());
        } else {
            adapter.update(deviceBeans);//刷新设备
        }
    }

    @Override
    public void onBluetoothFinish() {
        //搜索结束
        progress.setVisibility(View.GONE);
        int num=adapter.getItemCount();
        Tt("成功搜索到"+num+"个设备");
        if(num>0){
            sortMode="ASC";
            sort();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销广播
        BluetoothUtils.getInstance().onDestroy();
    }

    @OnClick({R.id.fab})
    public void onClick(View view) {
        if(BluetoothUtils.getInstance().isDiscovering()){
            return;
        }
        Tt("开始搜索...");
        adapter.clear();//清空搜索历史
        progress.setVisibility(View.VISIBLE);
        BluetoothUtils.getInstance().startDiscovery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sort_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        sort();
        return true;
    }

    private void initView() {
        //注册广播
        BluetoothUtils.getInstance().initBluetooth(this);
        //绑定搜索数据回调
        BluetoothUtils.getInstance().setBluetoothListener(this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);
        adapter = new DerviceAdapter(this);

        mRecyclerView.setAdapter(adapter);
        DeviceActivity self=this;

        adapter.setOnInViewClickListener(R.id.itemCartView, new BaseRecyclerViewAdapter.onInternalClickListener<DeviceBean>() {
            @Override
            public void OnClickListener(View parentV, View v, Integer position, DeviceBean values) {
                DeviceBean bean=(DeviceBean) values;
                String devicename=bean.getName()+"【"+bean.getAddress()+"】";

                self.runOnUiThread(()->{
                    if(SPUtils.isEnableModule==false)
                    {
                        Toast.makeText(self.getApplicationContext(),"请启用模块后再进行操作！",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(self);
                    builder.setMessage("是否选择 "+devicename +"作为解锁设备？");
                    builder.setCancelable(false);
                    builder.setTitle("设备选择");
                    builder.setPositiveButton("确定", (dialog, which) -> {
                        dialog.dismiss();
                        //这里保存数据
                        // 5.1.1：名称为 Unknown 的设备其 MAC 可能为空，
                        // 空地址写入配置会导致后续读取/解锁异常，这里先拦截
                        if (bean.getAddress() == null || bean.getAddress().isEmpty()) {
                            Toast.makeText(self, "设备地址无效，无法选择", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        ConfigUtil.setString("mac", bean.getAddress());
                        self.finish();
                        MainActivity.self.runOnUiThread(()->{
                            MainActivity.self. readConfig();
                        });
                    });
                    builder.setNegativeButton("取消", (dialog, which) -> {
                        dialog.dismiss();
                    });
                    builder.create().show();
                });

            }

            @Override
            public void OnLongClickListener(View parentV, View v, Integer position, DeviceBean values) {
                DeviceBean bean = (DeviceBean) values;
                String devicename = bean.getName() + "【" + bean.getAddress() + "】";
                self.runOnUiThread(() -> {
                    new AlertDialog.Builder(self)
                            .setTitle("取消配对")
                            .setMessage("确定要取消与 " + devicename + " 的蓝牙配对吗？")
                            .setPositiveButton("确定", (dialog, which) -> {
                                dialog.dismiss();
                                removeBond(bean.getAddress());
                            })
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                });
            }
        });

        progress.setVisibility(View.VISIBLE);
        //开始搜索
        BluetoothUtils.getInstance().startDiscovery();
    }

    //排序
    private void sort() {
        if(adapter.getItemCount()==0 || BluetoothUtils.getInstance().isDiscovering())return;
        String mode=sortMode.equals("ASC")?"DESC":"ASC";
        ArrUtils.sortList(adapter.getList(),"rssi",mode);
        sortMode=mode;
        adapter.notifyDataSetChanged();
    }

    @SuppressLint("MissingPermission")
    private void removeBond(String address) {
        if (address == null || address.isEmpty()) {
            Toast.makeText(this, "设备地址无效", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "缺少蓝牙权限，无法取消配对", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Toast.makeText(this, "未找到设备", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean result = device.removeBond();
        if (result) {
            Toast.makeText(this, "正在取消配对...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "取消配对失败", Toast.LENGTH_SHORT).show();
        }
    }

}
