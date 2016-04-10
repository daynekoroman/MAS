package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;


public class RequestPerformerBehaviour extends OneShotBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(DriverAgent.class);


    @Override
    public void action() {
        DriverAgent driverAgent = (DriverAgent) myAgent;

        Set<DriverDescription> goodDrivers = driverAgent.getGoodTrips();
        for (DriverDescription driver : goodDrivers) {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.addReceiver(driver.getAid());
            LOG.info("I send a message to {}  with cost  {}", driver.getName(), driver.getTrip().getCost());
            cfp.setContent(String.valueOf(driver.getTrip().getCost()));
            cfp.setConversationId("bring-up");
            driverAgent.send(cfp);
        }

    }
}
