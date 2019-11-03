package org.fog.utils;

public class tupleToNeighbor {
    protected double cpuLength;
    protected double fileSize;
    protected double predictTime;
    protected String moduleName;
    protected int cloudletId;
    protected boolean canBe = false;
    protected double neighborTime = -1;

    public tupleToNeighbor(int cloudletId, double cpuLength, double fileSize, double predictTime, String moduleName){
        this.cloudletId = cloudletId;
        this.cpuLength = cpuLength;
        this.fileSize = fileSize;
        this.predictTime = predictTime;
        this.moduleName = moduleName;
    }

    public int getCloudletId(){ return cloudletId;}

    public double getCpuLength() {
        return cpuLength;
    }

    public double getFileSize() {
        return fileSize;
    }

    public void setPredictTime(double predictTime){ this.predictTime = predictTime;}

    public double getPredictTime(){
        return predictTime;
    }

    public String getModuleName(){
        return moduleName;
    }

    public void setCanBe(boolean canbe) { this.canBe =canbe;}

    public boolean getCanBe(){return  canBe;}

    public void setNeighborTime(double neighborTime){this.neighborTime = neighborTime;}

    public double getNeighborTime(){return neighborTime;}
}
