package com.mapr.traffic;

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
public class Car {
    // about 22 miles per hour
    private static final int DRIVING_SPEED = 10;

    // the parking spot that we have our eye on
    private ParkingSpot ourSpot = null;

    // the bounds of our small world
    private final double maxX;
    private final double maxY;

    // should we search randomly for a parking place?
    private boolean useRandomWalk = false;

    // can we reserve a spot that we see at a distance?
    private boolean useReservations = true;

    enum State {
        PARKED,
        TRAVELING,
        SEARCHING
    }

    private double currentX, currentY;
    private double targetX, targetY;

    private State state;

    public Car(World sim) {
        state = State.PARKED;
        sim.schedule(s -> {
            this.startDriving(sim);
            return null;
        }, sim.nextLogNormal(3, 5));
        maxX = sim.get("xMax");
        maxY = sim.get("yMax");
    }

    private void startDriving(World sim) {
        ourSpot.unpark();
        targetX = gridify(sim.nextDouble(sim.get("xMax")));
        targetY = gridify(sim.nextDouble(sim.get("yMax")));
        state = State.TRAVELING;
        sim.schedule(this::apply, sim.now());
    }

    private double gridify(double z) {
        return 100 * Math.rint(z / 100);
    }

    private Void apply(World sim) {
        switch (state) {
            case PARKED:
                this.startDriving(sim);
                break;
            case TRAVELING:
                boolean arrived = stepTowardTarget(sim);
                if (arrived) {
                    state = State.SEARCHING;
                    targetX = -1;
                    targetY = -1;
                    sim.schedule(this::apply, sim.now());
                }
                break;
            case SEARCHING:
                if (useRandomWalk) {
                    ParkingSpot spot = sim.search(currentX, currentY, 100);
                    if (spot == null) {
                        randomStep(sim);
                    } else {
                        spot.park(this);
                        state = State.PARKED;
                        sim.schedule(this::apply, sim.now() + sim.nextLogNormal(600, 1.5));
                    }
                } else {
                    if (ourSpot == null || ourSpot.isInUse()) {
                        ourSpot = sim.search(currentX, currentY, 2000);
                        if (useReservations) {
                            ourSpot.park(this);
                        }
                    }
                    if (ourSpot == null || ourSpot.isInUse()) {
                        // nothing anywhwere near but we can't stop driving
                        randomStep(sim);
                    } else {
                        if (Math.abs(currentX - ourSpot.getX()) + Math.abs(currentY - ourSpot.getY()) < 100) {
                            // arrived!
                            if (!useReservations) {
                                ourSpot.park(this);
                            }
                            state = State.PARKED;
                            sim.schedule(this::apply, sim.now() + sim.nextLogNormal(600, 1.5));
                        } else {
                            targetX = ourSpot.getX();
                            targetY = ourSpot.getY();
                            stepTowardTarget(sim);
                        }
                    }
                }
                return null;
        }
        return null;
    }

    private void randomStep(World sim) {
        double dx = 0;
        double dy = 0;
        double u = sim.nextDouble(1);
        if (u < 0.25 && maxX >= currentX + 100) {
            dx = 100;
        } else if (u < 0.5 && currentX >= 100) {
            dx = -100;
        } else if (u < 0.75 && maxY >= currentY + 100) {
            dy = 100;
        } else if (currentY >= 100) {
            dy = -100;
        } else {
            // shouldn't be possible
            dx = 0;
            dy = 0;
        }
        driveTo(sim, dx, dy);
    }

    private boolean stepTowardTarget(World sim) {
        boolean arrived = false;
        double dx = targetX - currentX;
        double dy = targetY - currentY;
        if (dx < 100 && dy < 100) {
            // pretty much arrived
            arrived = true;
        } else {
            double u = sim.nextDouble(1);
            // pick an x or y-direction to step
            if (u < Math.abs(dx / (dx + dy))) {
                // take a step in x
                dx = Math.copySign(Math.max(Math.abs(dx), 100), dx);
            } else {
                // take a step in y
                dy = Math.copySign(Math.max(Math.abs(dy), 100), dy);
            }
            driveTo(sim, dx, dy);
        }
        return arrived;
    }

    private void driveTo(World sim, double dx, double dy) {
        currentX += dx;
        currentY += dy;
        sim.schedule(this::apply, sim.now() + 100 / DRIVING_SPEED + sim.nextDouble(1));
    }
}
