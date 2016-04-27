package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.SubsetGenerator;
import com.rida.tools.Trip;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;


import java.util.*;


public class ServerPassengerBehaviour extends TickerBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(ServerPassengerBehaviour.class);
    private static final long serialVersionUID = -4274531231778761295L;
    private DriverAgent driverAgent;
    private boolean sendedRequests = false;
    private double bestCost = Double.NEGATIVE_INFINITY;
    private final double chaufferCoefficientLimit = 3.456;
    private final double passengerCoefficientNewCost = 1.2;
    private final double passengerCoefficientLimit = 2.0736;
    private HashMap<String, Double> costsForChauffers = new HashMap<>();
    private Set<DriverDescription> potentialChauffeurs = new HashSet<>();
    private boolean waitForConfirm = false;
    private AID expectedChauffeur = null;


    public ServerPassengerBehaviour(Agent a, long period) {
        super(a, period);
    }


    private void becomeChaufferAndInformAll() {
        driverAgent.becomeChauffeur();
        LOG.info("chauffeur now!");

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setConversationId("i'm chauffeur");
        for (DriverDescription dd : potentialChauffeurs) {
            msg.addReceiver(dd.getAid());
            LOG.info(" inform about new chauffeur other potential chauffeur - " + dd.getAid().getLocalName());
        }
        for (DriverDescription dd : driverAgent.getPotentialPassengers()) {
            msg.addReceiver(dd.getAid());
            LOG.info("inform about new chauffeur other potential passenger - " + dd.getAid().getLocalName());
        }
        driverAgent.send(msg);
        potentialChauffeurs.clear();
        costsForChauffers.clear();

        if (waitForConfirm)
            throw new IllegalStateException("WHAAAAAAAAAAT!!");
    }


    private void removePotentialChauffeur(AID aid) {
        for (DriverDescription dd : potentialChauffeurs) {
            if (dd.getName().equals(aid.getName())) {
                potentialChauffeurs.remove(dd);
                return;
            }
        }

        throw new IllegalStateException("not found potential chauffer while removing");
    }


    private boolean shouldBeChauffeur() {
        driverAgent.sortPotentialPassengersByCost();

        double maxProfitIfChauffeur = driverAgent.getSumCostPotentialPassenger();

        return (maxProfitIfChauffeur > chaufferCoefficientLimit * bestCost);
    }


    private void sendRequests() {
        sendedRequests = true;

        potentialChauffeurs = driverAgent.getGoodTrips();
        if (potentialChauffeurs.size() == 0) {
            becomeChaufferAndInformAll();
            LOG.info("chauffeur because there is no one good driver");
            return;
        }

        double currentCost = 0;
        for (DriverDescription driver : potentialChauffeurs) {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.addReceiver(driver.getAid());
            currentCost = driver.getTrip().getCost();
            LOG.info(" send a message to " +
                    driver.getAid().getLocalName() + "  with cost " + currentCost);
            cfp.setContent(String.valueOf(currentCost));
            cfp.setConversationId("bring-up");
            driverAgent.send(cfp);
            costsForChauffers.put(driver.getName(), currentCost);

            if (currentCost > bestCost)
                bestCost = currentCost;
        }
    }


    private void receiveRequestFromPotentialChauffeurs() {
        if (driverAgent.isChauffeur())
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = null;

        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;

            if (costsForChauffers.get(msg.getSender().getName()) == null)
                continue;

            ACLMessage newMsg = msg.createReply();
            double newCost = Double.parseDouble(msg.getContent());
            newCost *= passengerCoefficientNewCost;

            if (newCost > passengerCoefficientLimit * costsForChauffers.get(msg.getSender().getName())) {
                newMsg.setContent("Too expensive");
                newMsg.setPerformative(ACLMessage.REFUSE);
                removePotentialChauffeur(msg.getSender());
                if (!costsForChauffers.containsKey(msg.getSender().getName())) {
                    throw new IllegalStateException("got refuse from unknown chauffeur");
                }
                costsForChauffers.remove(msg.getSender().getName());
                LOG.info(" refuse because this guy - " +
                        msg.getSender().getLocalName() + " too expensive");

                if (potentialChauffeurs.size() == 0) {
                    if (waitForConfirm)
                        throw new IllegalStateException("try to become chauffeur while waiting confirm");

                    becomeChaufferAndInformAll();
                    LOG.info(" chauffeur because other chauffeurs are too expensive or rejected my proposal");
                    return;
                }

            } else {
                newMsg.setContent(String.valueOf(newCost));
                newMsg.setPerformative(ACLMessage.CFP);
                LOG.info(" new cost(" + newCost + ") for chauffeur " +
                        msg.getSender().getLocalName());
            }
            driverAgent.send(newMsg);
        }
    }


    private void receiveRequestFromPotentialPassengers() {
        if (driverAgent.isChauffeur())
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = null;
        double currentCost = 0;
        List<ACLMessage> messages = new LinkedList<>();

        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;
            AID driverAID = msg.getSender();


            boolean flag = driverAgent.addPotentialPassengerByName(driverAID);
            if (flag) {
                LOG.info(" i(passenger) add new potential passenger - " +
                        msg.getSender().getLocalName());
                currentCost = Double.parseDouble(msg.getContent());
                driverAgent.setCostToPotentialPassenger(driverAID, currentCost);
                messages.add((ACLMessage) msg.clone());
                LOG.info(" got msg CFP from potential passenger " +
                        msg.getSender().getLocalName());
            }
        }

        if (!driverAgent.isChauffeur()) {
            if (!waitForConfirm && shouldBeChauffeur()) {
                becomeChaufferAndInformAll();
                LOG.info("i'm chauffeur because it's more profitable");
            } else {
                for (ACLMessage message : messages) {
                    ACLMessage newMsg = message.createReply();
                    newMsg.setContent(message.getContent());
                    newMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                    driverAgent.send(newMsg);
                    LOG.info(" need more money, sended to " +
                            message.getSender().getLocalName());
                }
            }
        }
    }


    private void handleRefuseFromPotentialPassengers() {
        if (driverAgent.isChauffeur())
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = null;

        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;

            driverAgent.removePotentialPassenger(msg.getSender());
        }
    }


    private void handleInformAboutChauffeur() {
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("i'm chauffeur"));

        ACLMessage msg = null;
        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;

            driverAgent.removePotentialPassenger(msg.getSender());
            LOG.info(" i've got message about new chauffeur - " +
                    msg.getSender().getLocalName());
        }
    }


    private void handleAcceptFromPotentialChauffeur() {
        if (waitForConfirm || driverAgent.isChauffeur())
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(
                ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchConversationId("bring-up"));

        ACLMessage msg = null;
        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;

            LOG.info(" have handled accept proposal from chauffeur - " +
                    msg.getSender().getLocalName());
            waitForConfirm = true;
            ACLMessage newMsg = msg.createReply();
            newMsg.setPerformative(ACLMessage.AGREE);
            expectedChauffeur = msg.getSender();
            myAgent.send(newMsg);
            break;

        }
    }


    private void handleConfirmFromPotentialChauffeur() {
        if (!waitForConfirm || driverAgent.isChauffeur())
            return;


        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                MessageTemplate.MatchConversationId("bring-up"));

        ACLMessage msg = null;
        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;

            if (!msg.getSender().getName().equals(expectedChauffeur.getName())) {
                throw new IllegalStateException("confirm from unexpected chauffeur - " + msg.getSender().getLocalName());
            }

            ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
            newMsg.setConversationId("i'm gone");

            for (DriverDescription dd : driverAgent.getDrivers()) {
                if (dd.getAid() != msg.getSender() && !dd.getAid().getLocalName().equals(myAgent.getAID().getLocalName())) {
                    newMsg.addReceiver(dd.getAid());
                    LOG.info(" notificate that i'm gone like passenger " +
                            dd.getAid().getLocalName());
                }
            }

            driverAgent.send(newMsg);
            LOG.info(" gone like passenger(with " +
                    msg.getSender().getLocalName() + " )");
            myAgent.doDelete();
            break;
        }
    }


    private void handleDisconfirmFromPotentialChauffeur() {
        if (!waitForConfirm || driverAgent.isChauffeur())
            return;


        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM),
                MessageTemplate.MatchConversationId("bring-up"));

        ACLMessage msg = null;
        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;

            waitForConfirm = false;
            expectedChauffeur = null;
            LOG.info(" i've handled disconfirm from chauffeur " +
                    msg.getSender().getLocalName());
            return;
        }
    }


    private void handleInformAboutLeaving() {
        if (driverAgent.isChauffeur())
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("i'm gone"));

        ACLMessage msg = null;
        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;

            LOG.info(" i've(passenger) got msg about leaving " +
                    msg.getSender().getLocalName());
            driverAgent.addGoneDriver(msg.getSender());

            driverAgent.removePotentialPassenger(msg.getSender());
            for (DriverDescription dd : potentialChauffeurs) {
                if (dd.getName().equals(msg.getSender().getName())) {
                    potentialChauffeurs.remove(dd);
                    break;
                }
            }
            costsForChauffers.remove(msg.getSender().getName());
            if (!driverAgent.isChauffeur() && potentialChauffeurs.isEmpty()) {
                if (waitForConfirm)
                    throw new IllegalStateException("try to become chauffeur while waiting confirm");

                becomeChaufferAndInformAll();
                LOG.info(" chauffeur because all guys leaving");
                return;
            }

        }

    }


    @Override
    public void onTick() {
        driverAgent = (DriverAgent) myAgent;
        if (!sendedRequests) {
            sendRequests();
            return;
        }

        if (driverAgent.isChauffeur())
            return;

        handleInformAboutLeaving();
        receiveRequestFromPotentialPassengers();
        receiveRequestFromPotentialChauffeurs();
        handleRefuseFromPotentialPassengers();
        handleInformAboutChauffeur();
        handleAcceptFromPotentialChauffeur();
        handleDisconfirmFromPotentialChauffeur();
        handleConfirmFromPotentialChauffeur();
    }

}