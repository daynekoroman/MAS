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
    private static final long serialVersionUID = 7075582068677079540L;

    private transient DriverDescription description = null;
    private Graph mapGraph = null;
    private transient Set<DriverDescription> drivers = null;
    private transient Set<DriverDescription> potentialDrivers = null;


    private ArrayList<DriverDescription> potentialPassengers = new ArrayList<>();
    private boolean isChauffeurFlag = false;
    private transient Set<AID> goneDrivers = null;
    private static final int CAR_CAPACITY = 4;


    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        mapGraph = (Graph) args[0];
        int from = Integer.parseInt(args[1].toString());
        int to = Integer.parseInt(args[2].toString());
        Trip trip = new Trip(from, to);
        description = new DriverDescription(this.getName(), this.getAID(), trip);
        LOG.info(" DriverAgent created. I want to go from " + from +
                " to " + to);
        drivers = new HashSet<>();
        potentialDrivers = new HashSet<>();
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

    public void addDriver(DriverDescription driverDescription) {
        drivers.add(driverDescription);
    }


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
        }
        return set;
    }

    public DriverDescription getDescription() {
        return description;
    }

    public Graph getMapGraph() {
        return mapGraph;
    }

    private boolean havePotentialPassenger(AID aid) {
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
        Collections.sort(potentialPassengers);
    }


    public double getSumCostPotentialPassenger() {
        double sum = 0.0;
        for (int i = 0; i < CAR_CAPACITY && i < potentialPassengers.size(); i++) {
            sum += potentialPassengers.get(i).getTrip().getCost();
        }

        return sum;
    }


    public List<DriverDescription> getPotentialPassengers() {
        return potentialPassengers;
    }


    public Set<DriverDescription> getSetPotentialPassengers() {
        return new HashSet<>(potentialPassengers);
    }

}
