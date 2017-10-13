package com.mapr.traffic;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
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

    public class Event implements Comparable<Event> {
        double t;

        Function<T, Void> action;
        private final double when;

        @SuppressWarnings("WeakerAccess")
        public Event(Function<T, Void> action, double when) {
            this.action = action;
            this.when = when;
        }

        @Override
        public int compareTo(Event o) {
            return Double.compare(o.t, t);
        }
    }

    private PriorityQueue<Event> todo;

    public void run() {
        Event next = todo.poll();
        while (next != null) {
            t = next.t;
            //noinspection unchecked
            next.action.apply((T) this);
        }
    }

    void schedule(Function<T, Void> action, double when) {
        todo.add(new Event(action, when));
    }
}
