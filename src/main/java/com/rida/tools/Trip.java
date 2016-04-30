package com.rida.tools;

import java.io.Serializable;

/**
 * Класс описывающий поездку
 * Created by daine on 03.04.2016.
 */
public class Trip implements Serializable {
    private static final long serialVersionUID = -6975350472936081045L;
    private double cost;
    private int from;
    private int to;


    public Trip(int from, int to) {
        setFrom(from);
        setTo(to);
    }

    public Trip(Trip trip) {
        setFrom(trip.getFrom());
        setTo(trip.getTo());
        setCost(trip.getCost());
    }

    public double getCost() {
        return cost;
    }

    void setCost(double cost) {
        this.cost = cost;
    }


    public int getFrom() {
        return from;
    }

    private void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    private void setTo(int to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return String.format("From %d To %d by %1.2f€", from, to, cost);
    }
    
}
