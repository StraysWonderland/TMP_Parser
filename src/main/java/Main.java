
import GraphStructure.StraysParser;

import java.io.IOException;

public class Main {

    private static String path = "bw.osm.pbf";

    public static void main(String[] args) {
        try {
            if (args[0] != null)
                path = args[0];

            StraysParser parser = new StraysParser();
            parser.parse(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}