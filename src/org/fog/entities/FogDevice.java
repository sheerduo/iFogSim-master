package org.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.function.DoubleBinaryOperator;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;

public class FogDevice extends PowerDatacenter {
    protected Queue<Tuple> northTupleQueue;
    protected Queue<Pair<Tuple, Integer>> southTupleQueue;
    protected Queue<Pair<Tuple, Integer>> neighborTupleQueue;
    protected List<String> activeApplications;

    protected Map<String, Application> applicationMap;
    protected Map<String, List<String>> appToModulesMap;
    protected Map<Integer, Double> childToLatencyMap;


    protected Map<Integer, Integer> cloudTrafficMap;

    protected double lockTime;

    /**
     * ID of the parent Fog Device
     */
    protected int parentId;

    /**
     * ID of the Controller
     */
    protected int controllerId;
    /**
     * IDs of the children Fog devices
     */
    protected List<Integer> childrenIds;

    protected Map<Integer, List<String>> childToOperatorsMap;
    /**
     * Flag denoting whether the link neighbor from this FogDevice is busy  @李凯
     */
    protected boolean isNeighborBusy;

    /**
     * Flag denoting whether the link southwards from this FogDevice is busy
     */
    protected boolean isSouthLinkBusy;

    /**
     * Flag denoting whether the link northwards from this FogDevice is busy
     */
    protected boolean isNorthLinkBusy;

    protected double uplinkBandwidth;
    protected double downlinkBandwidth;
    protected double uplinkLatency;
    //@李凯
    protected double neighborLatency = 10;
    protected double neighborBandwidth=10000;
    protected List<Pair<Integer, Double>> associatedActuatorIds;
    protected List<Pair<Integer, Integer>> tupleFromNeighbor = new ArrayList<Pair<Integer, Integer>>();
    protected List<Pair<Integer, SimEvent>> tupleWaitSendToNeighbor = new ArrayList<Pair<Integer, SimEvent>>();
    protected Map<Integer, Integer> tupleSendNum = new HashMap<Integer, Integer>();
    protected Map<Integer,Pair<Integer,Double>> neighborCandidate = new HashMap<Integer,Pair<Integer,Double>>();
    protected long ok1 = 0;
    protected long notOk = 0;
    protected Map<Integer, List<String>> appToModulesMapNeighbor =new HashMap<Integer, List<String>>(){{}};

    protected Map<Integer, Map<String, Integer>> sensorModuleChaineMap = new HashMap<>();


    protected int tasknum = 0;
    protected List<Pair<Integer,Integer>> tupleExecuting = new ArrayList<Pair<Integer,Integer>>();//pair.first=actualTupleId pair.second=cloudeletId
    protected Map<String , Integer> moduleNums = new HashMap<String , Integer>();
    protected Map<String, Pair<String, Integer>> moduleNameToId = new HashMap<String, Pair<String, Integer>>();//映射moduleName to vm.Uid <Uid, <name,level>>
    protected File file = new File("D:\\fog1\\fog2\\"+getName()+".txt");
    protected Writer out = new FileWriter(file);

    protected double energyConsumption;
    protected double lastUtilizationUpdateTime;
    protected double lastUtilization;
    private int level;


    protected double ratePerMips;

    protected double totalCost;

    protected Map<String, Map<String, Integer>> moduleInstanceCount;

    /**
     *自加NeighborInArea
     */
    protected NeighborInArea selfInfo;
    protected List<NeighborInArea> neighbors = new ArrayList<NeighborInArea>();

