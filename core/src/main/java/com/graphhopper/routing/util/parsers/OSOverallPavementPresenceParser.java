



package com.graphhopper.routing.util.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.parsers.helpers.OSMValueExtractor;
import com.graphhopper.storage.IntsRef;

import java.util.Collections;


public class OSOverallPavementPresenceParser implements TagParser {

    private final DecimalEncodedValue pavementEncoder;

    public OSOverallPavementPresenceParser(DecimalEncodedValue pavementEncoder) {
        this.pavementEncoder = pavementEncoder;
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way, IntsRef relationFlags) {
        OSMValueExtractor.extractPercent(edgeFlags, way, pavementEncoder, Collections.singletonList("os_overallpavementpresence"));
    }
}
