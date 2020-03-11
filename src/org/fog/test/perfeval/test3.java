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
import org.fog.placement.GenertedController;
import org.fog.placement.ModuleMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.*;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

public class test3 {
    static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<FogDevice> mobiles_D = new ArrayList<FogDevice>();
    static List<FogDevice> mobiles_H = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();

    static int numOfDepts = 2;
    static int numOfMobilesPerDept = 1;

    private static List<AreaOfDevice> areas = new ArrayList<AreaOfDevice>();

    static int sensorNum = 0;
    public static void main(String[] args) {
        Log.disable();

        double tem = 20000;
        int T = 5000;
        int N = 50;
        double q = 0.9;


        /*int i = 0;
        while (i++<100){
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events
            CloudSim.init(num_user, calendar, trace_flag);
            System.out.println("这是 i   " + i);
            appInit();


        }*/



        //tem为初始最大温度，T为外循环次数，N为内循环次数,q为降温系数
        int K=0;
        int Loop=0;
        int count=0;//记录随机变差过程中的接受次数
        double edif=0;//这是新解与旧解的差值
        //准备完毕，外循环开始
        while(K<T) {
            Loop=0;
            while(Loop<N) {
                //System.out.println("Loop     " + Loop);
               //将当前解复制一份，为后续获取邻域解做准备
                //执行一次逆转，获取邻域解

                int num_user = 1; // number of cloud users
                Calendar calendar = Calendar.getInstance();
                boolean trace_flag = false; // mean trace events
                CloudSim.init(num_user, calendar, trace_flag);
               // System.out.println("这是 i   " + i);
                appInit();
                CloudSim.stopSimulation();
                edif = BestResult.getCurrentValue() - BestResult.getTempValue();
                if(edif<=0) {
                    Loop++;
                    BestResult.setTempValue(BestResult.getCurrentValue());
                    if(BestResult.getCurrentValue() < BestResult.getBestVaule()){
                        BestResult.setBestVaule(BestResult.getCurrentValue());
                    }
                    //让这个邻域解的评价值取代旧解，并且把终止序列也一并取代
                }else {
                    if(Math.exp(0-(edif/tem))>Math.random()) {
                        count++;
                        Loop++;
                        BestResult.setTempValue(BestResult.getCurrentValue());
                    }else {
                        Loop++;
                    }
                }
                System.out.println("current loop: " + (K*50+Loop) + "   the best value : " + BestResult.getBestVaule() + "  Temp value :" + BestResult.getTempValue() + " current value :" + BestResult.getCurrentValue());

            }
            K++;
            tem=q*tem;//等比例降温，有需要时，此温度能作为跳出外循环的控制条件
        }
        System.out.println("the best value : " + BestResult.getBestVaule());

        System.exit(0);
    }

    public static void appInit(){
       try {
           FogUtils.initFogUtils();
           areas.clear();
           fogDevices.clear();
           mobiles_D.clear();
           mobiles_H.clear();
           sensors.clear();
           actuators.clear();
           TimeKeeper.getInstance().init();
           String appId0 = "DCNSFog";
           String appId1 = "HealthCareSystem";

           FogBroker broker0 = new FogBroker("broker_0");
           FogBroker broker1 = new FogBroker("broker_1");

           Application application_d = createApplication1(appId0, broker0.getId());
           Application application_h = createApplication2(appId1, broker1.getId());

           createFogDevices();

           createDCNSDevices(broker0.getId(), appId0);
           createHealthCareDevices(broker1.getId(), appId1);

           ModuleMapping moduleMapping_d = ModuleMapping.createModuleMapping(); // initializing a module mapping
           ModuleMapping moduleMapping_h = ModuleMapping.createModuleMapping(); // initializing a module mapping

           List<Application> apps = new ArrayList<>();
           apps.add(application_d);
           apps.add(application_h);
           Map<String, ModuleMapping> mappings = new HashMap<>();
           mappings.put(application_d.getAppId(), moduleMapping_d);
           mappings.put(application_h.getAppId(), moduleMapping_h);
           GenertedController genertedController = new GenertedController("generted", fogDevices, sensors, actuators, apps, mappings , areas, new ArrayList<String>());
           TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
           genertedController.startGenerted(false,null, null);
       }catch (Exception e){

       }
    }

