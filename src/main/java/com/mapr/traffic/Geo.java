package com.mapr.traffic;

import com.google.common.geometry.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple quad-tree substitute for S2.
 *
 * The world is a finite plane and latitude and longitude are taken as Euclidean
 * coordinates.
 */
class Geo {
    static S2CellId point(double latitude, double longitude) {
        return S2CellId.fromLatLng(S2LatLng.fromDegrees(latitude, longitude));
    }

    static double latitude(S2CellId point) {
        return point.toLatLng().lat().degrees();
    }

    static double longitude(S2CellId point) {
        return point.toLatLng().lng().degrees();

    }

    static List<S2CellId> regionSearch(S2LatLng point, double dx, double dy) {
        new S2RegionCoverer()
                .getCovering(S2Cap.fromAxisAngle(point.toPoint(), S1Angle.radians(dx / S2LatLng.EARTH_RADIUS_METERS)));
        S2LatLngRect r = S2LatLngRect.fromCenterSize(point,
                S2LatLng.fromRadians(dx / S2LatLng.EARTH_RADIUS_METERS, dy / S2LatLng.EARTH_RADIUS_METERS));

        ArrayList<S2CellId> covering = new ArrayList<>();
        new S2RegionCoverer().getCovering(r, covering);
        return covering;
    }
}
