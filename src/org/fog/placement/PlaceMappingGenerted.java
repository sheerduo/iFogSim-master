package org.fog.placement;

import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.*;
import org.fog.utils.BestPlacement;
import org.fog.utils.TimeKeeper;

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
    List<String> placedModules;
    Map<String, Integer> sensorsAssociated = new HashMap<>() ;  //返回不同类型的sensor
    Map<Integer, List<Sensor>> sensorsOfDevcie = new HashMap<>(); //每个device对应的sensor数量
    Map<String, Integer> actuatorsAssociated = new HashMap<>();
    public PlaceMappingGenerted(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                                List<Application> applications, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas, List<String> placedModules){

        setFogDevices(fogDevices);
        setSensors(sensors);
        setActuators(actuators);
        setApplications(applications);

        setModuleMappings(moduleMappings);
        //System.out.println("这是  " + getModuleMappings().get(0));
        setAreas(areas);
        this.placedModules = placedModules;
        /*for(String appId : moduleMappings.keySet()){
            ModuleMapping devMap = moduleMappings.get(appId);
            if(placedModules.contains(moduleMappings.get(devId))) {
                placedModules.add(moduleMappings.get(devId)))
            }
        }*/
        for(AreaOfDevice area: areas) {
           // System.out.println("actuators: " + actuatorsAssociated + "  areasize: " + area.getArea().size());
            for(FogDevice dev: area.getArea()){
                if(!sensorsOfDevcie.containsKey(dev.getId())){
                    sensorsOfDevcie.put(dev.getId(), new ArrayList<>());
                }
                //System.out.println(dev.getName() + " child  " + dev.getChildrenIds());
                List<Sensor> sens = new ArrayList<>();
                for(Integer decId : dev.getChildrenIds()){

                    sens.addAll(getAssociatedSensors(getFogDeviceById(decId)));
                    sensorsOfDevcie.put(dev.getId(), sens);

                    getAssociatedActuators(getDeviceById(decId));

                }
                //System.out.println("列表：  " + sensorsOfDevcie);
                List<Sensor> ssss = sensorsOfDevcie.get(6);
               // for(Sensor s : ssss){
                    ///System.out.println("6666  " + ssss.size());
                //}
            }
           // genertedPlacement(area, 0);

        }
    }

    public List<ModulePlacement> gener(List<FogDevice> deviceTable, Map<String, Map<Integer, List<String>>> sensor_moduleChain){
       // Map<String, Map<Integer, Map<String, Integer>>> result;
        List<ModuleMapping> moduleMappingList = new ArrayList<>();
        for(String appName : getModuleMappings().keySet()){ //将已放置的放入
            moduleMappingList.add(getModuleMappings().get(appName));
        }

        Map<String, Map<Integer, Map<String, Integer>>> templeChain = BestPlacement.getTepTupleChain();
        for(String appname : templeChain.keySet()){
            System.out.println("appname:  ");
            Map<Integer, Map<String, Integer>> chain = templeChain.get(appname);
            for(Integer s : chain.keySet()){
                System.out.println(s + " : " + chain.get(s));
            }
        }
        Map<String, List<Integer>> replaceChain = BestPlacement.getReplaceSensorChain();
        for(String app : replaceChain.keySet()){
            List<Integer> replaceSensor = replaceChain.get(app);
            for(Integer sensorId : replaceSensor){

                Map<String, Integer> chain = new HashMap<>();
                List<String> moduleChain = sensor_moduleChain.get(app).get(sensorId);
                int Max = deviceTable.size();
                int temp = 0;
                int mid = Max-1;
                boolean flag = true;
                int tempRan=0;
                for(String module : moduleChain){
                    int ran = 0;
                    if(flag){//第一次
                        ran = random.nextInt(Max)+1;
                        tempRan = ran;
                       // System.out.println( "  ranA " + ran);
                        if(ran>mid){
                            temp=Max;
                        }

                        flag=false;
                    }else {//其它次
                        //ran = random.nextInt(Max) + 1;
                        //ran = random.nextInt(Max-1) + 1;
                       // int gai = random.nextInt(100);

                        if(temp==0){

                            ran = random.nextInt(Max) + 1;
                            if(ran>mid){
                                temp=Max;
                            }
                        }else {
                            ran = Max;
                        }
                    }
                   chain.put(module, deviceTable.get(ran-1).getId());
                }
                templeChain.get(app).put(sensorId, chain);
            }
        }

        int numOfApp = 0;
        Map<Integer, Map<String, Integer>> sensorModuleChaineMap = new HashMap<>(); //Integer- sensorId   String - moduleMap  Integer-deviceId
        for(String appname : templeChain.keySet()){
            //Map<Integer, Map<String, Integer>> sensorChain = new HashMap<>();
            Map<Integer, Map<String, Integer>> mm = templeChain.get(appname);//从生成的结果中取出的
            ModuleMapping moduleMapping = moduleMappingList.get(numOfApp);
            for(Integer sensorId : mm.keySet()){
                Map<String, Integer> moduleChain2Device = new HashMap<>();//要放入sensor的
                Map<String, Integer> module_dev = mm.get(sensorId);
                Application app = applications.get(numOfApp);
                for(Sensor sensor : getSensors()){
                    if (sensor.getId() == sensorId){
                        int gatewayId = sensor.getGatewayDeviceId();
                        String gatewayName = getDeviceById(gatewayId).getName();
                        List<String> modNames = moduleMapping.getModuleMapping().get(gatewayName);
                        moduleChain2Device.put(modNames.get(0), gatewayId);
                    }
                }

                for(String moduleName : module_dev.keySet()){
                    moduleMapping.addModuleToDevice(moduleName, getDeviceById(module_dev.get(moduleName)).getName());
                    moduleChain2Device.put(moduleName, module_dev.get(moduleName));
                }
                //sensorChain.put(sensorId, moduleChain2Device);
                sensorModuleChaineMap.put(sensorId, moduleChain2Device);
            }
            //sensorModuleChaineMap.put(appname, sensorChain);
            numOfApp++;
        }
        BestPlacement.setTepTupleChain(templeChain);
        for (FogDevice device : areas.get(0).getArea()) {
            List<Sensor> sensors = sensorsOfDevcie.get(device.getId());
            for (Sensor sensor1 : sensors) {
                //if(chain.keySet().contains(sensor1.getId())) {
                sensor1.setChainMap(sensorModuleChaineMap.get(sensor1.getId()));
                //}
            }
        }

        BestPlacement.setTepMapping(moduleMappingList);
        List<ModulePlacement> modulePlacementList = new ArrayList<>();
        for(int i=0;i<2;i++){
            ModulePlacement modulePlacement1 = new ModulePlacementMapping(fogDevices, applications.get(i), moduleMappingList.get(i));
            modulePlacement1.mapModules();
            modulePlacementList.add(modulePlacement1);
        }
        return modulePlacementList;
    }

    public List<ModulePlacement> placeStatic(Map<String, Map<Integer, Map<String, Integer>>> templeChain){
        List<ModuleMapping> moduleMappingList = new ArrayList<>();
        for(String appName : getModuleMappings().keySet()){ //将已放置的放入
            moduleMappingList.add(getModuleMappings().get(appName));
        }
        int numOfApp = 0;
        Map<Integer, Map<String, Integer>> sensorModuleChaineMap = new HashMap<>();
        for(String appname : templeChain.keySet()){
            //Map<Integer, Map<String, Integer>> sensorChain = new HashMap<>();
            Map<Integer, Map<String, Integer>> mm = templeChain.get(appname);//从生成的结果中取出的
            ModuleMapping moduleMapping = moduleMappingList.get(numOfApp);

            for(Integer sensorId : mm.keySet()){
                Map<String, Integer> moduleChain2Device = new HashMap<>();//要放入sensor的
                Map<String, Integer> module_dev = mm.get(sensorId);
                Application app = applications.get(numOfApp);
                for(Sensor sensor : getSensors()){
                    if (sensor.getId() == sensorId){
                        int gatewayId = sensor.getGatewayDeviceId();
                        String gatewayName = getDeviceById(gatewayId).getName();
                        List<String> modNames = moduleMapping.getModuleMapping().get(gatewayName);
                        moduleChain2Device.put(modNames.get(0), gatewayId);
                    }
                }

                for(String moduleName : module_dev.keySet()){
                    moduleMapping.addModuleToDevice(moduleName, getDeviceById(module_dev.get(moduleName)).getName());
                    moduleChain2Device.put(moduleName, module_dev.get(moduleName));
                }
                //sensorChain.put(sensorId, moduleChain2Device);
                sensorModuleChaineMap.put(sensorId, moduleChain2Device);
            }
            //sensorModuleChaineMap.put(appname, sensorChain);
            numOfApp++;
        }
        BestPlacement.setTepTupleChain(templeChain);
        for (FogDevice device : areas.get(0).getArea()) {
            List<Sensor> sensors = sensorsOfDevcie.get(device.getId());
            for (Sensor sensor1 : sensors) {
                //if(chain.keySet().contains(sensor1.getId())) {
                sensor1.setChainMap(sensorModuleChaineMap.get(sensor1.getId()));
                //}
            }
        }

        BestPlacement.setTepMapping(moduleMappingList);
        List<ModulePlacement> modulePlacementList = new ArrayList<>();
        for(int i=0;i<2;i++){
            ModulePlacement modulePlacement1 = new ModulePlacementMapping(fogDevices, applications.get(i), moduleMappingList.get(i));
            modulePlacement1.mapModules();
            modulePlacementList.add(modulePlacement1);
        }
        return modulePlacementList;
    }

    /**
     * 返回 the list of module  顺序与 application 相当
     * @return
     */

    public List<ModulePlacement> generted(){
        Map<String, Map<Integer, Map<String, Integer>>> result = new HashMap<>();

        List<ModuleMapping> moduleMappingList = new ArrayList<>();
        for(String appName : getModuleMappings().keySet()){ //将已放置的放入
            moduleMappingList.add(getModuleMappings().get(appName));
        }
        for(AreaOfDevice  area : areas){
            List<String> hasPlacedeModules = new ArrayList<>();
            hasPlacedeModules.addAll(sensorsAssociated.keySet()); // ADDING ALL SENSORS TO PLACED LIST
            hasPlacedeModules.addAll(actuatorsAssociated.keySet()); // ADDING ALL ACTUATORS TO PLACED LIST
            result = genertedPlacement(area, 3);//生成每个sensor的moduleChain

            Map<Integer, Map<String, Integer>> sensorModuleChaineMap = new HashMap<>(); //Integer- sensorId   String - moduleMap  Integer-deviceId
            int numOfApp = 0;
            for(String appname : result.keySet()){
                //Map<Integer, Map<String, Integer>> sensorChain = new HashMap<>();
                Map<Integer, Map<String, Integer>> mm = result.get(appname);//从生成的结果中取出的
                ModuleMapping moduleMapping = moduleMappingList.get(numOfApp);
                for(Integer sensorId : mm.keySet()){
                    Map<String, Integer> moduleChain2Device = new HashMap<>();//要放入sensor的
                    Map<String, Integer> module_dev = mm.get(sensorId);
                    Application app = applications.get(numOfApp);
                    for(Sensor sensor : getSensors()){
                        if (sensor.getId() == sensorId){
                            int gatewayId = sensor.getGatewayDeviceId();
                            String gatewayName = getDeviceById(gatewayId).getName();
                            List<String> modNames = moduleMapping.getModuleMapping().get(gatewayName);
                            moduleChain2Device.put(modNames.get(0), gatewayId);
                        }
                    }

                    for(String moduleName : module_dev.keySet()){
                        moduleMapping.addModuleToDevice(moduleName, getDeviceById(module_dev.get(moduleName)).getName());
                        moduleChain2Device.put(moduleName, module_dev.get(moduleName));
                    }

                    //sensorChain.put(sensorId, moduleChain2Device);
                    sensorModuleChaineMap.put(sensorId, moduleChain2Device);
                }

                numOfApp++;
            }
            BestPlacement.setTepTupleChain(result);

                for (FogDevice device : area.getArea()) {
                    List<Sensor> sensors = sensorsOfDevcie.get(device.getId());
                    for (Sensor sensor1 : sensors) {
                        //if(chain.keySet().contains(sensor1.getId())) {
                            sensor1.setChainMap(sensorModuleChaineMap.get(sensor1.getId()));
                        //}
                    }
                }

        }
        BestPlacement.setTepMapping(moduleMappingList);
        List<ModulePlacement> modulePlacementList = new ArrayList<>();
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
    public Map<String, Map<Integer, Map<String, Integer>>> genertedPlacement(AreaOfDevice area, int level){
        Map<String, Map<Integer, Map<String, Integer>>> result1 = new HashMap<>();
        List<FogDevice> devices = new ArrayList<>();
        for(FogDevice d : area.getArea()){
            devices.add(d);
        }
        devices.add(getDeviceByName("cloud"));

        int Max = devices.size();
        //将Area内以及上级的device都纳入范围
        // FogDevice dd = area.getArea().get(0);
        //        boolean flag = true;
       /* while(flag){
            dd = getDeviceById(dd.getParentId());
            //System.out.println("begin div   " + dd.getName());
            if(dd.getName().equals("cloud")){//向上寻找所有上级device
                flag = false;
            }
            devices.add(dd);
            Max++;
        }*/
        //切记前面的device为低层级的 后面的为高层级的
        for(Application app:getApplications()){
            int max = Max-1;//初始max代表所有device
            int min = 0;
            Map<Integer, Map<String, Integer>> modulemap = new HashMap<Integer, Map<String, Integer>>();  //每一个sensor链的module应映射方案  链是对照APPedge的顺序而定的  刨除了sensor和actuators
            for(FogDevice de: area.getArea()) {
                List<Sensor> sensorOfde = sensorsOfDevcie.get(de.getId());
                for (Sensor sen : sensorOfde) {

                    if (sen.getTupleType().equals(app.getEdges().get(0).getSource())) {
                        Map<String, Integer> de2place = new HashMap<>();
                        int numOfArea = area.getArea().size();
                        int mid = numOfArea;
                        int temp = 0;
                        List<String> hasPlacedModules = new ArrayList<>();
                        for(String ss : placedModules){
                            hasPlacedModules.add(ss);
                        }
                        for (AppEdge edge : app.getEdges()) {//placeedModules提前只能指定底层级的       appedge的最后一个一定是actuator
                            if (!hasPlacedModules.contains(edge.getDestination())) {
                                int terminalId = sen.getGatewayDeviceId();
                                FogDevice terminal = getDeviceById(terminalId);
                                de2place.put(edge.getDestination(), terminal.getParentId());
                                hasPlacedModules.add(edge.getDestination());
                                /*if(temp==0) {
                                    int ran = random.nextInt(Max-1)+1;
                                    if (ran > mid) {
                                        temp = Max;
                                    }
                                   // System.out.println("ran     " + ran + "  max  " + Max);
                                    de2place.put(edge.getDestination(), devices.get(ran - 1).getId());
                                    hasPlacedModules.add(edge.getDestination());
                                }else{
                                    de2place.put(edge.getDestination(), devices.get(temp-1).getId());
                                    hasPlacedModules.add(edge.getDestination());
                                }*/
                            }
                        }
                        modulemap.put(sen.getId(), de2place);
                        min = 0;
                    }

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
       // System.out.println("actuators num : " + getActuators().size());
        for(Actuator actuator : getActuators()){
            //System.out.println("actuator.tupleType:  " + actuator.getActuatorType());
            if(actuator.getGatewayDeviceId()==device.getId()){
                if(!endpoints.containsKey(actuator.getActuatorType())) {
                    endpoints.put(actuator.getActuatorType(), 0);

                }
                if(!actuatorsAssociated.containsKey(actuator.getActuatorType())){
                    actuatorsAssociated.put(actuator.getActuatorType(), 0);
                }
                endpoints.put(actuator.getActuatorType(), endpoints.get(actuator.getActuatorType())+1);

                actuatorsAssociated.put(actuator.getActuatorType(), actuatorsAssociated.get(actuator.getActuatorType())+1);
               // System.out.println(actuatorsAssociated);
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
                if(!sensorsAssociated.containsKey(sensor.getTupleType())) {
                    sensorsAssociated.put(sensor.getTupleType(), 0);
                }
                sensorsAssociated.put(sensor.getTupleType(), sensorsAssociated.get(sensor.getTupleType())+1);
                //System.out.println("sensor.tupleType:  " + sensor.getTupleType());
                result.add(sensor);
            }
        }
        //System.out.println(device.getId() +  " result大小  " + result.size());
        return result;
    }

    public Map<String, List<Integer>> generReplaceSensor(Map<String, Map<Integer, List<String>>> chain){
        Map<String, List<Integer>>  rePlaceSensor = new HashMap<>();
        Set<String> apps = chain.keySet();

        for(Integer sensor111 :  TimeKeeper.getInstance().getSensorIdToActuator().keySet()){
            System.out.println("SensorId : " + sensor111 + " : " + TimeKeeper.getInstance().getSensorIdToActuator().get(sensor111) + "  num: " + TimeKeeper.getInstance().getSensorIdToNum().get(sensor111));
        }

        for(String app:apps){
            Map<Integer, List<String>> chainOfApp = chain.get(app);
            Set<Integer> sensors = chainOfApp.keySet();
            List<Double> delays = new ArrayList<>();
            List<Integer> sensorsToReplace = new ArrayList<>();
            for(Integer sensorId:sensors){
                //System.out.println("问题" + sensorId);
                double delay = Double.MAX_VALUE;
                if(TimeKeeper.getInstance().getSensorIdToActuator().keySet().contains(sensorId)){
                    delay = TimeKeeper.getInstance().getSensorIdToActuator().get(sensorId);
                }
                if(delays.size()==0) {     //第一个直接放入
                    delays.add(0, delay);
                    sensorsToReplace.add(0, sensorId);
                    continue;
                }
                if(delays.size()<2){
                    if(delay<delays.get(delays.size()-1)){
                        delays.add(delay);
                        sensorsToReplace.add(sensorId);
                    }else {
                        for (int index = delays.size()-1; index >= 0; index--) {
                            if (delay<delays.get(index)){
                                delays.add(index+1, delay);
                                sensorsToReplace.add(index+1, sensorId);
                                break;
                            }
                            if(index == 0){
                                delays.add(index, delay);
                                sensorsToReplace.add(index, sensorId);
                            }
                        }
                    }
                }else {
                    if(delay>delays.get(delays.size()-1)) {
                        delays.remove(delays.size() - 1);
                        sensorsToReplace.remove(sensorsToReplace.size()-1);
                        for (int index = delays.size()-1; index >= 0; index--) {
                            if (delay < delays.get(index)) {
                                delays.add(index + 1, delay);
                                sensorsToReplace.add(index + 1, sensorId);
                                break;
                            }
                            if (index == 0) {
                                delays.add(index, delay);
                                sensorsToReplace.add(index, sensorId);
                            }
                        }
                    }
                }
            }
            if(app.equals("DCNSFog")){
                sensorsToReplace = new ArrayList<>();
            }
            rePlaceSensor.put(app, sensorsToReplace);
        }
        System.out.println("需要重新放置" + rePlaceSensor);
        return rePlaceSensor;
    }

    /**
     * 生成fogdevice列表
     * @param area
     * @return
     */
    public List<FogDevice> generDeviceTable(AreaOfDevice area){
        List<FogDevice> deviceTable = new ArrayList<>();
        for(FogDevice d : area.getArea()){
            deviceTable.add(d);
        }
        deviceTable.add(getDeviceByName("cloud"));
        return deviceTable;
    }

    /**
     *生成sensor-module chain
     */
    public Map<String, Map<Integer, List<String>>> generChain(){
        AreaOfDevice area = areas.get(0);
        Map<String, Map<Integer, List<String>>> result1 = new HashMap<>();
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
            int max = Max-1;//初始max代表所有device
            int min = 0;
            Map<Integer,List<String>> modulemap = new HashMap<Integer, List<String>>();  //每一个sensor链的module应映射方案  链是对照APPedge的顺序而定的  刨除了sensor和actuators
            for(FogDevice de: area.getArea()) {
                List<Sensor> sensorOfde = sensorsOfDevcie.get(de.getId());
                for (Sensor sen : sensorOfde) {

                    if (sen.getTupleType().equals(app.getEdges().get(0).getSource())) {
                        List<String> de2place = new ArrayList<>();
                        int numOfArea = area.getArea().size();
                        int mid = numOfArea;
                        int temp = mid;
                        List<String> hasPlacedModules = new ArrayList<>();
                        for(String ss : placedModules){
                            hasPlacedModules.add(ss);
                        }
                        for (AppEdge edge : app.getEdges()) {//placeedModules提前只能指定底层级的       appedge的最后一个一定是actuator
                            if (!hasPlacedModules.contains(edge.getDestination())) {

                                de2place.add(edge.getDestination());
                                hasPlacedModules.add(edge.getDestination());
                            }
                        }
                        modulemap.put(sen.getId(), de2place);
                        min = 0;
                    }

                }
            }
            result1.put(app.getAppId(), modulemap);
        }

        return result1;
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
