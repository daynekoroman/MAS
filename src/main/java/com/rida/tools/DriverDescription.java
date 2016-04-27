package com.rida.tools;

import java.util.Comparator;

import jade.core.AID;

/**
 * Описывает водителя
 * Created by daine on 04.04.2016.
 */
public class DriverDescription {
    private Trip trip;
    private String name;
    private AID aid;


    public String getName() {
        return name;
    }

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

    public static Comparator<DriverDescription> costComp = new Comparator<DriverDescription>() {

        public int compare(DriverDescription dd1, DriverDescription dd2) {
            double x1 = dd1.getTrip().getCost();
            double x2 = dd2.getTrip().getCost();

            return (x1 < x2 ? 1 : x1 > x2 ? -1 : 0);
        }
    };

}




