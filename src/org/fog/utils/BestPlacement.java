package org.fog.utils;

import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class BestPlacement {
    //现在只计算一个area
    private static Map<String, Map<Integer, Map<String, Integer>>> bestTupleChain = null;
    private static List<ModuleMapping> bestMapping = null;
    private static Map<String, Map<Integer, Map<String, Integer>>> tepTupleChain = null;
    private static List<ModuleMapping> tepMapping = null;
    private static Map<String, List<Integer>> replaceSensorChain = null;
    private static String temTime;
    protected static File file = new File("D:\\fog1\\fog4\\result.txt");
    protected static Writer out;

    static {
        try {
            out = new FileWriter(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Map<Integer, Map<String, Integer>>> getBestTupleChain() {
        return bestTupleChain;
    }

    public static void setBestTupleChain(Map<String, Map<Integer, Map<String, Integer>>> bestTupleChain) {
        BestPlacement.bestTupleChain = bestTupleChain;
    }

    public static List<ModuleMapping> getBestMapping() {
        return bestMapping;
    }

    public static void setBestMapping(List<ModuleMapping> bestMapping) {
        BestPlacement.bestMapping = bestMapping;
    }

    public static Map<String, Map<Integer, Map<String, Integer>>> getTepTupleChain() {
        return tepTupleChain;
    }

    public static void setTepTupleChain(Map<String, Map<Integer, Map<String, Integer>>> tepTupleChain) {
        BestPlacement.tepTupleChain = tepTupleChain;
    }

    public static List<ModuleMapping> getTepMapping() {
        return tepMapping;
    }

    public static void setTepMapping(List<ModuleMapping> tepMapping) {
        BestPlacement.tepMapping = tepMapping;
    }

    public static Map<String, List<Integer>> getReplaceSensorChain() {
        return replaceSensorChain;
    }

    public static void setReplaceSensorChain(Map<String, List<Integer>> replaceSensorChain) {
        BestPlacement.replaceSensorChain = replaceSensorChain;
    }

    public static void newTemTime(String time){
        temTime = time;
    }

    public static void addTemTime(String time){
        temTime = temTime + "\r\n" + time;
    }

    public static void saveResult() throws IOException {
        out.write("Best sensor tuple chain : \r\n" );
        if(bestTupleChain != null) {
           for (String appId : bestTupleChain.keySet()) {
                out.write(appId + "\r\n");
                Map<Integer, Map<String, Integer>> tupleMap = bestTupleChain.get(appId);

                for (Integer sensorId : tupleMap.keySet()) {
                    out.write(sensorId + " : ");
                    Map<String, Integer> devChain = tupleMap.get(sensorId);
                    for (String moduleName : devChain.keySet()) {
                        out.write(moduleName + " : " + devChain.get(moduleName));
                    }
                    out.write("  \r\n");
                }
           }
        }
        out.write("Best module mapping : \r\n" );
        if(bestMapping != null){
            for(ModuleMapping mapping : bestMapping){
                Map<String, List<String>> moduleMapping = mapping.getModuleMapping();
                for(String devName : moduleMapping.keySet()){
                    out.write(devName + " : ");
                    List<String> modules = moduleMapping.get(devName);
                    for (String moduleName : modules){
                        out.write(moduleName + ", ");
                    }
                    out.write(" \r\n");
                }
            }
        }
        out.write(" \r\n \r\n \r\n");
        out.flush();
    }

    public static void saveBestTime() throws IOException {
        out.write("best time : \r\n" );
        out.write(temTime + "\r\n");
    }

}
