package com.rida.tools;

import jade.core.AID;

/**
 * Описывает водителя
 * Created by daine on 04.04.2016.
 */
public class DriverDescription implements Comparable {
    private Trip trip;
    private DriverDescription self = this;

    public String getName() {
        return name;
    }

    private String name;


    private AID aid;//уникальный идентификатор


    public DriverDescription(String name, AID aid, Trip trip) {
        this.trip = trip;
        this.name = name;
        this.aid = aid;
    }

    public DriverDescription(DriverDescription dd) {
        this.aid = dd.getAid();
        try {
            this.trip = (Trip) dd.getTrip().clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        this.name = dd.getName();
    }

    @Override
    public boolean equals(Object obj) {
        DriverDescription t = (DriverDescription) obj;
        if (this.name.equals(getName()))
            return true;
        else
            return false;
    }

    @Override
    public int compareTo(Object o) {
        /*DriverDescription compDriverDescription = (DriverDescription) o;
        if (reverseProfit == compDriverDescription.reverseProfit) return 0;
        if (reverseProfit > compDriverDescription.reverseProfit) {
            return 1;
        } else {
            return -1;
        }*/
        return 0;
    }

    public void setCost(double cost) {
        trip.setCost(cost);
    }

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public AID getAid() {
        return aid;
    }

    @Override
    public String toString() {
        return (name.toString().split("@")[0] + " " + trip);
    }

    /**
     * Геттеры и сеттеры для приввтных полей класса
     */
}