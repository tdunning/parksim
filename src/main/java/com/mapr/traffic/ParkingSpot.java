package com.mapr.traffic;

import com.google.common.base.Preconditions;
import com.google.common.geometry.S2LatLng;

/**
 * Represents a parking spot. Parking spots exist in some location.
 * They can be reserved for limited amounts of time. They can also
 * be occupied.
 */
class ParkingSpot {
    private double x, y;
    private S2LatLng location;
    private boolean filled = false;
    private double reservedUntil = 0;
    private Car reservedBy = null;

    @SuppressWarnings("WeakerAccess")
    public ParkingSpot(double x, double y) {
        this.x = x;
        this.y = y;
        location = Geo.getS2LatLng(x, y);
    }

    @SuppressWarnings("WeakerAccess")
    public double getX() {
        return x;
    }

    @SuppressWarnings("WeakerAccess")
    public double getY() {
        return y;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isInUse(Sim<World> w) {
        checkExpiration(w);

        // no parked car here, nor a live reservation
        return filled || reservedBy != null;
    }

    @SuppressWarnings("WeakerAccess")
    public void park(Sim<World> w, Car car) {
        checkExpiration(w);

        if (filled) {
            throw new IllegalStateException("Tried to park in filled space");
        }
        if (reservedBy == null || reservedBy == car) {
            filled = true;
            reservedUntil = 0;
            reservedBy = null;
        } else {
            throw new IllegalStateException("Tried to park in reserved space");
        }
    }

    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public void reserve(Sim<World> w, Car who, double duration) {
        reservedUntil = w.now() + duration;
        reservedBy = who;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean confirmReservation(Sim<World> w, Car car) {
        checkExpiration(w);
        return reservedBy == car;
    }

    @SuppressWarnings("WeakerAccess")
    public void unpark() {
        filled = false;
        reservedUntil = 0;
        reservedBy = null;
    }

    @SuppressWarnings("WeakerAccess")
    public S2LatLng getLocation() {
        return location;
    }

    private void checkExpiration(Sim<World> w) {
        if (reservedUntil < w.now()) {
            // if reservedUntil was not already 0, then a reservation expired
            reservedUntil = 0;
            reservedBy = null;
        }
    }}
