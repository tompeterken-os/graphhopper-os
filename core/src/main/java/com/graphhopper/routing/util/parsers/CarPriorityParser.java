package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.*;
//import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

import java.util.*;

import static com.graphhopper.routing.ev.RouteNetwork.*;
import static com.graphhopper.routing.util.PriorityCode.*;
import static com.graphhopper.routing.util.parsers.AbstractAccessParser.FERRIES;
import static com.graphhopper.routing.util.parsers.AbstractAccessParser.INTENDED;
import static com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser.getMaxSpeed;
import static com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser.isValidSpeed;

/**
 * new Car Priority Parser
 * Prevents cars from routing along tracks or 'private' roads (from vehicle access in MRN) 
 * unless the destination lies on these roads
 */

public class CarPriorityParser implements TagParser {
    final Set<String> ferries = new HashSet<>(FERRIES);
    final Set<String> intendedValues = new HashSet<>(INTENDED);
    final Set<String> safeHighwayTags = new HashSet<>();
    final Set<String> avoidHighwayTags = new HashSet<>();
    protected HashSet<String> sidewalkValues = new HashSet<>(5);
    protected HashSet<String> sidewalksNoValues = new HashSet<>(5);
    protected final DecimalEncodedValue priorityWayEncoder;
    protected EnumEncodedValue<RouteNetwork> carRouteEnc;
    protected Map<RouteNetwork, Integer> routeMap = new HashMap<>();

    public CarPriorityParser(EncodedValueLookup lookup, PMap properties) {
        this(lookup.getDecimalEncodedValue(VehiclePriority.key(properties.getString("name", "car"))),
                lookup.getEnumEncodedValue(CarNetwork.KEY, RouteNetwork.class)
        );
    }

    protected CarPriorityParser(DecimalEncodedValue priorityEnc, EnumEncodedValue<RouteNetwork> carRouteEnc) {
        this.carRouteEnc = carRouteEnc;
        priorityWayEncoder = priorityEnc;

        avoidHighwayTags.add("track");

        routeMap.put(INTERNATIONAL, UNCHANGED.getValue());
        routeMap.put(NATIONAL, UNCHANGED.getValue());
        routeMap.put(REGIONAL, UNCHANGED.getValue());
        routeMap.put(LOCAL, UNCHANGED.getValue());
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way, IntsRef relationFlags) {
    //public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String highwayValue = way.getTag("highway");
        //Integer priorityFromRelation = routeMap.get(carRouteEnc.getEnum(false, edgeId, edgeIntAccess));
        Integer priorityFromRelation = routeMap.get(carRouteEnc.getEnum(false, edgeFlags));
        //if (highwayValue == null) {
        //    //if (FerrySpeedCalculator.isFerry(way)) isFerry not a method in 7.0
        //    if (way.hasTag("route", ferries))
        //        priorityWayEncoder.setDecimal(false, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, false)));
        //        if (priorityWayEncoder.isStoreTwoDirections())
        //            priorityWayEncoder.setDecimal(true, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, true)));
        //} else {
        if (highwayValue != null) {
            priorityWayEncoder.setDecimal(false, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, false)));
            if (priorityWayEncoder.isStoreTwoDirections())
                priorityWayEncoder.setDecimal(true, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, true)));
        }
    }

    public int handlePriority(ReaderWay way, Integer priorityFromRelation, Boolean reverse) {
       TreeMap<Double, Integer> weightToPrioMap = new TreeMap<>();
       if (priorityFromRelation == null)
           weightToPrioMap.put(0d, UNCHANGED.getValue());
       else
           weightToPrioMap.put(110d, priorityFromRelation);
       collect(way, weightToPrioMap, reverse);
       // pick priority with biggest order value
       return weightToPrioMap.lastEntry().getValue();
    }



    /**
     * @param weightToPrioMap associate a weight with every priority. This sorted map allows
     *                        subclasses to 'insert' more important priorities as well as overwrite determined priorities.
     */
    void collect(ReaderWay way, TreeMap<Double, Integer> weightToPrioMap, Boolean reverse) {
        String highway = way.getTag("highway");

        if (way.hasTag("motor_vehicle", "private"))
            weightToPrioMap.put(40d, PRIVATE_DESTINATION.getValue());

        if (way.hasTag("motor_vehicle:forward", "private"))
            if (reverse == false)
                weightToPrioMap.put(40d, PRIVATE_DESTINATION.getValue());

        if (way.hasTag("motor_vehicle:backward", "private"))
            if (reverse == true)
                weightToPrioMap.put(40d, PRIVATE_DESTINATION.getValue());

        if (way.hasTag("fictional", "yes"))
                weightToPrioMap.put(40d, PRIVATE_DESTINATION.getValue());

        if (avoidHighwayTags.contains(highway)) {
            weightToPrioMap.put(40d, PRIVATE_DESTINATION.getValue());
        } 

    }
}