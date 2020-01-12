package org.fog.utils;

public class BestResult {
    private static double bestVaule = Integer.MAX_VALUE;

    private static double currentValue = Integer.MAX_VALUE;

    private static double tempValue = Integer.MAX_VALUE;

    public static double getBestVaule() {
        return bestVaule;
    }

    public static void setBestVaule(double bestVaule) {
        BestResult.bestVaule = bestVaule;
    }

    public static double getCurrentValue() {
        return currentValue;
    }

    public static void setCurrentValue(double currentValue) {
        BestResult.currentValue = currentValue;
    }

    public static double getTempValue() {
        return tempValue;
    }

    public static void setTempValue(double tempValue) {
        BestResult.tempValue = tempValue;
    }
}
