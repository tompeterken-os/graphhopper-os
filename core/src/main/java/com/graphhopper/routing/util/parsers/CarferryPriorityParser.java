package com.graphhopper.routing.util.parsers;

import com.graphhopper.routing.ev.*;
import com.graphhopper.util.PMap;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.routing.util.FerrySpeedCalculator;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.PriorityCode;

public class CarferryPriorityParser extends CarPriorityParser {

    public CarferryPriorityParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getDecimalEncodedValue(VehiclePriority.key(properties.getString("name", "carferry"))),
                lookup.getEnumEncodedValue(CarNetwork.KEY, RouteNetwork.class)
        );
    }

    public CarferryPriorityParser(DecimalEncodedValue priorityEnc, EnumEncodedValue<RouteNetwork> carRouteEnc) {
        super(priorityEnc, carRouteEnc);

       // addPushingSection("path");

        //avoidHighwayTags.add("trunk");
        //avoidHighwayTags.add("trunk_link");
        //avoidHighwayTags.add("primary");
        //avoidHighwayTags.add("primary_link");
        //avoidHighwayTags.add("secondary");
        //avoidHighwayTags.add("secondary_link");

        //preferHighwayTags.add("service");
        //preferHighwayTags.add("tertiary");
        //preferHighwayTags.add("tertiary_link");
        //preferHighwayTags.add("residential");
        //preferHighwayTags.add("unclassified");

        //setSpecificClassBicycle("touring");
    }
    
    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way, IntsRef relationFlags) {
    //public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        String highwayValue = way.getTag("highway");
        //Integer priorityFromRelation = routeMap.get(carRouteEnc.getEnum(false, edgeId, edgeIntAccess));
        Integer priorityFromRelation = routeMap.get(carRouteEnc.getEnum(false, edgeFlags));
        if (highwayValue == null) {
            //if (FerrySpeedCalculator.isFerry(way)) isFerry not a method in 7.0
            if (way.hasTag("route", ferries))
                priorityWayEncoder.setDecimal(false, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, false)));
                if (priorityWayEncoder.isStoreTwoDirections())
                    priorityWayEncoder.setDecimal(true, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, true)));
        } else {
            priorityWayEncoder.setDecimal(false, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, false)));
            if (priorityWayEncoder.isStoreTwoDirections())
                priorityWayEncoder.setDecimal(true, edgeFlags, PriorityCode.getValue(handlePriority(way, priorityFromRelation, true)));
        }
    }
}
