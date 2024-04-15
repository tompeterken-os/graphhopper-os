/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * Used to store a priority value in the way flags of an edge. Used in combination with
 * PriorityWeighting
 *
 * @author Peter Karich
 */

/**
 * Modified to allow even lower priority factors 
 * (values all multiplied by 10 but now divide by 100 to get the factor)
 */

public enum PriorityCode {
    EXCLUDE(0),
    CUSTOM_DESTINATION(1),
    REACH_DESTINATION(10),
    VERY_BAD(30),
    BAD(50),
    AVOID_MORE(60),
    AVOID(80),
    SLIGHT_AVOID(90),
    UNCHANGED(100),
    SLIGHT_PREFER(110),
    PREFER(120),
    VERY_NICE(130),
    BEST(150);

    private final int value;
    public static final TreeMap<Integer, PriorityCode> VALUES = new TreeMap<>();

    static {
        PriorityCode[] v = values();
        for (PriorityCode priorityCode : v) {
            VALUES.put(priorityCode.getValue(), priorityCode);
        }
    }

    PriorityCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static double getFactor(int value) {
        return (double) value / 100.0;
    }

    public static double getValue(int value) {
        return getFactor(value);
    }

    public PriorityCode worse() {
        Map.Entry<Integer, PriorityCode> ret = VALUES.lowerEntry(this.getValue());
        return ret == null ? EXCLUDE : ret.getValue();
    }

    public static PriorityCode valueOf(int integ) {
        Map.Entry<Integer, PriorityCode> ret = VALUES.ceilingEntry(integ);
        return ret == null ? BEST : ret.getValue();
    }

    public PriorityCode better() {
        Map.Entry<Integer, PriorityCode> ret = VALUES.higherEntry(this.getValue());
        return ret == null ? BEST : ret.getValue();
    }
}
