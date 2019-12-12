package org.fog.placement;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.AreaOfDevice;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.TimeKeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenertedController extends Controller {

    private PlaceMappingGenerted placeMappingGenerted;
    private List<Application> apps ;
    Map<String, ModuleMapping> moduleMappings;
    List<AreaOfDevice> areas;
    public GenertedController(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators, List<Application> apps, Map<String, ModuleMapping> moduleMappings, List<AreaOfDevice> areas){
        super(name, fogDevices, sensors, actuators);
        this.apps = apps;
        placeMappingGenerted = new PlaceMappingGenerted(fogDevices, sensors, actuators ,apps, moduleMappings, areas);
    }

    public void startGenerted(){
        CloudSim.startSimulation();
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

        send(getId(), Config.PRE_SIMULATION_TIME, FogEvents.STOP_SIMULATION);

        for(FogDevice dev : getFogDevices())
            sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);
    }

    @Override
    public void processOtherEvent(SimEvent ev) {
        switch(ev.getTag()){
            case FogEvents.STOP_GENERTED_SIMULATION:
                CloudSim.stopSimulation();
                System.exit(0);
                break;
        }
    }

    public void generted(){

    }

    public void SA_method(double tem,int T,int N,double q) {
        //tem为初始最大温度，T为外循环次数，N为内循环次数,q为降温系数
        int K=0;
        int Loop;
        int count=0;//记录随机变差过程中的接受次数
        int best = Integer.MAX_VALUE;
        while(K<T){
            Loop=0;
            while(Loop<N) {
                //device 清空
                placeMappingGenerted.generted();//产生新的邻域解
                double value = valuePlacement();
                if (value - best < 0) {
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

        double value = 0.0;
        for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()){
            double value1 = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
            value+=value1;
            System.out.println(getStringForLoopId(loopId) + " ---> "+value1 + " nums: " + TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loopId));
        }
        return value;
    }

}
