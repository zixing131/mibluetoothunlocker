package zixing.bluetooth.unlocker.bean;

public class DeviceBean {

    private String name;
    private String address;
    private int rssi = Integer.MIN_VALUE;
    private int bondState = 10;
    private int time;
    private boolean status;
    private double distance;

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
    public boolean hasRssi() { return zixing.bluetooth.unlocker.utils.TrustedDevice.validRssi(rssi); }
    public int getBondState() { return bondState; }
    public void setBondState(int state) { bondState = state; status = state == 12; }
    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }
}
