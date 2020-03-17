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
import org.fog.placement.GenertedController;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.NeighborInArea;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.io.File;
import java.io.Writer;
import java.util.*;

public class testplace {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<FogDevice> mobiles_D = new ArrayList<FogDevice>();
    static List<FogDevice> mobiles_H = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static int numOfDepts = 2;
    static int numOfMobilesPerDept = 1;

    private static List<AreaOfDevice> areas = new ArrayList<AreaOfDevice>();

    static int sensorNum = 0;
    protected static File file = new File("D:\\fog1\\fog2\\result.txt");
    protected static Writer out;

    private static boolean firstTime = true;
    private static Map<String, Map<Integer, List<String>>> sensor_moduleChain;
    private static List<FogDevice> devcieTable;
    private static GenertedController genertedController;

    private static int numOfSensor = 12;

    public static void main(String[] args) {

        Log.printLine("Starting TwoApps...");

        try {
            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId0 = "DCNSFog";
            String appId1 = "HealthCareSystem";

            FogBroker broker0 = new FogBroker("broker_0");
            FogBroker broker1 = new FogBroker("broker_1");

            Application application_d = createApplication1(appId0, broker0.getId());
            Application application_h = createApplication2(appId1, broker1.getId());

            createFogDevices();

            createDCNSDevices(broker0.getId(), appId0);
            createHealthCareDevices(broker1.getId(),appId1);

            ModuleMapping moduleMapping_d = ModuleMapping.createModuleMapping(); // initializing a module mapping
            ModuleMapping moduleMapping_h = ModuleMapping.createModuleMapping(); // initializing a module mapping

            List<Application> apps = new ArrayList<>();
            apps.add(application_d);
            apps.add(application_h);
            Map<String, ModuleMapping> mappings = new HashMap<>();
            mappings.put(application_d.getAppId(), moduleMapping_d);
            mappings.put(application_h.getAppId(), moduleMapping_h);

            for(FogDevice device : fogDevices) {
                if (device.getName().startsWith("md")) {
                    moduleMapping_d.addModuleToDevice("Client-1", device.getName());  // fixing all instances of the Client module to the Smartphones
                }
                if (device.getName().startsWith("mh")) {
                    moduleMapping_h.addModuleToDevice("Client", device.getName());  // fixing all instances of the Client module to the Smartphones
                }
            }
            List placedModules = new ArrayList();
            placedModules.add("Client");
            placedModules.add("Display");
            placedModules.add("Client-1");
            placedModules.add("Display-1");
            List<Application> apps1 = new ArrayList<>();
            apps1.add(application_d);
            apps1.add(application_h);
            mappings.put(application_d.getAppId(), moduleMapping_d);
            mappings.put(application_h.getAppId(), moduleMapping_h);

            Map<String, Map<Integer, Map<String, Integer>>> sensorChain = new HashMap<>();
           for(Application a: apps){
               sensorChain.put(a.getAppId(), new HashMap<>());
           }
           Map<String, Integer> chain = new HashMap<>();
            chain.put("Data_filtering-1", 18);
            chain.put("Data_processing-1", 12);
            chain.put("Event_handler-1", 12);
            sensorChain.get(apps.get(0).getAppId()).put(27, chain);

            chain.put("Data_filtering-1", 9);
            chain.put("Data_processing-1", 21);
            chain.put("Event_handler-1", 12);
            sensorChain.get(apps.get(0).getAppId()).put(29, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 12);
            chain.put("Data_processing-1", 9);
            chain.put("Event_handler-1", 18);
            sensorChain.get(apps.get(0).getAppId()).put(31, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 18);
            chain.put("Data_processing-1", 12);
            chain.put("Event_handler-1", 12);
            sensorChain.get(apps.get(0).getAppId()).put(33, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 25);
            chain.put("Data_processing-1", 15);
            chain.put("Event_handler-1", 6);
            sensorChain.get(apps.get(0).getAppId()).put(35, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 18);
            chain.put("Data_processing-1", 21);
            chain.put("Event_handler-1", 15);
            sensorChain.get(apps.get(0).getAppId()).put(37, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 18);
            chain.put("Data_processing-1", 9);
            chain.put("Event_handler-1", 21);
            sensorChain.get(apps.get(0).getAppId()).put(39, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 12);
            chain.put("Data_processing-1", 6);
            chain.put("Event_handler-1", 21);
            sensorChain.get(apps.get(0).getAppId()).put(41, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 12);
            chain.put("Data_processing-1", 21);
            chain.put("Event_handler-1", 18);
            sensorChain.get(apps.get(0).getAppId()).put(43, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering-1", 12);
            chain.put("Data_processing-1", 9);
            chain.put("Event_handler-1", 18);
            sensorChain.get(apps.get(0).getAppId()).put(45, chain);



            chain = new HashMap<>();
            chain.put("Data_filtering", 21);
            chain.put("Data_processing", 15);
            chain.put("Event_handler", 15);
            sensorChain.get(apps.get(1).getAppId()).put(47, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering", 18);
            chain.put("Data_processing", 18);
            chain.put("Event_handler", 12);
            sensorChain.get(apps.get(1).getAppId()).put(49, chain);

            chain = new HashMap<>();
            chain.put("Data_filtering", 15);
            chain.put("Data_processing", 4);
            chain.put("Event_handler", 4);
            sensorChain.get(apps.get(1).getAppId()).put(51, chain);






           /*for(Sensor sensor : sensors){
                if(sensor.getAppId().equals(appId0)){
                    Map<String, Integer> chain = new HashMap<>();
                    int fatherId=0;
                    for(FogDevice device : fogDevices){
                        if (device.getId()==sensor.getGatewayDeviceId()){
                            fatherId = device.getParentId();
                            break;
                        }
                    }
                    if(sensor.getId()==24) {
                        chain.put("Data_filtering-1", 6);
                        chain.put("Data_processing-1", 9);
                        chain.put("Event_handler-1", 12);
                    }else {
                        chain.put("Data_filtering-1", 4);
                        chain.put("Data_processing-1", 4);
                        chain.put("Event_handler-1", 4);
                    }
                    sensorChain.get(appId0).put(sensor.getId(), chain);
                }else {
                    Map<String, Integer> chain = new HashMap<>();
                    chain.put("Data_filtering", 4);
                    chain.put("Data_processing", 4);
                    chain.put("Event_handler", 4);
                    sensorChain.get(appId1).put(sensor.getId(), chain);
                }
           }*/

            genertedController = new GenertedController("generted", fogDevices, sensors, actuators, apps1, mappings , areas, placedModules);

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            genertedController.startStaticGenerted(sensorChain);
            for(Integer sensor111 :  TimeKeeper.getInstance().getSensorIdToActuator().keySet()){
                System.out.println("SensorId : " + sensor111 + " : " + TimeKeeper.getInstance().getSensorIdToActuator().get(sensor111) + "  num: " + TimeKeeper.getInstance().getSensorIdToNum().get(sensor111));
            }
            genertedController.printNetworkUsageDetails();
            genertedController.printCostDetails();
            genertedController.printPowerDetails();
            CloudSim.stopSimulation();

        }catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
    }
    private static void createFogDevices() {
        FogDevice cloud = createFogDevice("cloud", 50000, 40000, 10000, 10000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.setParentId(-1);
        FogDevice proxy = createFogDevice("proxy-server", 2500, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Proxy Server (level=1)
        proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
        proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms

        fogDevices.add(cloud);
        fogDevices.add(proxy);

        List<FogDevice> gateways = new ArrayList<FogDevice>();
        List<NeighborInArea> neighbors = new ArrayList<NeighborInArea>();
        List<FogDevice> AreaFogDevices = new ArrayList<FogDevice>();
        for(int i=0;i<5;i++){
            FogDevice gateway = addGw1(i+"", proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
            gateways.add(gateway);
            AreaFogDevices.add(gateway);
            neighbors.add(gateway.getSelfInfo());
        }
        for(int i=0;i<3;i++){
            FogDevice gateway = addGw2(i+"", proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
            gateways.add(gateway);
            AreaFogDevices.add(gateway);
            neighbors.add(gateway.getSelfInfo());
        }
        AreaOfDevice areaOfDevice = new AreaOfDevice(AreaFogDevices);
        areas.add(areaOfDevice);
        for(FogDevice fog: gateways){
            fog.setNeighbors(neighbors);
        }
    }

    private static FogDevice addGw1(String id, int parentId){
        FogDevice dept = createFogDevice("dd-"+id, 3000, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);
        fogDevices.add(dept);
        dept.setParentId(parentId);
        dept.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms
        for(int i=0;i<numOfMobilesPerDept;i++){
            int numofDc = 2;
            int numofHe = 1;
            //List<FogDevice> AreaFogDevices = new ArrayList<FogDevice>();
            List<NeighborInArea> neighbors = new ArrayList<NeighborInArea>();
            //List<NeighborInArea> neighborsOfH = new ArrayList<NeighborInArea>();
            String mobileId = id+"-"+i;
            for(int num=0;num<numofDc;num++){
                FogDevice mobile_d = addMobile_D(mobileId+"-"+num, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
                mobile_d.setUplinkLatency(10); // latency of connection between the smartphone and proxy server is 4 ms
                fogDevices.add(mobile_d);
                //AreaFogDevices.add(mobile_d);
                neighbors.add(mobile_d.getSelfInfo());
            }
            // AreaOfDevice areaOfDevice = new AreaOfDevice(AreaFogDevices);
            //areas.add(areaOfDevice);
           /* for(FogDevice fog : AreaFogDevices){
                fog.setNeighbors(neighbors);
            }*/
        }
        return dept;
    }
    private static FogDevice addGw2(String id, int parentId){
        FogDevice dept = createFogDevice("dh-"+id, 3000, 4000, 10000, 10000, 2, 0.0, 107.339, 83.4333);
        fogDevices.add(dept);
        dept.setParentId(parentId);
        dept.setUplinkLatency(4); // latency of connection between gateways and proxy server is 4 ms
        for(int i=0;i<numOfMobilesPerDept;i++){
            int numofDc = 2;
            int numofHe = 1;
            //List<FogDevice> AreaFogDevices = new ArrayList<FogDevice>();
            List<NeighborInArea> neighbors = new ArrayList<NeighborInArea>();
            String mobileId = id+"-"+i;

            for(int num=0;num<numofHe;num++){
                FogDevice mobile_h = addMobile_H(mobileId+"-"+num, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
                mobile_h.setUplinkLatency(10); // latency of connection between the smartphone and proxy server is 4 ms
                fogDevices.add(mobile_h);
                //AreaFogDevices.add(mobile_h);
                neighbors.add(mobile_h.getSelfInfo());
            }
            // AreaOfDevice areaOfDevice = new AreaOfDevice(AreaFogDevices);
            // areas.add(areaOfDevice);
            /*for(FogDevice fog : AreaFogDevices){
                fog.setNeighbors(neighbors);
            }*/
        }
        return dept;
    }

    private static FogDevice addMobile_D(String id, int parentId){
        FogDevice mobile = createFogDevice("md-"+id, 3000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
        mobile.setParentId(parentId);
        mobiles_D.add(mobile);
        return mobile;
    }

    private static FogDevice addMobile_H(String id, int parentId){
        FogDevice mobile = createFogDevice("mh-"+id, 5000, 2048, 10000, 270, 3, 0, 87.53, 82.44);
        mobile.setParentId(parentId);
        mobiles_H.add(mobile);
        return mobile;
    }

    private static void createDCNSDevices(int userId, String appId) {
        for(FogDevice mobile : mobiles_D){
            String id = mobile.getName();
            Sensor eegSensor = new Sensor("s-"+appId+"-"+id, "Heart_Sensor-1", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of EEG sensor follows a deterministic distribution
            sensors.add(eegSensor);
            Actuator display = new Actuator("a-"+appId+"-"+id, userId, appId, "Display-1");
            actuators.add(display);
            eegSensor.setGatewayDeviceId(mobile.getId());
            eegSensor.setLatency(1.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
            display.setGatewayDeviceId(mobile.getId());
            display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
        }
    }

    private static void createHealthCareDevices(int userId, String appId) {
        for(FogDevice mobile : mobiles_H){
            String id = mobile.getName();
            Sensor eegSensor = new Sensor("s-"+appId+"-"+id, "Heart_Sensor", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of EEG sensor follows a deterministic distribution
            sensors.add(eegSensor);
            Actuator display = new Actuator("a-"+appId+"-"+id, userId, appId, "Display");
            actuators.add(display);
            eegSensor.setGatewayDeviceId(mobile.getId());
            eegSensor.setLatency(1.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
            display.setGatewayDeviceId(mobile.getId());
            display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
        }
    }


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
            fogdevice = new MyFogDevice(nodeName, characteristics,
                    new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
        NeighborInArea neighborInfo = new NeighborInArea(fogdevice.getId(),mips,ram,upBw,downBw,ratePerMips,busyPower,idlePower);
        fogdevice.setLevel(level);
        fogdevice.setSelfInfo(neighborInfo);

        return fogdevice;
    }

    private static Application createApplication1(String appId, int userId){

        Application application = Application.createApplication(appId, userId);
        /*
         * Adding modules (vertices) to the application model (directed graph)
         */
        application.addAppModule("Client-1",500,10,500, 0);
        application.addAppModule("Data_filtering-1",1000,10,100, 1);
        application.addAppModule("Data_processing-1",1500,10,100, 2);
        application.addAppModule("Event_handler-1",1000, 10, 200, 3);
        //application.addAppModule("Client1",1000,10,500, 0);

        application.addAppEdge("Heart_Sensor-1", "Client-1", 500, 250, "Heart_Sensor-1", Tuple.UP, AppEdge.SENSOR,999, 1);
        application.addAppEdge("Client-1", "Data_filtering-1", 150, 250, "raw_heart-1", Tuple.UP, AppEdge.MODULE,10, 2);
        application.addAppEdge("Data_filtering-1", "Data_processing-1", 350, 750, "filtered_heart-1", Tuple.UP, AppEdge.MODULE,10, 3);
        application.addAppEdge("Data_processing-1", "Event_handler-1", 300, 350, "analyzed_heart-1", Tuple.UP, AppEdge.MODULE,10, 4);
        application.addAppEdge("Event_handler-1", "Client-1", 500, 250, "response_heart-1", Tuple.DOWN, AppEdge.MODULE,10,5);
        application.addAppEdge("Client-1", "Display-1", 250, 250, "display_data-1", Tuple.DOWN, AppEdge.ACTUATOR,10,1);
        ///application.addAppEdge("Event_handler", "Display", 500, 500, "response_heart", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("Client-1","Heart_Sensor-1","raw_heart-1", new FractionalSelectivity(1.0));
        application.addTupleMapping("Data_filtering-1","raw_heart-1","filtered_heart-1", new FractionalSelectivity(1.0));
        application.addTupleMapping("Data_processing-1","filtered_heart-1","analyzed_heart-1", new FractionalSelectivity(1.0));
        application.addTupleMapping("Event_handler-1","analyzed_heart-1","response_heart-1", new FractionalSelectivity(1.0));
        application.addTupleMapping("Client-1","response_heart-1","display_data-1", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("Heart_Sensor-1");add("Client-1");add("Data_filtering-1");add("Data_processing-1");add("Event_handler-1");add("Client-1");add("Display-1");}});
        //final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("Event_handler");add("Client");add("Display");}});
        List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
        application.setLoops(loops);
        return application;
    }

    private static Application createApplication2(String appId, int userId){
        Application application = Application.createApplication(appId, userId);


        application.addAppModule("Client",1000,10,500, 0);
        application.addAppModule("Data_filtering",2000,10,100, 1);
        application.addAppModule("Data_processing",3000,10,100, 2);
        application.addAppModule("Event_handler",2000, 10, 200, 3);
        //application.addAppModule("Client1",1000,10,500, 0);

        application.addAppEdge("Heart_Sensor", "Client", 1000, 500, "Heart_Sensor", Tuple.UP, AppEdge.SENSOR,999, 1);
        application.addAppEdge("Client", "Data_filtering", 3500, 500, "raw_heart", Tuple.UP, AppEdge.MODULE,10, 2);
        application.addAppEdge("Data_filtering", "Data_processing", 3000, 1500, "filtered_heart", Tuple.UP, AppEdge.MODULE,10, 3);
        application.addAppEdge("Data_processing", "Event_handler", 2000, 700, "analyzed_heart", Tuple.UP, AppEdge.MODULE,10, 4);
        application.addAppEdge("Event_handler", "Client", 1000, 500, "response_heart", Tuple.DOWN, AppEdge.MODULE,10,5);
        application.addAppEdge("Client", "Display", 500, 500, "display_data", Tuple.DOWN, AppEdge.ACTUATOR,10,1);
        ///application.addAppEdge("Event_handler", "Display", 500, 500, "response_heart", Tuple.DOWN, AppEdge.ACTUATOR);

        application.addTupleMapping("Client","Heart_Sensor","raw_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Data_filtering","raw_heart","filtered_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Data_processing","filtered_heart","analyzed_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Event_handler","analyzed_heart","response_heart", new FractionalSelectivity(1.0));
        application.addTupleMapping("Client","response_heart","display_data", new FractionalSelectivity(1.0));

        final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("Heart_Sensor");add("Client");add("Data_filtering");add("Data_processing");add("Event_handler");add("Client");add("Display");}});
        //final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("Event_handler");add("Client");add("Display");}});
        List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
        application.setLoops(loops);
        return application;
    }
}

