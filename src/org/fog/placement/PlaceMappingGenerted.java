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

    private Map<String, List<String>> mapping = new HashMap<>();
    private Map<String, ModuleMapping> moduleMappings;
    protected List<Sensor> sensors;
    protected List<Actuator> actuators;
    protected List<AreaOfDevice> areas;//areas of devices   第三级节点下的所有子节点组成一个area  model放置在area与上级节点之间选择
    protected Map<Integer, Double> currentCpuLoad;
    private Random random = new Random();
    List<String> placedModules = new ArrayList<String>();
    Map<String, Integer> sensorsAssociated = new HashMap<>() ;  //返回不同类型的sensor
    Map<Integer, List<Sensor>> sensorsOfDevcie = new HashMap<>(); //每个device对应的sensor数量
    Map<String, Integer> actuatorsAssociated = new HashMap<>();
    public PlaceMappingGenerted(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                                List<Application> applications, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas){

        setFogDevices(fogDevices);
        setSensors(sensors);
        setActuators(actuators);
        setApplications(applications);
        setModuleMappings(moduleMappings);
        setAreas(areas);
        for(AreaOfDevice area: areas) {
            for(FogDevice dev: area.getArea()){
                if(!sensorsOfDevcie.containsKey(dev.getId())){
                    sensorsOfDevcie.put(dev.getId(), new ArrayList<>());
                }
                for(Integer decId : dev.getChildrenIds()){
                    List<Sensor> sens = getAssociatedSensors(getFogDeviceById(decId));
                    System.out.println("列表：  " + sens);
                    sensorsOfDevcie.put(dev.getId(), sens);
                }
                System.out.println("列表：  " + sensorsOfDevcie);

            }
           // genertedPlacement(area, 0);
        }
    }

    /**
     * 返回 the list of module  顺序与 application 相当
     * @return
     */

    public List<ModulePlacement> generted(){
        Map<String, Map<Integer, List<Integer>>> result = new HashMap<>();

        List<ModuleMapping> moduleMappingList = new ArrayList<>();
        for(int i=0;i<2;i++){
            ModuleMapping moduleMapping1 = ModuleMapping.createModuleMapping();
            moduleMappingList.add(moduleMapping1);
        }
        //System.out.println("0000随多少 " );
        for(AreaOfDevice  area : areas){
            //System.out.println("11111 " + area.getArea().size());

            for(FogDevice device : area.getArea()) {
                //System.out.println("2222");
                //FogDevice device = getFogDeviceById(deviceId);
                getAssociatedSensors(device);  //返回不同类型的sensor
                sensorsOfDevcie.put(device.getId(), getAssociatedSensors(device));
                actuatorsAssociated = getAssociatedActuators(device);
                placedModules.addAll(sensorsAssociated.keySet()); // ADDING ALL SENSORS TO PLACED LIST
                placedModules.addAll(actuatorsAssociated.keySet()); // ADDING ALL ACTUATORS TO PLACED LIST
            }
            result = genertedPlacement(area, 3);
            Map<Integer, Map<String, Integer>> sensorModuleChaineMap = new HashMap<>(); //Integer- sensorId   String - moduleMap  Integer-deviceId



            //List<String> appName = result.keySet();
            int numOfApp = 0;
            for(String appname : result.keySet()){
                Map<Integer, List<Integer>> mm = result.get(appname);
                ModuleMapping moduleMapping = moduleMappingList.get(numOfApp);
                for(Integer sensorId : mm.keySet()){
                    Map<String, Integer> moduleChain2Device = new HashMap<>();
                    List<Integer> devices = mm.get(sensorId);
                    Application app = applications.get(numOfApp);
                    List<AppEdge> edges = app.getEdges();
                    for(int i=0;i<devices.size();i++){
                        AppEdge edge = edges.get(i);
                        String module = edge.getDestination();
                        Integer devId = devices.get(i);
                        moduleChain2Device.put(module, devId);
                        String devName = getDeviceById(devId).getName();
                        moduleMapping.addModuleToDevice(edge.getDestination(), devName);
                        if(!mapping.containsKey(devName)){
                            mapping.put(devName, new ArrayList<String>());
                        }
                        List<String> modules = mapping.get(devName);
                        modules.add(module);
                        mapping.put(devName, modules);
                    }
                    sensorModuleChaineMap.put(sensorId, moduleChain2Device);
                }
                numOfApp++;
            }

            for(FogDevice device : area.getArea()){
                List<Sensor> sensors = sensorsOfDevcie.get(device.getId());
                for(Sensor sensor1 : sensors){
                    //System.out.println(sensor1.getId() + "  " + sensorModuleChaineMap.get(sensor1.getId()));
                    sensor1.setChainMap(sensorModuleChaineMap.get(sensor1.getId()));
                }
                //device.setSensorModuleChaineMap(sensorModuleChaineMap);
            }
        }
        System.out.println("modulemapping:  ");
        for(int i=0; i<2;i++){
            System.out.println(moduleMappingList.get(i).moduleMapping);
        }
        //mapModules();
        List<ModulePlacement> modulePlacementList = new ArrayList<>();
       // ModulePlacement modulePlacement1 = new ModulePlacementMapping(fogDevices, applications.get(0), moduleMappingList.get(0));
        //ModulePlacement modulePlacement2 = new ModulePlacementMapping(fogDevices, applications.get(1), moduleMappingList.get(1));
        for(int i=0;i<2;i++){
            ModulePlacement modulePlacement1 = new ModulePlacementMapping(fogDevices, applications.get(i), moduleMappingList.get(i));
            modulePlacement1.mapModules();
            modulePlacementList.add(modulePlacement1);
        }
        return modulePlacementList;
    }

    @Override
    protected void mapModules() {
        //Map<String, List<String>> mapping = moduleMapping.getModuleMapping();

    }

    /**
     * 主调用接口 生成各个App的MapPlacement
     *
     * @return String- App Name   Integer-SensorId  List<Integer>  顺序代表edge顺序   Integer代表deviceId
     */
    public Map<String, Map<Integer, List<Integer>>> genertedPlacement(AreaOfDevice area, int level){
        Map<String, Map<Integer, List<Integer>>> result1 = new HashMap<>();
        List<FogDevice> devices = new ArrayList<>();
        for(FogDevice d : area.getArea()){
            devices.add(d);
        }
        FogDevice dd = area.getArea().get(0);
        boolean flag = true;
        int Max = area.getArea().size();
        //将Area内以及上级的device都纳入范围
        while(flag){
            dd = getDeviceById(dd.getParentId());
            //System.out.println("begin div   " + dd.getName());
            if(dd.getName().equals("cloud")){//向上寻找所有上级device
                flag = false;
            }
            devices.add(dd);
            Max++;
        }
        //切记前面的device为低层级的 后面的为高层级的
        for(Application app:getApplications()){
            //ModuleMapping mapping = ModuleMapping.createModuleMapping();
            //int sensorNum = sensorsAssociated.get(app.getEdges().get(0).getSource());//获取sensor数量  sensor数量代表需要放置的module数量！
            int max = Max-1;//初始max代表所有device
            int min = 0;
            //int sensors = 10;
            //boolean flag1 = true;
            Map<Integer, List<Integer>> modulemap = new HashMap<Integer, List<Integer>>();  //每一个sensor链的module应映射方案  链是对照APPedge的顺序而定的  刨除了sensor和actuators
            for(FogDevice de: area.getArea()) {
                List<Sensor> sensorOfde = sensorsOfDevcie.get(de.getId());
                System.out.println("sensor size: " + sensorOfde);
                for (Sensor sen : sensorOfde) {

                    if (sen.getTupleType().equals(app.getEdges().get(0).getSource())) {
                        List<Integer> de2place = new ArrayList<>();
                        int numOfArea = area.getArea().size();
                        int mid = numOfArea;
                        int temp = mid;
                        System.out.println("area size: " + numOfArea);
                        List<String> hasPlacedModules = new ArrayList<>();
                        for(String ss : placedModules){
                            hasPlacedModules.add(ss);
                        }
                        for (AppEdge edge : app.getEdges()) {//placeedModules提前只能指定底层级的       appedge的最后一个一定是actuator
                            //System.out.println("hasPlacedModules:  " + placedModules + "edge.destination: " + edge.getDestination());
                            if (!hasPlacedModules.contains(edge.getDestination())) {
                               // System.out.println("edge.destination:  " + edge.getDestination());
                                int ran = random.nextInt(max - min + 1);
                                int place2 = min + ran;
                                //int place2 = (int)(min+Math.random()*(max-min));
                                // System.out.println("temp: " + place2  + "  device num: " + (devices.size()-1) + "  min:" + min + "  max:" +max + "  ran: " + ran);
                                if (place2 > mid && place2 > temp) {
                                    temp = place2;
                                    min = temp;
                                }
                                de2place.add(devices.get(place2).getId());
                                hasPlacedModules.add(edge.getDestination());
                            }
                        }
                        modulemap.put(sen.getId(), de2place);
                        //sSystem.out.println("de2place  " + de2place);
                        min = 0;
                    }

                }
            }//}
            result1.put(app.getAppId(), modulemap);
            //System.out.println("result:  " + result1);
        }

        //System.out.println("devices : ");
       /* for(FogDevice device : devices){
            System.out.print(device.getId() + "  ");
        }*/
        //System.out.println( "generted mapping" + result1);


        return result1;
    }

    public void placeFunction(AreaOfDevice area, int level){
        Map<Integer, Map<String, Integer>> moduleNumOnDevice = new HashMap<>();// map<deviceId, map<module, num>>
        Map<String, Map<Integer, List<Integer>>> result = genertedPlacement(area,level);
        for(Application app : applications){
            Map<Integer, List<Integer>> appMap = result.get(app.getAppId());
        }
    }

    /**
     *寻找相关的actuators 和 sensors
     * @param device
     * @return
     */
    private Map<String, Integer> getAssociatedActuators(FogDevice device) {
        Map<String, Integer> endpoints = new HashMap<String, Integer>();
        //System.out.println("actuators num : " + getActuators().size());
        for(Actuator actuator : getActuators()){
            //System.out.println("actuator.tupleType:  " + actuator.getActuatorType());
            if(actuator.getGatewayDeviceId()==device.getId()){
                if(!endpoints.containsKey(actuator.getActuatorType()))
                    endpoints.put(actuator.getActuatorType(), 0);
                endpoints.put(actuator.getActuatorType(), endpoints.get(actuator.getActuatorType())+1);
            }
        }
        return endpoints;
    }

    private List<Sensor> getAssociatedSensors(FogDevice device) {   //默认每个device只接受一种Sensor
       // Map<Integer, Integer> endpoints = new HashMap<Integer, Integer>();
        List<Sensor> result = new ArrayList<>();
        for(Sensor sensor : getSensors()){
            //System.out.println("这是decId  " + sensor.getId());
            if(sensor.getGatewayDeviceId()==device.getId()){
                if(!sensorsAssociated.containsKey(sensor.getTupleType()))
                    sensorsAssociated.put(sensor.getTupleType(), 0);
                sensorsAssociated.put(sensor.getTupleType(), sensorsAssociated.get(sensor.getTupleType()+1));
               // System.out.println("sensor.tupleType:  " + sensor.getTupleType());
                result.add(sensor);
            }
        }
        System.out.println("result大小  " + result.size());
        return result;
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