    public FogDevice(
            String name,
            FogDeviceCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval,
            double uplinkBandwidth, double downlinkBandwidth, double uplinkLatency, double ratePerMips) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
        setCharacteristics(characteristics);
        setVmAllocationPolicy(vmAllocationPolicy);
        setLastProcessTime(0.0);
        setStorageList(storageList);
        setVmList(new ArrayList<Vm>());
        setSchedulingInterval(schedulingInterval);
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setRatePerMips(ratePerMips);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host : getCharacteristics().getHostList()) {
            host.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        // If this resource doesn't have any PEs then no useful at all
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }
        // stores id of this class
        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);


        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        setChildToLatencyMap(new HashMap<Integer, Double>());
        System.out.println(getName() + "  :  " + getId());
    }

    public FogDevice(
            String name, long mips, int ram,
            double uplinkBandwidth, double downlinkBandwidth, double ratePerMips, PowerModel powerModel) throws Exception {
        super(name, null, null, new LinkedList<Storage>(), 0);

        List<Pe> peList = new ArrayList<Pe>();

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

        int hostId = FogUtils.generateEntityId();
        long storage = 1000000; // host storage
        int bw = 10000;

        PowerHost host = new PowerHost(
                hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                powerModel
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));

        String arch = Config.FOG_DEVICE_ARCH;
        String os = Config.FOG_DEVICE_OS;
        String vmm = Config.FOG_DEVICE_VMM;
        double time_zone = Config.FOG_DEVICE_TIMEZONE;
        double cost = Config.FOG_DEVICE_COST;
        double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
        double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
        double costPerBw = Config.FOG_DEVICE_COST_PER_BW;

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        setCharacteristics(characteristics);

        setLastProcessTime(0.0);
        setVmList(new ArrayList<Vm>());
        setUplinkBandwidth(uplinkBandwidth);
        setDownlinkBandwidth(downlinkBandwidth);
        setUplinkLatency(uplinkLatency);
        setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
        for (Host host1 : getCharacteristics().getHostList()) {
            host1.setDatacenter(this);
        }
        setActiveApplications(new ArrayList<String>());
        if (getCharacteristics().getNumberOfPes() == 0) {
            throw new Exception(super.getName()
                    + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
        }


        getCharacteristics().setId(super.getId());

        applicationMap = new HashMap<String, Application>();
        appToModulesMap = new HashMap<String, List<String>>();
        northTupleQueue = new LinkedList<Tuple>();
        southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
        setNorthLinkBusy(false);
        setSouthLinkBusy(false);


        setChildrenIds(new ArrayList<Integer>());
        setChildToOperatorsMap(new HashMap<Integer, List<String>>());

        this.cloudTrafficMap = new HashMap<Integer, Integer>();

        this.lockTime = 0;

        this.energyConsumption = 0;
        this.lastUtilization = 0;
        setTotalCost(0);
        setChildToLatencyMap(new HashMap<Integer, Double>());
        setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
        System.out.println(getName() + "  :  " + getId());
    }

    /**
     * Overrides this method when making a new and different type of resource. <br>
     * <b>NOTE:</b> You do not need to override {@link # body()} method, if you use this method.
     *
     * @pre $none
     * @post $none
     */
    protected void registerOtherEntity() {

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

    /**
     * Perform miscellaneous resource management tasks
     * @param ev
     */
    protected void manageResources(SimEvent ev) {
        updateEnergyConsumption();
        send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
    }

    /**
     * Updating the number of modules of an application module on this device
     * @param ev instance of SimEvent containing the module and no of instances
     */
    protected void updateModuleInstanceCount(SimEvent ev) {
        ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
        String appId = config.getModule().getAppId();
        if(!moduleInstanceCount.containsKey(appId))
            moduleInstanceCount.put(appId, new HashMap<String, Integer>());
        moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
        System.out.println(getName()+ " Creating "+config.getInstanceCount()+" instances of module "+config.getModule().getName());
    }

    private AppModule getModuleByName(String moduleName){
        AppModule module = null;
        for(Vm vm : getHost().getVmList()){
            if(((AppModule)vm).getName().equals(moduleName)){
                module=(AppModule)vm;
                break;
            }
        }
        return module;
    }

    /**
     * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, only one tuple is sent DOWN while instanceCount number of tuples are sent UP.
     * @param ev SimEvent instance containing the edge to send tuple on
     */
    protected void sendPeriodicTuple(SimEvent ev) {
        AppEdge edge = (AppEdge)ev.getData();
        String srcModule = edge.getSource();
        AppModule module = getModuleByName(srcModule);

        if(module == null)
            return;

        int instanceCount = module.getNumInstances();
        /*
         * Since tuples sent through a DOWN application edge are anyways broadcasted, only UP tuples are replicated
         */
        for(int i = 0;i<((edge.getDirection()==Tuple.UP)?instanceCount:1);i++){
            //System.out.println(CloudSim.clock()+" : Sending periodic tuple "+edge.getTupleType());
            Tuple tuple = applicationMap.get(module.getAppId()).createTuple(edge, getId(), module.getId());
            updateTimingsOnSending(tuple);
            sendToSelf(tuple);
        }
        send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
    }

    protected void processActuatorJoined(SimEvent ev) {
        int actuatorId = ev.getSource();
        double delay = (double)ev.getData();
        getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
    }


    protected void updateActiveApplications(SimEvent ev) {
        Application app = (Application)ev.getData();
        getActiveApplications().add(app.getAppId());
    }


    public String getOperatorName(int vmId){
        for(Vm vm : this.getHost().getVmList()){
            if(vm.getId() == vmId)
                return ((AppModule)vm).getName();
        }
        return null;
    }

    /**
     * Update cloudet processing without scheduling future events.
     *
     * @return the double
     */
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;

        for (PowerHost host : this.<PowerHost> getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }

            Log.formatLine(
                    "%.2f: [Host #%d] utilization is %.2f%%",
                    currentTime,
                    host.getId(),
                    host.getUtilizationOfCpu() * 100);
        }

        if (timeDiff > 0) {
            Log.formatLine(
                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
                    getLastProcessTime(),
                    currentTime);

            for (PowerHost host : this.<PowerHost> getHostList()) {
                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
                double utilizationOfCpu = host.getUtilizationOfCpu();
                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
                        previousUtilizationOfCpu,
                        utilizationOfCpu,
                        timeDiff);
                timeFrameDatacenterEnergy += timeFrameHostEnergy;

                Log.printLine();
                Log.formatLine(
                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
                        currentTime,
                        host.getId(),
                        getLastProcessTime(),
                        previousUtilizationOfCpu * 100,
                        utilizationOfCpu * 100);
                Log.formatLine(
                        "%.2f: [Host #%d] energy is %.2f W*sec",
                        currentTime,
                        host.getId(),
                        timeFrameHostEnergy);
            }

            Log.formatLine(
                    "\n%.2f: Data center's energy is %.2f W*sec\n",
                    currentTime,
                    timeFrameDatacenterEnergy);
        }

        setPower(getPower() + timeFrameDatacenterEnergy);

        checkCloudletCompletion();

        /** Remove completed VMs **/
        /**
         * Change made by HARSHIT GUPTA
         */
		/*for (PowerHost host : this.<PowerHost> getHostList()) {
			for (Vm vm : host.getCompletedVms()) {
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getVmList().remove(vm);
				Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
			}
		}*/

        Log.printLine();

        setLastProcessTime(currentTime);
        return minTime;
    }


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
                                //System.out.println(getName() + "   " + pair.getFirst() + "  " + pair.getSecond());
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


                        //out.write("");
                        double executeTime = TimeKeeper.getInstance().tupleEndedExecution(tuple);
                        try{
                            out.write(tuple.getDestModuleName()+ ":" + executeTime + "\r\n");
                            out.flush();
                            //out.

                        }catch (IOException ex){

                        }
						/*if(getName().startsWith("d")){
							System.out.println("d  " + tuple.getDestModuleName() + "   " + executeTime);
						}*/
                        //System.out.println(getName() + "  "  + tuple.getDestModuleName() + "   " + executeTime);
                        Application application = getApplicationMap().get(tuple.getAppId());
                        Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
                        List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
                        for(Tuple resTuple : resultantTuples){
                            resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
                            resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
                            boolean flag = false;
                            //System.out.println(getName() + "   i'm used     " + resTuple.getSrcModuleName());//修改了将来自Neighbor的Tuple返回原Neighbor

                            for (Pair<Integer, Integer> pair : tupleFromNeighbor) {
                                // System.out.println("i'm used     ");
                                if (pair.getFirst() == cl.getCloudletId()) {
                                    if(resTuple.getDirection() != Tuple.UP) {
                                        updateTimingsOnSending(resTuple);
                                        if(pair.getSecond()!=getId()) {
                                            sendNeighbor(resTuple, pair.getSecond());
                                            tupleFromNeighbor.remove(pair);
                                            flag = true;
                                        }
                                        //System.out.println(getName() + "   i'm used     " + cl.getCloudletId() + "   " + pair.getSecond() + "  " + resTuple.getSrcModuleName() + " ---> " +resTuple.getDestModuleName());
                                        //System.out.println("i'm used     " + cl.getCloudletId() + "   " + pair.getSecond() + "  " + resTuple.getSrcModuleName() + " ---> " + resTuple.getDestModuleName() + "   " + tupleFromNeighbor.size());
                                        break;
                                    }else{
                                        //System.out.println("i'm used     " + cl.getCloudletId() + "   " + pair.getSecond() + "  " + resTuple.getSrcModuleName() + " ---> " + resTuple.getDestModuleName() + "   " + tupleFromNeighbor.size());
                                        Pair<Integer, Integer> pair1 = new Pair<>(resTuple.getCloudletId(), pair.getSecond());
                                        resTuple.setAcutualsource(pair.getSecond());
                                        tupleFromNeighbor.remove(pair);
                                        tupleFromNeighbor.add(pair1);

                                        break;
                                    }
                                }
                            }
                            /*if(getName().startsWith("md"))
								System.out.println(getName() + "   i'm used     " + cl.getCloudletId() + "   " + resTuple.getSrcModuleName() + " ---> " +
										resTuple.getDestModuleName() + "   " + tupleFromNeighbor.size());*/
                            if(flag) continue;
                            updateTimingsOnSending(resTuple);
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

    protected void updateTimingsOnSending(Tuple resTuple) {
        // TODO ADD CODE FOR UPDATING TIMINGS WHEN A TUPLE IS GENERATED FROM A PREVIOUSLY RECIEVED TUPLE.
        // WILL NEED TO CHECK IF A NEW LOOP STARTS AND INSERT A UNIQUE TUPLE ID TO IT.
        String srcModule = resTuple.getSrcModuleName();
        String destModule = resTuple.getDestModuleName();
        //System.err.println("i'm used   "+getName()+ "  " + resTuple.getTupleType());
        for(AppLoop loop : getApplicationMap().get(resTuple.getAppId()).getLoops()){
            if(loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)){
                int tupleId = TimeKeeper.getInstance().getUniqueId();
                resTuple.setActualTupleId(tupleId);
                if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
                    TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
                TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
                TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());

                //Logger.debug(getName(), "\tSENDING\t"+tuple.getActualTupleId()+"\tSrc:"+srcModule+"\tDest:"+destModule);

            }
        }
    }

    protected int getChildIdWithRouteTo(int targetDeviceId){
        for(Integer childId : getChildrenIds()){
            if(targetDeviceId == childId)
                return childId;
            if(((FogDevice)CloudSim.getEntity(childId)).getChildIdWithRouteTo(targetDeviceId) != -1)
                return childId;
        }
        return -1;
    }

    protected int getChildIdForTuple(Tuple tuple){
        if(tuple.getDirection() == Tuple.ACTUATOR){
            int gatewayId = ((Actuator)CloudSim.getEntity(tuple.getActuatorId())).getGatewayDeviceId();
            return getChildIdWithRouteTo(gatewayId);
        }
        return -1;
    }

    protected void updateAllocatedMips(String incomingOperator){
        getHost().getVmScheduler().deallocatePesForAllVms();


        for(final Vm vm : getHost().getVmList()){
            if(vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule)vm).getName().equals(incomingOperator)){//有任务在运行或者就是目标APPModule
                //System.out.println("111111      "+getName()+"     "+ vm.getCloudletScheduler().runningCloudlets()+"  AppModule:"+((AppModule)vm).getName()+"  incomingOperator:"+incomingOperator);
                if(getName()=="cloud"){
                    //System.out.println("usde");
                    getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                        protected static final long serialVersionUID = 1L;

                        {
                            add((double) getHost().getTotalMips());

                        }
                    });
                }else {
                    if (tasknum < 1) {
                        //System.out.println(incomingOperator);
                        getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                            protected static final long serialVersionUID = 1L;

                            {
                                add((double) getHost().getTotalMips());

                            }
                        });
                    } else {
                        //System.err.println(incomingOperator);
                        getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>() {
                            protected static final long serialVersionUID = 1L;

                            {
                                add((double) vm.getMips());

                            }
                        });
                    }
                }
            }else{
                //System.out.println("222222");
                getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
                    protected static final long serialVersionUID = 1L;
                    {add(0.0);}});
            }
        }

        updateEnergyConsumption();

    }

    private void updateEnergyConsumption() {
        double totalMipsAllocated = 0;
        for(final Vm vm : getHost().getVmList()){
            AppModule operator = (AppModule)vm;
            operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
                    .getAllocatedMipsForVm(operator));
            totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
        }

        double timeNow = CloudSim.clock();
        double currentEnergyConsumption = getEnergyConsumption();
        double newEnergyConsumption = currentEnergyConsumption + (timeNow-lastUtilizationUpdateTime)*getHost().getPowerModel().getPower(lastUtilization);
        setEnergyConsumption(newEnergyConsumption);

		/*if(getName().equals("d-0")){
			System.out.println("------------------------");
			System.out.println("Utilization = "+lastUtilization);
			System.out.println("Power = "+getHost().getPowerModel().getPower(lastUtilization));
			System.out.println(timeNow-lastUtilizationUpdateTime);
		}*/

        double currentCost = getTotalCost();
        double newcost = currentCost + (timeNow-lastUtilizationUpdateTime)*getRatePerMips()*lastUtilization*getHost().getTotalMips();
        setTotalCost(newcost);

        lastUtilization = Math.min(1, totalMipsAllocated/getHost().getTotalMips());
        lastUtilizationUpdateTime = timeNow;
    }

    protected void processAppSubmit(SimEvent ev) {
        Application app = (Application)ev.getData();
        applicationMap.put(app.getAppId(), app);
        //System.out.println(getName() + "   " + getHost().getTotalMips());
    }

    protected void addChild(int childId){
        if(CloudSim.getEntityName(childId).toLowerCase().contains("sensor"))
            return;
        if(!getChildrenIds().contains(childId) && childId != getId())
            getChildrenIds().add(childId);
        if(!getChildToOperatorsMap().containsKey(childId))
            getChildToOperatorsMap().put(childId, new ArrayList<String>());
    }

    protected void updateCloudTraffic(){
        int time = (int)CloudSim.clock()/1000;
        if(!cloudTrafficMap.containsKey(time))
            cloudTrafficMap.put(time, 0);
        cloudTrafficMap.put(time, cloudTrafficMap.get(time)+1);
    }

    protected void sendTupleToActuator(Tuple tuple){
		/*for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			if(actuatorId == tuple.getActuatorId()){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		int childId = getChildIdForTuple(tuple);
		if(childId != -1)
			sendDown(tuple, childId);*/
        for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
            int actuatorId = actuatorAssociation.getFirst();
            double delay = actuatorAssociation.getSecond();
            String actuatorType = ((Actuator)CloudSim.getEntity(actuatorId)).getActuatorType();
            if(tuple.getDestModuleName().equals(actuatorType)){
                send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
                return;
            }
        }
        for(int childId : getChildrenIds()){
            sendDown(tuple, childId);
        }
    }
    int numClients=0;
    protected void processTupleArrival(SimEvent ev){
        Tuple tuple = (Tuple)ev.getData();
        //System.out.println(getName() + "task" + ++tasknum);
		/*if(!canBeProcessedBySelf(tuple)){
			sendNeighbor(tuple,neighbors.get(0).getId());
			return;
		}*/

        if(getName().equals("cloud")){
            updateCloudTraffic();
            //System.out.println("cloud:  " + tuple.getDestModuleName());
        }


		/*if(getName().equals("d-0") && tuple.getTupleType().equals("_SENSOR")){
			System.out.println(++numClients);
		}*/
        Logger.debug(getName(), "Received tuple "+tuple.getCloudletId()+"with tupleType = "+tuple.getTupleType()+"\t| Source : "+
                CloudSim.getEntityName(ev.getSource())+"|Dest : "+CloudSim.getEntityName(ev.getDestination()));
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);

        if(FogUtils.appIdToGeoCoverageMap.containsKey(tuple.getAppId())){
        }

        if(tuple.getDirection() == Tuple.ACTUATOR){  //仍然需要修改
            //System.out.println("Received Actuator  " + ev.getSource() + "  " + getName());
            sendTupleToActuator(tuple);
            return;
        }

        if(getHost().getVmList().size() > 0){
            final AppModule operator = (AppModule)getHost().getVmList().get(0);
            if(CloudSim.clock() > 0){
                getHost().getVmScheduler().deallocatePesForVm(operator);
                getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
                    protected static final long serialVersionUID = 1L;
                    {add((double) getHost().getTotalMips());}});
            }
        }


        if(getName().equals("cloud") && tuple.getDestModuleName()==null){
            sendNow(getControllerId(), FogEvents.TUPLE_FINISHED, null);
        }

        if(appToModulesMap.containsKey(tuple.getAppId())){
            if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())){
                int vmId = -1;
                for(Vm vm : getHost().getVmList()){
                    if(((AppModule)vm).getName().equals(tuple.getDestModuleName()))
                        vmId = vm.getId();
                }
                if(vmId < 0
                        || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) &&
                        tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId )){
                    return;
                }
                tuple.setVmId(vmId);
                //Logger.error(getName(), "Executing tuple for operator " + moduleName);

                updateTimingsOnReceipt(tuple);

                executeTuple(ev, tuple.getDestModuleName());
            }else if(tuple.getDestModuleName()!=null){
                if(tuple.getDirection() == Tuple.UP)
                        sendUp(tuple);
                    else if(tuple.getDirection() == Tuple.DOWN){
                        for(int childId : getChildrenIds())
                        sendDown(tuple, childId);
                }
            }else{
                sendUp(tuple);
            }
        }else{
            if(tuple.getDirection() == Tuple.UP)
                sendUp(tuple);
            else if(tuple.getDirection() == Tuple.DOWN){
                for(int childId : getChildrenIds())
                    sendDown(tuple, childId);
            }
        }
    }

    protected void updateTimingsOnReceipt(Tuple tuple) {
        Application app = getApplicationMap().get(tuple.getAppId());
        String srcModule = tuple.getSrcModuleName();
        String destModule = tuple.getDestModuleName();
        List<AppLoop> loops = app.getLoops();
        for(AppLoop loop : loops){
            if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){
                Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                if(startTime==null)
                    break;
                if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
                    TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
                    TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
                }
                double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
                int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
                double delay = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
                TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
                double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
                TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
                TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
                break;
            }
        }
    }

    protected void processSensorJoining(SimEvent ev){
        send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
    }

    protected void executeTuple(SimEvent ev, String moduleName){
        Logger.debug(getName(), "Executing tuple on module "+moduleName);
        Tuple tuple = (Tuple)ev.getData();
		/*if(getName().startsWith("m")){
			System.out.println("i'm used in m");
		}*/

        AppModule module = getModuleByName(moduleName);

        if(tuple.getDirection() == Tuple.UP){
            String srcModule = tuple.getSrcModuleName();
            if(!module.getDownInstanceIdsMaps().containsKey(srcModule))
                module.getDownInstanceIdsMaps().put(srcModule, new ArrayList<Integer>());
            if(!module.getDownInstanceIdsMaps().get(srcModule).contains(tuple.getSourceModuleId()))
                module.getDownInstanceIdsMaps().get(srcModule).add(tuple.getSourceModuleId());

            int instances = -1;
            for(String _moduleName : module.getDownInstanceIdsMaps().keySet()){
                instances = Math.max(module.getDownInstanceIdsMaps().get(_moduleName).size(), instances);
            }
            module.setNumInstances(instances);
        }

        TimeKeeper.getInstance().tupleStartedExecution(tuple);
        updateAllocatedMips(moduleName);
        processCloudletSubmit(ev, false);
        updateAllocatedMips(moduleName);
        tupleExecuting.add(new Pair<Integer, Integer>(tuple.getActualTupleId(),tuple.getCloudletId()));
        //System.out.println(getName() + "   " + tuple.getSrcModuleName() + "  ----->  " + tuple.getDestModuleName() + "   " + tuple.getActualTupleId() + "   " + tuple.getCloudletId() + "  num:  " + tupleExecuting.size());
        //System.out.println(getName() + "   " + tupleExecuting);
        if(!moduleNums.containsKey(tuple.getDestModuleName())){
            moduleNums.put(tuple.getDestModuleName(), 0);
        }
        int num = moduleNums.get(tuple.getDestModuleName());
        moduleNums.put(tuple.getDestModuleName(), num + 1);
        tasknum++;
        //System.out.println(getName() + "   " + moduleNums + "    tuple nums:  " + tupleExecuting.size());
		/*if(getName().startsWith("d")){
			System.out.println("d used");
		}*/
		/*for(Vm vm : getHost().getVmList()){
			Logger.error(getName(), "MIPS allocated to "+((AppModule)vm).getName()+" = "+getHost().getTotalAllocatedMipsForVm(vm));
		}*/
    }

    protected void processModuleArrival(SimEvent ev){
        AppModule module = (AppModule)ev.getData();
        for(String moId :moduleNameToId.keySet()){
            if(moduleNameToId.get(moId).getFirst().equals(module.getName())){
                return;
            }
        }
        //System.out.println(module.getName() + "   " + module.getLevel());
        String appId = module.getAppId();
        if(!appToModulesMap.containsKey(appId)){
            appToModulesMap.put(appId, new ArrayList<String>());
        }
        //System.out.println(getName() + "   " + module.getName() + "   " + module.getUid());
        Pair<String, Integer> pair = new Pair<String, Integer>(module.getName(), module.getLevel());
        moduleNameToId.put(module.getUid(), pair);
        //System.out.println(getName() + "   "+ moduleNameToId);
        appToModulesMap.get(appId).add(module.getName());
        processVmCreate(ev, false);
        if (module.isBeingInstantiated()) {
            module.setBeingInstantiated(false);
        }

        initializePeriodicTuples(module);

        //if(!neighbors.isEmpty()){
        //System.out.println(getName() + "   " + appToModulesMap + "  " + neighbors);

        for(NeighborInArea neighbor : neighbors){
            //System.out.println(appToModulesMap);
            sendNeighbor(appToModulesMap, neighbor.getId());
        }
        //}

        module.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(module).getVmScheduler()
                .getAllocatedMipsForVm(module));
    }

    private void initializePeriodicTuples(AppModule module) {
        String appId = module.getAppId();
        Application app = getApplicationMap().get(appId);
        //System.out.println(app.getPeriodicEdges(module.getName()));
        //System.out.println(module.getName());
        //app.getPeriodicEdges(module.getName());
        if(app.getPeriodicEdges(module.getName()).size()>0) {
            List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
            for (AppEdge edge : periodicEdges) {
                send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
            }
        }
    }

    protected void processOperatorRelease(SimEvent ev){
        this.processVmMigrate(ev, false);
    }


    protected void updateNorthTupleQueue(){
        if(!getNorthTupleQueue().isEmpty()){
            Tuple tuple = getNorthTupleQueue().poll();
            sendUpFreeLink(tuple);
        }else{
            setNorthLinkBusy(false);
        }
    }

    protected void sendUpFreeLink(Tuple tuple){
        double networkDelay = tuple.getCloudletFileSize()/getUplinkBandwidth();
        setNorthLinkBusy(true);
        send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
        send(parentId, networkDelay+getUplinkLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
        //System.out.println("i'm used  "+networkDelay);
        NetworkUsageMonitor.sendingTuple(getUplinkLatency(), tuple.getCloudletFileSize());
    }



    protected void sendUp(Tuple tuple){
        if(parentId > 0){
            if(!isNorthLinkBusy()){
                sendUpFreeLink(tuple);
            }else{
                northTupleQueue.add(tuple);
            }
        }
    }

    protected void updateSouthTupleQueue(){
        if(!getSouthTupleQueue().isEmpty()){
            Pair<Tuple, Integer> pair = getSouthTupleQueue().poll();
            sendDownFreeLink(pair.getFirst(), pair.getSecond());
        }else{
            setSouthLinkBusy(false);
        }
    }

    protected void sendDownFreeLink(Tuple tuple, int childId){
        double networkDelay = tuple.getCloudletFileSize()/getDownlinkBandwidth();
        //Logger.debug(getName(), "Sending tuple with tupleType = "+tuple.getTupleType()+" DOWN");
        setSouthLinkBusy(true);
        double latency = getChildToLatencyMap().get(childId);
        send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
        send(childId, networkDelay+latency, FogEvents.TUPLE_ARRIVAL, tuple);
        NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
    }

    protected void sendDown(Tuple tuple, int childId){
        if(getChildrenIds().contains(childId)){
            if(!isSouthLinkBusy()){
                sendDownFreeLink(tuple, childId);
            }else{
                southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
            }
        }
    }

    /**
     * @李凯
     */
    protected void updateNeighborTupleQueue(){
        if(!getNeighborTupleQueue().isEmpty()){
            Pair<Tuple, Integer> pair = getNeighborTupleQueue().poll();
            sendNeighborFreeLink(pair.getFirst(), pair.getSecond());
        }else{
            setNeighborBusy(false);
        }
    }

    protected void sendNeighborFreeLink(Tuple tuple, int neighborId){
        double networkDelay = tuple.getCloudletFileSize()/getNeighborBandwidth();
        setNorthLinkBusy(true);
        if(tuple.getAcutualsource()==-1){
            tuple.setAcutualsource(this.getId());
        }
        send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
        send(neighborId, networkDelay+getNeighborLatency(), FogEvents.NEIGHBOR_TUPLE_ARRIVE, tuple);
        NetworkUsageMonitor.sendingTuple(getNeighborLatency(), tuple.getCloudletFileSize());
    }

    protected void sendNeighbor(Tuple tuple, int neighborId){
        for(NeighborInArea neighborInArea : neighbors){
            if(neighborInArea.getId()==neighborId){
                tuple.setFromneighbor(neighborId);
                if(!isNeighborBusy()){
                    sendNeighborFreeLink(tuple, neighborId);
                }else{
                    neighborTupleQueue.add(new Pair<Tuple, Integer>(tuple, neighborId));
                }
                return;
            }
        }
        if(tuple.getDirection()==Tuple.UP){
            sendUp(tuple);
            //System.out.println(getName() + "     up    " + tuple.getDestModuleName()  + "   neiborId  " + neighborId);
        }else if(tuple.getDirection()==Tuple.DOWN){
            for(int childId : getChildrenIds())
                sendDown(tuple, childId);
            //System.out.println(getName() + "     down   " + tuple.getDestModuleName() + "   neiborId  " + neighborId);
        }else if(tuple.getDirection()==Tuple.ACTUATOR){
            sendTupleToActuator(tuple);
            //System.out.println(getName() + "     to actuator   " + tuple.getDestModuleName() + "   neiborId  " + neighborId);
        }
    }

    protected void sendNeighbor(Map<String, List<String>> appToModulesMap, int neighborId){
        //System.out.println("i'm used 11111111111111111");
        send(neighborId, getNeighborLatency(), FogEvents.NEIGHBOR_MODULE_LAUNCH, appToModulesMap);

    }
    protected void sendNeighbor(boolean canbe, int neighborId) {
        send(neighborId, getNeighborLatency(), FogEvents.ANSWER_NEIGHBOR, canbe); //未考虑队列 不知道能不能行
    }
    protected void sendNeighbor(tupleToNeighbor tupleToNeighbor, int neighborId, int event){  //0->ask  1->answer
        if(event == 0) {
            send(neighborId, getNeighborLatency(), FogEvents.ASK_NEIGHBOR, tupleToNeighbor); //未考虑队列 不知道能不能行
            NetworkUsageMonitor.sendingTuple(getNeighborLatency(), 20);
        }
        if(event == 1) {
            send(neighborId, getNeighborLatency(), FogEvents.ANSWER_NEIGHBOR, tupleToNeighbor);
            NetworkUsageMonitor.sendingTuple(getNeighborLatency(), 20);
        }
    }
    /**
     * @likai
     *
     */

    protected boolean canBeProcessedBySelf(SimEvent ev){
        //boolean canBe = true;
        Tuple tuple = (Tuple)ev.getData();
        //System.out.println(getName() + "    " + tuple.getSrcModuleName());
        AppModule module = getModuleByName(tuple.getDestModuleName());
        if(module == null){
            processTupleArrival(ev);
            return true;
        }
        //System.out.println(tuple.getDestModuleName());
        //System.out.println(module.getName() + "   " + module.getLevel());
        //System.out.println(module.getLevel());
        //Vm vmModule = new Vm();
        Map<String, Double> resLength = new HashMap<String, Double>();
        for(final Vm vm :getHost().getVmList()){
            //System.out.println(module.getUid() + "   " + getName() + "  " + vm.getUid() + "   " + vm.getCloudletScheduler().getRestCloudletCpuLength());
            if(vm.getCloudletScheduler().runningCloudlets()!=0){

                resLength.put(vm.getUid(), vm.getCloudletScheduler().getRestCloudletCpuLength().getSecond());
            }
        }

        //System.out.println(getName() + "   " + resLength);
        for(final Vm vm :getHost().getVmList()){				//寻找该module
            if(((AppModule)vm).getName().equals(tuple.getDestModuleName())){
                int num = vm.getCloudletScheduler().runningCloudlets();

                List<Double> totalCapacity = getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module);
				/*if(totalCapacity.size()!=0&&num==0) {
					System.out.println("totalCapacy nums : " + totalCapacity.size() + "  totalCapacy.get(0) " + totalCapacity.get(0) + "   num:  " + num);
				}*/
                //System.err.println(moduleNums);
                if(moduleNums.containsKey(tuple.getDestModuleName())&&num!=0) {
                    //Pair<Double,Double> pair = vm.getCloudletScheduler().getRestCloudletCpuLength();
                    //double totalLength = pair.getSecond();
                    double totalLength = resLength.get(vm.getUid());
                    if(totalCapacity.size()>0) {
                        double time = (totalLength + tuple.getCloudletLength()) / totalCapacity.get(0);
                        //vm.getCloudletScheduler().getcl
                        //System.out.println(getName() + "   used  " + time + "   " + tuple.getDeadline());
                        if (time > tuple.getDeadline()) {
                            //System.err.println(getName() + tuple.getDestModuleName() + "   " + time + "  totalLength " + totalLength + "  totalCapacity  " + totalCapacity.get(0) + "  tupleLength " + tuple.getCloudletLength());
                            if(AskNeibor(tuple, tuple.getDeadline())) {
                                //System.out.println(getName() + "  " + tuple.getDestModuleName() +  "  num " + num +  "  " +time + "  totalLength " + totalLength + "  totalCapacity  " + totalCapacity.get(0) + "  tupleLength " + tuple.getCloudletLength());
                                //System.err.println(tuple.getDestModuleName()+"   deadline:  "+tuple.getDeadline());
                                tupleWaitSendToNeighbor.add(new Pair<>(tuple.getCloudletId(), ev));
                                //System.err.println(getName() + "   used");
                                return false;
                            }else{
                                sendUp(tuple);
                            }
                        }
                    }
                }else if(tasknum>0&&num<1){
                    //System.out.println(tuple.getDestModuleName() + "tuple.level:  "  + tuple.getLevel());
                    List<Double> mips = new ArrayList<Double>();
                    mips.add((double) getHost().getTotalMips());
                    Map<String, List<Double>> mipsForVm = getHost().getVmScheduler().getPredictTime(vm.getUid(), mips);
                    List<Double> mipsofVm = mipsForVm.get(vm.getUid());
                    int levelOfmodule = module.getLevel();
                    for(String key :mipsForVm.keySet()){
                        //System.out.println(getName() + "  " + key + "   " + mipsForVm + "   " + resLength);
                        if(mipsForVm.get(key).size()>0) {
                            if (mipsForVm.get(key).get(0) > 0) {
                                if (moduleNameToId.get(key).getSecond() < levelOfmodule) {
                                    if (resLength.containsKey(key)) {//存在不对应的情况  应注意
                                        double pretime = (resLength.get(key) / mipsForVm.get(key).get(0));
                                        //System.out.println(getName() + "   used  " + pretime + "   " + mipsForVm.get(key).get(0));
                                        if(pretime > tuple.getDeadline()){
                                            //System.out.println(getName() + "   used  " + key + "  module " + module.getUid());
                                            //System.out.println(resLength + "  " + mipsForVm.get(key).get(0));
                                            if(AskNeibor(tuple, tuple.getDeadline())) {
                                                //System.out.println(getName() + "  " + tuple.getDestModuleName() +  "  num " + num +  "  " +time + "  totalLength " + totalLength + "  totalCapacity  " + totalCapacity.get(0) + "  tupleLength " + tuple.getCloudletLength());
                                                //System.err.println(tuple.getDestModuleName()+"   deadline:  "+tuple.getDeadline());
                                                tupleWaitSendToNeighbor.add(new Pair<>(tuple.getCloudletId(), ev));
                                                return false;
                                            }
                                        }

                                    }
                                }
                            }
                        }
                    }
                    //System.out.println(vm.getUid() + "   " + mipsForVm + "  " );
                    //System.out.println(vm.getUid() + "   " + mipsForVm + "  " + mipsofVm);
                    //System.out.println(getName() + "   used    " + tuple.getDestModuleName() + "  " + tuple.getCloudletLength() + "    " + mipsofVm.get(0));
					/*if(tuple.getCloudletLength()/mipsofVm.get(0)>tuple.getDeadline()){
                        System.out.println(getName() +  "  usde  " + module.getName() + "   " + tuple.getCloudletLength()/mipsofVm.get(0) + "  " + mipsofVm.get(0));
						if(AskNeibor(tuple, tuple.getDeadline())) {
							//System.out.println(getName() + "  " + tuple.getDestModuleName() +  "  num " + num +  "  " +time + "  totalLength " + totalLength + "  totalCapacity  " + totalCapacity.get(0) + "  tupleLength " + tuple.getCloudletLength());
							//System.err.println(tuple.getDestModuleName()+"   deadline:  "+tuple.getDeadline());
							tupleWaitSendToNeighbor.add(new Pair<>(tuple.getCloudletId(), ev));
							return false;
						}
					}*/


                }
                //int num = vm.getCloudletScheduler().runningCloudlets();
                /*double networkDelay = tuple.getCloudletFileSize()/getNeighborBandwidth();
                double capacity = vm.getCloudletScheduler().getVmCapacity(getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module));

                //double capacity = totalCapacity / (double)(num+1);
                double predictTime = tuple.getCloudletLength() / capacity;*/
				/*if(vm.getCloudletScheduler().runningCloudlets()<1){
					processTupleArrival(ev);
					return true;
				}*/
                //System.out.println(getName() + "   moduleName:"+ tuple.getDestModuleName() + "   capacity:  " + capacity + "   totalCapacity:" + totalCapacity + "  num:"+num + "  predictTime" + predictTime +"  transmit:"+ (5*(networkDelay+getNeighborLatency())));
                //System.out.println(getName() + "   " + num);
				/*if(predictTime > tuple.getDeadline()){
					//System.out.println(getName() + "    " + tuple.getSrcModuleName() +  "   ----->"+ tuple.getDestModuleName() + "   capacity:  " + capacity + "   totalCapacity:" + totalCapacity + "  num:"+num + "  predictTime" + predictTime +"  deadline:" + tuple.getDeadline());
					if(AskNeibor(tuple, tuple.getDeadline())) {
						//System.err.println(tuple.getDestModuleName()+"   deadline:  "+tuple.getDeadline());
						tupleWaitSendToNeighbor.add(new Pair<>(tuple.getCloudletId(), ev));
						return false;
					}
				}*/
                processTupleArrival(ev);
                return true;
            }
        }
        //vmModule.getCloudletScheduler().get
        processTupleArrival(ev);
        return true;
    }



    protected void sendTupleToNeighbor(Tuple tuple){
        int neighborId = neighbors.get(0).getId();

        sendNeighborFreeLink(tuple, neighborId);
    }

    protected void processTupleFromNeighbor(SimEvent ev){
        Tuple tuple = (Tuple)ev.getData();

        //if(getName().startsWith("mh")){
        //System.out.println(getName() + "   get task from neighbor  " + tuple.getSrcModuleName() + " ----> " + tuple.getDestModuleName());
        //}

        int tupleNeighborid=ev.getSource();
        int cloudletId = ((Tuple)ev.getData()).getCloudletId();
        tupleFromNeighbor.add(new Pair<>(cloudletId, tuple.getAcutualsource()));
        //System.out.println("i'm used     " + tupleFromNeighbor.size() + "  " + getName());
        //List<Vm> vmList = getVmList();

        /*getHost().getAvailableMips();
        System.out.println(vmList.size() + " " + getName());
        for(Vm vm1 : vmList){
            System.out.print("  name: " + vm1.getMips() + "    currentMips: " +  getHost().getAvailableMips());
        }*/
        processTupleArrival(ev);
    }

    protected double getPredictTime(Vm vm, double length, double fileSize, String moduleName){
	    /*double totalLength = 0;
		for(final Vm vm1 :getHost().getVmList()){
	    	if(moduleNums.containsKey(((AppModule)vm1).getName())){
				Pair<Double,Double> pair = vm1.getCloudletScheduler().getRestCloudletCpuLength();
				totalLength += pair.getSecond();
			}
		}*/

		/*for(final Vm vm :getHost().getVmList()){				//寻找该module
			if(((AppModule)vm).getName().equals(tuple.getDestModuleName())){
				int num = vm.getCloudletScheduler().runningCloudlets();
				List<Double> totalCapacity = getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module);
				if(moduleNums.containsKey(tuple.getDestModuleName())&&num!=0) {
					Pair<Double,Double> pair = vm.getCloudletScheduler().getRestCloudletCpuLength();
					double totalLength = pair.getSecond();
					if(totalCapacity.size()>0) {
						double time = (totalLength + tuple.getCloudletLength()) / totalCapacity.get(0);
						//vm.getCloudletScheduler().getcl
						if (time > tuple.getDeadline()) {
							//System.out.println(time + "  totalLength " + totalLength + "  totalCapacity  " + totalCapacity.get(0) + "  tupleLength " + tuple.getCloudletLength());
							if(AskNeibor(tuple, tuple.getDeadline())) {
								//System.err.println(tuple.getDestModuleName()+"   deadline:  "+tuple.getDeadline());
								tupleWaitSendToNeighbor.add(new Pair<>(tuple.getCloudletId(), ev));
								return false;
							}
						}
					}
				}*/
        double predictTime =length/ getHost().getAvailableMips();
        //double predictTime =(length+totalLength)/ getHost().getTotalMips();
        //System.out.println(getName() + "   " + predictTime);
        return predictTime;
    }

    protected double canProcessNeighborTuple(double length, double fileSize, double predictTimeOfSelf, String moduleName, int evfrom){
        //System.out.println(getName() + "   get neighbor tuple");
        double networkDelay = fileSize/getNeighborBandwidth();
        //AppModule module = getModuleByName(moduleName);
        AppModule module = getModuleByName(moduleName);
        //double predictTime = getPredictTime(module,length,fileSize,moduleName);
        //module.getUid();
        Map<String, Double> resLength = new HashMap<String, Double>();
        for(final Vm vm :getHost().getVmList()){
            //System.out.println(module.getUid() + "   " + getName() + "  " + vm.getUid() + "   " + vm.getCloudletScheduler().getRestCloudletCpuLength());
            if(vm.getCloudletScheduler().runningCloudlets()!=0){

                resLength.put(vm.getUid(), vm.getCloudletScheduler().getRestCloudletCpuLength().getSecond());
            }
        }
        if(tasknum<1)
            return 1;
        for(final Vm vm :getHost().getVmList()){				//寻找该module
            if(((AppModule)vm).getName().equals(moduleName)){
                //double predictTime = getPredictTime(vm, length, fileSize, moduleName)+networkDelay+getNeighborLatency();
                //System.out.println(moduleName + "   predictTime:   " +  (getPredictTime(vm, length, fileSize, moduleName)+networkDelay+3*getNeighborLatency()) + "   " + predictTimeOfSelf);
                /*if(vm.getCloudletScheduler().runningCloudlets()>0){

                    List<Double> Capacity = getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module);
					System.out.println(getName() + "   " + vm.getCloudletScheduler().runningCloudlets() + "   " + Capacity + "   " + resLength.get(module.getUid()) + "   " +moduleName);
                    double predictTime = (resLength.get(module.getUid()) + length) / Capacity.get(0) ;
                    if(predictTime + networkDelay + 3 * getNeighborLatency() < 20){
                        //System.err.println(getName() + "  " + moduleName + " can be processing1111  " + predictTime);
                        return true;
                    }else{
						System.err.println(getName() + "  " + moduleName + "   " + vm.getCloudletScheduler().runningCloudlets() + "  can not be processing  come from " + evfrom);
                    	return false;
					}
                }*/
                List<Double> Capacity = getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module);
                if(Capacity.size()>0){
                    if(Capacity.get(0)!=0){
                        //System.out.println(getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module) + "   " + resLength + " " + module.getUid());
                        double resLen = 0.0;
                        if(resLength.containsKey(module.getUid())){
                            resLen = resLength.get(module.getUid());
                        }
                        double predictTime = (resLen + length) / Capacity.get(0) ;
                        if(predictTime + networkDelay + 3 * getNeighborLatency() < 20){//predictTime + networkDelay + 3 * getNeighborLatency()
                            /*System.err.println(getName() + "  " + moduleName + " can be processing1111  " + predictTime);*/
                            return predictTime;
                        }else{
                            //System.err.println(getName() + "  " + moduleName + "   " + vm.getCloudletScheduler().runningCloudlets() + "  can not be processing  come from " + evfrom);
                            return -1;
                        }
                    }
                }
                if(vm.getCloudletScheduler().runningCloudlets()<1&&tasknum>0){

                    List<Double> mips = new ArrayList<Double>();
                    mips.add((double) getHost().getTotalMips());
                    Map<String, List<Double>> mipsForVm = getHost().getVmScheduler().getPredictTime(vm.getUid(), mips);
                    List<Double> mipsofVm = mipsForVm.get(module.getUid());
                    for(String key : mipsForVm.keySet()){
                        if(resLength.containsKey(key)) {
                            if (mipsForVm.get(key).size() > 0) {
                                if (mipsForVm.get(key).get(0) > 0) {
                                    if (moduleNameToId.get(key).getSecond() < module.getLevel()) {

                                        //System.out.println(resLength + "   " + mipsForVm + "  " + key);
                                        if ((resLength.get(key) / mipsForVm.get(key).get(0)) < getUplinkLatency()) {
/*
                                            System.out.println(getName() + "  " + moduleName + " can be processing111111111111111111  " + tasknum + "   " + (resLength.get(key) / mipsForVm.get(key).get(0)));
*/
                                            continue;


                                        }else {
                                            //System.out.println("used");
                                            return -1;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    double predicttime = length / mipsForVm.get(module.getUid()).get(0);
                    if (predicttime + networkDelay + 3 * getNeighborLatency()  < 20) {//predicttime + networkDelay + 3 * getNeighborLatency()
                        //System.err.println(getName() + "  " + moduleName + " can be processing222  " + predicttime);
                        return predicttime;
                    }

                }


            }
        }

        return -1;
    }

    protected void canProcessNeighborTuple(SimEvent ev){
        //boolean canbe = true;
        // System.out.println("canProcessNeighborTuple");
        tupleToNeighbor tupleInfo = ((tupleToNeighbor)ev.getData());
        double predictime = canProcessNeighborTuple(tupleInfo.getCpuLength(),tupleInfo.getFileSize(),tupleInfo.getPredictTime(), tupleInfo.getModuleName(), ev.getSource());
        //System.out.println(getName() + "   " + tupleInfo.getCloudletId() + "    " + predictime);
        if(predictime>0){
            tupleInfo.setCanBe(true);
            tupleInfo.setNeighborTime(predictime);
            //System.err.println(getName() + "  " + tupleInfo.getModuleName() + " can be processing1111  " + predictime + "  " + ev.getSource());
        }
        sendNeighbor(tupleInfo, ev.getSource(),1);
        //System.out.println("canProcessNeighborTuple used");
    }


    protected boolean AskNeibor(Tuple tuple, double predictTime){
        boolean flag = false;
        int num=1;
        for(NeighborInArea neighborInArea : neighbors){
            //System.out.println(getName() + "  asked" );
            //System.out.println(neighborInArea.getId()+ "   " + getId() + "     " + appToModulesMapNeighbor);
            if(neighborInArea.getId()!=getId()) {
                if (appToModulesMapNeighbor.get(neighborInArea.getId()).contains(tuple.getDestModuleName())) {
                    tupleToNeighbor tupleInfo = new tupleToNeighbor(tuple.getCloudletId(), tuple.getCloudletLength(), tuple.getCloudletFileSize(), predictTime, tuple.getDestModuleName());
                    sendNeighbor(tupleInfo, neighborInArea.getId(), 0);
                    tupleSendNum.put(tuple.getCloudletId(), num++);
                    flag = true;
                }
            }
        }

        return flag;

        //System.out.println("Askneibor used");
    }

    protected void getTheAnswer(SimEvent ev) {//写下思路：processTupleArrival 划分三种情况：1、直接处理 2、 根据answer 判断发送任务还是直接处理 3、判断能否处理后 在决定询问neighbor还是自己处理
        //李凯  20190528

        tupleToNeighbor info = ((tupleToNeighbor) ev.getData());

        if(!tupleSendNum.containsKey(info.getCloudletId())){
            return;
        }
        neighborCandidate.get(1);
        for (Pair<Integer, SimEvent> pair : tupleWaitSendToNeighbor) {
            if (pair.getFirst() == info.getCloudletId()) {
                if (info.getCanBe()) {
                    //System.out.println("getTheAnswer used");

                    if(!neighborCandidate.containsKey(info.getCloudletId())){
                        Pair<Integer,Double> pair1 = new Pair<>(ev.getSource(), info.getNeighborTime());
                        neighborCandidate.put(info.getCloudletId(), pair1);
                    }else{
                        double time = neighborCandidate.get(info.getCloudletId()).getSecond();
                        if(time>info.getNeighborTime()){
                            Pair<Integer,Double> pair1 = new Pair<>(ev.getSource(), info.getNeighborTime());
                            neighborCandidate.put(info.getCloudletId(), pair1);
                        }
                    }
                    int num = tupleSendNum.get(info.getCloudletId());
                    tupleSendNum.put(info.getCloudletId(),num-1);

                    //System.out.println(getName() + "   ok  " + (++ok1) + "   " + info.getModuleName());
                    //return;
                }else {
                    //System.out.println("getTheAnswer used");
                    int num = tupleSendNum.get(info.getCloudletId());
                    tupleSendNum.put(info.getCloudletId(),num-1);
                }

                if(tupleSendNum.get(info.getCloudletId())==0){
                    if(!neighborCandidate.containsKey(info.getCloudletId())){
                        Tuple tuple = (Tuple)(pair.getSecond()).getData();
                        sendUp(tuple);
                        tupleWaitSendToNeighbor.remove(pair);
                        tupleSendNum.remove(info.getCloudletId());
                        //System.out.println("getTheAnswer used");
                        return;
                    }else{
                        //System.out.println(getName() + "  " + neighborCandidate.get(info.getCloudletId()).getFirst() + "  " + neighborCandidate.get(info.getCloudletId()).getSecond());
                        sendNeighbor(((Tuple)pair.getSecond().getData()), neighborCandidate.get(info.getCloudletId()).getFirst());
                        tupleWaitSendToNeighbor.remove(pair);
                        tupleSendNum.remove(info.getCloudletId());
                        return;
                    }
                }
            }
        }


    }

    protected void setNeighborModuleList (SimEvent ev){ //怎么会被调用？？？
        Map<String, List<String>> appToModulesMapOfNeighbor = (Map<String, List<String>>)ev.getData();
        //System.out.println(getName() + "    " + appToModulesMapOfNeighbor + "  " + ev.getSource());
        if(appToModulesMapNeighbor.containsKey(ev.getSource())){
            appToModulesMapNeighbor.remove(ev.getSource());
        }
        if(appToModulesMapOfNeighbor!=null) {
            List<String> modules = new ArrayList<String>();
            for (String key : appToModulesMapOfNeighbor.keySet()) {
                List<String> moduleNameList = appToModulesMapOfNeighbor.get(key);

                for (String moduleName : moduleNameList){
                    modules.add(moduleName);
                }

            }
            appToModulesMapNeighbor.put(ev.getSource(), modules);
            //System.out.println(getName() + "    " + appToModulesMapNeighbor + "  " + ev.getSource());
        }

    }

    public void clearAppToModuleMap(){
        this.appToModulesMap.clear();
    }



    protected void sendToSelf(Tuple tuple){
        send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
    }
    public PowerHost getHost(){
        return (PowerHost) getHostList().get(0);
    }
    public int getParentId() {
        return parentId;
    }
    public void setParentId(int parentId) {
        this.parentId = parentId;
    }
    public List<Integer> getChildrenIds() {
        return childrenIds;
    }
    public void setChildrenIds(List<Integer> childrenIds) {
        this.childrenIds = childrenIds;
    }
    public double getUplinkBandwidth() {
        return uplinkBandwidth;
    }
    public void setUplinkBandwidth(double uplinkBandwidth) {
        this.uplinkBandwidth = uplinkBandwidth;
    }
    public double getNeighborBandwidth(){return neighborBandwidth;}
    public void setNeighborBandwidth(double neighborBandwidth){this.neighborBandwidth=neighborBandwidth;}

    public void setNeighborLatency(double neighborLatency){
        this.neighborLatency = neighborLatency;
    }

    public double getNeighborLatency() {
        return neighborLatency;
    }

    public double getUplinkLatency() {
        return uplinkLatency;
    }
    public void setUplinkLatency(double uplinkLatency) {
        this.uplinkLatency = uplinkLatency;
    }
    public boolean isSouthLinkBusy() {
        return isSouthLinkBusy;
    }
    public boolean isNorthLinkBusy() {
        return isNorthLinkBusy;
    }
    public boolean isNeighborBusy() {
        return isNeighborBusy;
    }
    public void setSouthLinkBusy(boolean isSouthLinkBusy) {
        this.isSouthLinkBusy = isSouthLinkBusy;
    }
    public void setNorthLinkBusy(boolean isNorthLinkBusy) {
        this.isNorthLinkBusy = isNorthLinkBusy;
    }
    public void setNeighborBusy(boolean isNeighborBusy){
        this.isNeighborBusy = isNeighborBusy;
    }
    public int getControllerId() {
        return controllerId;
    }
    public void setControllerId(int controllerId) {
        this.controllerId = controllerId;
    }
    public List<String> getActiveApplications() {
        return activeApplications;
    }
    public void setActiveApplications(List<String> activeApplications) {
        this.activeApplications = activeApplications;
    }
    public Map<Integer, List<String>> getChildToOperatorsMap() {
        return childToOperatorsMap;
    }
    public void setChildToOperatorsMap(Map<Integer, List<String>> childToOperatorsMap) {
        this.childToOperatorsMap = childToOperatorsMap;
    }

    public Map<String, Application> getApplicationMap() {
        return applicationMap;
    }

    public void setApplicationMap(Map<String, Application> applicationMap) {
        this.applicationMap = applicationMap;
    }

    public Queue<Tuple> getNorthTupleQueue() {
        return northTupleQueue;
    }

    public void setNorthTupleQueue(Queue<Tuple> northTupleQueue) {
        this.northTupleQueue = northTupleQueue;
    }

    public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
        return southTupleQueue;
    }

    public Queue<Pair<Tuple, Integer>> getNeighborTupleQueue(){
        return neighborTupleQueue;
    }

    public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
        this.southTupleQueue = southTupleQueue;
    }

    public double getDownlinkBandwidth() {
        return downlinkBandwidth;
    }

    public void setDownlinkBandwidth(double downlinkBandwidth) {
        this.downlinkBandwidth = downlinkBandwidth;
    }

    public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
        return associatedActuatorIds;
    }

    public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
        this.associatedActuatorIds = associatedActuatorIds;
    }

    public double getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(double energyConsumption) {
        this.energyConsumption = energyConsumption;
    }
    public Map<Integer, Double> getChildToLatencyMap() {
        return childToLatencyMap;
    }

    public void setChildToLatencyMap(Map<Integer, Double> childToLatencyMap) {
        this.childToLatencyMap = childToLatencyMap;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getRatePerMips() {
        return ratePerMips;
    }

    public void setRatePerMips(double ratePerMips) {
        this.ratePerMips = ratePerMips;
    }
    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public Map<String, Map<String, Integer>> getModuleInstanceCount() {
        return moduleInstanceCount;
    }

    public void setModuleInstanceCount(
            Map<String, Map<String, Integer>> moduleInstanceCount) {
        this.moduleInstanceCount = moduleInstanceCount;
    }

    /**
     * 自加
     */
    public void setNeighbors(List<NeighborInArea> neighbors){
        this.neighbors = neighbors;
    }

    public List<NeighborInArea> getNeighbors(){
        return neighbors;
    }

    public void setSelfInfo(NeighborInArea selfInfo){
        this.selfInfo = selfInfo;
    }

    public NeighborInArea getSelfInfo(){
        return selfInfo;
    }

    public Map<Integer, Map<String, Integer>> getSensorModuleChaineMap() {
        return sensorModuleChaineMap;
    }

    public void setSensorModuleChaineMap(Map<Integer, Map<String, Integer>> sensorModuleChaineMap) {
        this.sensorModuleChaineMap.putAll(sensorModuleChaineMap);
        //System.out.println(this.getName() + "  :  sensorModuleChaineMap:  " + sensorModuleChaineMap);
    }

    public void clearTupleFromNeighbor(){
        this.tupleFromNeighbor.clear();
        getHost().getVmScheduler().deallocatePesForAllVms();
    }
}