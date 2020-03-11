package org.fog.placement;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.AreaOfDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.test.perfeval.test3;
import org.fog.utils.*;

import java.util.List;
import java.util.Map;

public class GenertedController extends Controller {

    public test3 App = new test3();

    private int num = 0;
    public PlaceMappingGenerted placeMappingGenerted;
    private List<Application> apps ;

    Map<String, ModuleMapping> moduleMappings;
    List<AreaOfDevice> areas;
    public GenertedController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, List<Application> apps, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas, List<String> placedModules){
        super(name, fogDevices, sensors, actuators);
        this.apps = apps;
        placeMappingGenerted = new PlaceMappingGenerted(fogDevices, sensors, actuators ,apps, moduleMappings, areas, placedModules);
    }

    public void initController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, List<Application> apps, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas){

    }

    public void startGenerted(boolean isFirst, List<FogDevice> deviceTable, Map<String, Map<Integer, List<String>>> sensor_moduleChain){
        //CloudSim.startSimulation();
        if(isFirst) {
            firstGener(deviceTable, sensor_moduleChain);
        }else {
            Gener(deviceTable, sensor_moduleChain);
        }
    }

    @Override
    public void startEntity() {
        for(String appId : applications.keySet()){
            if(getAppLaunchDelays().get(appId)==0)
                processAppSubmit(applications.get(appId));
            else
                send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
        }

        send(getId(), Config.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);

        send(getId(),  Config.PRE_SIMULATION_TIME, FogEvents.STOP_GENERTED_SIMULATION);
        System.out.println("重新开始过");
        for(FogDevice dev : getFogDevices())
            sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);
    }

    @Override
    public void processOtherEvent(SimEvent ev) {
        switch(ev.getTag()){
            case FogEvents.STOP_GENERTED_SIMULATION:
                CloudSim.stopSimulation();
                double value = valuePlacement();
                BestResult.setCurrentValue(value);
                /*if(BestResult.getBestVaule() > value){
                    BestResult.setBestVaule(value);
                }*/
                clearDeviceAppMap();
               // CloudSim.finishSimulation();
                /*if(num>=0) {
                    List<ModulePlacement> mapList = placeMappingGenerted.generted();//产生新的邻域解
                    for(int i=0;i<mapList.size();i++){
                        submitApplication(apps.get(i), mapList.get(i));
                    }
                    *//*int num_user = 1; // number of cloud users
                    Calendar calendar = Calendar.getInstance();
                    boolean trace_flag = false; // mean trace events
                    CloudSim.init(num_user, calendar, trace_flag);*//*
                    CloudSim.finishSimulation();
                }*/
               // System.exit(0);
                //break;
        }
    }

    private void clearFogDeviceModule(){

    }

    public void generted(){

    }

    public void firstGener(List<FogDevice> deviceTable, Map<String, Map<Integer, List<String>>> sensor_moduleChain) {
        List<ModulePlacement> mapList = placeMappingGenerted.generted();//产生新的邻域解
        for(int i=0;i<mapList.size();i++){
            submitApplication(apps.get(i), mapList.get(i));
        }
        CloudSim.startSimulation();
    }

    public void Gener(List<FogDevice> deviceTable, Map<String, Map<Integer, List<String>>> sensor_moduleChain){
        List<ModulePlacement> mapList = placeMappingGenerted.gener(deviceTable, sensor_moduleChain);//产生新的邻域解
        for(int i=0;i<mapList.size();i++){
            submitApplication(apps.get(i), mapList.get(i));
        }
        CloudSim.startSimulation();
    }

    /**
     * 评价函数
     * @return
     */
    public double valuePlacement(){

        double value = 0.0;
        //System.out.println("used  " + TimeKeeper.getInstance().getLoopIdToTupleIds().keySet().size());
        BestPlacement.newTemTime("");
        for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){
            double value1 = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
            value+=value1;
            String time = getStringForLoopId(loopId) + " ---> "+value1 + " nums: " + TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loopId);
            BestPlacement.addTemTime(time);
            System.out.println(time);
            TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loopId, 0.0);
            TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loopId, 0);
        }
        return value;
    }

    private void clearDeviceAppMap(){
        for(FogDevice d : fogDevices){
            d.clearAppToModuleMap();
            d.clearTupleFromNeighbor();
        }
    }

    public Map<String, Map<Integer, List<String>>> generChain(){
        //AreaOfDevice area = areas.get(0);
        return placeMappingGenerted.generChain();
    }
}
