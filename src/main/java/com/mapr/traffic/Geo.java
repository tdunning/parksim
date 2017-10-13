package com.mapr.traffic;

/**
 * Simple quad-tree substitute for S2.
 *
 * The world is a finite plane and latitude and longitude are taken as Euclidean
 * coordinates.
 */
class Geo {
    static long point(double latitude, double longitude) {
        // scale coordinates into [-1,1] x [-1,1] square
        long y = limit(latitude, 90);
        long x = limit(longitude, 180);
        long r = 0;
        for (int i = 0; i < 32; i++) {
            r = (r << 2) + ((x >>> 31) << 1) + ((y >>> 31));
            x = (x << 1) & 0xffff_ffffL;
            y = (y << 1) & 0xffff_ffffL;
        }
        return r;
    }

    static double latitude(long point) {
        return extract(point, 90);
    }

    static double longitude(long point) {
        return extract(point >> 1, 180);
    }

    static long boundingBoxMin(long point, double dx, double dy) {
        double y = latitude(point);
        double x = longitude(point);
        return point(y - dy, x - dx);
    }

    static long boundingBoxMax(long point, double dx, double dy) {
        double y = latitude(point);
        double x = longitude(point);
        return point(y + dy, x + dx);
    }

    static double extract(long point, double limit) {
        point = point & 0x5555_5555_5555_5555L;
        long r = 0;
        for (int i = 0; i < 32; i++) {
            r = r + ((point >> i) & (1L << i));
        }
        double x = 2.0 * r * limit / 0xffff_ffffL;
        if (x > limit) {
            return x - 2 * limit;
        } else {
            return x;
        }
    }

    static long limit(double x, double limit) {
        if (Math.abs(x) > limit) {
            throw new IllegalArgumentException("Bad coordinate");
        }
        return 0xffff_ffffL & (long) (0x7fff_ffffL * (x / limit));
    }
}
