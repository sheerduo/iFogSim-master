package org.fog.entities;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;

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
                        //System.out.println(getName() + "  Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
                        for(Tuple resTuple : resultantTuples){
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
                            canBeProcessedBySelf(resTuple);
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
        if(tuple.getDirection() == Tuple.ACTUATOR || tuple.getDirection() == Tuple.DOWN ){
            sendTupleToActuator(tuple);
            return false;
        }
        Map<String, Integer> chainMap = tuple.getChainMap();
        int targetDevice = chainMap.get(tuple.getDestModuleName());

    /*    int sourceSensor = tuple.getSourceSensor();
        String moduleName = tuple.getDestModuleName();*/
        //System.out.println(this.getId() + "  sourceSensor:  " + sourceSensor + " moduleName  " + moduleName + " tuple.direction " + tuple.getDirection() + "  sensorModuleChainMap  " + sensorModuleChaineMap);
       // int toDevcieId = sensorModuleChaineMap.get(sourceSensor).get(moduleName);
        if(targetDevice==this.getId()){
            processTupleArrival(ev);
            return true;
        }
        sendNeighbor(tuple, targetDevice);
        return false;
    }

    protected boolean canBeProcessedBySelf(Tuple tuple){
        //int sourceSensor = tuple.getSourceSensor();
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
