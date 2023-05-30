package zixing.bluetooth.unlocker.utils;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;


import java.util.ArrayList;
import java.util.Set;

import zixing.bluetooth.unlocker.bean.DeviceBean;

public class BluetoothUtils {
    final String TAG = getClass().getName();
    private Context context;
    private static BluetoothUtils bluetoothInstance;
    private BluetoothAdapter bluetoothAdapter ;
    private BluetoothInterface bluetoothInterface;
    private BluetoothUtils (){}
    private String dev_mac_adress = "";
    ArrayList<DeviceBean> deviceBeans = new ArrayList<>();

    public static BluetoothUtils getInstance() {
        if (bluetoothInstance == null) {
            bluetoothInstance = new BluetoothUtils();
        }
        return bluetoothInstance;
    }

    public void initBluetooth(Context context){
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        registerBroadcas(context);

        BluetoothAdapter mDefaultBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mDefaultBluetoothAdapter.isEnabled() == false) {
            BluetoothUtils.getInstance().enable();
        }


    }

    @SuppressLint("MissingPermission")
    private void initBondBluetooth() {
        //获取蓝牙适配器
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //得到已匹配的蓝牙设备列表
        @SuppressLint("MissingPermission") Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        if(bondedDevices!=null && bondedDevices.size()>0)
        {
            for (BluetoothDevice device:bondedDevices
                 ) {
                    int rssi = 3;
                    DeviceBean bean=new DeviceBean();
                    bean.setAddress(device.getAddress());
                    bean.setRssi(rssi);
                    bean.setName(device.getName());
                    bean.setDistance(getDistance(rssi));
                    bean.setStatus(device.getBondState() == BluetoothDevice.BOND_BONDED);
                    deviceBeans.add(bean);
                    if(!dev_mac_adress.contains(device.getAddress())){
                        dev_mac_adress += device.getAddress();
                        bluetoothInterface.addBluetoothDervice(bean);
                    }else {
                        bluetoothInterface.updateBluetoothDervice(bean);
                    }
                    Log.e(TAG,device.getName()+"："+device.getAddress()+" "+rssi+"  "+bean.getDistance());
            }
        }
    }

    public  void setBluetoothListener(BluetoothInterface bluetoothInterface){
        this.bluetoothInterface = bluetoothInterface;
    }

    private void registerBroadcas(Context context){
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);//状态改变
//        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);//行动扫描模式改变了
//        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//动作状态发生了变化
        context.registerReceiver(bluetoothBroadcast, intent);
        Log.i(TAG,"registerReceiver");

    }

    public BroadcastReceiver bluetoothBroadcast = new BroadcastReceiver(){
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                /* 搜索结果 */
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getName()!= null){
                    int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0);
                    DeviceBean bean=new DeviceBean();
                    bean.setAddress(device.getAddress());
                    bean.setRssi(rssi);
                    bean.setName(device.getName());
                    bean.setDistance(getDistance(rssi));
                    bean.setStatus(device.getBondState() == BluetoothDevice.BOND_BONDED);
                    deviceBeans.add(bean);
                    if(!dev_mac_adress.contains(device.getAddress())){
                        dev_mac_adress += device.getAddress();
                        bluetoothInterface.addBluetoothDervice(bean);
                    }else {
                        bluetoothInterface.updateBluetoothDervice(bean);
                    }
                    Log.e(TAG,device.getName()+"："+device.getAddress()+" "+rssi+"  "+bean.getDistance());
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())){
                //正在搜索
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())){
                // 搜索完成
                dev_mac_adress = "";
                bluetoothInterface.onBluetoothFinish();
                deviceBeans.clear();
            }
        }
    };


    /** 开启蓝牙 */
    @SuppressLint("MissingPermission")
    public void enable(){
        if (bluetoothAdapter !=null && !bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
    }
    /** 关闭蓝牙 */
    @SuppressLint("MissingPermission")
    public void disable(){
        if (bluetoothAdapter !=null && bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
    }

    /** 取消搜索 */
    @SuppressLint("MissingPermission")
    public void cancelDiscovery(){
        if(isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
    }

    /** 开始搜索 */
    @SuppressLint("MissingPermission")
    public void startDiscovery(){
        initBondBluetooth();
        if (bluetoothAdapter !=null && bluetoothAdapter.isEnabled()){
            bluetoothAdapter.startDiscovery();
        }
    }

    /** 判断蓝牙是否打开 */
    public boolean isEnabled(){
        if (bluetoothAdapter !=null){
            return bluetoothAdapter.isEnabled();
        }
        return false;
    }
    /** 判断当前是否正在查找设备，是返回true */
    @SuppressLint("MissingPermission")
    public boolean isDiscovering(){
        if (bluetoothAdapter !=null){
            return bluetoothAdapter.isDiscovering();
        }
        return false;
    }

    public void onDestroy(){
        cancelDiscovery();
       if(context!=null)
       {
           context.unregisterReceiver(bluetoothBroadcast);
       }
       bluetoothInstance = null;
    }

    public interface BluetoothInterface{
        void addBluetoothDervice(DeviceBean deviceBeans);
        void updateBluetoothDervice(DeviceBean deviceBeans);
        void onBluetoothFinish();
    }

    /**
     * 根据rssi值估算出距离
     * @param rssi 信号强度
     * @return 距离(单位：米)
     */

    public double getDistance(int rssi) {
        double RSSI_1M=50.0;//发射端和接收端相隔1米时的信号强度
        double N_VALUE=2.5;//环境衰减因子
        double rssiAbs = Math.abs(rssi);
        double power = (rssiAbs - RSSI_1M) / (10 * N_VALUE);
        return Math.pow(10.0, power);
    }
}
