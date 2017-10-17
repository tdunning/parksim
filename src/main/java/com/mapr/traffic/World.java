package com.mapr.traffic;

import com.google.common.geometry.*;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simulates a world with bounds and parking spaces.
 */
class World extends Sim<World> {
    // our testing ground is 3km on a side
    private static final int X_MAX = 3000;
    private static final int Y_MAX = 3000;

    private SortedMap<Long, ParkingSpot> spots = new TreeMap<>();

    World() {
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

    /**
     * Finds the parking spot closest to a particular point, but only if that
     * spot is withing the limit.
     *
     * @param x     Target location for parking spot (m from origin).
     * @param y     Target location for parking spot (m from origin).
     * @param limit Maximum allowable distance from target to parking spot.
     * @return The nearest parking spot or null if no suitable spot can be found.
     */
    ParkingSpot search(double x, double y, double limit) {
        return getParkingSpot(spots, x, y, limit);
    }

    /**
     * Does the actual parking spot search. This is exposed this way for testing.
     *
     * @param spots The table to search.
     * @param x     The target.
     * @param y     The target.
     * @param limit Maximum allowable distance from target to parking spot.
     * @return The nearest spot or null.
     */
    ParkingSpot getParkingSpot(SortedMap<Long, ParkingSpot> spots, double x, double y, double limit) {
        S2LatLng base = Geo.getS2LatLng(x, y);

        AtomicReference<ParkingSpot> best = new AtomicReference<>();
        AtomicReference<Double> closest = new AtomicReference<>(Double.MAX_VALUE);
        Geo.scan(spots, x, y, limit,
                ParkingSpot::getLocation,
                p -> {
                    double ds = p.getLocation().getEarthDistance(base);
                    if (!p.isInUse(this) && (best.get() == null || ds < closest.get())) {
                        best.set(p);
                        closest.set(ds);
                    }
                    return true;
                });
        return best.get();
    }
}
