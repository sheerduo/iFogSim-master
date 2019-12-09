package org.fog.placement;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.AreaOfDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 李凯
 * 评价函数写在这个里面
 * 生成函数写在PlaceMappingGenerted里面
 */
public class IntelAgent extends ModulePlacement {
    /**
     * 空方法 只为使用ModulePlacement 中的方法。
     */
    @Override
    protected void mapModules() {
    }

    private List<FogDevice> fogDevices;
    private List<Application> applications;
    private Map<String, List<Integer>> moduleToDeviceMap;
    private Map<Integer, List<AppModule>> deviceToModuleMap;
    private Map<Integer, Map<String, Integer>> moduleInstanceCountMap;
    ModuleMapping moduleMapping;
    protected List<Sensor> sensors;
    protected List<Actuator> actuators;
    protected Map<Integer, Double> currentCpuLoad;
    private PlaceMappingGenerted placeMappingGenerted;
    private List<AreaOfDevice> areas;
    /**
     * 最后生成结果
     * 每个App对应一个moduleMapping
     * 外部调用要先将moduleMapping与APP映射到map中传入
     */
    private Map<String, ModuleMapping> bestModuleMappings;
    /**
     * 每个App对应一个moduleMapping
     * 外部调用要先将moduleMapping与APP映射到map中传入
     */
    private Map<String, ModuleMapping> moduleMappings;


    public IntelAgent(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                      List<Application> applications, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas){
        this.setFogDevices(fogDevices);
        this.setApplications(applications);
        //this.setModuleMapping(moduleMapping);
       /* this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
        this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());*/
        setSensors(sensors);
        setActuators(actuators);
        setModuleMappings(moduleMappings);
        setCurrentCpuLoad(new HashMap<Integer, Double>());
        setAreas(areas);
       /* setCurrentModuleMap(new HashMap<Integer, List<String>>());
        setCurrentModuleLoadMap(new HashMap<Integer, Map<String, Double>>());
        setCurrentModuleInstanceNum(new HashMap<Integer, Map<String, Integer>>());*/
        for(FogDevice dev : getFogDevices()){
            getCurrentCpuLoad().put(dev.getId(), 0.0);
           /* getCurrentModuleLoadMap().put(dev.getId(), new HashMap<String, Double>());
            getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
            getCurrentModuleInstanceNum().put(dev.getId(), new HashMap<String, Integer>());*/
        }
        placeMappingGenerted = new PlaceMappingGenerted(fogDevices, sensors, actuators,applications, moduleMappings,areas);
    }

    /**
     * 待填写
     * @return
     */
    public Map<String, ModuleMapping> findBestModuleMappings(){
        for(AreaOfDevice area : areas) {
            SA_method(0, 0, 0, 0);//TODO  启动算法 开始迭代
        }
        return bestModuleMappings;//返回结果
    }

    public void SA_method(double tem,int T,int N,double q) {
        //tem为初始最大温度，T为外循环次数，N为内循环次数,q为降温系数
        int K=0;
        int Loop=0;
        int count=0;//记录随机变差过程中的接受次数
        while(K<T){
            Loop=0;
            while(Loop<N) {
                //placeMappingGenerted.genertedPlacement();//产生新的邻域解
                double value = valuePlacement();
                if (value < 0) {
                    if(Math.exp(0-(value/tem))>Math.random()) {
                        count++;
                        Loop++;
                        //新解不优于  但决定向其他方向寻找
                        //TODO
                        //placeMappingGenerted.genertedPlacement();//生成新解
                    }else{
                        Loop++;
                        //新解不优于 继续寻找
                        //TODO
                    }
                } else {
                    Loop++;
                    //新解优于当前解  替换
                    //TODO
                }
            }
        }
    }

    /**
     * 评价函数
     * @return
     */
    public double valuePlacement(){
        return 0.0;
    }


    @Override
    public List<FogDevice> getFogDevices() {
        return fogDevices;
    }

    @Override
    public void setFogDevices(List<FogDevice> fogDevices) {
        this.fogDevices = fogDevices;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    @Override
    public Map<String, List<Integer>> getModuleToDeviceMap() {
        return moduleToDeviceMap;
    }

    @Override
    public void setModuleToDeviceMap(Map<String, List<Integer>> moduleToDeviceMap) {
        this.moduleToDeviceMap = moduleToDeviceMap;
    }

    @Override
    public Map<Integer, List<AppModule>> getDeviceToModuleMap() {
        return deviceToModuleMap;
    }

    @Override
    public void setDeviceToModuleMap(Map<Integer, List<AppModule>> deviceToModuleMap) {
        this.deviceToModuleMap = deviceToModuleMap;
    }

    @Override
    public Map<Integer, Map<String, Integer>> getModuleInstanceCountMap() {
        return moduleInstanceCountMap;
    }

    @Override
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

    public Map<Integer, Double> getCurrentCpuLoad() {
        return currentCpuLoad;
    }

    public void setCurrentCpuLoad(Map<Integer, Double> currentCpuLoad) {
        this.currentCpuLoad = currentCpuLoad;
    }

    public PlaceMappingGenerted getPlaceMappingGenerted() {
        return placeMappingGenerted;
    }

    public void setPlaceMappingGenerted(PlaceMappingGenerted placeMappingGenerted) {
        this.placeMappingGenerted = placeMappingGenerted;
    }

    public Map<String, ModuleMapping> getModuleMappings() {
        return moduleMappings;
    }

    public void setModuleMappings(Map<String, ModuleMapping> moduleMappings) {
        this.moduleMappings = moduleMappings;
    }

    public Map<String, ModuleMapping> getBestModuleMappings() {
        return bestModuleMappings;
    }

    public void setBestModuleMappings(Map<String, ModuleMapping> bestModuleMappings) {
        this.bestModuleMappings = bestModuleMappings;
    }

    public List<AreaOfDevice> getAreas() {
        return areas;
    }

    public void setAreas(List<AreaOfDevice> areas) {
        this.areas = areas;
    }
}
