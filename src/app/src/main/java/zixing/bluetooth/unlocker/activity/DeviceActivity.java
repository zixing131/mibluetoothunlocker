package zixing.bluetooth.unlocker.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import butterknife.BindView;
import butterknife.OnClick;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import zixing.bluetooth.unlocker.R;
import zixing.bluetooth.unlocker.adapter.DerviceAdapter;
import zixing.bluetooth.unlocker.adapter.base.BaseRecyclerViewAdapter;
import zixing.bluetooth.unlocker.bean.DeviceBean;
import zixing.bluetooth.unlocker.utils.*;
import zixing.bluetooth.unlocker.UnlockerApp;

public class DeviceActivity extends BaseActivity implements BluetoothUtils.BluetoothInterface {
    public static final String EXTRA_CONFIGURED_ONLY = "configured_only";
    @BindView(R.id.recyclerview) RecyclerView recyclerView;
    @BindView(R.id.progress) ProgressBar progress;
    private DerviceAdapter adapter;
    private BluetoothUtils bluetooth;
    private DeviceMetadata metadata;
    private boolean strongestFirst = true;
    private boolean configuredOnly;
    private Chip hideUnnamed, commonOnly;

    @Override public int getLayoutId() { return R.layout.activity_add_device; }
    @Override public void initToolbar() {
        super.initToolbar();
        configuredOnly = getIntent().getBooleanExtra(EXTRA_CONFIGURED_ONLY, false);
        txtCenterTitle.setText(configuredOnly ? "管理解锁设备" : "添加配对设备");
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        metadata = new DeviceMetadata(this);
        adapter = new DerviceAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        bluetooth = BluetoothUtils.getInstance();
        bluetooth.initBluetooth(this);
        bluetooth.setBluetoothListener(this);
        hideUnnamed = findViewById(R.id.filter_named);
        commonOnly = findViewById(R.id.filter_common);
        findViewById(R.id.device_filters).setVisibility(configuredOnly ? View.GONE : View.VISIBLE);
        ((TextView) findViewById(R.id.list_hint)).setText(configuredOnly
                ? "仅显示已配置设备，刷新不会添加其他设备"
                : "刷新搜索附近设备 · 常用默认包含已配对和已配置设备");
        if (state != null) {
            hideUnnamed.setChecked(state.getBoolean("hide_unnamed"));
            commonOnly.setChecked(state.getBoolean("common_only"));
            strongestFirst = state.getBoolean("strongest_first", true);
        }
        hideUnnamed.setOnCheckedChangeListener((button, checked) -> refresh());
        commonOnly.setOnCheckedChangeListener((button, checked) -> refresh());
        adapter.setOnInViewClickListener(R.id.itemCartView, new BaseRecyclerViewAdapter.onInternalClickListener<DeviceBean>() {
            @Override public void OnClickListener(View p, View v, Integer position, DeviceBean bean) { showDevice(bean); }
            @Override public void OnLongClickListener(View p, View v, Integer position, DeviceBean bean) { showDevice(bean); }
        });
        progress.setVisibility(View.GONE);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("hide_unnamed", hideUnnamed.isChecked());
        state.putBoolean("common_only", commonOnly.isChecked());
        state.putBoolean("strongest_first", strongestFirst);
        super.onSaveInstanceState(state);
    }
    @Override protected void onResume() { super.onResume(); bluetooth.loadKnownDevices(); refresh(); }
    @Override protected void onStop() { bluetooth.cancelDiscovery(); super.onStop(); }
    @Override protected void onDestroy() { bluetooth.onDestroy(); super.onDestroy(); }
    @Override public void addBluetoothDervice(DeviceBean bean) { refresh(); }
    @Override public void updateBluetoothDervice(DeviceBean bean) { refresh(); }
    @Override public void onBluetoothFinish() { progress.setVisibility(View.GONE); refresh(); }
    @Override public void onBluetoothMessage(String message) { Tt(message); refresh(); }
    private TrustedDevice configured(String address) {
        for (TrustedDevice device : ConfigUtil.getDevices(0)) if (device.address.equals(address)) return device;
        return null;
    }
    private void refresh() {
        if (hideUnnamed == null) return;
        List<DeviceBean> list = bluetooth.getDeviceBeans();
        Set<String> configured = new HashSet<>();
        for (TrustedDevice device : ConfigUtil.getDevices(0)) configured.add(device.address);
        list.removeIf(bean -> !DeviceListPolicy.visible(configuredOnly, configured.contains(bean.getAddress()),
                hideUnnamed.isChecked(), bean.getName(), commonOnly.isChecked(),
                metadata.isCommon(bean.getAddress(), bean.isStatus() || configured.contains(bean.getAddress()))));
        Comparator<DeviceBean> order = Comparator.comparingInt(bean -> bean.hasRssi() ? bean.getRssi() : -1000);
        if (strongestFirst) order = order.reversed();
        list.sort(order.thenComparing(DeviceBean::getAddress));
        adapter.setList(list);
        TextView empty = findViewById(R.id.empty_devices);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(configuredOnly ? "还没有配置解锁设备\n请从主页右上角「添加配对设备」添加"
                : hideUnnamed.isChecked() || commonOnly.isChecked() ? "没有符合筛选条件的设备\n可关闭筛选，或刷新重新搜索"
                : "暂未发现设备\n点击右下角刷新，开始搜索");
    }
    @android.annotation.SuppressLint("MissingPermission")
    @OnClick(R.id.fab) public void onClick(View view) {
        bluetooth.loadKnownDevices();
        if (configuredOnly && ConfigUtil.getDevices(0).isEmpty()) { refresh(); return; }
        if (!BluetoothPermissions.canScan(this)) {
            ActivityCompat.requestPermissions(this, BluetoothPermissions.required(), 1001); return;
        }
        if (!bluetooth.isEnabled()) { startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return; }
        if (bluetooth.isDiscovering()) { bluetooth.cancelDiscovery(); return; }
        if (bluetooth.startDiscovery()) progress.setVisibility(View.VISIBLE);
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 1001) {
            bluetooth.loadKnownDevices();
            Tt(BluetoothPermissions.canScan(this) ? "权限已授予，点击刷新开始" : "未获得搜索权限，可在系统设置中授权");
        }
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) { getMenuInflater().inflate(R.menu.sort_menu, menu); return true; }
    @Override public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() != R.id.id_menu_sort) return false;
        strongestFirst = !strongestFirst; refresh();
        Tt(strongestFirst ? "按信号从强到弱排序" : "按信号从弱到强排序"); return true;
    }
    private void showDevice(DeviceBean bean) {
        String address = TrustedDevice.normalizeAddress(bean.getAddress());
        if (address.isEmpty()) { Tt("设备地址无效"); return; }
        bluetooth.cancelDiscovery();
        boolean saved = configured(address) != null;
        List<AppDialogs.Choice> choices = new ArrayList<>();
        choices.add(new AppDialogs.Choice(saved ? "修改解锁方式" : "添加为解锁设备", "设置信号阈值检测或已连接解锁", false, false, () -> chooseMode(bean)));
        choices.add(new AppDialogs.Choice(bean.isStatus() ? "系统蓝牙设置" : "配对此设备",
                bean.isStatus() ? "管理连接与系统配对" : "通过系统确认完成配对", false, false, () -> {
                    if (bean.isStatus()) startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
                    else if (!BluetoothPermissions.canConnect(this)) ActivityCompat.requestPermissions(this, BluetoothPermissions.required(), 1001);
                    else bluetooth.pair(address);
                }));
        choices.add(new AppDialogs.Choice("备注名称", "设置易辨认的名称，仅保存在本机", false, false, () -> rename(bean)));
        boolean common = metadata.isCommon(address, saved || bean.isStatus());
        choices.add(new AppDialogs.Choice(common ? "设为不常用" : "设为常用",
                "用于添加页面的「仅常用」筛选，不影响解锁", false, false, () -> {
                    metadata.setCommon(address, !common); refresh();
                }));
        if (saved) choices.add(new AppDialogs.Choice("移除解锁设备", "保留系统配对，停止此设备的解锁检测", false, true, () ->
                AppDialogs.builder(this).setTitle("移除解锁设备？").setMessage(displayName(bean) + "\n系统配对将保留。")
                        .setPositiveButton("移除", (d, w) -> { Tt(ConfigUtil.removeDevice(address) ? "已移除" : "保存失败，请重试"); refresh(); })
                        .setNegativeButton("保留", null).show()));
        if (bean.isStatus()) choices.add(new AppDialogs.Choice("取消系统配对", "同时从解锁列表移除，需要重新配对才能连接", false, true, () -> confirmUnpair(bean)));
        AppDialogs.choices(this, displayName(bean), address, choices);
    }
    private String displayName(DeviceBean bean) {
        String name = DeviceListPolicy.usableName(bean.getName());
        return name.isEmpty() ? "未命名设备" : name;
    }
    private void rename(DeviceBean bean) {
        EditText input = new EditText(this);
        input.setSingleLine(true); input.setHint("例如：我的手环");
        input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(60)});
        input.setText(DeviceListPolicy.usableName(bean.getName()));
        LinearLayout field = new LinearLayout(this);
        int pad = AppDialogs.dp(this, 24); field.setPadding(pad, AppDialogs.dp(this, 8), pad, 0);
        field.addView(input, new LinearLayout.LayoutParams(-1, -2));
        AppDialogs.builder(this).setTitle("备注名称").setMessage("留空恢复系统名称或广播名称").setView(field)
                .setPositiveButton("保存", (d, w) -> {
                    metadata.setLabel(bean.getAddress(), input.getText().toString());
                    bluetooth.loadKnownDevices(); refresh();
                }).setNegativeButton("取消", null).show();
    }
    private void chooseMode(DeviceBean bean) {
        if (!UnlockerApp.isModuleEnabled()) { Tt("请先在 LSPosed 中启用模块"); return; }
        TrustedDevice current = configured(bean.getAddress());
        List<AppDialogs.Choice> choices = new ArrayList<>();
        choices.add(new AppDialogs.Choice("信号阈值解锁", "适用于 BLE 手环、手表；两次有效信号达到主页阈值才解锁",
                current != null && TrustedDevice.PROXIMITY.equals(current.mode), false, () -> saveMode(bean, TrustedDevice.PROXIMITY)));
        choices.add(new AppDialogs.Choice("已连接解锁", "适用于眼镜、耳机等；须已配对且实际连接，不检查距离",
                current != null && TrustedDevice.CONNECTED.equals(current.mode), false, () -> saveMode(bean, TrustedDevice.CONNECTED)));
        AppDialogs.choices(this, "选择解锁方式", displayName(bean) + " · 任一配置设备满足条件即可解锁", choices);
    }
    private void saveMode(DeviceBean bean, String mode) {
        if (TrustedDevice.CONNECTED.equals(mode) && !bean.isStatus()) { Tt("请先完成系统配对，再选择已连接解锁"); return; }
        boolean saved = ConfigUtil.putDevice(bean.getAddress(), mode);
        Tt(saved ? "已保存解锁方式" : "保存或同步失败，请重试"); refresh();
    }
    private void confirmUnpair(DeviceBean bean) {
        AppDialogs.builder(this).setTitle("取消系统配对？")
                .setMessage("将取消系统配对，并从解锁列表移除此设备。")
                .setPositiveButton("取消配对", (dialog, which) -> {
                    if (bluetooth.removeBond(bean.getAddress())) Tt("正在取消配对");
                    else { Tt("请在系统蓝牙设置中取消配对"); startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
                }).setNegativeButton("保留", null).show();
    }
}
