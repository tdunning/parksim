package com.mapr.traffic;

import com.google.common.geometry.S2CellId;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class GeoTest {
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

    @Test
    public void scan() {
        Random rand = new Random(1);

        List<ParkingSpot> data = new ArrayList<>();
        SortedMap<Long, ParkingSpot> table = new TreeMap<>();

        for (int i = 0; i < 10000; i++) {
            double y = rand.nextDouble() * 180 - 90;
            double x = rand.nextDouble() * 360 - 180;
            addPoint(data, table, y, x);
            addPoint(data, table, y + rand.nextDouble() * 10, x + rand.nextDouble() * 10);
            addPoint(data, table, y + rand.nextDouble() * 10, x + rand.nextDouble() * 10);
            addPoint(data, table, y + rand.nextDouble() * 10, x + rand.nextDouble() * 10);
        }

        World w = new World();
        double delta = 1;
        for (int i = 0; i < 200; i++) {
            ParkingSpot p1 = data.get(rand.nextInt(data.size()));
            ParkingSpot point2 = w.getParkingSpot(table, p1.getX(), p1.getY(), 100);
            assertEquals(p1.getX(), point2.getX(), 2 * delta);
            assertEquals(p1.getY(), point2.getY(), 2 * delta);
        }
    }

    private void addPoint(List<ParkingSpot> data, SortedMap<Long, ParkingSpot> table, double y, double x) {
        ParkingSpot p = new ParkingSpot(x, y);
        data.add(p);
        table.put(S2CellId.fromLatLng(p.getLocation()).id(), p);
    }
}