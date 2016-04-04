package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Created by daine on 03.04.2016.
 */
public class ListenYP extends OneShotBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(ListenYP.class);

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
            DFAgentDescription[] result = DFService.search(myAgent, template);

            for (DFAgentDescription aResult : result) {

                boolean flag = myAgent.getName().equals(aResult.getName().toString().split(" ")[3]);
                if (!flag) {
                    Iterator<ServiceDescription> iter = aResult.getAllServices();
                    ServiceDescription s = iter.next();
                    driverAgent.addDriver(s.getName(), parseName(aResult), aResult.getName());
                    LOG.info("get YP {} {}", parseName(aResult), s.getName());
                }
            }

            driverAgent.calculateProfit();

        } catch (FIPAException e) {
            e.printStackTrace();
        }

    }
}
