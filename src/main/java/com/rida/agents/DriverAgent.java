package com.rida.agents;

import com.rida.behaviours.RegisterYellowPages;
import com.rida.behaviours.YellowPageListenBehaviour;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.Trip;
import jade.core.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Агент - водитель
 * Created by daine on 03.04.2016.
 */
public class DriverAgent extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(DriverAgent.class);
    private DriverDescription description;

    private Set<DriverDescription> drivers;
    private Set<DriverDescription> passengers;

    @Override
    protected void setup() {
        super.setup();
        Object[] args = getArguments();
        Graph mapGraph = (Graph) args[0];
        int from = Integer.parseInt(args[1].toString());
        int to = Integer.parseInt(args[2].toString());
        Trip trip = new Trip(from, to);
        description = new DriverDescription(this.getName(), this.getAID(), trip);
        LOG.info("{} DriverAgent created. I want to go from {} to {}", new Date(), from, to);
        drivers = new HashSet<>();
        passengers = new HashSet<>();

        addBehaviour(new RegisterYellowPages());
        addBehaviour(new YellowPageListenBehaviour());
        /*addBehaviour(new RequestPerformerBehaviour());
        addBehaviour(new RequestRecieveServer());*/
    }

    public DriverDescription getDriverDescriptionByName(String name) {
        for (DriverDescription dd : drivers) {
            if (dd.getName().equals(name)) {
                return dd;
            }
        }
        throw new IllegalStateException("Not found driver");
    }

    public void addDriver(DriverDescription driverDescription) {
        drivers.add(driverDescription);
    }

    public void addPassenger(DriverDescription ds) {
        passengers.add(ds);
    }

    public void calculateProfit() {
        for (DriverDescription descr : drivers) {

        }
    }

    public Set<DriverDescription> getPassengers() {
        return new HashSet<>(passengers);

    }


    /*public ArrayList<Trip> getGoodTrips() {
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
}
