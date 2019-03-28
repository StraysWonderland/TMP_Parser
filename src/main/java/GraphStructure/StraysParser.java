package GraphStructure;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import gnu.trove.list.TLongList;

import java.io.*;
import java.util.*;

public class StraysParser {

    private static String path = "target.osm.pbf";

    private Integer[] ids;
    private int[] edgeSource;
    private int[] edgeTarget;
    private int[] edgeDistance;
    private int numberOfEdges;

    private HashMap<Long, Integer> nodeMap;
    private double[][] nodes;

    private InputStream input;
    private OsmIterator iterator;

    private static List<String> pedWaysList = Arrays.asList("residential", "service", "living_street", "pedestrian", "track",
            "footway", "bridleway", "steps", "path", "cycleway", "trunk", "primary", "secondary", "tertiary",
            "trunk_link", "primary_link", "secondary_link", "tertiary_link", "road");
    private static Set<String> legalStreetsPEDSTRIAN = new HashSet<>(pedWaysList);

    public void parse(String filePath) throws IOException {

        path = filePath;

        System.out.println("start counting desirable edges");
        // Count number of ways; needed for array creation.
        numberOfEdges = countDesirableEdges();
        //int numberOfEdges = 124565264;
        System.out.println(String.format("Number of ways counted: %s", numberOfEdges));

        edgeSource = new int[numberOfEdges];
        edgeTarget = new int[numberOfEdges];
        edgeDistance = new int[numberOfEdges];

        System.out.println("edges array allocated");

        nodeMap = new HashMap<>();

        // Reset iterator and retrieve desired edges and nodes
        input = new FileInputStream(path);
        iterator = new PbfIterator(input, true);
        retrieveDesiredEdgesAndNodes();
        input.close();

        System.out.println(String.format("added %d nodes", nodeMap.size()));
        nodes = new double[3][nodeMap.size()];
        System.out.println("Created nodes array");

        // Reset iterator and get lat and lng for all nodes
        input = new FileInputStream(path);
        iterator = new PbfIterator(input, true);

        // Get node information.
        for (EntityContainer container : iterator) {
            String type = container.getType().toString();
            if ("Node".equals(type)) {
                OsmNode node = (OsmNode) container.getEntity();
                long ID = node.getId();
                if (nodeMap.containsKey(ID)) {
                    int pos = nodeMap.get(ID);
                    nodes[0][pos] = pos;
                    nodes[1][pos] = node.getLatitude();
                    nodes[2][pos] = node.getLongitude();
                }
            }
        }

        System.out.println("Filled nodes array with data");

        // Calculate distance for each edge and then calculate the time needed to travel
        // along this edge.
        for (int i = 0; i < numberOfEdges; i++) {
            double startNodeLat = nodes[1][edgeSource[i]];
            double startNodeLng = nodes[2][edgeSource[i]];
            double destNodeLat = nodes[1][edgeTarget[i]];
            double destNodeLng = nodes[2][edgeTarget[i]];

            // Replace speed limit by weight of edge.
            double dist = euclideanDist(startNodeLat, startNodeLng, destNodeLat, destNodeLng);
            edgeDistance[i] = (int) (dist * 10000);
        }

        System.out.println("Filled edges array with distances");

        // create index Array to refer to original edges and sort edges array
        ids = new Integer[numberOfEdges];
        for (int i = 0; i < numberOfEdges; i++)
            ids[i] = i;
        Arrays.sort(ids, Comparator.comparingInt(o -> edgeSource[o]));

        System.out.println("sorted edges array");

        writeNodesFile();
        writeEdgesFile();

        System.out.println("finished writing to file");
    }


    /**
     * @return Number of desirable edges
     * @throws IOException
     */
    private static int countDesirableEdges() throws IOException {
        int numWays = 0;
        // Open PBF file.
        InputStream input = new FileInputStream(path);
        // Iterate over PBF file and count number of edges.
        OsmIterator iterator = new PbfIterator(input, true);
        for (EntityContainer container : iterator) {
            String type = container.getType().toString();
            if (type.equals("Way")) {
                Map<String, String> WayTags = OsmModelUtil.getTagsAsMap(container.getEntity());
                OsmWay way = (OsmWay) container.getEntity();
                String highway = WayTags.get("highway");
                if (highway != null && legalStreetsPEDSTRIAN.contains(highway)) {
                    int NumberOfNodes = way.getNumberOfNodes();
                    numWays += (2 * NumberOfNodes) - 2;
                }
            }
        }
        input.close();
        return numWays;
    }

