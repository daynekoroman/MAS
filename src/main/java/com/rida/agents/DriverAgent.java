package com.rida.agents;

import com.rida.behaviours.ListenYP;
import com.rida.behaviours.RegisterYellowPages;
import com.rida.behaviours.RequestPerformer;
import com.rida.behaviours.RequestRecieveServer;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.Trip;
import jade.core.AID;
import jade.core.Agent;
import jdk.nashorn.internal.ir.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by daine on 03.04.2016.
 */
public class DriverAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(DriverAgent.class);
    private int from, to;

    public Graph getMapGraph() {
        return mapGraph;
    }

    private Graph mapGraph;
    private Set<DriverDescription> drivers;
    private Set<DriverDescription> passengers;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        mapGraph = (Graph) args[0];
        from = Integer.parseInt(args[1].toString());
        to = Integer.parseInt(args[2].toString());
        LOG.info("DriverAgent created. I want to go from {} to {}", from, to);
        drivers = new HashSet<DriverDescription>();
        passengers = new HashSet<DriverDescription>();

        addBehaviour(new RegisterYellowPages());
        addBehaviour(new ListenYP());
        addBehaviour(new RequestPerformer());
        addBehaviour(new RequestRecieveServer());
    }

    public DriverDescription getDriverDescriptionByName(String name) {
        for (DriverDescription dd : drivers) {
            if (dd.getName().equals(name)) {
                return dd;
            }
        }
        throw new IllegalStateException("Not found driver");
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    public void addDriver(String name, String way, AID a) {
        drivers.add(new DriverDescription(name, way, a, mapGraph));
    }

    public void addPassenger(DriverDescription ds) {
        passengers.add(ds);
    }

    public void calculateProfit() {
        for (DriverDescription descr : drivers) {
            descr.calcWayLength();
            descr.calcProfit(from, to);
        }
    }

    public Set<DriverDescription> getPassenger() {
        return passengers;
    }

    /**
     * те, кому нас могут отвезти выгодно
     */
    public ArrayList<Trip> getGoodTrips() {
        ArrayList<Trip> list = new ArrayList<>();
        for (DriverDescription driver : drivers) {
            if (driver.getProfit() > 0 && driver.getReverseProfit() <= driver.getProfit()) {
                Random rand = new Random();
                double x = (double) driver.getProfit() * 0.0001 * (rand.nextInt() % 2000 - 1000);
                list.add(new Trip(driver.getValue(), driver.getProfit() + x));
            }
        }

        return list;
    }


}
