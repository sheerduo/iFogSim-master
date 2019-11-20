package org.fog.placement;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.AreaOfDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModulePlacementIntelgent extends ModulePlacement {
    //app列表
    private List<Application> applications;
    protected List<Sensor> sensors;
    protected List<Actuator> actuators;

    //迭代体返回迭代后的解
    private IntelAgent agent;
    private Map<String, ModuleMapping> moduleMappings;
    private List<AreaOfDevice> areas;
    private Map<String, ModuleMapping> bestModuleMappings;
    /**
     * 生成最后结果
     */
    @Override
    protected void mapModules() {
        agent = new IntelAgent(getFogDevices(), getSensors(), getActuators(), getApplications(), getModuleMappings(), areas);
        bestModuleMappings = agent.findBestModuleMappings();
        //将module依照map放置到device中
        for(;;){//第一层依照app放置
            for(;;){//第二层依照app中的device放置

            }
        }
    }

    public ModulePlacementIntelgent(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                                    List<Application> applications, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas) {
        this.setFogDevices(fogDevices);
        this.setApplications(applications);
        //this.setModuleMapping(moduleMapping);
        setSensors(sensors);
        setActuators(actuators);
        setModuleMappings(moduleMappings);
        setAreas(areas);
        for (FogDevice device : getFogDevices())
            getModuleInstanceCountMap().put(device.getId(), new HashMap<String, Integer>());
        mapModules();
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
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


