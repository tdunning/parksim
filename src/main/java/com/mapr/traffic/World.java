package com.mapr.traffic;

import com.google.common.geometry.*;

import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;

/**
 * Simulates a world with bounds and parking spaces.
 */
public class World extends Sim<World> {
    // our testing ground is 3km on a side
    private static final int X_MAX = 3000;
    private static final int Y_MAX = 3000;

    // MapR headquarters in 2017
    private static final double BASE_LATITUDE = 37.4185099;
    private static final double BASE_LONGITUDE = -121.9450038;

    private SortedMap<Long, ParkingSpot> spots = new TreeMap<>();

    public World() {
        set("xMax", X_MAX);
        set("yMax", Y_MAX);
        // parking spots are every 10 meters within our test ground
        for (double x = 5; x < X_MAX; x += 10) {
            for (double y = 5; y < Y_MAX; y += 10) {
                ParkingSpot spot = new ParkingSpot(x, y);
                spots.put(S2CellId.fromLatLng(spot.getLocation()).id(), spot);
            }
        }
    }

    ParkingSpot search(double x, double y, double limit) {
        return getParkingSpot(spots, x, y, limit);
    }

     static ParkingSpot getParkingSpot(SortedMap<Long, ParkingSpot> spots, double x, double y, double limit) {
        S2LatLng base = getS2LatLng(x, y);
        List<S2CellId> searches = Geo.regionSearch(base, limit / 2, limit / 2);

        AtomicReference<ParkingSpot> best = new AtomicReference<>();
        AtomicReference<Double> closest = new AtomicReference<>(Double.MAX_VALUE);
        World.scan(spots, x, y, limit, p -> {
            double ds = distance(p.getLocation(), base);
            if (!p.isInUse() && ds < limit && (best.get() == null || ds < closest.get())) {
                best.set(p);
                closest.set(ds);
            }
            return true;
        });
        return best.get();
    }

    private static void scan(SortedMap<Long, ParkingSpot> spots, double x, double y, double limit, Function<ParkingSpot, Boolean> action) {
        S2LatLng base = getS2LatLng(x, y);
        List<S2CellId> searches = Geo.regionSearch(base, limit / 2, limit / 2);
        for (S2CellId search : searches) {
            long a = search.childBegin(S2CellId.MAX_LEVEL).id();
            long b = search.childEnd(S2CellId.MAX_LEVEL).id();
            Set<Long> keys = spots.tailMap(a).keySet();
            for (Long k : keys) {
                if (k > b) {
                    break;
                }
                if (!action.apply(spots.get(k))) {
                    break;
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static S2LatLng getS2LatLng(double x, double y) {
        return S2LatLng.fromDegrees(BASE_LATITUDE, BASE_LONGITUDE)
                .add(S2LatLng.fromRadians(x / S2LatLng.EARTH_RADIUS_METERS, y / S2LatLng.EARTH_RADIUS_METERS));
    }

    private static double longitude(double x) {
        return S2LatLng.fromDegrees(BASE_LATITUDE, BASE_LONGITUDE)
                .add(S2LatLng.fromRadians(0, x / S2LatLng.EARTH_RADIUS_METERS)).lngDegrees();
    }

    private static double latitude(double y) {
        return S2LatLng.fromDegrees(BASE_LATITUDE, BASE_LONGITUDE)
                .add(S2LatLng.fromRadians(0, y / S2LatLng.EARTH_RADIUS_METERS)).latDegrees();
    }

    private static double distance(S2LatLng a, S2LatLng b) {
        return a.getEarthDistance(b);
    }
}
