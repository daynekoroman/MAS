package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.Trip;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by daine on 03.04.2016.
 */
public class RequestPerformer extends OneShotBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(DriverAgent.class);

    @Override
    public void action() {
        DriverAgent driverAgent = (DriverAgent) myAgent;
        ArrayList<Trip> goodTrips = driverAgent.getGoodTrips();
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

        for (Trip trip : goodTrips) {
            cfp.addReceiver(trip.getAid());

            LOG.info("I send a message to {}  with cost  {}", trip.getAid().getName(), trip.getCost());
        }

        cfp.setContent(myAgent.getName());
        cfp.setConversationId("bring-up");
        driverAgent.send(cfp);

    }
}
