package org.fog.entities;

import java.util.List;

public class AreaOfDevice {
    List<FogDevice> area;

    public AreaOfDevice(List<FogDevice> area){
        setArea(area);
    }

    public List<FogDevice> getArea() {
        return area;
    }

    public void setArea(List<FogDevice> area) {
        this.area = area;
    }
}
