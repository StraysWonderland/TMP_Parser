import GraphStructure.GraphParserPBF;

import java.io.FileNotFoundException;

public class Main {

    public static void main(String[] args) {
        GraphParserPBF graphParserPBF = new GraphParserPBF();
        try {
            graphParserPBF.parseFromPbf();
            graphParserPBF.retrieveAmenityPOIs();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}