package com.mapr.traffic;

import com.google.common.base.Preconditions;
import com.google.common.geometry.S2LatLng;

import java.util.function.Function;

/**
 * A car has a state machine consisting of states representing
 * when it is parked, traveling to a new destination, or searching
 * for a parking place near a destination.
 *
 * Parking lasts for a somewhat randomized time period. Traveling
 * to a new destination is done by randomly driving on a 100m grid
 * ignoring any other cars.
 *
 * Once the destination is reached, parking is found by one of two
 * methods. In the first, the car drives in a random walk starting at
 * its destination until it finds an empty parking place. In the second,
 * searches are done with increasing radius until an empty spot is found.
 * If that spot is taken when the car gets to the spot, the process is
 * repeated.
 *
 * The goal is to demonstrate considerably less traffic at high load
 * conditions. A secondary benefit would be a smaller distance between
 * destination and parking spot.
 *
 * In this implementation, there are no actually roads. The cars just
 * do walks on a grid. Simulating traffic is likely to accentuate the
 * issues as parking goes critical.
 */
class Car {
    // about 22 miles per hour
    private static final double DRIVING_SPEED = 10;

    // the parking spot that we have our eye on
    private ParkingSpot ourSpot = null;

    // should we search randomly for a parking place?
    private boolean useRandomWalk = false;

    // can we reserve a spot that we see at a distance?
    private boolean useReservations = true;

    @SuppressWarnings("WeakerAccess")
    public ParkingSpot getSpot() {
        return ourSpot;
    }

    enum State {
        PARKED,
        TRAVELING,
        SEARCHING
    }

    private double currentX, currentY;
    private double targetX, targetY;

    private State state;

    @SuppressWarnings("WeakerAccess")
    public Car(World sim) {
        // start parked (but not in any parking spot... we don't worry about assigning parking spaces
        // before the universe has begun).
        state = State.PARKED;
        sim.schedule(s -> {
            this.startDriving(sim);
            return null;
        }, sim.now() + sim.nextLogNormal(10, 5));
    }

    @SuppressWarnings("WeakerAccess")
    public void setUseRandomWalk(boolean useRandomWalk) {
        this.useRandomWalk = useRandomWalk;
    }

    @SuppressWarnings("WeakerAccess")
    public void setUseReservations(boolean useReservations) {
        this.useReservations = useReservations;
    }

    /**
     * Transitions from parked to traveling. This involves designating a destination
     * and starting to drive that way.
     *
     * @param sim The world
     * @return Void
     */
    private Void startDriving(World sim) {
        Preconditions.checkState(state == State.PARKED, String.format("Unexpected state = %s", state));

        if (ourSpot != null) {
            ourSpot.unpark();
        }
        targetX = gridify(sim.nextDouble(sim.get("xMax")));
        targetY = gridify(sim.nextDouble(sim.get("yMax")));
        state = State.TRAVELING;
        sim.schedule(this::drive, sim.now());
        return null;
    }

    private double gridify(double z) {
        return 100 * Math.rint(z / 100);
    }

    /**
     * Driving means that we have identified a destination and are taking steps to get there.
     * The way we do that is to take random steps in x or y such that our distance to target
     * is always decreasing. Once we get near the target, we start searching for a parking
     * place.
     *
     * @param sim The world
     * @return Void
     */
    private Void drive(World sim) {
        Preconditions.checkState(state == State.TRAVELING, String.format("Unexpected state = %s", state));
        boolean arrived = stepTowardTarget(sim, targetX, targetY, useReservations ? 800 : 100, this::drive);
        if (arrived) {
            state = State.SEARCHING;
            sim.schedule(this::search, sim.now());
        }
        return null;
    }

