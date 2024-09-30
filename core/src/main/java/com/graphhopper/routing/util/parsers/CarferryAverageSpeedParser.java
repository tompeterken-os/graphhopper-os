package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.parsers.helpers.OSMValueExtractor;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;

public class CarferryAverageSpeedParser extends CarAverageSpeedParser {

    public CarferryAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "carferry"))),
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "carferry"))).getNextStorableValue(CAR_MAX_SPEED)
        );
    }

    public CarferryAverageSpeedParser(DecimalEncodedValue speedEnc, double maxPossibleSpeed) {
        super(speedEnc, maxPossibleSpeed);
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
}