    private static void createFogDevices() {
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25); // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.setParentId(-1);
        FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333); // creates the fog device Proxy Server (level=1)
        proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
        proxy.setUplinkLatency(50); // latency of connection from Proxy Server to the Cloud is 100 ms

        fogDevices.add(cloud);
        fogDevices.add(proxy);

        List<FogDevice> gateways = new ArrayList<FogDevice>();
        List<NeighborInArea> neighbors = new ArrayList<NeighborInArea>();
        for(int i=0;i<2;i++){
            FogDevice gateway = addGw1(i+"", proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
            gateways.add(gateway);
            neighbors.add(gateway.getSelfInfo());
        }
        for(int i=0;i<2;i++){
            FogDevice gateway = addGw2(i+"", proxy.getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
            gateways.add(gateway);
            neighbors.add(gateway.getSelfInfo());
        }
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
            int numofDc = 5;
            int numofHe = 1;
            List<FogDevice> AreaFogDevices = new ArrayList<FogDevice>();
            List<NeighborInArea> neighbors = new ArrayList<NeighborInArea>();
            //List<NeighborInArea> neighborsOfH = new ArrayList<NeighborInArea>();
            String mobileId = id+"-"+i;
            for(int num=0;num<numofDc;num++){
                FogDevice mobile_d = addMobile_D(mobileId+"-"+num, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
                mobile_d.setUplinkLatency(10); // latency of connection between the smartphone and proxy server is 4 ms
                fogDevices.add(mobile_d);
                AreaFogDevices.add(mobile_d);
                neighbors.add(mobile_d.getSelfInfo());
            }
            AreaOfDevice areaOfDevice = new AreaOfDevice(AreaFogDevices);
            areas.add(areaOfDevice);
           /* for(int num=0;num<numofHe;num++){
                FogDevice mobile_h = addMobile_H(mobileId+"-"+num, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
                mobile_h.setUplinkLatency(10); // latency of connection between the smartphone and proxy server is 4 ms
                fogDevices.add(mobile_h);
                AreaFogDevices.add(mobile_h);
                neighbors.add(mobile_h.getSelfInfo());
            }*/
            for(FogDevice fog : AreaFogDevices){
                fog.setNeighbors(neighbors);
            }
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
            int numofHe = 5;
            List<FogDevice> AreaFogDevices = new ArrayList<FogDevice>();
            List<NeighborInArea> neighbors = new ArrayList<NeighborInArea>();
            //List<NeighborInArea> neighborsOfH = new ArrayList<NeighborInArea>();
            String mobileId = id+"-"+i;
            /*for(int num=0;num<numofDc;num++){
                FogDevice mobile_d = addMobile_D(mobileId+"-"+num, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
                mobile_d.setUplinkLatency(10); // latency of connection between the smartphone and proxy server is 4 ms
                fogDevices.add(mobile_d);
                AreaFogDevices.add(mobile_d);
                neighbors.add(mobile_d.getSelfInfo());
            }*/
            for(int num=0;num<numofHe;num++){
                FogDevice mobile_h = addMobile_H(mobileId+"-"+num, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
                mobile_h.setUplinkLatency(10); // latency of connection between the smartphone and proxy server is 4 ms
                fogDevices.add(mobile_h);
                AreaFogDevices.add(mobile_h);
                neighbors.add(mobile_h.getSelfInfo());
            }
            AreaOfDevice areaOfDevice = new AreaOfDevice(AreaFogDevices);
            areas.add(areaOfDevice);
            for(FogDevice fog : AreaFogDevices){
                fog.setNeighbors(neighbors);
            }
        }
        return dept;
    }

    private static FogDevice addMobile_D(String id, int parentId){
        FogDevice mobile = createFogDevice("md-"+id, 5000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
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
            Sensor eegSensor = new Sensor("s-"+appId+"-"+id, "CAMERA", userId, appId, new DeterministicDistribution(5)); // inter-transmission time of EEG sensor follows a deterministic distribution
            sensors.add(eegSensor);
            Actuator display = new Actuator("a-"+appId+"-"+id, userId, appId, "PTZ_CONTROL");
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
        application.addAppModule("motion_detector", 1000, 10, 500, 0);
        application.addAppModule("object_detector",1000, 10, 500, 1);
        application.addAppModule("object_tracker", 1000, 10, 500, 2);
        application.addAppModule("user_interface", 10, 3);

        /*
         * Connecting the application modules (vertices) in the application model (directed graph) with edges
         */
        application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "CAMERA", Tuple.UP, AppEdge.SENSOR,999, 1); // adding edge from CAMERA (sensor) to Motion Detector module carrying tuples of type CAMERA
        application.addAppEdge("motion_detector", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", Tuple.UP, AppEdge.MODULE,20, 2); // adding edge from Motion Detector to Object Detector module carrying tuples of type MOTION_VIDEO_STREAM
        application.addAppEdge("object_detector", "user_interface", 1500, 2000, "DETECTED_OBJECT", Tuple.UP, AppEdge.MODULE, 20, 3); // adding edge from Object Detector to User Interface module carrying tuples of type DETECTED_OBJECT
        application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", Tuple.UP, AppEdge.MODULE, 20, 4); // adding edge from Object Detector to Object Tracker module carrying tuples of type OBJECT_LOCATION
        application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", Tuple.DOWN, AppEdge.ACTUATOR); // adding edge from Object Tracker to PTZ CONTROL (actuator) carrying tuples of type PTZ_PARAMS

        /*
         * Defining the input-output relationships (represented by selectivity) of the application modules.
         */
        application.addTupleMapping("motion_detector", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0)); // 1.0 tuples of type MOTION_VIDEO_STREAM are emitted by Motion Detector module per incoming tuple of type CAMERA
        application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0)); // 1.0 tuples of type OBJECT_LOCATION are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM
        application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(0.05)); // 0.05 tuples of type MOTION_VIDEO_STREAM are emitted by Object Detector module per incoming tuple of type MOTION_VIDEO_STREAM

        /*
         * Defining application loops (maybe incomplete loops) to monitor the latency of.
         * Here, we add two loops for monitoring : Motion Detector -> Object Detector -> Object Tracker and Object Tracker -> PTZ Control
         */
        final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("CAMERA");add("motion_detector");add("object_detector");add("object_tracker");}});
        final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}});
        List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};

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
        application.addAppEdge("Client", "Data_filtering", 4000, 500, "raw_heart", Tuple.UP, AppEdge.MODULE,10, 2);
        application.addAppEdge("Data_filtering", "Data_processing", 5500, 1500, "filtered_heart", Tuple.UP, AppEdge.MODULE,10, 3);
        application.addAppEdge("Data_processing", "Event_handler", 3000, 700, "analyzed_heart", Tuple.UP, AppEdge.MODULE,10, 4);
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


