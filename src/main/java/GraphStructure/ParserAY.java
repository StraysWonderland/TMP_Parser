package GraphStructure;

import DTO.Highway;
import Data.HighwayHandling;
import de.topobyte.osm4j.core.model.iface.*;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ParserAY {

    private final String GRAPH_PATH = "target.osm.ayp";
    private final String pbfPath = "target.osm.pbf";

    private HashMap<Long, double[]> nodeLookup = new HashMap<>();
    private List<double[]> edges = new ArrayList<>();
    private List<String> leisures = new ArrayList<>();
    private List<double[]> nodes;
    private PbfIterator pbfIterator;
    List<String > pedWaysList = Arrays.asList("residential", "service", "living_street", "pedestrian", "track",
            "footway", "bridleway", "steps", "path", "cycleway", "trunk", "primary", "secondary", "tertiary",
            "unclassified", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "road");

    Set<String> legalStreetsPEDSTRIAN =   new HashSet<>(pedWaysList);
    public ParserAY() throws IOException {

        // List with all the node IDs we need in the edges
        lookUpNodes();
        System.out.println("Looked up relevant nodes");

        // Get local_id, latitude and longitude for each relevant node
        addNodes();
        System.out.println("Added geo coordinates");

        sortNodes();

        // Splitting ways into edges
        addEdges();
        System.out.println("Added edges");

        sortEdges();
        writeIntoFile();
    }

    /**
     * Iterate through all ways of isMarker "highway" and are usable for car routing. Write out all nodes, that appear in
     * those ways to that we can exclude all nodes that are not needed in addNodes.
     *
     * @throws FileNotFoundException
     */
    private void lookUpNodes() throws FileNotFoundException {
        pbfIterator = new PbfIterator(new FileInputStream(new File(pbfPath)), false);
        for (EntityContainer container : pbfIterator) {
            if (container.getType() == EntityType.Way) {
                OsmWay osmWay = (OsmWay) container.getEntity();
                Map<String, String> WayTags = OsmModelUtil.getTagsAsMap(container.getEntity());
                String highway = WayTags.get("highway");
                String sidewalk = WayTags.get("sidewalk");
                String motorroad = WayTags.get("motorroad");
                if ((sidewalk != null && (sidewalk.equals("yes") || sidewalk.equals("right") || sidewalk.equals("left")
                        || sidewalk.equals("both")))
                        || ((motorroad == null || !motorroad.equals("yes")) && highway != null
                        && legalStreetsPEDSTRIAN.contains(highway))) {
                    for (int i = 0; i < osmWay.getNumberOfNodes(); i++) {
                        if (!nodeLookup.containsKey(osmWay.getNodeId(i))) {
                            nodeLookup.put(osmWay.getNodeId(i), new double[]{});
                        }
                    }
                }
            }
        }
    }

    /**
     * Iterate through all nodes and check if they are contained in the lookup map. If so, put the latitude and longitude
     * as additional information to these entries.
     *
     * @throws FileNotFoundException
     */
    private void addNodes() throws FileNotFoundException {
        pbfIterator = new PbfIterator(new FileInputStream(new File(pbfPath)), false);
        for (EntityContainer container : pbfIterator) {
            if (container.getType() == EntityType.Node) {
                OsmNode osmNode = (OsmNode) container.getEntity();
                if (nodeLookup.containsKey(osmNode.getId())) {
                    nodeLookup.put(osmNode.getId(), new double[]{(double) osmNode.getId()
                            , osmNode.getLatitude()
                            , osmNode.getLongitude()
                            , Double.MAX_VALUE
                    });
                }
            }
        }
    }

    /**
     * Iterate through all ways of isMarker "highway" and are usable for car routing. Parse these ways to edges and if there
     * is a bidirectional way, parse the reverse edges out of this way, too.
     *
     * @throws FileNotFoundException
     */
    private void addEdges() throws FileNotFoundException {
        pbfIterator = new PbfIterator(new FileInputStream(new File(pbfPath)), false);
        for (EntityContainer container : pbfIterator) {
            if (container.getType() == EntityType.Way) {
                OsmWay osmWay = (OsmWay) container.getEntity();
                if (isHighway(osmWay)) {
                    parseWayToEdges(osmWay);
                }
            }
        }
    }

    /**
     * Parse the given OsmWay to edges of format: SourceID, TargetID, Cost, Highway Type, Maxspeed
     * After that, it will be checked, if the Way is a oneway. If not, parse the given OsmWay to reverse edges of the same
     * format.
     *
     * @param osmWay An object of isMarker OsmWay which is relevant for car routing.
     */
    private void parseWayToEdges(OsmWay osmWay) {
        for (int i = 0; i < osmWay.getNumberOfNodes() - 1; i++) {
            double[] currentEdge = new double[5];
            long lo = osmWay.getNodeId(1);
            float[] edgeType = getMaxSpeed(osmWay);
            currentEdge[0] = nodeLookup.get(osmWay.getNodeId(i))[0];
            currentEdge[1] = nodeLookup.get(osmWay.getNodeId(i + 1))[0];
            currentEdge[3] = (double) edgeType[0];
            currentEdge[4] = (double) edgeType[1];

            double metricDistance = haversine(nodes.get((int) currentEdge[0])[1],
                    nodes.get((int) currentEdge[0])[2],
                    nodes.get((int) currentEdge[1])[1],
                    nodes.get((int) currentEdge[1])[2]);

            currentEdge[2] = 100 * metricDistance * 3600 / currentEdge[4];
            edges.add(currentEdge);
        }
        if (!HighwayHandling.isOneWay(osmWay)) {
            parseWayToReverseEdges(osmWay);
        }
    }

    /**
     * Parse the given OsmWay to reverse edges of format: SourceID, TargetID, Cost, Highway Type, Maxspeed
     *
     * @param osmWay An object of isMarker OsmWay which is relevant for car routing and has no oneway tag.
     */
    private void parseWayToReverseEdges(OsmWay osmWay) {
        for (int i = osmWay.getNumberOfNodes() - 1; i > 0; i--) {
            double[] currentReverseEdge = new double[5];
            float[] edgeType = getMaxSpeed(osmWay);
            currentReverseEdge[0] = nodeLookup.get(osmWay.getNodeId(i))[0];
            currentReverseEdge[1] = nodeLookup.get(osmWay.getNodeId(i - 1))[0];
            currentReverseEdge[3] = (double) edgeType[0];
            currentReverseEdge[4] = (double) edgeType[1];

            double metricDistance = haversine(nodes.get((int) currentReverseEdge[0])[1],
                    nodes.get((int) currentReverseEdge[0])[2],
                    nodes.get((int) currentReverseEdge[1])[1],
                    nodes.get((int) currentReverseEdge[1])[2]);

            currentReverseEdge[2] = 100 * metricDistance * 3600 / currentReverseEdge[4];
            edges.add(currentReverseEdge);
        }
    }

    /**
     * The entries in the node loopup map will be written into an ArrayList and sorted by their global node ID.
     * The index of a node is now the local node ID and is written into the lookup map as value of the key which is
     * the global ID of the node.
     */
    private void sortNodes() {
        nodes = new ArrayList<>(nodeLookup.values());
        Collections.sort(nodes, new GlobalIdComparator());
        for (int i = 0; i < nodes.size(); i++) {
            int localId = i;
            long globalId = (long) nodes.get(i)[0];
            nodeLookup.put(globalId, new double[]{
                    (double) localId
                    , (double) 0
                    , (double) 0
            });
        }
    }

    // Sort edges by their local source node id
    private void sortEdges() {
        Collections.sort(edges, new GlobalIdComparator());
    }

    private void writeIntoFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        System.out.println("Writing ...");

        // Save data to file
        PrintWriter writer = new PrintWriter("ressources\\de." + timeStamp + ".nodes",
                "UTF-8");
        writer.println(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            double[] node = nodes.get(i);

            writer.print(i + " ");
 /*           writer.print("" + (int) node[0] + " ");
            writer.print("" + (int) node[1] + " ");
            writer.println("" + (int) node[2]);
*/
            // first entry is the global id and we already appended the index for the local id
            for (int j = 1; j < node.length - 1; j++) {
                writer.print(node[j] + " ");
            }
            writer.println(" ");
        }


        writer.close();
        writer = new PrintWriter("ressources\\de." + timeStamp +".edges",
                "UTF-8");
        writer.println(edges.size());

        // Write edges
        for (int i = 0; i < edges.size(); i++) {
            double[] edge = edges.get(i);
            writer.print("" + (int) edge[0] + " ");
            writer.print("" + (int) edge[1] + " ");
            writer.println("" + (int) edge[2]);
        }
        writer.close();
        System.out.println("Finished parsing.");
    }

    /**
     * Custom comparator which compares the first elements of two arrays of isMarker double.
     */
    private static class GlobalIdComparator implements Comparator<double[]> {
        @Override
        public int compare(double[] o1, double[] o2) {
            return Double.compare(o1[0], o2[0]);
        }
    }


    private boolean isHighway(OsmWay osmWay) {
        boolean highWay = false;
        for (int i = 0; i < osmWay.getNumberOfTags(); i++) {
            if (osmWay.getTag(i).getKey().equals("highway")) {
                int finalI = i;
                if (Arrays.stream(Highway.values()).anyMatch(x -> x.getName().equals(osmWay.getTag(finalI).getValue()))) {
                    highWay = true;
                }
            }
        }
        return highWay;
    }


    public static float[] getMaxSpeed(OsmWay osmWay) {
        for (int j = 0; j < osmWay.getNumberOfTags(); j++) {
            if (osmWay.getTag(j).getKey().equals("highway")) {
                OsmTag osmTag = osmWay.getTag(j);
                float type = Highway.valueOf(osmTag.getValue()).getType();
                float maxSpeed = Highway.valueOf(osmTag.getValue()).getMaxSpeed();
                return new float[]{type, maxSpeed};
            }
        }
        return new float[]{};
    }


    public static double euclideanDistance(double latNode1, double lngNode1, double latNode2, double lngNode2) {
        double x = latNode1 - latNode2;
        double y = (lngNode1 - lngNode2) * Math.cos(latNode2);
        return Math.sqrt(x * x + y * y) * 110.25;
    }

    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6372.8; // In kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
}