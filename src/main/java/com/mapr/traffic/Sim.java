package com.mapr.traffic;

import java.util.*;
import java.util.function.Function;

/**
 * Simple discrete event simulator.
 */
public class Sim<T extends Sim> {
    private Map<String, Double> properties = new HashMap<>();
    private Random rand = new Random();
    private double t = 0;

    @SuppressWarnings("WeakerAccess")
    public double now() {
        return t;
    }

    @SuppressWarnings("WeakerAccess")
    public double nextLogNormal(double mean, double spread) {
        return Math.exp(rand.nextGaussian() * Math.log(spread) + Math.log(mean));
    }

    public int nextInt(int bound) {
        return rand.nextInt(bound);
    }

    @SuppressWarnings("WeakerAccess")
    public void set(String key, double value) {
        properties.put(key, value);
    }

    @SuppressWarnings("WeakerAccess")
    public double get(String key) {
        return properties.get(key);
    }

    @SuppressWarnings("WeakerAccess")
    public double nextDouble(double max) {
        return rand.nextDouble() * max;
    }

    @SuppressWarnings("WeakerAccess")
    public Collection<Event<T>> getFuture() {
        return todo;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean step() {
        Event next = todo.poll();
        if (next != null) {
            t = next.when;
            //noinspection unchecked
            next.action.apply(this);
            return true;
        } else {
            return false;
        }
    }

    public static class Event<T> implements Comparable<Event<T>> {
        private Function<T, Void> action;
        private final double when;

        @SuppressWarnings("WeakerAccess")
        public Event(Function<T, Void> action, double when) {
            this.action = action;
            this.when = when;
        }

        @Override
        public int compareTo(Event<T> o) {
            return Double.compare(o.when, when);
        }
    }

    private PriorityQueue<Event<T>> todo = new PriorityQueue<>();

    public void run(double limit) {
        //noinspection StatementWithEmptyBody
        while (t < limit && step()) {
            // ignore
        }
    }

    void schedule(Function<T, Void> action, double when) {
        todo.add(new Event<>(action, when));
    }
}
