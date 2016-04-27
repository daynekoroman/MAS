package com.rida.agents;

import com.rida.behaviours.*;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.Helper;
import com.rida.tools.Trip;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Агент - водитель
 * Created by daine on 03.04.2016.
 */
public class DriverAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(DriverAgent.class);
    private DriverDescription description;
    private Graph mapGraph;
    private Set<DriverDescription> drivers;
    private Set<DriverDescription> potentialDrivers;
    private Set<DriverDescription> passengers;
    private Set<DriverDescription> bestPassengers;
    private int potentialPassengerCount = 0;


    private ArrayList<DriverDescription> potentialPassengers = new ArrayList<>();
    private boolean isChauffeurFlag = false;
    public HashSet<AID> goneDrivers = new HashSet<>();
    public final int CAR_CAPACITY = 4;


    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        mapGraph = (Graph) args[0];
        int from = Integer.parseInt(args[1].toString());
        int to = Integer.parseInt(args[2].toString());
        Trip trip = new Trip(from, to);
        description = new DriverDescription(this.getName(), this.getAID(), trip);
        System.out.println(new Date() + " DriverAgent created. I want to go from " + from +
                " to " + to);
        drivers = new HashSet<>();
        potentialDrivers = new HashSet<>();
        passengers = new HashSet<>();
        SequentialBehaviour sequentialBehaviour = new SequentialBehaviour();
        sequentialBehaviour.addSubBehaviour(new RegisterYellowPagesBehaviour());
        sequentialBehaviour.addSubBehaviour(new YellowPageListenBehaviour(this, 1000));
        ParallelBehaviour parallelBehaviour = new ParallelBehaviour();
        parallelBehaviour.addSubBehaviour(new ServerPassengerBehaviour(this, 700));
        parallelBehaviour.addSubBehaviour(new ServerChauffeurBehaviour(this, 700));
        sequentialBehaviour.addSubBehaviour(parallelBehaviour);
        addBehaviour(sequentialBehaviour);
    }

    public Set<DriverDescription> getDrivers() {
        return drivers;
    }

    //    public DriverDescription getDriverDescriptionByName(AID aid) {
//        for (DriverDescription dd : drivers) {
//            if (dd.getAid().toString().equals(aid.toString())) {
//                return dd;
//            }
//        }
//        throw new IllegalStateException("Not found driver");
//    }
//
//    public DriverDescription getPaseengerDescriptionByName(AID aid) {
//        for (DriverDescription dd : passengers) {
//            if (dd.getAid().toString().equals(aid.toString())) {
//                return dd;
//            }
//        }
//        throw new IllegalStateException("Not found passenger");
//    }
//
    public void addDriver(DriverDescription driverDescription) {
        drivers.add(driverDescription);
    }
//
//    public void addPassenger(DriverDescription ds) {
//        passengers.add(ds);
//    }


//    public Set<DriverDescription> getPassengers() {
//        return new HashSet<>(passengers);
//
//    }

//    public Set<DriverDescription> getPotentialDrivers() {
//        return potentialDrivers;
//    }

    public Set<DriverDescription> getGoodTrips() {
        Set<DriverDescription> set = new HashSet<>();
        Trip agentTrip = description.getTrip();
        Random rand = new Random();

        for (DriverDescription driver : drivers) {
            Trip otherDriverTrip = driver.getTrip();
            int profit = Helper.calcProfit(otherDriverTrip, agentTrip, mapGraph);
            int reverseProfit = Helper.calcProfit(agentTrip, otherDriverTrip, mapGraph);
            if (profit > 0 && reverseProfit <= profit) {
                double x = (double) profit * 0.00001 * (rand.nextInt() % 200 - 100);
                driver.setCost(x + profit);
                set.add(driver);
                potentialDrivers.add(driver);
            }
            if (reverseProfit > 0 && reverseProfit >= profit) {
                potentialPassengerCount++;
            }
        }
        return set;
    }

    /*
    public int calcAmountPotentialPassengers() {
        int count = 0;
        for (DriverDescription descr : drivers) {
            if (descr.getReverseProfit() > 0 && descr.getReverseProfit() <= descr.getProfit()) {
                count++;
            }
        }

        return count;
    }*/
    public DriverDescription getDescription() {
        return description;
    }

    public Graph getMapGraph() {
        return mapGraph;
    }

//    public void deletePotentialDriver(DriverDescription dd){
//        potentialDrivers.remove(dd);
//    }
//
//    public void deletePassenger(DriverDescription dd){
//        passengers.remove(dd);
//    }

    public void setBestPassengers(Set<DriverDescription> bestPassengers) {
        this.bestPassengers = bestPassengers;
    }

    public Set<DriverDescription> getBestPassengers() {
        return bestPassengers;
    }

    public boolean havePotentialPassenger(AID aid) {
        for (DriverDescription dd : potentialPassengers) {
            if (dd.getName().equals(aid.getName())) {
                return true;
            }
        }

        return false;
    }


    public boolean addPotentialPassengerByName(AID aid) {
        if (goneDrivers.contains(aid))
            return false;

        if (havePotentialPassenger(aid))
            return true;


        for (DriverDescription dd : drivers) {

            if (dd.getAid().getName().equals(aid.getName())) {
                potentialPassengers.add(dd);
                return true;
            }
        }

        throw new IllegalStateException("Not found potential passenger by name " + aid.getName());
    }


    public boolean removePotentialPassenger(AID aid) {
        for (DriverDescription dd : potentialPassengers) {
            if (dd.getAid().getName().equals(aid.getName())) {
                potentialPassengers.remove(dd);
                return true;
            }
        }

        return false;
//    	throw new IllegalStateException("not found potential passenger while removing");
    }


    public void setCostToPotentialPassenger(AID aid, double cost) {
        for (DriverDescription descr : potentialPassengers) {
            if (descr.getAid().getName().equals(aid.getName())) {
                descr.setCost(cost);
                return;
            }
        }

        throw new IllegalStateException("Not found potential passenger for set cost");
    }


    public boolean isChauffeur() {
        return isChauffeurFlag;
    }


    public void becomeChauffeur() {
        isChauffeurFlag = true;
    }


    public void sortPotentialPassengersByCost() {

        Collections.sort(potentialPassengers, DriverDescription.costComp);
    }


    public double getSumCostPotentialPassenger() {
        double sum = 0.0;
        for (int i = 0; i < CAR_CAPACITY && i < potentialPassengers.size(); i++) {
            sum += potentialPassengers.get(i).getTrip().getCost();
        }

        return sum;
    }


    public ArrayList<DriverDescription> getPotentialPassengers() {
        return potentialPassengers;
    }


    public HashSet<DriverDescription> getSetPotentialPassengers() {
        return new HashSet<>(potentialPassengers);
    }

//    public int getPotentialPassengerCount() {
//        return potentialPassengerCount;
//    }
}
