import GraphStructure.GraphParserPBF;
import GraphStructure.PBFParser;
import GraphStructure.ParserAY;

import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
       // GraphParserPBF graphParserPBF = new GraphParserPBF();
       // PBFParser parser = new PBFParser();

       // graphParserPBF.parseFromPbf();


        try {
            ParserAY parserAY = new ParserAY();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}