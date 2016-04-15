package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by daine on 10.04.2016.
 */
public class ProposeRecieverServerBehaviour extends CyclicBehaviour {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ProposeRecieverServerBehaviour.class);
    DriverAgent driverAgent;
    private Set<DriverDescription> waitingDrivers = new HashSet<DriverDescription>();
    private Set<DriverDescription> waitingPassengers = new HashSet<DriverDescription>();
    private int stage = 0;
    private int accCount = 0;

    private int getTripDrivers(String content) {
        int res = 0;
        for (int i = 0; i < content.split(" ").length; i ++){
            for (DriverDescription dd : driverAgent.getPotentialDrivers()){
                if (dd.getName().equals(content.split(" ")[i])){
                    res++;
                    break;
                }
            }
        }
        return res;
    }

    @Override
    public void action() {
        driverAgent = (DriverAgent) myAgent;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            waitingDrivers.add(driverAgent.getDriverDescriptionByName(msg.getSender()));
            if ((driverAgent.getPotentialDrivers().size() == waitingDrivers.size() + getTripDrivers(msg.getContent())) && stage == 0) {
                DriverDescription top = null;
                double min = 999999999;
                for (DriverDescription dd : waitingDrivers) {
                    if (top == null || dd.getTrip().getCost() < min) {
                        top = dd;
                        min = dd.getTrip().getCost();
                    }
                }
                msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                msg.addReceiver(top.getAid());
                msg.setConversationId("bring-up");
                driverAgent.send(msg);

                stage = 1;
            }
        }
        ///////////////////////////////////////////////////////

        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                MessageTemplate.MatchConversationId("bring-up"));
        msg = myAgent.receive(mt);

        if (msg != null) {
            waitingPassengers.add(driverAgent.getPaseengerDescriptionByName(msg.getSender()));
            accCount ++;
            msg = new ACLMessage(ACLMessage.CANCEL);
            if (accCount == driverAgent.getBestPassengers().size()) {
                for (DriverDescription dd : driverAgent.getPassengers()) {
                    if (!waitingPassengers.contains(dd)) {
                        msg.addReceiver(dd.getAid());
                    }
                }

                msg.setConversationId("bring-up");
                myAgent.send(msg);
                String s = "i'll bring up";
                msg = new ACLMessage(ACLMessage.CONFIRM);
                for (DriverDescription dd : driverAgent.getBestPassengers()) {
                    s += "\r\n\t\t" + dd.toString();
                    msg.addReceiver(dd.getAid());
                }
                msg.setConversationId("bring-up");
                myAgent.send(msg);
                LOG.info("{}", s);

                myAgent.doDelete();
                return;
            }
        }

        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
                MessageTemplate.MatchConversationId("bring-up"));
        msg = myAgent.receive(mt);
        if (msg != null){
            //waitingPassengers.add(driverAgent.getPaseengerDescriptionByName(msg.getSender()));
            driverAgent.deletePassenger(driverAgent.getDriverDescriptionByName(msg.getSender()));
            if (waitingPassengers.size() == driverAgent.getBestPassengers().size()){
                msg = new ACLMessage(ACLMessage.DISCONFIRM);
                for (DriverDescription dd : waitingDrivers){
                    msg.addReceiver(dd.getAid());
                }
                msg.setConversationId("bring-up");
                myAgent.send(msg);
            }
            myAgent.addBehaviour(new ProposePerformer());
        }

        ///////////////////////////////////////////

        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM),
                MessageTemplate.MatchConversationId("bring-up"));
        msg = myAgent.receive(mt);

        if (msg != null) {
            waitingDrivers.remove(driverAgent.getPaseengerDescriptionByName(msg.getSender()));
            stage = 0;
        }
        /////////////////////////////////////////////

        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CANCEL),
                MessageTemplate.MatchConversationId("bring-up"));
        msg = myAgent.receive(mt);

        if (msg != null) {
            //waitingDrivers.add(driverAgent.getDriverDescriptionByName(msg.getSender()));
            driverAgent.deletePotentialDriver(driverAgent.getDriverDescriptionByName(msg.getSender()));
            waitingDrivers.remove(driverAgent.getDriverDescriptionByName(msg.getSender()));
            stage = 0;
        }
        /////////////////////////////////////////////

        mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                MessageTemplate.MatchConversationId("bring-up"));
        msg = myAgent.receive(mt);

        if (msg != null) {
            ACLMessage propose = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            for (DriverDescription dd : waitingDrivers){
                if (!dd.getAid().equals(msg.getSender()))
                    propose.addReceiver(dd.getAid());
            }
            propose.setContent("No.Thank you.");
            propose.setConversationId("bring-up");
            driverAgent.send(propose);

            propose = new ACLMessage(ACLMessage.CANCEL);
            for (DriverDescription dd : driverAgent.getPassengers()){
                propose.addReceiver(dd.getAid());
            }
            LOG.info("I'm done");
            propose.setContent("Not today");
            propose.setConversationId("bring-up");
            driverAgent.send(propose);
            driverAgent.doDelete();
        }
    }
}
