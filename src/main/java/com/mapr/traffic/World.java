package com.mapr.traffic;

import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Simulates a world with bounds and parking spaces.
 */
public class World extends Sim<World> {
    private static final int X_MAX = 3000;
    private static final int Y_MAX = 3000;
    private static final double EARTH_RADIUS_KM = 12_756 / 2;
    private static final double RADIANS_TO_DEGREES = 180 / Math.PI;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180;

    private SortedMap<Long, ParkingSpot> spots = new TreeMap<>();

    public World() {
        set("xMax", X_MAX);
        set("yMax", Y_MAX);
        for (double x = 5; x < X_MAX; x += 10) {
            for (double y = 5; y < Y_MAX; y += 10) {
                ParkingSpot spot = new ParkingSpot(x, y);
                double latitude = latitude(y);
                double longitude = longitude(x);
                spots.put(Geo.point(latitude, longitude), spot);
            }
        }
    }

    ParkingSpot search(double x, double y, double limit) {
        double longitude = longitude(x);
        double latitude = latitude(y);
        long point = Geo.point(latitude, longitude);
        double dLat = limit / EARTH_RADIUS_KM * RADIANS_TO_DEGREES;
        long a = Geo.boundingBoxMin(point, dLat * Math.cos(40 * DEGREES_TO_RADIANS), dLat);
        long b = Geo.boundingBoxMax(point, dLat * Math.cos(40 * DEGREES_TO_RADIANS), dLat);

        Set<Long> keys = spots.tailMap(a).keySet();
        ParkingSpot best = null;
        double closest = Double.MAX_VALUE;
        for (Long k : keys) {
            if (k > b) {
                break;
            }
            ParkingSpot p = spots.get(k);
            double ds = distance(p, longitude, latitude);
            if (!p.isInUse() && ds < limit && (best == null || ds < closest)) {
                best = p;
                closest = ds;
            }
        }
        return best;
    }

    private static double longitude(double x) {
        return -102 + x / EARTH_RADIUS_KM * RADIANS_TO_DEGREES * Math.cos(40 * DEGREES_TO_RADIANS);
    }

    private static double latitude(double y) {
        return 40 + y / EARTH_RADIUS_KM * RADIANS_TO_DEGREES;
    }

    private static double xDistance(double longitude) {
        longitude += 102;
        return longitude * EARTH_RADIUS_KM * DEGREES_TO_RADIANS * Math.cos(40 * DEGREES_TO_RADIANS);
    }

    private static double yDistance(double latitude) {
        latitude -= 40;
        return latitude * EARTH_RADIUS_KM * DEGREES_TO_RADIANS;
    }

    private static double distance(ParkingSpot p, double longitude, double latitude) {
        return Math.abs(p.getX() - xDistance(longitude)) + Math.abs(p.getY() - yDistance(latitude));
    }
}
