package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.RouteNetwork;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

import java.util.*;

import static com.graphhopper.routing.ev.RouteNetwork.*;
import static com.graphhopper.routing.util.PriorityCode.*;
import static com.graphhopper.routing.util.parsers.AbstractAccessParser.INTENDED;

import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.PointList;

public class FootferryAverageSpeedParser extends AbstractAverageSpeedParser implements TagParser {
    static final double SLOW_SPEED = 2.0;
    static final double MEAN_SPEED = 5.0;
    // larger value required - ferries are faster than pedestrians
    public static final double FERRY_SPEED = 15.0;
    final Set<String> safeHighwayTags = new HashSet<>();
    final Set<String> allowedHighwayTags = new HashSet<>();
    final Set<String> avoidHighwayTags = new HashSet<>();
    protected HashSet<String> sidewalkValues = new HashSet<>(5);
    protected HashSet<String> sidewalksNoValues = new HashSet<>(5);
    protected Map<RouteNetwork, Integer> routeMap = new HashMap<>();

    public FootferryAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "footferry"))));
    }

    protected FootferryAverageSpeedParser(DecimalEncodedValue speedEnc) {
        super(speedEnc, speedEnc.getNextStorableValue(FERRY_SPEED)); //how to handle ferry speed?

        routeMap.put(INTERNATIONAL, UNCHANGED.getValue());
        routeMap.put(NATIONAL, UNCHANGED.getValue());
        routeMap.put(REGIONAL, UNCHANGED.getValue());
        routeMap.put(LOCAL, UNCHANGED.getValue());
    }

    protected double getElevation(ReaderWay way, Boolean reverse) {

        Double elevation;

        if (reverse) {
                elevation = Double.valueOf(way.getTag("ascent:backward", "0.0"));
            } else {
               elevation = Double.valueOf(way.getTag("ascent:forward", "0.0"));
            }

        return elevation;
    }

   protected double getEdgeDistance(ReaderWay way) {
        PointList pointList = way.getTag("point_list", null);

        double distance;

        if (pointList != null) {   
            distance = DistanceCalcEarth.calcDistance(pointList, false) / 1000.0;
        } else {
            distance = 0.0;
        }

        return distance;
   }

    protected double getSpeed(ReaderWay way, Boolean reverse) {

        double speed; 
        double naismith_speed;
        double elevation = getElevation(way, reverse);
        double distance = getEdgeDistance(way);

        if (distance > 0.0) {
            naismith_speed = (distance)/(distance/MEAN_SPEED + elevation/600.0);
            if (naismith_speed > 0.05) {
                speed = naismith_speed;
            } else {
                speed = 0.05;
            }
        } else {
            speed = MEAN_SPEED;
        }
        return speed;
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way) {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null) {
            if (way.hasTag("route", ferries)) {
                double ferrySpeed = ferrySpeedCalc.getSpeed(way);
                setSpeed(edgeFlags, true, true, ferrySpeed);
            }
            if (!way.hasTag("railway", "platform") && !way.hasTag("man_made", "pier"))
                return;
        }

        double speedFwd = getSpeed(way, false);
        double speedBwd = getSpeed(way, true);

        
        setSpeed(false, edgeFlags, way.hasTag("highway", "steps") ? MEAN_SPEED - 2 : speedFwd);
            if (avgSpeedEnc.isStoreTwoDirections())
                setSpeed(true, edgeFlags, way.hasTag("highway", "steps") ? MEAN_SPEED - 2 : speedBwd);

        //String sacScale = way.getTag("sac_scale");
        //if (sacScale != null) {
        //    setSpeed(edgeFlags, true, true, "hiking".equals(sacScale) ? MEAN_SPEED : SLOW_SPEED);
        //} else {
        //    setSpeed(edgeFlags, true, true, way.hasTag("highway", "steps") ? MEAN_SPEED - 2 : MEAN_SPEED);
        //}
    }


    // should the below still be included?
    void setSpeed(IntsRef edgeFlags, boolean fwd, boolean bwd, double speed) {
        if (speed > getMaxSpeed())
            speed = getMaxSpeed();
        if (fwd)
            avgSpeedEnc.setDecimal(false, edgeFlags, speed);
        if (bwd && avgSpeedEnc.isStoreTwoDirections())
            avgSpeedEnc.setDecimal(true, edgeFlags, speed);
    }
}
