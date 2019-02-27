package Data;

import DTO.Highway;
import de.topobyte.osm4j.core.model.iface.OsmWay;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HighwayHandling {

    private static final List<String> desiredTypesValues = Arrays.asList(
            "motorway",
            "trunk",
            "primary",
            "secondary",
            "tertiary",
            "unclassified",
            "residential",
            "service",
            "living_street",
            "pedestrian",
            "track",
            "footway",
            "bridleway",
            "steps",
            "path",
            "cycleway",
            "unclassified",
            "motorway_link",
            "primary_link",
            "secondary_link",
            "tertiary_link",
            "trunk_link",
            "primary_link",
            "road");

    public static Set<String> desiredHighwayTypes = new HashSet<>(desiredTypesValues);

    public static boolean isHighway(String currentType) {
        if (currentType == null)
            return false;

        return Arrays.stream(Highway.values()).anyMatch(x -> x.getName().equals(currentType));
    }

    public static boolean isOneWay(OsmWay osmWay) {
        boolean oneWay = false;
        for (int j = 0; j < osmWay.getNumberOfTags(); j++) {
            oneWay = ((osmWay.getTag(j).getKey().equals("oneway")
                    && osmWay.getTag(j).getValue().equals("yes"))
                    || osmWay.getTag(j).getKey().equals("motorway")
                    || osmWay.getTag(j).getKey().equals("motorway_link")) || oneWay;
        }
        return oneWay;
    }
}