    /**
     * Retrieves the desired edges and corresponding nodes from the pbf file.
     * stores edge data in edge arrays
     * stores id of corresponding nodes in nodeMap
     */
    private void retrieveDesiredEdgesAndNodes() {
        String highway;
        String sidewalk;
        String motorroad;
        Map<String, String> WayTags;
        TLongList wayNodes;
        int wayNodesSize;
        int currentEdgeIndex = 0;
        int numberNodes = 0;

        // Create edges and store node IDs
        for (EntityContainer container : iterator) {
            String type = container.getType().toString();
            if ("Way".equals(type)) {
                WayTags = OsmModelUtil.getTagsAsMap(container.getEntity());

                highway = WayTags.get("highway");
                sidewalk = WayTags.get("sidewalk");
                motorroad = WayTags.get("motorroad");

                if ((sidewalk != null
                        && (sidewalk.equals("yes")
                        || sidewalk.equals("right")
                        || sidewalk.equals("left")
                        || sidewalk.equals("both")))
                        || ((motorroad == null || !motorroad.equals("yes"))
                        && highway != null
                        && legalStreetsPEDSTRIAN.contains(highway))) {

                    wayNodes = OsmModelUtil.nodesAsList((OsmWay) container.getEntity());
                    wayNodesSize = wayNodes.size();

                    if (!nodeMap.containsKey(wayNodes.get(0))) {
                        nodeMap.put(wayNodes.get(0), numberNodes);
                        numberNodes++;
                    }

                    for (int i = 1; i < wayNodesSize; i++) {
                        if (!nodeMap.containsKey(wayNodes.get(i))) {
                            nodeMap.put(wayNodes.get(i), numberNodes);
                            numberNodes++;
                        }
                        edgeSource[currentEdgeIndex] = nodeMap.get(wayNodes.get(i - 1));
                        edgeTarget[currentEdgeIndex] = nodeMap.get(wayNodes.get(i));
                        currentEdgeIndex++;
                    }
                    for (int i = wayNodesSize - 1; i > 0; i--) {
                        edgeSource[currentEdgeIndex] = nodeMap.get(wayNodes.get(i));
                        edgeTarget[currentEdgeIndex] = nodeMap.get(wayNodes.get(i - 1));
                        currentEdgeIndex++;
                    }
                }
            }
        }
    }

    /**
     * Write Edges into a custom txt-file
     *
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    private void writeEdgesFile() throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer;
        writer = new PrintWriter("ressources\\de.osm.edges",
                "UTF-8");
        writer.println(edgeSource.length);
        for (int i = 0; i < numberOfEdges; i++) {
            writer.print(edgeSource[ids[i]] + " ");
            writer.print(edgeTarget[ids[i]] + " ");
            writer.print(edgeDistance[ids[i]]);
            writer.println(" ");
        }
        writer.close();
    }

    /**
     * Write Nodes into a custom txt.file
     *
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     */
    private void writeNodesFile() throws FileNotFoundException, UnsupportedEncodingException {
        // Save data to file
        PrintWriter writer = new PrintWriter("ressources\\de.osm.nodes",
                "UTF-8");
        writer.println(nodes[0].length);
        for (int i = 0; i < nodes[0].length; i++) {
            writer.print((int) nodes[0][i] + " ");
            writer.print(nodes[1][i] + " ");
            writer.println(nodes[2][i]);
        }
        writer.close();
    }


    /**
     * @param node1_lat latitude of start node
     * @param node1_lng longitude of start node
     * @param node2_lat latitude of target node
     * @param node2_lng longitude of target node
     * @return distance between two nodes
     */
    private static double euclideanDist(double node1_lat, double node1_lng, double node2_lat, double node2_lng) {
        double degLen = 110.25;
        double x = node1_lat - node2_lat;
        double y = (node1_lng - node2_lng) * Math.cos(node2_lat);
        return Math.sqrt(x * x + y * y) * degLen;
    }


}