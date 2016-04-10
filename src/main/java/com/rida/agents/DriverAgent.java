package com.rida.agents;

import com.rida.behaviours.RegisterYellowPages;
import com.rida.behaviours.RequestPerformerBehaviour;
import com.rida.behaviours.RequestRecieveServer;
import com.rida.behaviours.YellowPageListenBehaviour;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.Helper;
import com.rida.tools.Trip;
import jade.core.AID;
import jade.core.Agent;
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
    private Set<DriverDescription> passengers;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        mapGraph = (Graph) args[0];
        int from = Integer.parseInt(args[1].toString());
        int to = Integer.parseInt(args[2].toString());
        Trip trip = new Trip(from, to);
        description = new DriverDescription(this.getName(), this.getAID(), trip);
        LOG.info("{} DriverAgent created. I want to go from {} to {}", new Date(), from, to);
        drivers = new HashSet<>();
        passengers = new HashSet<>();

        addBehaviour(new RegisterYellowPages());
        addBehaviour(new YellowPageListenBehaviour());
        addBehaviour(new RequestPerformerBehaviour());
        addBehaviour(new RequestRecieveServer());
    }

    public Set<DriverDescription> getDrivers() {
        return drivers;
    }

    public DriverDescription getDriverDescriptionByName(AID aid) {
        for (DriverDescription dd : drivers) {
            if (dd.getAid().toString().equals(aid.toString())) {
                return dd;
            }
        }
        throw new IllegalStateException("Not found driver");
    }

    public DriverDescription getPaseengerDescriptionByName(AID aid) {
        for (DriverDescription dd : passengers) {
            if (dd.getAid().toString().equals(aid.toString())) {
                return dd;
            }
        }
        throw new IllegalStateException("Not found passenger");
    }

    public void addDriver(DriverDescription driverDescription) {
        drivers.add(driverDescription);
    }

    public void addPassenger(DriverDescription ds) {
        passengers.add(ds);
    }


    public Set<DriverDescription> getPassengers() {
        return new HashSet<>(passengers);

    }


    public Set<DriverDescription> getGoodTrips() {
        Set<DriverDescription> set = new HashSet<>();
        Trip agentTrip = description.getTrip();

        for (DriverDescription driver : drivers) {
            Trip driverTrip = driver.getTrip();
            int profit = Helper.calcProfit(driverTrip, agentTrip, mapGraph);
            int reverseProfit = Helper.calcProfit(agentTrip, driverTrip, mapGraph);
            if (profit > 0 && reverseProfit <= profit) {
                Random rand = new Random();
                double x = (double) profit * 0.0001 * (rand.nextInt() % 2000 - 1000);
                driver.setCost(x + profit);
                set.add(driver);
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
}
