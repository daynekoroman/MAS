package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import com.rida.tools.Trip;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.util.leap.Iterator;


/**
 * Поведение считывающее информацию с сервиса желтых страниц
 */
public class YellowPageListenBehaviour extends WakerBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(YellowPageListenBehaviour.class);
    private static final long serialVersionUID = -3203854470448955284L;

    public YellowPageListenBehaviour(Agent a, long timeout) {
        super(a, timeout);
    }

    @Override
    public void onWake() {

        DriverAgent driverAgent = (DriverAgent) myAgent;
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bring-up");
        template.addServices(sd);

        try {
            DFAgentDescription[] yellowPageResults;
            yellowPageResults = DFService.search(myAgent, template);

            for (DFAgentDescription result : yellowPageResults) {
                DriverDescription driverDescription = parseDriverDescription(result);
                if (!driverAgent.getDescription().equals(driverDescription)) {
                    driverAgent.addDriver(driverDescription);
                    LOG.info(" Got Yellow page Service from " +
                            driverDescription.getName() +
                            " with trip " + driverDescription.getTrip());
                }
            }

        } catch (FIPAException e) {
            LOG.error("Error in FIPA Protocol", e);
        }

    }

    private DriverDescription parseDriverDescription(DFAgentDescription result) {
        DriverDescription driverDescription = null;

        Iterator iter = result.getAllServices();
        while (iter.hasNext()) {
            ServiceDescription s = (ServiceDescription) iter.next();
            Iterator properties = s.getAllProperties();
            Trip trip = parseTrip(properties);
            driverDescription = new DriverDescription(s.getName(),
                    result.getName(), trip);
        }
        return driverDescription;
    }

    private Trip parseTrip(Iterator properties) {
        int from = 0;
        int to = 0;
        while (properties.hasNext()) {
            Property property = (Property) properties.next();
            Object o = property.getValue();
            String propertyName = property.getName();
            switch (propertyName) {
                case "from":
                    from = Integer.parseInt(o.toString());
                    break;
                case "to":
                    to = Integer.parseInt(o.toString());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return new Trip(from, to);
    }
}
