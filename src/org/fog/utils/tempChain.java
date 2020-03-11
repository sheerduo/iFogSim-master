package org.fog.utils;

import java.util.Map;

public class tempChain {
   private static Map<Integer, Map<String, Integer>> chain;

    public static Map<Integer, Map<String, Integer>> getChain() {
        return chain;
    }

    public static void setChain(Map<Integer, Map<String, Integer>> chain) {
        tempChain.chain = chain;
    }
}
