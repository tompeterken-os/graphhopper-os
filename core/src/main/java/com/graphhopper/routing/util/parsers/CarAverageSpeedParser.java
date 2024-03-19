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
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CarAverageSpeedParser extends AbstractAverageSpeedParser implements TagParser {

    protected final Map<String, Double> trackTypeSpeedMap = new HashMap<>();
    protected final Set<String> badSurfaceSpeedMap = new HashSet<>();
    // This value determines the maximal possible on roads with bad surfaces
    private final double badSurfaceSpeed;

    /**
     * A map which associates string to speed. Get some impression:
     * http://www.itoworld.com/map/124#fullscreen
     * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing/Maxspeed
     */
    protected final Map<String, Double> defaultSpeedMap = new HashMap<String, Double>();

    public CarAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "car"))),
                lookup.getDecimalEncodedValue(FerrySpeed.KEY));
    }

    public CarAverageSpeedParser(DecimalEncodedValue speedEnc, DecimalEncodedValue ferrySpeed) {
        super(speedEnc, ferrySpeed);

        badSurfaceSpeedMap.add("cobblestone");
        badSurfaceSpeedMap.add("unhewn_cobblestone");
        badSurfaceSpeedMap.add("sett");
        badSurfaceSpeedMap.add("grass_paver");
        badSurfaceSpeedMap.add("gravel");
        badSurfaceSpeedMap.add("fine_gravel");
        badSurfaceSpeedMap.add("pebblestone");
        badSurfaceSpeedMap.add("sand");
        badSurfaceSpeedMap.add("paving_stones");
        badSurfaceSpeedMap.add("dirt");
        badSurfaceSpeedMap.add("earth");
        badSurfaceSpeedMap.add("ground");
        badSurfaceSpeedMap.add("wood");
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
        String highwayValue = way.getTag("highway", "");
//        Double speed = defaultSpeedMap.get(highwayValue);
//
//        // even inaccessible edges get a speed assigned
//        if (speed == null) speed = 10.0;
//
//        if (highwayValue.equals("track")) {
//            String tt = way.getTag("track type");
//            if (!Helper.isEmpty(tt)) {
//                Double tInt = trackTypeSpeedMap.get(tt);
//                if (tInt != null)
//                    speed = tInt;
//            }
//        }

        Double speed;

        // First find a avgspeed:forward or avgspeed:backward
        if (reverse) {
            speed = Double.valueOf(way.getTag("avgspeed:backward"));
        } else {
            speed = Double.valueOf(way.getTag("avgspeed:forward"));
        }

        // If no speed yet, use avgspeed
        if ((speed == 0.0) | (speed == null)) speed = Double.valueOf(way.getTag("avgspeed"));

        // If speed too small but exists, set to 1
        if ((speed < 1.0) & (speed > 0.0)) speed = 1.0;

        // If still no avgspeed, use maxspeed:forward and maxspeed:backward
        if ((speed == 0.0) | (speed == null)) {
            if (reverse) {
                speed = Double.valueOf(way.getTag("maxspeed:backward"));
            } else {
                speed = Double.valueOf(way.getTag("maxspeed:forward"));
            }
        }

        // If still no speed, use maxspeed
        if ((speed == 0.0) | (speed == null)) speed = Double.valueOf(way.getTag("maxspeed", "0.0"));

        // If still no speed, use default
        if ((speed == 0.0) | (speed == null)) speed = defaultSpeedMap.get(highwayValue);

        // even inaccessible edges get a speed assigned
        if ((speed == 0.0) | (speed == null)) speed = 5.0;

        return speed;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way) {
        if (FerrySpeedCalculator.isFerry(way)) {
            double ferrySpeed = FerrySpeedCalculator.minmax(ferrySpeedEnc.getDecimal(false, edgeId, edgeIntAccess), avgSpeedEnc);
            setSpeed(false, edgeId, edgeIntAccess, ferrySpeed);
            if (avgSpeedEnc.isStoreTwoDirections())
                setSpeed(true, edgeId, edgeIntAccess, ferrySpeed);
            return;
        }

        // get assumed speed from highway type
        double speedFwd = getSpeed(way, false);
        double speedBwd = getSpeed(way, true);
//        speed = applyBadSurfaceSpeed(way, speed);

        setSpeed(false, edgeId, edgeIntAccess, applyMaxSpeed(way, speedFwd, false));
        setSpeed(true, edgeId, edgeIntAccess, applyMaxSpeed(way, speedBwd, true));
    }

    /**
     * @param way   needed to retrieve tags
     * @param speed speed guessed e.g. from the road type or other tags
     * @return The assumed speed.
     */
    protected double applyMaxSpeed(ReaderWay way, double speed, boolean bwd) {
        double maxSpeed = getMaxSpeed(way, bwd);
        return Math.min(140, isValidSpeed(maxSpeed) ? Math.max(1, maxSpeed * 0.9) : speed);
    }

    /**
     * @param way   needed to retrieve tags
     * @param speed speed guessed e.g. from the road type or other tags
     * @return The assumed speed
     */
    protected double applyBadSurfaceSpeed(ReaderWay way, double speed) {
        // limit speed if bad surface
        if (badSurfaceSpeed > 0 && isValidSpeed(speed) && speed > badSurfaceSpeed) {
            String surface = way.getTag("surface", "");
            int colonIndex = surface.indexOf(":");
            if (colonIndex != -1)
                surface = surface.substring(0, colonIndex);
            if (badSurfaceSpeedMap.contains(surface))
                speed = badSurfaceSpeed;
        }
        return speed;
    }
}
