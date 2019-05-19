package com.imperva.sampler;

import java.util.HashMap;
import java.util.TreeMap;

public class ThreadGroupSamples {

    public HashMap<Long, String> threadNamesMap = new HashMap<>();
    public TreeMap<String, SampleCount> samplingMap = new TreeMap<>();
    public int maxDepth = 0;

}
