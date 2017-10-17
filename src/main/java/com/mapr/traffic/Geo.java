package com.mapr.traffic;

import com.google.common.geometry.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;

/**
 * Simple geometric processing using S2.
 */
class Geo {
    // MapR headquarters in 2017
    private static final double BASE_LATITUDE = 37.4185099;
    private static final double BASE_LONGITUDE = -121.9450038;

    static S2CellId point(double latitude, double longitude) {
        return S2CellId.fromLatLng(S2LatLng.fromDegrees(latitude, longitude));
    }

    static double latitude(S2CellId point) {
        return point.toLatLng().lat().degrees();
    }

    static double longitude(S2CellId point) {
        return point.toLatLng().lng().degrees();

    }

    private static List<S2CellId> regionSearch(S2LatLng point, double dx, double dy) {
        new S2RegionCoverer()
                .getCovering(S2Cap.fromAxisAngle(point.toPoint(), S1Angle.radians(dx / S2LatLng.EARTH_RADIUS_METERS)));
        S2LatLngRect r = S2LatLngRect.fromCenterSize(point,
                S2LatLng.fromRadians(dx / S2LatLng.EARTH_RADIUS_METERS, dy / S2LatLng.EARTH_RADIUS_METERS));

        ArrayList<S2CellId> covering = new ArrayList<>();
        new S2RegionCoverer().getCovering(r, covering);
        return covering;
    }

    static <T> void scan(SortedMap<Long, T> spots, double x, double y, double limit,
                         Function<T, S2LatLng> getLocation, Function<T, Boolean> action) {
        S2LatLng base = getS2LatLng(x, y);
        List<S2CellId> searches = regionSearch(base, limit / 2, limit / 2);
        for (S2CellId search : searches) {
            long a = search.childBegin(S2CellId.MAX_LEVEL).id();
            long b = search.childEnd(S2CellId.MAX_LEVEL).id();
            Set<Long> keys = spots.tailMap(a).keySet();
            for (Long k : keys) {
                if (k > b) {
                    break;
                }
                T p = spots.get(k);
                if (base.getEarthDistance(getLocation.apply(p)) < limit) {
                    if (!action.apply(p)) {
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static S2LatLng getS2LatLng(double x, double y) {
        return S2LatLng.fromDegrees(BASE_LATITUDE, BASE_LONGITUDE)
                .add(S2LatLng.fromRadians(x / S2LatLng.EARTH_RADIUS_METERS, y / S2LatLng.EARTH_RADIUS_METERS));
    }
}
