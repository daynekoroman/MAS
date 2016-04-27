package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.Trip;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Поведение отвечающее за регистрацию агента
 * в сервисе желтые страницы.
 */
public class RegisterYellowPagesBehaviour extends OneShotBehaviour {


    private static final Logger LOG = LoggerFactory.getLogger(RegisterYellowPagesBehaviour.class);

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
            LOG.error("Failed to register agent in Yellow Pages Service caused {}\n", fe);
        }
        LOG.info("register YP");
    }

}
