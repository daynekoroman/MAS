package com.rida.tools;


import jade.core.AID;

import java.io.Serializable;

/**
 * Описывает водителя
 * Created by daine on 04.04.2016.
 */
public class DriverDescription implements Comparable<DriverDescription>, Serializable {
    private static final long serialVersionUID = -3554852054925105341L;
    private transient Trip trip;
    private String name;
    private AID aid;


    public DriverDescription(String name, AID aid, Trip trip) {
        this.trip = trip;
        this.name = name;
        this.aid = aid;
    }


    public AID getAid() {
        return aid;
    }


    public void setCost(double cost) {
        trip.setCost(cost);
    }

    public Trip getTrip() {
        return trip;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " " + trip;
    }


    @Override
    public int hashCode() {
        return aid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }
        DriverDescription driverDescription = (DriverDescription) obj;
        return this.aid.equals(driverDescription.getAid());
    }



    @Override
    public int compareTo(DriverDescription o) {
        double cost1 = this.getTrip().getCost();
        double cost2 = o.getTrip().getCost();
        return cost1 < cost2 ? 1 : cost1 > cost2 ? -1 : 0;
    }
}