    /**
     * Takes a step toward the target. If we are close (very close in random walk mode)
     * we signal that so we can transition to search.
     *
     * @param sim        The world
     * @param destX      Where we want to go
     * @param destY      Where we want to go
     * @param nextAction A function representing the next state to enter
     * @return True if we are close enough to target that we should transition to searching
     */
    private boolean stepTowardTarget(World sim, double destX, double destY, double limit, Function<World, Void> nextAction) {
        boolean arrived = false;
        double dx = destX - currentX;
        double dy = destY - currentY;
        double ds = distanceTo(Geo.getS2LatLng(destX, destY));
        if (ds < limit) {
            // pretty much arrived
            arrived = true;
        } else {
            double u = sim.nextDouble(1);
            // pick an x or y-direction to step
            if (u < Math.abs(dx / (dx + dy))) {
                // take a step in x
                dx = Math.copySign(Math.min(Math.abs(dx), 100), dx);
                dy = 0;
            } else {
                // take a step in y
                dx = 0;
                dy = Math.copySign(Math.min(Math.abs(dy), 100), dy);
            }
            currentX += dx;
            currentY += dy;
            sim.schedule(nextAction, sim.now() + 100 / DRIVING_SPEED + sim.nextDouble(1));
        }
        return arrived;
    }

    /**
     * Searching means we are near our destination and are looking for a parking place.
     *
     * In random walk mode, that means that we just wander aimlessly until we find an open spot.
     * In other modes, we will have started searching further away from our destination so we
     * do a geo-search for an open spot and reserve it. Once we reserve a spot, we drive to it
     * and if the reservation didn't expire getting there, we park.
     *
     * @param sim The world simulator. Mostly used to find out what time it is.
     * @return Void
     */
    private Void search(World sim) {
        Preconditions.checkState(state == State.SEARCHING, String.format("Unexpected state = %s", state));

        if (useRandomWalk) {
            ourSpot = sim.search(currentX, currentY, 100);
            if (ourSpot == null) {
                randomSearchStep(sim);
            } else {
                ourSpot.park(sim, this);
                state = State.PARKED;
                sim.schedule(this::startDriving, sim.now() + sim.nextLogNormal(600, 1.5));
            }
        } else {
            boolean reservationFail = useReservations && ourSpot != null && !ourSpot.confirmReservation(sim, this);
            if (ourSpot == null || reservationFail || !ourSpot.isInUse(sim)) {
                ourSpot = sim.search(targetX, targetY, 2000);
                if (useReservations && ourSpot != null) {
                    ourSpot.reserve(sim, this, 30);
                }
            }
            assert ourSpot == null || !useReservations || ourSpot.confirmReservation(sim, this);

            if (ourSpot == null) {
                // nothing anywhwere near but we can't stop driving so we should wander
                randomSearchStep(sim);
            } else {
                if (useReservations && !ourSpot.confirmReservation(sim, this)) {
                    // yowza, our reservation expired!
                    // this really should be impossible
                    ourSpot = null;
                    randomSearchStep(sim);
                } else {
                    if (stepTowardTarget(sim, ourSpot.getX(), ourSpot.getY(), 100, this::search)) {
                        // arrived!
                        ourSpot.park(sim, this);
                        state = State.PARKED;
                        sim.schedule(this::startDriving, sim.now() + sim.nextLogNormal(600, 1.5));
                    }
                }
            }
        }
        return null;
    }

    double distanceTo(S2LatLng location) {
        return location.getEarthDistance(Geo.getS2LatLng(currentX, currentY));
    }

    double distance(double x1, double y1, double x2, double y2) {
        return Geo.getS2LatLng(x1, y1).getEarthDistance(Geo.getS2LatLng(x2, y2));
    }

    /**
     * Takes a step in a random direction, being careful not to step off the edge
     * of the world.
     *
     * @param sim The world
     */
    private void randomSearchStep(World sim) {
        double dx = 0;
        double dy = 0;
        double u = sim.nextDouble(1);
        if (u < 0.25 && sim.get("xMax") >= currentX + 100) {
            dx = 100;
        } else if (u < 0.5 && currentX >= 100) {
            dx = -100;
        } else if (u < 0.75 && sim.get("yMax") >= currentY + 100) {
            dy = 100;
        } else if (currentY >= 100) {
            dy = -100;
        } else {
            // shouldn't be possible
            dx = 0;
            dy = 0;
        }
        currentX += dx;
        currentY += dy;
        sim.schedule(this::search, sim.now() + 100 / DRIVING_SPEED + sim.nextDouble(1));
    }


    @SuppressWarnings("WeakerAccess")
    public State getState() {
        return state;
    }
}
