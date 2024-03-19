package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.util.PMap;

import java.util.HashMap;
import java.util.Map;

import static com.graphhopper.routing.ev.RouteNetwork.*;
import static com.graphhopper.routing.util.PriorityCode.UNCHANGED;

import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.PointList;

public class FootAverageSpeedParser extends AbstractAverageSpeedParser implements TagParser {
    static final double SLOW_SPEED = 2;
    static final double MEAN_SPEED = 5;
    protected Map<RouteNetwork, Integer> routeMap = new HashMap<>();

    public FootAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "foot"))),
                lookup.getDecimalEncodedValue(FerrySpeed.KEY));
    }

    protected FootAverageSpeedParser(DecimalEncodedValue speedEnc, DecimalEncodedValue ferrySpeed) {
        super(speedEnc, ferrySpeed);

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
        double elevation = getElevation(way, reverse);
        double distance = getEdgeDistance(way);
    
        if (distance > 0.0) {
            speed = (distance)/(distance/MEAN_SPEED + elevation/600.0);
        } else {
            speed = MEAN_SPEED;
        }
        return speed;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way) {
        String highwayValue = way.getTag("highway");
        if (highwayValue == null) {
            if (FerrySpeedCalculator.isFerry(way)) {
                double ferrySpeed = FerrySpeedCalculator.minmax(ferrySpeedEnc.getDecimal(false, edgeId, edgeIntAccess), avgSpeedEnc);
                setSpeed(false, edgeId, edgeIntAccess, ferrySpeed);
                if (avgSpeedEnc.isStoreTwoDirections())
                    setSpeed(true, edgeId, edgeIntAccess, ferrySpeed);
            }
            if (!way.hasTag("railway", "platform") && !way.hasTag("man_made", "pier"))
                return;
        }

        double speedFwd = getSpeed(way, false);
        double speedBwd = getSpeed(way, true);

        setSpeed(false, edgeId, edgeIntAccess, way.hasTag("highway", "steps") ? MEAN_SPEED - 2 : speedFwd);
            if (avgSpeedEnc.isStoreTwoDirections())
                setSpeed(true, edgeId, edgeIntAccess, way.hasTag("highway", "steps") ? MEAN_SPEED - 2 : speedBwd);
    }
}
