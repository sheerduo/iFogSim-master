package org.fog.entities;

import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.utils.FogEvents;

import java.util.List;
import java.util.Map;

public class MyFogDevice extends FogDevice{



    //private Map<Integer, List<Integer>> sensorModuleMap;

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


    @Override
    protected boolean canBeProcessedBySelf(SimEvent ev) {
        return super.canBeProcessedBySelf(ev);


    }

    /**
     * 解析sensorModuleMap  放在上一层？？
     * @param sensorModuleMap
     */
    @Override
    public void setSensorModuleMap(Map<String, Map<Integer, List<Integer>>> sensorModuleMap) {  //
        super.setSensorModuleMap(sensorModuleMap);
    }
}
