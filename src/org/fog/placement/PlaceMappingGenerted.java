package org.fog.placement;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.AreaOfDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlaceMappingGenerted {
    private List<FogDevice> fogDevices;
    private List<Application> applications;
    private Map<String, List<Integer>> moduleToDeviceMap;
    private Map<Integer, List<AppModule>> deviceToModuleMap;
    private Map<Integer, Map<String, Integer>> moduleInstanceCountMap;

    private ModuleMapping moduleMapping;
    /**
     * 每个App对应一个moduleMapping
     */
    private Map<String, ModuleMapping> moduleMappings;
    protected List<Sensor> sensors;
    protected List<Actuator> actuators;
    protected List<AreaOfDevice> areas;//areas of devices   第三级节点下的所有子节点组成一个area  model放置在area与上级节点之间选择
    protected Map<Integer, Double> currentCpuLoad;
    public PlaceMappingGenerted(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                                List<Application> applications, Map<String, ModuleMapping> moduleMappings){

        setFogDevices(fogDevices);
        setSensors(sensors);
        setActuators(actuators);
        setApplications(applications);
        setModuleMappings(moduleMappings);
        setAreas(new ArrayList<>());
    }

    /**
     * 主调用接口 生成各个App的MapPlacement
     * @return
     */
    public Map<String,ModuleMapping> genertedPlacement(){

        return null;
    }

    public List<FogDevice> getFogDevices() {
        return fogDevices;
    }

    public void setFogDevices(List<FogDevice> fogDevices) {
        this.fogDevices = fogDevices;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public Map<String, List<Integer>> getModuleToDeviceMap() {
        return moduleToDeviceMap;
    }

    public void setModuleToDeviceMap(Map<String, List<Integer>> moduleToDeviceMap) {
        this.moduleToDeviceMap = moduleToDeviceMap;
    }

    public Map<Integer, List<AppModule>> getDeviceToModuleMap() {
        return deviceToModuleMap;
    }

    public void setDeviceToModuleMap(Map<Integer, List<AppModule>> deviceToModuleMap) {
        this.deviceToModuleMap = deviceToModuleMap;
    }

    public Map<Integer, Map<String, Integer>> getModuleInstanceCountMap() {
        return moduleInstanceCountMap;
    }

    public void setModuleInstanceCountMap(Map<Integer, Map<String, Integer>> moduleInstanceCountMap) {
        this.moduleInstanceCountMap = moduleInstanceCountMap;
    }

    public ModuleMapping getModuleMapping() {
        return moduleMapping;
    }

    public void setModuleMapping(ModuleMapping moduleMapping) {
        this.moduleMapping = moduleMapping;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }

    public List<Actuator> getActuators() {
        return actuators;
    }

    public void setActuators(List<Actuator> actuators) {
        this.actuators = actuators;
    }

    public Map<String, ModuleMapping> getModuleMappings() {
        return moduleMappings;
    }

    public void setModuleMappings(Map<String, ModuleMapping> moduleMappings) {
        this.moduleMappings = moduleMappings;
    }

    public List<AreaOfDevice> getAreas() {
        return areas;
    }

    public void setAreas(List<AreaOfDevice> areas) {
        this.areas = areas;
    }
}
