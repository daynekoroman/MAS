package com.rida.tools;

import jade.core.AID;

/**
 * Created by daine on 03.04.2016.
 */
public class Trip {
    private AID aid;
    private double cost;

    public Trip(AID aid, double cost) {
        this.aid = aid;
        this.cost = cost;
    }


    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public AID getAid() {
        return aid;
    }
}
