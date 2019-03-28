# TMP_Parser
To run parser, place a pbf file in the root directory.
Run the jar file preferably by running the command:

    java -jar -Xmx=<AllocatedRAM> <JARNAME> <path to or name of pfb file>

    <JARNAME> should be TMPParser.jar, lying in the root of the directory,
    <name of pbf file> should usually be "target.osm.pbf" or the path to the file, if it is placed somewhere else
    <allocatedRam> shout at least be 16G

Example:

    java -jar -Xmx=20G TMPParser.jar target.osm.pbf

Alternatively, launch intelliJ and run the application ( set jav heapspace accordingly via Help -> customVMoptions -> Xmxs )
Created Files

The parser will create to files under TMPParser\ressources.

    Rename these files to "de.osm.edges" and "de.osm.nodes".
    Copy the files to the project directory of the actual application and place them in TravellingMisanthropistProblem\ressources. ( two already parsed files for bw can be found here as well )
