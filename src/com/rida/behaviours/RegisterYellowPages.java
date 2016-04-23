package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import com.rida.tools.Trip;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.leap.ArrayList;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Created by daine on 03.04.2016.
 */
public class RegisterYellowPages extends OneShotBehaviour {


//    private static final Logger LOG = LoggerFactory.getLogger(RegisterYellowPages.class);

    @Override
    public void action() {
        DriverAgent driverAgent = (DriverAgent) myAgent;
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(driverAgent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bring-up");
        sd.setName(driverAgent.getName());
        Trip trip = driverAgent.getDescription().getTrip();
        sd.addProperties(new Property("from", trip.getFrom()));
        sd.addProperties(new Property("to", trip.getTo()));
        dfd.addServices(sd);
        try {
            DFService.register(driverAgent, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
//        LOG.info("register YP");
        
        System.out.println(myAgent.getLocalName() + " register YP");
    }

}
