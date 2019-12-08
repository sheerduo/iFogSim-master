package org.fog.placement;

import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.*;

import java.util.*;

public class PlaceMappingGenerted extends ModulePlacement{
    private List<FogDevice> fogDevices;
    private List<Application> applications;
    private Map<String, List<Integer>> moduleToDeviceMap;
    private Map<Integer, List<AppModule>> deviceToModuleMap;
    private Map<Integer, Map<String, Integer>> moduleInstanceCountMap;
   // private Map<String, List<Integer>> result = new HashMap<>();
    //private List<String> placedModules = new ArrayList<String>();
    private ModuleMapping moduleMapping;
    /**
     * 每个App对应一个moduleMapping
     */
    private Map<String, ModuleMapping> moduleMappings;
    protected List<Sensor> sensors;
    protected List<Actuator> actuators;
    protected List<AreaOfDevice> areas;//areas of devices   第三级节点下的所有子节点组成一个area  model放置在area与上级节点之间选择
    protected Map<Integer, Double> currentCpuLoad;

    Map<String, Integer> sensorsAssociated ;  //返回不同类型的sensor
    Map<Integer, Integer> sensorsOfDevcie; //每个device对应的sensor数量
    Map<String, Integer> actuatorsAssociated;
    public PlaceMappingGenerted(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                                List<Application> applications, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas){

        setFogDevices(fogDevices);
        setSensors(sensors);
        setActuators(actuators);
        setApplications(applications);
        setModuleMappings(moduleMappings);
        setAreas(areas);
        for(AreaOfDevice  area : areas){
            List<String> placedModules = new ArrayList<String>();
            for(FogDevice device : area.getArea()) {
                //FogDevice device = getFogDeviceById(deviceId);
                getAssociatedSensors(device);  //返回不同类型的sensor
                sensorsOfDevcie.put(device.getId(), getAssociatedSensors(device));
                actuatorsAssociated = getAssociatedActuators(device);
                placedModules.addAll(sensorsAssociated.keySet()); // ADDING ALL SENSORS TO PLACED LIST
                placedModules.addAll(actuatorsAssociated.keySet()); // ADDING ALL ACTUATORS TO PLACED LIST
            }
            genertedPlacement(placedModules, area, 3);
        }
    }

    @Override
    protected void mapModules() {

    }

    /**
     * 主调用接口 生成各个App的MapPlacement
     * @return
     */
    public Map<String, Map<Integer, List<Integer>>> genertedPlacement(List<String> placedModules, AreaOfDevice area, int level){
        Map<String, Map<Integer, List<Integer>>> result1 = new HashMap<>();
        List<FogDevice> devices = new ArrayList<>();
        for(FogDevice d : area.getArea()){
            devices.add(d);
        }
        FogDevice device1 = area.getArea().get(0);
        boolean flag = true;
        int Max = area.getArea().size();
        //将Area内以及上级的device都纳入范围
        while(flag){
            FogDevice dd = getDeviceById(device1.getParentId());
            if(dd.getName().equals("Cloud")){//向上寻找所有上级device
                flag = false;
            }
            devices.add(dd);
            Max++;
        }
        //切记前面的device为低层级的 后面的为高层级的
        for(Application app:getApplications()){
            //ModuleMapping mapping = ModuleMapping.createModuleMapping();
            //int sensorNum = sensorsAssociated.get(app.getEdges().get(0).getSource());//获取sensor数量  sensor数量代表需要放置的module数量！
            int max = Max;//初始max代表所有device
            int min = 1;
            //int sensors = 10;
            //boolean flag1 = true;
            Map<Integer, List<Integer>> modulemap = new HashMap<Integer, List<Integer>>();  //每一个sensor链的module应映射方案
            for(Sensor sen:sensors) {
                if(sen.getTupleType().equals(app.getEdges().get(0).getSource())) {
                    List<Integer> de2place = new ArrayList<>();
                    int numOfArea = area.getArea().size();
                    int mid = numOfArea;
                    int temp = mid;
                    for (AppEdge edge : app.getEdges()) {//placeedModules提前只能指定底层级的       appedge的最后一个一定是actuator
                        if (!placedModules.contains(edge.getDestination())) {
                            int place2 = (int)(Math.random()*(max-min));
                            if(place2>mid&&place2>temp){
                                temp = place2;
                                min = temp;
                            }
                            de2place.add(place2);
                   /* String moduleName = edge.getDestination();
                    int nums = (int) (Math.random() * (sensors));
                    int nn = area.getArea().size();
                    int n = (nums > nn) ? nn : nums;
                    List<Integer> ds = new ArrayList<>();
                    for (int j = 0; j < n; j++) {
                        boolean ff = true;
                        while (ff) {
                            int temp = (int) (Math.random() * (max-min));//max与min的差值代表位置的选择域
                            if (!ds.contains(temp)) {
                                ds.add(temp);
                                ff = false;
                            }
                            if(ds.indexOf(temp)>area.getArea().size()){
                                min = area.getArea().size();//如果上游module放置在了上层device中，则下游module的放置位置也将被限制在上游module中。
                            }
                        }
                    }
                    List<String> dds = new ArrayList<>();
                    for(int sd : ds){
                        String name = getFogDeviceById(sd).getName();
                        mapping.addModuleToDevice(moduleName, name);//放入
                    }*/
                        }
                    }
                    modulemap.put(sen.getId(), de2place);
                }

            }
            result1.put(app.getAppId(), modulemap);

        }
        return result1;
    }

    /**
     *寻找相关的actuators 和 sensors
     * @param device
     * @return
     */
    private Map<String, Integer> getAssociatedActuators(FogDevice device) {
        Map<String, Integer> endpoints = new HashMap<String, Integer>();
        for(Actuator actuator : getActuators()){
            if(actuator.getGatewayDeviceId()==device.getId()){
                if(!endpoints.containsKey(actuator.getActuatorType()))
                    endpoints.put(actuator.getActuatorType(), 0);
                endpoints.put(actuator.getActuatorType(), endpoints.get(actuator.getActuatorType())+1);
            }
        }
        return endpoints;
    }

    private int getAssociatedSensors(FogDevice device) {   //默认每个device只接受一种Sensor
        Map<Integer, Integer> endpoints = new HashMap<Integer, Integer>();
        int num = 0;
        for(Sensor sensor : getSensors()){
            if(sensor.getGatewayDeviceId()==device.getId()){
                if(!sensorsAssociated.containsKey(sensor.getTupleType()))
                    sensorsAssociated.put(sensor.getTupleType(), 0);
                sensorsAssociated.put(sensor.getTupleType(), endpoints.get(sensor.getTupleType())+1);
                num++;
            }
        }
        return num;
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
