package org.fog.entities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.utils.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyFogDevice extends FogDevice{

    public MyFogDevice(
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception{
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, uplinkBandwidth, downlinkBandwidth, uplinkLatency, ratePerMips);
    }


    @Override
    protected void processOtherEvent(SimEvent ev) {
        switch(ev.getTag()){
            case FogEvents.TUPLE_ARRIVAL:
                canBeProcessedBySelf(ev);
                //processTupleArrival(ev);
                break;
            case FogEvents.LAUNCH_MODULE:
                processModuleArrival(ev);
                break;
            case FogEvents.RELEASE_OPERATOR:
                processOperatorRelease(ev);
                break;
            case FogEvents.SENSOR_JOINED:
                processSensorJoining(ev);
                break;
            case FogEvents.SEND_PERIODIC_TUPLE:
                sendPeriodicTuple(ev);
                break;
            case FogEvents.APP_SUBMIT:
                processAppSubmit(ev);
                break;
            case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
                updateNorthTupleQueue();
                break;
            case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
                updateSouthTupleQueue();
                break;
            case FogEvents.ACTIVE_APP_UPDATE:
                updateActiveApplications(ev);
                break;
            case FogEvents.ACTUATOR_JOINED:
                processActuatorJoined(ev);
                break;
            case FogEvents.LAUNCH_MODULE_INSTANCE://never used
                updateModuleInstanceCount(ev);
                break;
            case FogEvents.NEIGHBOR_TUPLE_ARRIVE://发送至Neighbor
                processTupleFromNeighbor(ev);
                break;
            case FogEvents.UPDATE_NEIGHBOR_TUPLE_QUEUE:
                updateNeighborTupleQueue();
                break;
            case FogEvents.ASK_NEIGHBOR:
                canProcessNeighborTuple(ev);
                break;
            case FogEvents.ANSWER_NEIGHBOR:
                getTheAnswer(ev);
                break;
            case FogEvents.RESOURCE_MGMT:
                manageResources(ev);
            case FogEvents.NEIGHBOR_MODULE_LAUNCH:
                setNeighborModuleList(ev);
            default:
                break;
        }
    }

    /*
    **更改checkFinished
    * source 需要改变？？  先这么写了
     */

    @Override
    protected void checkCloudletCompletion() {
        boolean cloudletCompleted = false;
        List<? extends Host> list = getVmAllocationPolicy().getHostList();
        for (int i = 0; i < list.size(); i++) {
            Host host = list.get(i);
            for (Vm vm : host.getVmList()) {
                while (vm.getCloudletScheduler().isFinishedCloudlets()) {
                    Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
                    if (cl != null) {
                        for(Pair<Integer,Integer> pair: tupleExecuting){
                            if(pair.getSecond()==cl.getCloudletId()){
                                tupleExecuting.remove(pair);
                                break;
                            }
                        }
                        cloudletCompleted = true;
                        Tuple tuple = (Tuple)cl;
                        if(moduleNums.containsKey(tuple.getDestModuleName())){
                            int num = moduleNums.get(tuple.getDestModuleName());
                            moduleNums.put(tuple.getDestModuleName(), num-1);
                            tasknum--;
                        }
                        double executeTime = TimeKeeper.getInstance().tupleEndedExecution(tuple);
                        try{
                            out.write(tuple.getDestModuleName()+ ":" + executeTime + "\r\n");
                            out.flush();
                        }catch (IOException ex){

                        }
                        Application application = getApplicationMap().get(tuple.getAppId());
                        Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());

                        //System.out.println(tuple.getAcutualsource() + " source " +getName() + "   delay: " + tuple.getDestModuleName() + "  " + tuple.getDirection() + "    " + (CloudSim.clock()-TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId())));
                        //System.out.println(getName() + "  Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, tuple.getSourceDeviceId(), vm.getId());
                        /*if(tuple.getChainMap()!=null)
                            System.out.println("source  " + tuple.getChainMap());*/
                        for(Tuple resTuple : resultantTuples){
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
                            sendToSelf(resTuple);
                        }
                        sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
                    }
                }
            }
        }
        if(cloudletCompleted)
            updateAllocatedMips(null);
    }

    @Override
    protected boolean canBeProcessedBySelf(SimEvent ev) {

        boolean result = false;
        Tuple tuple = (Tuple)ev.getData();


        /*if(tuple.getSourceDeviceId()!=-1){
            System.out.println("source  " +  tuple.getSourceDeviceId() + "   module " + tuple.getDestModuleName());
        }*/
        if(tuple.getDirection() == Tuple.ACTUATOR){
            sendTupleToActuator(tuple);
            return false;
        }


        Map<String, Integer> chainMap = tuple.getChainMap();
        //System.out.println("chainMap   " + chainMap);

        int targetDevice = chainMap.get(tuple.getDestModuleName());
        if(targetDevice==this.getId()){
            processTupleArrival(ev);
            return true;
        }

        if(tuple.getDirection()==Tuple.DOWN){

            if(this.getLevel()<2) {
                if(tuple.getSourceSensor()==24){
                    //System.out.println("hased");
                }
                for (int childId : getChildrenIds())
                    sendDown(tuple, childId);
                return false;
            }else if(this.getChildrenIds().contains(tuple.getSourceDeviceId())){//是给自己的
                if(tuple.getSourceSensor()==24){
                    //System.out.println("hased");
                }
                    for(Integer child : getChildrenIds())
                        sendDown(tuple, child);
                    return false;
            }else {
                if(ev.getSource()==this.getId()){
                    if(tuple.getSourceSensor()==24){
                       // System.out.println("hased");
                    }
                    for(NeighborInArea neighborInArea :neighbors){
                        sendNeighbor(tuple, neighborInArea.getId());
                    }

                }
                return false;
            }

        }

        if(tuple.getDirection()==Tuple.UP && this.getLevel()==3) {
            //tuple.setAcutualsource(this.getParentId());
            sendUp(tuple);
            return false;
        }
        sendNeighbor(tuple, targetDevice);
        return false;
    }

    protected boolean canBeProcessedBySelf(Tuple tuple){
        //int sourceSensor = tuple.getSourceSensor();
        if(tuple.getDirection() == Tuple.ACTUATOR){ //
            sendTupleToActuator(tuple);
        }
        if(tuple.getDirection() == Tuple.DOWN){
            if(getLevel()!=2) {         //高级的发给孩子
                //System.out.println("位置3");
                for(Integer dd : getChildrenIds())
                    sendDown(tuple, dd);
                return false;
            }
            //System.out.println(tuple.getDestModuleName() + "  usde  " + this.getLevel());
            if(tuple.getSourceDeviceId()==this.getId()){
                //System.out.println("位置4");
                for(Integer dd : getChildrenIds())
                    sendDown(tuple, dd);
            }else {
                //System.out.println("位置5");
                sendNeighbor(tuple, tuple.getSourceDeviceId());//同级的发给邻居
            }
            return false;
        }
        String moduleName = tuple.getDestModuleName();
        //System.out.println(this.getId() + "  sourceSensor:  " + sourceSensor + " moduleName  " + moduleName + "  sensorModuleChainMap  " + sensorModuleChaineMap);
        Map<String, Integer> map = tuple.getChainMap();
        if(map.keySet().contains(moduleName)) {
            int toDevcieId = map.get(moduleName);
            if (toDevcieId == this.getId()) {
                updateTimingsOnSending(tuple);
                sendToSelf(tuple);
                return true;
            }
            if(this.getLevel()==3){
                tuple.setSourceDeviceId(this.getParentId());
            }
            sendNeighbor(tuple, toDevcieId);
            return false;
        }else {
            int actualId = tuple.getAcutualsource();
            sendNeighbor(tuple, actualId);
            return false;
        }
    }

    @Override
    protected void processTupleFromNeighbor(SimEvent ev) {
        Tuple tuple = (Tuple)ev.getData();
        int tupleNeighborid=ev.getSource();
        int cloudletId = ((Tuple)ev.getData()).getCloudletId();
        tupleFromNeighbor.add(new Pair<>(cloudletId, tuple.getAcutualsource()));
        processTupleArrival(ev);
    }
}
