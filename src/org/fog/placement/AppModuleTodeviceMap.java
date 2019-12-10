package org.fog.placement;

import java.util.List;
import java.util.Map;

public class AppModuleTodeviceMap {
    /**
     *String 对应application Integer对应sensorid， List<Integer> 对应sensor后的module的放置位置
     **/
    private Map<String, Map<Integer, List<Integer>>> app2DeviceMap;

    public AppModuleTodeviceMap (Map<String, Map<Integer, List<Integer>>> app2DeviceMap){
        this.app2DeviceMap = app2DeviceMap;
    }

    public Map<String, Map<Integer, List<Integer>>> getApp2DeviceMap() {
        return app2DeviceMap;
    }

    public void setApp2DeviceMap(Map<String, Map<Integer, List<Integer>>> app2DeviceMap) {
        this.app2DeviceMap = app2DeviceMap;
    }
}
