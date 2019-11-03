package org.fog.utils;

public class NeighborInArea {
    int id;
    long mips;
    int ram;
    long upBw;
    long downBw;
    double ratePerMips;
    double busyPower;
    double idlePower;
    public NeighborInArea(int id,long mips, int ram, long upBw, long downBw, double ratePerMips, double busyPower, double idlePower){
        this.id = id;
        this.mips = mips;
        this.ram = ram;
        this.upBw = upBw;
        this.downBw = downBw;
        this.ratePerMips = ratePerMips;
        this.busyPower = busyPower;
        this.idlePower = idlePower;
    }

    public void setId(int id){
        this.id = id;
    }
    public long getMips(){return mips;}
    public long getUpBw(){return upBw;}

    public long getDownBw() {
        return downBw;
    }

    public int getId() {
        return id;
    }

    public int getRam() {
        return ram;
    }

    public double getRatePerMips() {
        return ratePerMips;
    }

    public double getIdlePower() {
        return idlePower;
    }

    public double getBusyPower() {
        return busyPower;
    }
}
