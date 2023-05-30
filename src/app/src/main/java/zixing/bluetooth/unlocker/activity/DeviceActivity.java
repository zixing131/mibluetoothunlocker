package zixing.bluetooth.unlocker.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        adapter.update(deviceBeans);//刷新设备
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
                        ConfigUtil.setString("mac",bean.getAddress());
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

}
