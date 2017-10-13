package com.mapr.traffic;

/**
 * Represents a parking spot.
 */
class ParkingSpot {
    private double x, y;
    private boolean inUse = false;

    @SuppressWarnings("WeakerAccess")
    public ParkingSpot(double x, double y) {
        this.x = x;
        this.y = y;
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
    public boolean isInUse() {
        return inUse;
    }

    @SuppressWarnings("WeakerAccess")
    public void park(Car car) {
        inUse = true;
    }

    @SuppressWarnings("WeakerAccess")
    public void unpark() {
        inUse = false;
    }
}
