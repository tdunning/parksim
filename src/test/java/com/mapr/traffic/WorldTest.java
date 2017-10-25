package com.mapr.traffic;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.geometry.R2Vector;
import com.google.common.geometry.S2LatLng;
import org.junit.Test;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class WorldTest {
    /**
     * Verifies that geo-search is relatively efficient.
     *
     * The output consists of csv data with columns named i,j,x,y. The x and y
     * values are the coordinates (in meters) of the different locations.
     * The values of i indicate different regions that are scanned. Negative
     * values of j indicate parking spots that were found within 20 meters of
     * x=1500,y=1500. Non-negative values of j indicate the bounds of a region.
     *
     * The efficiency of the search can be seen by plotting these values with
     * a 20 meter radius circle. The R code in src/test/R/plot-scans.r does this.
     */
    @Test
    public void searchSize() throws Exception {
        World w = new World();
        PrintWriter out = new PrintWriter("scans.csv");
        out.printf("i,j,x,y\n");
        AtomicInteger points = new AtomicInteger();
        AtomicInteger regions = new AtomicInteger();
        AtomicInteger hits = new AtomicInteger();
        Multiset<Double> areas = HashMultiset.create();
        double x = w.get("xMax") / 2;
        double y = w.get("yMax") / 2;
        Geo.scan(w.getSpotTable(), x, y, 20,
                (p, i) -> {
                    hits.addAndGet(1);
                    R2Vector px = Geo.getXY(p.getLocation());
                    out.printf("%d,%d,%.12f,%.12f\n", i, -1, px.get(0), px.get(1));

                    double ds = p.getLocation().getEarthDistance(Geo.getS2LatLng(x, y));
                    assertEquals(0, ds, 30);
                    if (ds < 20) {
                        points.addAndGet(1);
                    }
                    return Boolean.TRUE;
                },
                (s, i) -> {
                    regions.addAndGet(1);
                    double v = s.approxArea() * S2LatLng.EARTH_RADIUS_METERS * S2LatLng.EARTH_RADIUS_METERS;
                    areas.add(Math.rint(v * 100) / 100);
                    for (int k = 0; k < 4; k++) {
                        S2LatLng vertex = new S2LatLng(s.getVertex(k));
                        R2Vector px = Geo.getXY(vertex);
                        out.printf("%d,%d,%.12f,%.12f\n", i, k, px.get(0), px.get(1));
                    }
                    return null;
                }
        );
        out.close();
        // how many points inside limit
        assertEquals(12, points.get());
        // how many points inside scan
        assertEquals(21, hits.get());
        // how many regions scanned
        assertEquals(8, regions.get());
        // how many unique sizes of scans
        assertEquals(4, areas.elementSet().size());
        // and then the specifics
        assertEquals(1, areas.count(1046.80));
        assertEquals(3, areas.count(261.70));
        assertEquals(1, areas.count(65.42));
        assertEquals(3, areas.count(16.36));
    }
}