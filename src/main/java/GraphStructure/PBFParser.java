package GraphStructure;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import gnu.trove.list.TLongList;

public class PBFParser {

	static String path = "bw.osm.pbf";

	public void parse() throws IOException {

		List<String > pedWaysList = Arrays.asList("residential", "service", "living_street", "pedestrian", "track",
				"footway", "bridleway", "steps", "path", "cycleway", "trunk", "primary", "secondary", "tertiary",
				"unclassified", "trunk_link", "primary_link", "secondary_link", "tertiary_link", "road");

		Set<String> legalStreetsPEDSTRIAN =   new HashSet<>(pedWaysList);

		// Count number of ways; needed for array creation.
		int numWays = countWays(legalStreetsPEDSTRIAN);
		//int numWays = 124565264;

		int[][] edges = new int[4][numWays];
		int edgesPos = 0;
		int numberNodes = 0;
		HashMap<Long, Integer> nodeMap = new HashMap<Long, Integer>();

		// Reset iterator
		InputStream input = new FileInputStream(path);
		OsmIterator iterator = new PbfIterator(input, true);

		// Create edges and store node IDs
		for (EntityContainer container : iterator) {
			String type = container.getType().toString();
			if (type.equals("Way")) {
				Map<String, String> WayTags = OsmModelUtil.getTagsAsMap(container.getEntity());
				String highway = WayTags.get("highway");
				String sidewalk = WayTags.get("sidewalk");
				String motorroad = WayTags.get("motorroad");
				if ((sidewalk != null && (sidewalk.equals("yes") || sidewalk.equals("right") || sidewalk.equals("left")
						|| sidewalk.equals("both")))
						|| ((motorroad == null || !motorroad.equals("yes")) && highway != null
								&& legalStreetsPEDSTRIAN.contains(highway))) {
					TLongList wayNodes = OsmModelUtil.nodesAsList((OsmWay) container.getEntity());
					if (!nodeMap.containsKey(wayNodes.get(0))) {
						nodeMap.put(wayNodes.get(0), numberNodes);
						numberNodes++;
					}
					for (int i = 1; i < wayNodes.size(); i++) {
						if (!nodeMap.containsKey(wayNodes.get(i))) {
							nodeMap.put(wayNodes.get(i), numberNodes);
							numberNodes++;
						}
						edges[0][edgesPos] = edgesPos;
						edges[1][edgesPos] = nodeMap.get(wayNodes.get(i - 1));
						edges[2][edgesPos] = nodeMap.get(wayNodes.get(i));
						edgesPos++;
					}
					for (int i = wayNodes.size() - 1; i > 0; i--) {
						edges[0][edgesPos] = edgesPos;
						edges[1][edgesPos] = nodeMap.get(wayNodes.get(i));
						edges[2][edgesPos] = nodeMap.get(wayNodes.get(i - 1));
						edgesPos++;
					}
				}
			}
		}

		System.out.println(nodeMap.size());
		double[][] nodes = new double[3][nodeMap.size()];
		System.out.println("YAY");

		// Reset iterator
		input.close();
		input = new FileInputStream(path);
		iterator = new PbfIterator(input, true);

		// Get node information.
		for (EntityContainer container : iterator) {
			String type = container.getType().toString();
			if (type.equals("Node")) {
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

		// Calculate distance for each edge and then calculate the time needed to travel
		// along this edge.
		for (int[] edge : edges) {
			double startNodeLat = nodes[1][edge[1]];
			double startNodeLng = nodes[2][edge[1]];
			double destNodeLat = nodes[1][edge[2]];
			double destNodeLng = nodes[2][edge[2]];
			double dist = euclideanDist(startNodeLat, startNodeLng, destNodeLat, destNodeLng);
			//int weight = (int) ((dist / edge[3]) * 100000);
			// Replace speed limit by weight of edge.
			edge[3] = (int) (dist*10000);
		}

		// Sort edges by ascending node id.
		Arrays.sort(edges, new Comparator<int[]>() {
			@Override
			public int compare(int[] o1, int[] o2) {
				return Double.compare(o1[1], o2[1]);
			}
		});

		// Save data to file
		PrintWriter writer = new PrintWriter("ressources\\de_nodes.fs",
				"UTF-8");
		writer.println(nodes[0].length);
		for (int i = 0; i < nodes[0].length; i++) {
			writer.print((int) nodes[0][i] + " ");
			writer.print(nodes[1][i] + " ");
			writer.println(nodes[2][i]);
		}
		writer.close();
		writer = new PrintWriter("ressources\\de_edges.fs",
				"UTF-8");
		writer.println(edges.length);
		for (int i = 0; i < edges.length; i++) {
			writer.print(edges[1][i] + " ");
			writer.print(edges[2][i] + " ");
			writer.println(edges[3][i]);
		}
		writer.close();
	}

	private static int countWays(Set<String> legalStreets) throws IOException {
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
				if (highway != null && legalStreets.contains(highway)) {
					int NumberOfNodes = way.getNumberOfNodes();
					numWays += (2 * NumberOfNodes) - 2;
					System.out.println("ways added: " + numWays);
					// numWays += NumberOfNodes - 1;
					// String oneWay = WayTags.get("oneway");
					// if (!highway.equals("motorway") && (oneWay == null || oneWay.equals("no"))) {
					// numWays += NumberOfNodes - 1;
					// }
				}
			}
		}
		input.close();
		return numWays;
	}

	private static int getSpeedLimit(String maxspeedTag, String highway) {
		if (maxspeedTag == null) {
			return getSpeed(highway);
		} else if (StringUtils.isNumeric(maxspeedTag)) {
			return Integer.parseInt(maxspeedTag);
		} else if (maxspeedTag.equals("none")) {
			return 150;
		} else if (maxspeedTag.equals("walk")) {
			return 5;
		} else {
			return getSpeed(highway);
		}
	}

	private static int getSpeed(String tag) {
		switch (tag) {
		case "motorway":
			return 120;
		case "motorway_link":
			return 60;
		case "trunk":
			return 100;
		case "trunk_link":
			return 50;
		case "primary":
			return 60;
		case "primary_link":
			return 50;
		case "secondary":
			return 50;
		case "secondary_link":
			return 50;
		case "tertiary":
			return 50;
		case "tertiary_link":
			return 50;
		case "unclassified":
			return 40;
		case "residential":
			return 30;
		case "service":
			return 10;
		case "living_street":
			return 5;
		default:
			return 50;
		}
	}

	private static double euclideanDist(double node1_lat, double node1_lng, double node2_lat, double node2_lng) {
		double degLen = 110.25;
		double x = node1_lat - node2_lat;
		double y = (node1_lng - node2_lng) * Math.cos(node2_lat);
		return Math.sqrt(x * x + y * y) * degLen;
	}

}