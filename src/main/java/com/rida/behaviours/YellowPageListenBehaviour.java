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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Created by daine on 03.04.2016.
 */
public class YellowPageListenBehaviour extends OneShotBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(YellowPageListenBehaviour.class);

    private String parseName(DFAgentDescription stupidName) {
        return stupidName.getName().toString().split(" ")[3].split("@")[0];
    }

    @Override
    public void action() {
        try {

            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        DriverAgent driverAgent = (DriverAgent) myAgent;
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bring-up");
        template.addServices(sd);

        try {
            DFAgentDescription[] YellowPageResults = DFService.search(myAgent, template);

            for (DFAgentDescription result : YellowPageResults) {

                boolean isNotMe = !myAgent.getAID().equals(result.getName());
                if (isNotMe) {
                    Iterator<ServiceDescription> iter = result.getAllServices();
                    while (iter.hasNext()) {
                        ServiceDescription s = iter.next();
                        Trip trip = getTrip(s.getAllProperties());
                        DriverDescription driverDescription = new DriverDescription(s.getName(), result.getName(), trip);
                        driverAgent.addDriver(driverDescription);
                        LOG.info("Got Yellow page Service  from {} with trip {}", driverDescription.getName().split("@")[0], driverDescription.getTrip());
                    }
                }
            }

            driverAgent.calculateProfit();

        } catch (FIPAException e) {
            e.printStackTrace();
        }

    }

    private Trip getTrip(Iterator<Property> properties) {
        int from = 0,to = 0;
        while (properties.hasNext()) {
            Property property = properties.next();
            Object o = property.getValue();
            String propertyName = property.getName();
            switch (propertyName){
                case "from":
                    from = Integer.parseInt(o.toString());
                    break;
                case  "to":
                    to = Integer.parseInt(o.toString());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return  new Trip(from,to);
    }
}
