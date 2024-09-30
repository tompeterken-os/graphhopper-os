package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.Roundabout;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

import java.util.Arrays;

public class CarferryAccessParser extends CarAccessParser {

    public CarferryAccessParser(EncodedValueLookup lookup, PMap properties) {
        this(lookup.getBooleanEncodedValue(VehicleAccess.key(properties.getString("name", "carferry"))),
                lookup.getBooleanEncodedValue(Roundabout.KEY),
                properties,
                TransportationMode.CAR);
    }

    public CarferryAccessParser(BooleanEncodedValue accessEnc, BooleanEncodedValue roundaboutEnc,
                                  PMap properties, TransportationMode transportationMode) {
        super(accessEnc, roundaboutEnc, properties, transportationMode);

        //barriers.remove("bus_trap");
        //barriers.remove("sump_buster");
//
        //trackTypeValues.clear();
        //trackTypeValues.addAll(Arrays.asList("grade1"));
    }

    //@Override
    public WayAccess getAccess(ReaderWay way) {
        String highwayValue = way.getTag("highway");
        String firstValue = way.getFirstPriorityTag(restrictions);
        if (highwayValue == null) {
            if (way.hasTag("route", ferries)) {
                if (restrictedValues.contains(firstValue))
                    return WayAccess.CAN_SKIP;
                if (intendedValues.contains(firstValue) ||
                        // implied default is allowed only if foot and bicycle is not specified:
                        firstValue.isEmpty() && !way.hasTag("foot") && !way.hasTag("bicycle"))
                    return WayAccess.FERRY;
            }
            return WayAccess.CAN_SKIP;
        }

        if ("service".equals(highwayValue) && "emergency_access".equals(way.getTag("service"))) {
            return WayAccess.CAN_SKIP;
        }

        if ("track".equals(highwayValue) && !trackTypeValues.contains(way.getTag("tracktype")))
            return WayAccess.CAN_SKIP;

        if (!highwayValues.contains(highwayValue))
            return WayAccess.CAN_SKIP;

        if (way.hasTag("impassable", "yes") || way.hasTag("status", "impassable"))
            return WayAccess.CAN_SKIP;

        // multiple restrictions needs special handling, see also motorcycle
        boolean permittedWayConditionallyRestricted = getConditionalTagInspector().isPermittedWayConditionallyRestricted(way);
        boolean restrictedWayConditionallyPermitted = getConditionalTagInspector().isRestrictedWayConditionallyPermitted(way);
        if (!firstValue.isEmpty()) {
            String[] restrict = firstValue.split(";");
            for (String value : restrict) {
                if (restrictedValues.contains(value) && !restrictedWayConditionallyPermitted)
                    return WayAccess.CAN_SKIP;
                if (intendedValues.contains(value) && !permittedWayConditionallyRestricted)
                    return WayAccess.WAY;
            }
        }

        if (isBlockFords() && ("ford".equals(highwayValue) || way.hasTag("ford")))
            return WayAccess.CAN_SKIP;

        if (permittedWayConditionallyRestricted)
            return WayAccess.CAN_SKIP;

        return WayAccess.WAY;
    
    }

    //@Override
    //public void handleWayTags(IntsRef edgeFlags, ReaderWay way) {
    //    WayAccess access = getAccess(way);
    //    if (access.canSkip())
    //        return;
//
    //    if (!access.isFerry()) {
//
    //        boolean isRoundabout = roundaboutEnc.getBool(false, edgeFlags);
    //        if (way.hasTag("oneway", oneways) || isRoundabout) {
    //            if (way.hasTag("oneway", "-1")) {
    //                accessEnc.setBool(true, edgeFlags, true);
    //            } else {
    //                accessEnc.setBool(false, edgeFlags, true);
    //            }
    //        } else {
    //            accessEnc.setBool(true, edgeFlags, true);
    //            accessEnc.setBool(false, edgeFlags, true);
    //        }
//
    //    } else {
    //        accessEnc.setBool(false, edgeFlags, true);
    //        accessEnc.setBool(true, edgeFlags, true);
    //    }
    //}
}
