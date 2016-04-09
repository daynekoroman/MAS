package com.rida.tools;

/**
 * Класс описывающий поездук
 * Created by daine on 03.04.2016.
 */
public class Trip {
    private double cost;
    private int from;
    private int to;


    public Trip(int from, int to) {
        setFrom(from);
        setTo(to);
    }


    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }


    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    @Override
    public String toString() {
        return String.format("From %d To %d",from, to);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return (Object) new Trip(this.from, this.to);
    }
}
