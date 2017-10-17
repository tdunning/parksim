package com.mapr.traffic;

import org.junit.Test;

import static org.junit.Assert.*;

public class CarTest {
    /**
     * Verifies the basics of a Car life-cycle
     */
    @Test
    public void basics() {
        for (int i = 0; i < 3; i++) {
            World w = new World();
            Car c = new Car(w);
            c.setUseRandomWalk((i & 2) != 0);
            c.setUseReservations((i & 1) != 0);

            for (int j = 0; j < 3; j++) {
                assertEquals(1, w.getFuture().size());
                assertEquals(Car.State.PARKED, c.getState());
                assertTrue(w.step());

                assertEquals(Car.State.TRAVELING, c.getState());
                assertEquals(1, w.getFuture().size());
                while (c.getState() == Car.State.TRAVELING) {
                    w.step();
                }

                assertEquals(Car.State.SEARCHING, c.getState());
                while (c.getState() == Car.State.SEARCHING) {
                    w.step();
                }

                assertEquals(Car.State.PARKED, c.getState());
                assertNotNull(c.getSpot());
                assertEquals(0, c.distanceTo(c.getSpot().getLocation()), 100);
            }
        }
    }
}