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
package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.storage.IntsRef; // not in v8??
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;

import com.graphhopper.routing.util.parsers.helpers.OSMValueExtractor;
// import com.graphhopper.routing.util.FerrySpeedCalculator;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CarAverageSpeedParser extends AbstractAverageSpeedParser implements TagParser {

    public static final double CAR_MAX_SPEED = 140.0; // not included in 8.0 - changed to double but can I remove?
    protected final Map<String, Double> trackTypeSpeedMap = new HashMap<>();
    protected final Set<String> badSurfaceSpeedMap = new HashSet<>();
    // This value determines the maximal possible on roads with bad surfaces
    private final double badSurfaceSpeed;

    /**
     * A map which associates string to speed. Get some impression:
     * http://www.itoworld.com/map/124#fullscreen
     * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Maxspeed
     */
    protected final Map<String, Double> defaultSpeedMap = new HashMap<>();

    public CarAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "car"))),
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "car"))).getNextStorableValue(CAR_MAX_SPEED)
        );
    }

    public CarAverageSpeedParser(DecimalEncodedValue speedEnc, double maxPossibleSpeed) {
        super(speedEnc, maxPossibleSpeed);

        badSurfaceSpeedMap.add("cobblestone");
        badSurfaceSpeedMap.add("grass_paver");
        badSurfaceSpeedMap.add("gravel");
        badSurfaceSpeedMap.add("sand");
        badSurfaceSpeedMap.add("paving_stones");
        badSurfaceSpeedMap.add("dirt");
        badSurfaceSpeedMap.add("ground");
        badSurfaceSpeedMap.add("grass");
        badSurfaceSpeedMap.add("unpaved");
        badSurfaceSpeedMap.add("compacted");

        // autobahn
        defaultSpeedMap.put("motorway", 100.0);
        defaultSpeedMap.put("motorway_link", 70.0);
        // bundesstraße
        defaultSpeedMap.put("trunk", 70.0);
        defaultSpeedMap.put("trunk_link", 65.0);
        // linking bigger town
        defaultSpeedMap.put("primary", 65.0);
        defaultSpeedMap.put("primary_link", 60.0);
        // linking towns + villages
        defaultSpeedMap.put("secondary", 60.0);
        defaultSpeedMap.put("secondary_link", 50.0);
        // streets without middle line separation
        defaultSpeedMap.put("tertiary", 50.0);
        defaultSpeedMap.put("tertiary_link", 40.0);
        defaultSpeedMap.put("unclassified", 30.0);
        defaultSpeedMap.put("residential", 30.0);
        // spielstraße
        defaultSpeedMap.put("living_street", 5.0);
        defaultSpeedMap.put("service", 20.0);
        // unknown road
        defaultSpeedMap.put("road", 20.0);
        // forestry stuff
        defaultSpeedMap.put("track", 15.0);

        trackTypeSpeedMap.put("grade1", 20.0); // paved
        trackTypeSpeedMap.put("grade2", 15.0); // now unpaved - gravel mixed with ...
        trackTypeSpeedMap.put("grade3", 10.0); // ... hard and soft materials
        trackTypeSpeedMap.put(null, defaultSpeedMap.get("track"));

        // limit speed on bad surfaces to 30 km/h
        badSurfaceSpeed = 30.0;
    }

    protected double getSpeed(ReaderWay way, Boolean reverse) {
        String highwayValue = way.getTag("highway");
       //Integer speed = defaultSpeedMap.get(highwayValue);

       //// even inaccessible edges get a speed assigned
       //if (speed == null) speed = 10;

       //if (highwayValue.equals("track")) {
       //    String tt = way.getTag("tracktype");
       //    if (!Helper.isEmpty(tt)) {
       //        Integer tInt = trackTypeSpeedMap.get(tt);
       //        if (tInt != null)
       //            speed = tInt;
       //    }
       //}
        // Extract general average speed tag
        Double avgSpeed = Double.valueOf(way.getTag("avgspeed", "0.0"));

        // Extract directed average speed tag
        Double directedAvgSpeed = Double.valueOf(way.getTag(reverse ? "avgspeed:backward" : "avgspeed:forward", "0.0"));

        // Extract max speed tag
        Double maxSpeed = Double.valueOf(way.getTag("maxspeed", "0.0"));

        // Retrieve default speed based on highway value
        Double defaultSpeed = defaultSpeedMap.get(highwayValue);

        // If the directed average speed exists, use that value
        // Note: isValidSpeed comes from AbstractAverageSpeedParser.
        // See getMaxSpeed in AbstractAverageSpeedParser for an example usage.
        if (isValidSpeed(directedAvgSpeed) && directedAvgSpeed > 0.0) {
            return Math.max(directedAvgSpeed, 1.0); // Ensure speed is at least 1
        }
        // Otherwise, if the general average speed exists, use that value
        if (isValidSpeed(avgSpeed) && avgSpeed > 0.0) {
            return Math.max(avgSpeed, 1.0); // Ensure speed is at least 1
        }

        // Otherwise, if the max speed exists, use that value
        if (isValidSpeed(maxSpeed) && maxSpeed > 0.0) {
            return Math.max(maxSpeed, 1.0); // Ensure speed is at least 1
        }

        // Otherwise, if there's a default speed, use that value
        if (defaultSpeed != null) {
            return Math.max(defaultSpeed, 1.0); // Ensure speed is at least 1
        }

        // If none of the above, then return 10.0
        return 10.0; //decide on value here???
        
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way) {
       //String highwayValue = way.getTag("highway");
       //if (highwayValue == null) {
       //    if (way.hasTag("route", ferries)) {
       //        double ferrySpeed = ferrySpeedCalc.getSpeed(way);
       //        setSpeed(false, edgeFlags, ferrySpeed);
       //        if (avgSpeedEnc.isStoreTwoDirections())
       //            setSpeed(true, edgeFlags, ferrySpeed);
       //    }
       //    return;
       //}

        // get assumed speed from highway type
        double speedFwd = getSpeed(way, false);
        double speedBwd = getSpeed(way, true);

        setSpeed(false, edgeFlags, applyMaxSpeed(way, speedFwd, false));
        setSpeed(true, edgeFlags, applyMaxSpeed(way, speedBwd, true));
    }

    /**
     * @param way   needed to retrieve tags
     * @param speed speed guessed e.g. from the road type or other tags
     * @return The assumed speed.
     */
    protected double applyMaxSpeed(ReaderWay way, double speed, boolean bwd) {
        double maxSpeed = getMaxSpeed(way, bwd);
        return isValidSpeed(maxSpeed) ? maxSpeed * 0.9 : speed;
    }

    /**
     * @param way   needed to retrieve tags
     * @param speed speed guessed e.g. from the road type or other tags
     * @return The assumed speed
     */
    protected double applyBadSurfaceSpeed(ReaderWay way, double speed) {
        // limit speed if bad surface
        if (badSurfaceSpeed > 0 && isValidSpeed(speed) && speed > badSurfaceSpeed && way.hasTag("surface", badSurfaceSpeedMap))
            speed = badSurfaceSpeed;
        return speed;
    }
}
