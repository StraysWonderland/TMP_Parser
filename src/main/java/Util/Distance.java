package Util;

public class Distance {

    public static double euclideanDistance(double latNode1, double lngNode1, double latNode2, double lngNode2) {
        double x = latNode1 - latNode2;
        double y = (lngNode1 - lngNode2) * Math.cos(latNode2);
        return Math.sqrt(x * x + y * y) * 110.25;
    }

}
