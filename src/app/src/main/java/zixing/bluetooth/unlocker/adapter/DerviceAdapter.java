package zixing.bluetooth.unlocker.adapter;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import zixing.bluetooth.unlocker.R;
import zixing.bluetooth.unlocker.adapter.base.BaseRecyclerViewAdapter;
import zixing.bluetooth.unlocker.bean.DeviceBean;

public class DerviceAdapter extends BaseRecyclerViewAdapter<DeviceBean> {

    public DerviceAdapter(Context context) {
        super(new ArrayList<>(),context);
    }
    @Override
    protected Animator[] getAnimators(View view) {
        if (view.getMeasuredHeight() <=0){
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.05f, 1.0f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.05f, 1.0f);
            return new ObjectAnimator[]{scaleX, scaleY};
        }
        return new Animator[]{
                ObjectAnimator.ofFloat(view, "scaleX", 1.05f, 1.0f),
                ObjectAnimator.ofFloat(view, "scaleY", 1.05f, 1.0f),
        };
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_dervice, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        super.onBindViewHolder(viewHolder, position);
        MyViewHolder holder = (MyViewHolder) viewHolder;
        DeviceBean data = list.get(position);
        if (data == null) return;
        String name = data.getName();
        holder.txtAddress.setText((name == null || name.isEmpty()) ? "未命名设备" : name);

        String addr = data.getAddress();
        holder.txtMac.setText((addr == null || addr.isEmpty()) ? "地址未知" : addr);
        if(data.hasRssi())
        {
            holder.txtRssi.setText(data.getRssi()+" dBm");
            holder.txtTime.setText(String.format("%.2f", data.getDistance())+"m");
        }
        else{
            holder.txtRssi.setText("信号未知");
            holder.txtTime.setText("距离未知");
        }
        holder.imageSignal.setImageResource(getRssiIcon(data.getRssi()));
        String state = data.getBondState() == 11 ? "配对中" : (data.isStatus() ? "已配对" : "未配对");
        for (zixing.bluetooth.unlocker.utils.TrustedDevice device : zixing.bluetooth.unlocker.utils.ConfigUtil.getDevices(0)) {
            if (device.address.equals(addr)) {
                state += zixing.bluetooth.unlocker.utils.TrustedDevice.CONNECTED.equals(device.mode) ? " · 连接解锁" : " · 距离解锁";
            }
        }
        holder.txtDesc.setText(state);
        holder.txtDesc.setVisibility(View.VISIBLE);
        animate(viewHolder, position);
    }

    public static int getRssiIcon(int rssi){
        if (!zixing.bluetooth.unlocker.utils.TrustedDevice.validRssi(rssi)) return R.mipmap.ic_rssi1;
        if (rssi >= -50){
            return R.mipmap.ic_rssi5;
        }else if (rssi >= -62){
            return R.mipmap.ic_rssi4;
        }else if (rssi >= -74){
            return R.mipmap.ic_rssi3;
        }else if (rssi >= -89){
            return R.mipmap.ic_rssi2;
        }else {
            return R.mipmap.ic_rssi1;
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.txtAddress)
        public TextView txtAddress;
        @BindView(R.id.txtMac )
        public TextView txtMac;
        @BindView(R.id.txtRssi)
        public TextView txtRssi;
        @BindView(R.id.txtTime)
        public TextView txtTime;
        @BindView(R.id.txtDesc)
        public TextView txtDesc;
        @BindView(R.id.imageSignal)
        public ImageView imageSignal;

        public MyViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }



}
