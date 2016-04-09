package com.rida.tools;

import jade.core.AID;

/**
 * Описывает водителя
 * Created by daine on 04.04.2016.
 */
public class DriverDescription implements Comparable {
    private String way;
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
        this.trip = dd.getTrip();
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

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }

    public AID getAid() {
        return aid;
    }

    /**
     * Геттеры и сеттеры для приввтных полей класса
     */
}