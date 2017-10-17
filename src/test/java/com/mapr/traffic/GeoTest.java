package com.mapr.traffic;

import com.google.common.geometry.S2CellId;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class GeoTest {
    private double SCALE = (0.0 + 0x7fff_ffff);

    @Test
    public void point() {
        assertEquals(0, Geo.point(0, 0));
        long point = Geo.point(0, 180);
        assertEquals(String.format("got %016x", point), 0x2aaa_aaaa_aaaa_aaaaL, point);
        point = Geo.point(90, 0);
        assertEquals(String.format("got %016x", point), 0x1555_5555_5555_5555L, point);
        point = Geo.point(90, 180);
        assertEquals(String.format("got %016x", point), 0x3fff_ffff_ffff_ffffL, point);
        point = Geo.point(-90, 180);
        assertEquals(String.format("got %016x", point), 0x6aaaaaaaaaaaaaabL, point);
        point = Geo.point(-90, -180);
        assertEquals(String.format("got %016x", point), 0xc000_0000_0000_0003L, point);

        Random rand = new Random(10);
        double latitude = rand.nextDouble() * 180 - 90;
        double longitude = rand.nextDouble() * 180 - 90;
        long p1 = Geo.point(latitude, longitude);
        long p2 = Geo.point(latitude + rand.nextDouble() * 1e-5, longitude + rand.nextDouble() * 1e-5);
        assertTrue(Long.bitCount(Long.highestOneBit(p1 ^ p2) - 1) < 16);
    }

    @Test
    public void limit() {
        assertEquals(1.0 / 90 * SCALE, Geo.limit(1.0, 90.0), 1);
        assertEquals(-1.0 / 90 * SCALE + 2 * (SCALE + 1.0), Geo.limit(-1.0, 90.0), 1);
        try {
            Geo.limit(91.0, 90.0);
            fail("Should have failed");
        } catch (IllegalArgumentException e) {
            // ignore
        }
        assertEquals(3.0 / 10 * SCALE, Geo.limit(3.0, 10.0), 1);
        assertTrue(Geo.limit(90.0, 90.0) / 0x8000_0000L < 1);
        assertTrue(Geo.limit(-90, 90.0) / 0x8000_0000L > -1);
    }

    @Test
    public void extract() {
        Random rand = new Random(1);
        for (int i = 0; i < 1000; i++) {
            double y = rand.nextDouble() * 180 - 90;
            double x = rand.nextDouble() * 360 - 180;
            long point = Geo.point(y, x);
            double z = Geo.extract(point, 90);
            assertEquals(y, z, 1e-5);
            z = Geo.extract(point >> 1, 180);
            assertEquals(x, z, 1e-5);
        }
    }

    @Test
    public void coordinates() {
        Random rand = new Random(1);
        for (int i = 0; i < 1000; i++) {
            double y = rand.nextDouble() * 180 - 90;
            double x = rand.nextDouble() * 360 - 180;
            S2CellId point = Geo.point(y, x);
            assertEquals(y, Geo.latitude(point), 1e-5);
            assertEquals(x, Geo.longitude(point), 1e-5);
        }
    }

    private static class Point {
        double x;
        double y;
        S2CellId code;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
            code = Geo.point(y, x);
        }
    }

    @Test
    public void scan() {
        Random rand = new Random(1);

        List<ParkingSpot> data = new ArrayList<>();
        SortedMap<Long, ParkingSpot> table = new TreeMap<>();

        for (int i = 0; i < 10000; i++) {
            double y = rand.nextDouble() * 180 - 90;
            double x = rand.nextDouble() * 360 - 180;
            addPoint(data, table, y, x);
            addPoint(data, table, y + rand.nextDouble() * 1e-4, x + rand.nextDouble() * 1e-4);
            addPoint(data, table, y + rand.nextDouble() * 1e-4, x + rand.nextDouble() * 1e-4);
            addPoint(data, table, y + rand.nextDouble() * 1e-4, x + rand.nextDouble() * 1e-4);
        }

        for (int i = 0; i < 200; i++) {
            ParkingSpot p1 = data.get(rand.nextInt(data.size()));

                assertEquals(p1.x, point2.x, 2 * delta);
                assertEquals(p1.y, point2.y, 2 * delta);
            }
            for (Point point2 : data) {
                if (Math.abs(point2.x - p1.x) <= delta && Math.abs(point2.y - p1.y) <= delta) {
                    k--;
                }
            }
            assertEquals(0, k);
        }
    }

    private void addPoint(List<ParkingSpot> data, SortedMap<Long, ParkingSpot> table, double y, double x) {
        ParkingSpot p = new ParkingSpot(x, y);
        data.add(p);
        table.put(S2CellId.fromLatLng(p.getLocation()).id(), p);
    }
}