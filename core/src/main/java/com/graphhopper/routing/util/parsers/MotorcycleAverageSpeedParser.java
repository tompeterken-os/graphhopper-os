package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.parsers.helpers.OSMValueExtractor;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

public class MotorcycleAverageSpeedParser extends CarAverageSpeedParser {
    public static final double MOTORCYCLE_MAX_SPEED = 120;

    public MotorcycleAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "motorcycle"))),
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "motorcycle"))).getNextStorableValue(MOTORCYCLE_MAX_SPEED)
        );
    }

    public MotorcycleAverageSpeedParser(DecimalEncodedValue speedEnc, double maxPossibleSpeed) {
        super(speedEnc, maxPossibleSpeed);

        defaultSpeedMap.clear();

        // autobahn
        defaultSpeedMap.put("motorway", 100.0);
        defaultSpeedMap.put("motorway_link", 70.0);
        // bundesstraße
        defaultSpeedMap.put("trunk", 80.0);
        defaultSpeedMap.put("trunk_link", 75.0);
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

        trackTypeSpeedMap.clear();
        trackTypeSpeedMap.put("grade1", 20.0);
        trackTypeSpeedMap.put(null, defaultSpeedMap.get("track"));
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way) {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null) {
            if (way.hasTag("route", ferries)) {
                double ferrySpeed = ferrySpeedCalc.getSpeed(way);
                setSpeed(false, edgeFlags, ferrySpeed);
                setSpeed(true, edgeFlags, ferrySpeed);
            }
        } else {
            double speedFwd = getSpeed(way, false);
            double speedBwd = getSpeed(way, true);
            setSpeed(true, edgeFlags, applyMaxSpeed(way, speedBwd, true));
            setSpeed(false, edgeFlags, applyMaxSpeed(way, speedFwd, true));
        }
    }

    protected double applyMaxSpeed(ReaderWay way, double speed, boolean bwd) {
        speed = super.applyMaxSpeed(way, speed, bwd);
        double maxMCSpeed = OSMValueExtractor.stringToKmh(way.getTag("maxspeed:motorcycle"));
        if (isValidSpeed(maxMCSpeed))
            speed = Math.min(maxMCSpeed * 0.9, speed);

        // limit speed to max 30 km/h if bad surface
        if (isValidSpeed(speed) && speed > 30 && way.hasTag("surface", badSurfaceSpeedMap))
            speed = 30;
        return speed;
    }
}
