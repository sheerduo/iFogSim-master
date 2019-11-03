package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.NeighborInArea;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class HealthCareSystem {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();
    static int numOfAreas = 2;
    static int numOfRouters = 2;
    static int numOfMobiles = 2;

    private static boolean CLOUD = false;

    public static void main(String[] args) {

        Log.printLine("Starting HealthCare...");
        try{
            Log.disable();
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "dcns"; // identifier of the application

            FogBroker broker = new FogBroker("broker");

            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());

            createFogDevices(broker.getId(), appId);

            Controller controller = null;

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
            for(FogDevice device : fogDevices){
                    if(device.getName().startsWith("m")){ // names of all Smart Cameras start with 'm'
                        moduleMapping.addModuleToDevice("Client", device.getName());  // fixing 1 instance of the Motion Detector module to each Smart Camera
                    /*moduleMapping.addModuleToDevice("object_detector", device.getName());
                    moduleMapping.addModuleToDevice("object_tracker", device.getName()  );*/
                    }
                /*if(device.getName().startsWith("r")){ // names of all Smart Cameras start with 'm'
                    moduleMapping.addModuleToDevice("Data_filtering", device.getName());  // fixing 1 instance of the Motion Detector module to each Smart Camera
                    moduleMapping.addModuleToDevice("Data_processing", device.getName());
                    moduleMapping.addModuleToDevice("Event_handler", device.getName()  );
                }
                if(device.getName().startsWith("f")){ // names of all Smart Cameras start with 'm'
                    moduleMapping.addModuleToDevice("Data_filtering", device.getName());  // fixing 1 instance of the Motion Detector module to each Smart Camera
                    moduleMapping.addModuleToDevice("Data_processing", device.getName());
                    moduleMapping.addModuleToDevice("Event_handler", device.getName()  );
                }*/
            }
           //moduleMapping.addModuleToDevice("user_interface", "cloud"); // fixing instances of User Interface module in the Cloud
            /*if(CLOUD){
                // if the mode of deployment is cloud-based
                //moduleMapping.addModuleToDevice("object_detector", "cloud"); // placing all instances of Object Detector module in the Cloud
                moduleMapping.addModuleToDevice("object_tracker", "cloud"); // placing all instances of Object Tracker module in the Cloud
            }*/

            controller = new Controller("master-controller", fogDevices, sensors,
                    actuators);

            controller.submitApplication(application,
                    (CLOUD)?(new ModulePlacementMapping(fogDevices, application, moduleMapping))
                            :(new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            CloudSim.startSimulation();

            CloudSim.stopSimulation();

            Log.printLine("HealthCare finished!");

        } catch (Exception e){
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }

        private static void createFogDevices(int userId, String appId){
        FogDevice cloud = createFogDevice("cloud", 40000, 40960, 1000, 10000, 0, 0.01, 500, 300);
        cloud.setParentId(-1);
        fogDevices.add(cloud);
        for(int i=0;i<numOfAreas;i++){
            FogDevice fog_gateway = createFogDevice("fog-gateway-"+i, 7000, 8192, 10000, 10000, 1, 0.0, 200, 100);
            fog_gateway.setParentId(cloud.getId());
            fog_gateway.setUplinkLatency(200);
            fogDevices.add(fog_gateway);
            for(int j=0;j<numOfRouters;j++){
                FogDevice router = addRouter(i+"-"+j, userId, appId, fog_gateway.getId());

            }

        }
    }

    private static FogDevice addRouter(String id, int userId, String appId, int parentId){
        FogDevice router = createFogDevice("router-"+id, 6000, 6144, 10000, 10000, 2, 0.0, 107, 83);
        fogDevices.add(router);

        router.setUplinkLatency(20);
        for(int i=0;i<numOfMobiles;i++){
            String mobileId = id+"-"+i;
            FogDevice mobile = addMobile(mobileId, userId, appId, router.getId());
            mobile.setUplinkLatency(5);
            fogDevices.add(mobile);
        }
        router.setParentId(parentId);
        return router;
    }

    private static FogDevice addMobile(String id, int userId, String appId, int parentId){
        FogDevice mobile = createFogDevice("mobile-"+id, 950, 2048, 100, 250, 3,0, 87, 82);
        mobile.setParentId(parentId);
        Sensor sensor = new Sensor("s-"+id, "Heart_Sensor", userId, appId, new DeterministicDistribution(10));
        sensors.add(sensor);
        Actuator actuator = new Actuator("a-"+id, userId, appId,"Display");
        sensor.setGatewayDeviceId(mobile.getId());
        sensor.setLatency(2.0);
        actuator.setGatewayDeviceId(mobile.getId());
        actuator.setLatency(2.0);
        actuators.add(actuator);
        return mobile;
    }

    /**
     * Creates a vanilla fog device
     * @param nodeName name of the device to be used in simulation
     * @param mips MIPS
     * @param ram RAM
     * @param upBw uplink bandwidth
     * @param downBw downlink bandwidth
     * @param level hierarchy level of the device
     * @param ratePerMips cost rate per MIPS used
     * @param busyPower
     * @param idlePower
     * @return
     */
    private static FogDevice createFogDevice(String nodeName, long mips,
                                             int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {

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
                new FogLinearPowerModel(busyPower, idlePower)
        );

        List<Host> hostList = new ArrayList<Host>();
        hostList.add(host);

        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.001; // the cost of using storage in this
        // resource
        double costPerBw = 0.0; // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
        // devices by now

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
       // NeighborInArea neighborInfo = new NeighborInArea(fogdevice.getId(),mips,ram,upBw,downBw,ratePerMips,busyPower,idlePower);
        fogdevice.setLevel(level);
        //fogdevice.setSelfInfo(neighborInfo);
        System.out.println(fogdevice.getName());
        return fogdevice;
    }

    private static Application createApplication(String appId, int userId){
        Application application = Application.createApplication(appId, userId);


        application.addAppModule("Client",1000,10,500, 0);
        application.addAppModule("Data_filtering",2500,10,100, 1);
        application.addAppModule("Data_processing",3000,10,100, 2);
        application.addAppModule("Event_handler",4000 , 10, 200, 3);

        application.addAppEdge("Heart_Sensor", "Client", 3000, 500, "Heart_Sensor", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("Client", "Data_filtering", 2000, 500, "raw_heart", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Data_filtering", "Data_processing", 1500, 1500, "filtered_heart", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Data_processing", "Event_handler", 1000, 700, "analyzed_heart", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Event_handler", "Client", 1000, 500, "response_heart", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("Client", "Display", 500, 500, "display_data", Tuple.DOWN, AppEdge.ACTUATOR);
        //application.addAppEdge("Event_handler", "Display", 500, 500, "display_data", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("Client","Heart_Sensor","raw_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Data_filtering","raw_heart","filtered_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Data_processing","filtered_heart","analyzed_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Event_handler","analyzed_heart","response_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Client","response_heart","display_data", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("Heart_Sensor");add("Client");add("Data_filtering");add("Data_processing");add("Event_handler");}});
        final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("Event_handler");add("Client");add("Display");}});
        List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
        application.setLoops(loops);
        return application;
    }
}
