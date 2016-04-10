package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.SubsetGenerator;
import com.rida.tools.Trip;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by daine on 03.04.2016.
 */
public class RequestRecieveServerBehaviour extends CyclicBehaviour {
    private DriverAgent driverAgent;
    private static int msgRecieved = 0;
    private static final Logger LOG = LoggerFactory.getLogger(RequestRecieveServerBehaviour.class);

    private static boolean done = false;


    private boolean shouldDrive() {
        Set<DriverDescription> d = driverAgent.getPotentialDrivers();
        Set<DriverDescription> p = driverAgent.getPassengers();
        if (d.size() != p.size())
            return false;
        for (DriverDescription dd : d) {
            for (DriverDescription ddp : p) {
                if (ddp.getName() == dd.getName() && dd.getTrip().getCost() > ddp.getTrip().getCost()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void action() {
        driverAgent = (DriverAgent) myAgent;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            AID driverName = msg.getSender();
            driverAgent.addPassenger(driverAgent.getDriverDescriptionByName(driverName));
            driverAgent.getPaseengerDescriptionByName(driverName).setCost(Double.parseDouble(msg.getContent()));
            msgRecieved ++;
            /*Set<DriverDescription> result = new HashSet<>();
            Set<DriverDescription> q = new HashSet<>();
            for (DriverDescription dd : driverAgent.getPassengers())
                q.add(new DriverDescription(dd));
            Set<DriverDescription> inq = new HashSet<>();
            calcMaxPassengersProfit(result, q, inq);*/
            // if (driverAgent.getMapGraph().bfs(driverAgent.getFrom(), driverAgent.getTo()) < getMaxProfit(result)){
        }
        if (msgRecieved == driverAgent.getPotentialPassengerCount() && !done) {
            done = true;
            //  }
            if (driverAgent.getPotentialDrivers().size() == 0 || shouldDrive()) {
                myAgent.addBehaviour(new ProposePerformer());
            }
        }
    }
}