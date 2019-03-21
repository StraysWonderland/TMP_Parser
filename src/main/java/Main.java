
import GraphStructure.StraysParser;

import java.io.IOException;

public class Main {

    public static void main(String[] args) {
/*        GraphParserPBF graphParserPBF = new GraphParserPBF();
        graphParserPBF.parseFromPbf();*/

       // PBFParser parser = new PBFParser();

        String path = "bw.osm.pbf";

        try {
            if(args[0] != null)
                path = args[0];

            StraysParser parser = new StraysParser();
            parser.parse(path);
        } catch (IOException e) {
            e.printStackTrace();
        }


/*        try {
            ParserAY parserAY = new ParserAY();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

}