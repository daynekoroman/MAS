package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.Consts;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.Helper;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.List;

/**
 * Поведение пассажира
 */
public class ServerPassengerBehaviour extends TickerBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(ServerPassengerBehaviour.class);
    private static final long serialVersionUID = -4274531231778761295L;
    private DriverAgent driverAgent;
    private boolean sendedRequests = false;
    private Set<DriverDescription> potentialChauffeurs = new HashSet<>();
    private boolean waitForConfirm = false;
    private AID expectedChauffeur = null;
    private double initialCost = Double.NaN;
    private Set<DriverDescription> passengersToAttempt = null;
    private Set<DriverDescription> agreedPassengers = null;
    private Set<DriverDescription> disagreedPassengers = null;
    private boolean deleted = false;
    private boolean failToTryChauffeurFlag = false;


    public ServerPassengerBehaviour(Agent a, long period) {
        super(a, period);
    }


    private boolean containsPotentialChauffeurByAID(AID aid) {
        for (DriverDescription dd : potentialChauffeurs) {
            if (dd.getName().equals(aid.getName())) {
                return true;
            }
        }

        return false;
    }


    private void becomeChaufferAndInformAll() {
        driverAgent.becomeChauffeur();
        LOG.info("chauffeur now!");
        sendMessage(null, ACLMessage.INFORM, Consts.IMCHAUFFER_ID, potentialChauffeurs);
        LOG.info(" inform about new chauffeur other potential chauffeurs");
        sendMessage(null, ACLMessage.INFORM, Consts.IMCHAUFFER_ID, driverAgent.getSetPotentialPassengers());
        LOG.info("inform about new chauffeur gother potential passengers");
        potentialChauffeurs.clear();
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

//        throw new IllegalStateException("not found potential chauffer while removing " + aid.getLocalName());
    }


    private boolean shouldBeChauffeur(Set<DriverDescription> currentPassengers) {

        Set<DriverDescription> passengers = Helper.getBestPassengers(driverAgent, currentPassengers);
        if (passengers == null || passengers.isEmpty())
            return false;
        boolean flag = (Helper.calcBestSetProfit(passengers, driverAgent.getMapGraph(), driverAgent.getDescription().getTrip()) > 0f);
        if (flag) {
            passengersToAttempt = new HashSet<>(passengers);
            agreedPassengers = new HashSet<>();
            disagreedPassengers = new HashSet<>();
        }

        return flag;
    }


    private double calcCost() {
        Graph g = driverAgent.getMapGraph();
        int from = driverAgent.getDescription().getTrip().getFrom();
        int to = driverAgent.getDescription().getTrip().getTo();
        double cost = g.bfs(from, to);
        Random rand = new Random();
        cost += cost * 0.00001 * (rand.nextInt() % 200 - 100);
        return cost;
    }

    private void checkBecomeChauffeur() {
        if (potentialChauffeurs.isEmpty() && passengersToAttempt == null) {
            if (waitForConfirm)
                throw new IllegalStateException();

            becomeChaufferAndInformAll();
            LOG.info("chauffeur because there is no one good driver");
        }
    }

    private void sendRequests() {
        sendedRequests = true;
        potentialChauffeurs = driverAgent.getGoodTrips();

        double cost = calcCost();
        initialCost = cost;
        ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
        cfp.setContent(String.valueOf(cost));
        cfp.setConversationId(Consts.BRINGUP_ID);

        for (DriverDescription driver : potentialChauffeurs) {
            cfp.addReceiver(driver.getAid());
            LOG.info(" send a message to " + driver.getAid().getLocalName() + "  with cost " + cost);

        }
        driverAgent.send(cfp);
    }


    private void receiveRejectFromPotentialChauffeurs() {
        if (driverAgent.isChauffeur() || deleted)
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage msg;

        while ((msg = myAgent.receive(mt)) != null) {
            replyOnRejectFromPotentialChauffeurs(msg);
        }

    }


    private void replyOnRejectFromPotentialChauffeurs(ACLMessage message) {

        AID senderAID = message.getSender();
        ACLMessage newMsg = message.createReply();
        double newCost = Double.parseDouble(message.getContent());
        newCost *= Consts.PASSANGER_COEF_NEW_COST;

        if (newCost > Consts.PASSANGER_COEF_LIMIT * initialCost) {
            removePotentialChauffeur(senderAID);

            if (!containsPotentialChauffeurByAID(senderAID))
                return;

            newMsg.setContent("Too expensive");
            newMsg.setPerformative(ACLMessage.REFUSE);

            LOG.info(" refuse because this guy - " + senderAID.getLocalName() + " too expensive");

            if (potentialChauffeurs.isEmpty()) {
                becomeChaufferAndInformAll();
                LOG.info(" chauffeur because other chauffeurs are too expensive or rejected my proposal");
                return;
            }

        } else {
            newMsg.setContent(String.valueOf(newCost));
            newMsg.setPerformative(ACLMessage.CFP);
            LOG.info(" new cost(" + newCost + ") for chauffeur " + senderAID.getLocalName());
        }
        driverAgent.send(newMsg);
    }


    private void receiveRequestFromPotentialPassengers() {
        if (driverAgent.isChauffeur() || deleted)
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage msg;
        double currentCost;
        List<ACLMessage> messages = new LinkedList<>();

        while ((msg = myAgent.receive(mt)) != null) {
            AID driverAID = msg.getSender();

            boolean flag = driverAgent.addPotentialPassengerByName(driverAID);
            if (flag) {

                currentCost = Double.parseDouble(msg.getContent());
                driverAgent.setCostToPotentialPassenger(driverAID, currentCost);
                messages.add((ACLMessage) msg.clone());
                LOG.info(" got msg CFP from potential passenger " + msg.getSender().getLocalName());
            }
        }

        if (passengersToAttempt == null && !waitForConfirm && shouldBeChauffeur(driverAgent.getSetPotentialPassengers())) {
            tryToBecomeChauffeur();
            LOG.info("i'm trying to become chauffeur");

        } else {
            for (ACLMessage message : messages) {
                ACLMessage newMsg = message.createReply();
                newMsg.setContent(message.getContent());
                newMsg.setPerformative(ACLMessage.REJECT_PROPOSAL);
                driverAgent.send(newMsg);
                LOG.info(" need more money, sended to " + message.getSender().getLocalName());
            }
        }

    }


    private void tryToBecomeChauffeur() {
        if (deleted)
            return;
        ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        msg.setConversationId(Consts.BRINGUP_ID);

        for (DriverDescription dd : passengersToAttempt) {
            msg.addReceiver(dd.getAid());
            LOG.info("try to bring passenger " + dd.getAid().getLocalName());
        }
        driverAgent.send(msg);
    }


    private void failTryToBeChauffeur() {

        ACLMessage newMsg = new ACLMessage(ACLMessage.DISCONFIRM);
        newMsg.setConversationId(Consts.BRINGUP_ID);
        for (DriverDescription dd : agreedPassengers) {
            newMsg.addReceiver(dd.getAid());
            LOG.info(" FAIL! disconfirm to " + dd.getAid().getLocalName());
        }
        driverAgent.send(newMsg);

        HashSet<DriverDescription> newSet = new HashSet<>(driverAgent.getSetPotentialPassengers());
        for (DriverDescription dd : disagreedPassengers) {
            Helper.removeFromCollectionByAID(newSet, dd.getAid());
        }
        passengersToAttempt = null;
        agreedPassengers = null;
        disagreedPassengers = null;

        if (shouldBeChauffeur(newSet)) {
            tryToBecomeChauffeur();
        }
    }


    private void handleForAttemptToBeChauffeur() {
        if (passengersToAttempt == null || deleted)
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CANCEL),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage msg;

        if ((msg = myAgent.receive(mt)) != null) {
            if (Helper.containsInCollectionByAID(passengersToAttempt, msg.getSender())) {
                failToTryChauffeurFlag = true;
                disagreedPassengers.add(Helper.getFromCollectionByAID(passengersToAttempt, msg.getSender()));
                Helper.removeFromCollectionByAID(passengersToAttempt, msg.getSender());

            }

            if (passengersToAttempt.isEmpty()) {
                failToTryChauffeurFlag = false;
                failTryToBeChauffeur();
                return;
            }
        }

        MessageTemplate goodMT = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.AGREE),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage goodMSG;

        while ((goodMSG = myAgent.receive(goodMT)) != null) {
            if (Helper.containsInCollectionByAID(passengersToAttempt, goodMSG.getSender())) {
                agreedPassengers.add(Helper.getFromCollectionByAID(passengersToAttempt, goodMSG.getSender()));
                Helper.removeFromCollectionByAID(passengersToAttempt, goodMSG.getSender());

            }
        }

        if (passengersToAttempt.isEmpty()) {
            if (failToTryChauffeurFlag) {
                failToTryChauffeurFlag = false;
                failTryToBeChauffeur();
            } else {

                ACLMessage successMSG = new ACLMessage(ACLMessage.CONFIRM);
                successMSG.setConversationId(Consts.BRINGUP_ID);
                for (DriverDescription dd : agreedPassengers) {
                    LOG.info("gone like chauffeur with passenger " + dd.getAid().getLocalName());
                    successMSG.addReceiver(dd.getAid());
                }
                driverAgent.send(successMSG);

                ACLMessage infoMSG = new ACLMessage(ACLMessage.INFORM);
                infoMSG.setConversationId(Consts.IMGONE_ID);
                Set<DriverDescription> allDrivers = driverAgent.getDrivers();
                for (DriverDescription dd : allDrivers) {
                    if (!Helper.containsInCollectionByAID(agreedPassengers, dd.getAid())) {
                        LOG.info("gone like chauffeur. Notification for " + dd.getAid().getLocalName());
                        infoMSG.addReceiver(dd.getAid());
                    }
                }
                driverAgent.send(infoMSG);
                deleted = true;
            }
        }
    }


    private void handleRefuseFromPotentialPassengers() {
        if (driverAgent.isChauffeur() || deleted)
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REFUSE),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage msg;

        while ((msg = myAgent.receive(mt)) != null) {
            driverAgent.removePotentialPassenger(msg.getSender());
        }
    }


    private void handleInformAboutChauffeur() {
        if (deleted)
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId(Consts.IMCHAUFFER_ID));

        ACLMessage msg;
        while ((msg = myAgent.receive(mt)) != null) {
            driverAgent.removePotentialPassenger(msg.getSender());
            LOG.info(" i've got message about new chauffeur - " +
                    msg.getSender().getLocalName());
        }
    }


    private void handleAcceptFromPotentialChauffeur() {
        if (driverAgent.isChauffeur() || deleted || waitForConfirm)
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(
                ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));

        ACLMessage msg;
        while ((msg = myAgent.receive(mt)) != null) {

            if (passengersToAttempt != null) {
                if (expectedChauffeur == msg.getSender())
                    throw new IllegalStateException("unexpected accept from " + msg.getSender().getLocalName());

                ACLMessage newMsg = msg.createReply();
                newMsg.setPerformative(ACLMessage.CANCEL);
                LOG.info("cancel accept from " + msg.getSender().getLocalName() + " because i'm trying to be chauffeur");
                myAgent.send(newMsg);
                continue;
            }

            LOG.info(" have handled accept proposal from chauffeur - " + msg.getSender().getLocalName());
            waitForConfirm = true;
            ACLMessage newMsg = msg.createReply();
            newMsg.setPerformative(ACLMessage.AGREE);
            expectedChauffeur = msg.getSender();
            LOG.info(" send AGREE to " + msg.getSender().getLocalName());
            myAgent.send(newMsg);
            break;

        }
    }


    private void sendMessage(String content, int performative, String id, Set<DriverDescription> receivers) {
        ACLMessage message = new ACLMessage(performative);
        message.setConversationId(id);
        for (DriverDescription dd : receivers) {
            AID reciverAID = dd.getAid();
            if (reciverAID.equals(driverAgent.getAID())) {
                message.addReceiver(dd.getAid());
            }
        }
        message.setContent(content);
        driverAgent.send(message);
    }


    private void handleConfirmFromPotentialChauffeur() {
        if (!waitForConfirm || driverAgent.isChauffeur() || deleted)
            return;


        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));

        ACLMessage message;
        while ((message = myAgent.receive(mt)) != null) {
            AID senderAID = message.getSender();
            if (!senderAID.equals(expectedChauffeur)) {
                throw new IllegalStateException("confirm from unexpected chauffeur - " + senderAID);
            }

            ACLMessage newMsg = new ACLMessage(ACLMessage.INFORM);
            newMsg.setConversationId(Consts.IMGONE_ID);

            for (DriverDescription dd : driverAgent.getDrivers()) {
                if (dd.getAid() != senderAID && !dd.getAid().getLocalName().equals(myAgent.getAID().getLocalName())) {
                    newMsg.addReceiver(dd.getAid());


                    LOG.info(" notificate " + dd.getAid().getLocalName() + " that i've gone like passenger ");
                }
            }

            driverAgent.send(newMsg);
            LOG.info(" CONFIRM! i gone like passenger(with " + senderAID.getLocalName() + " )");
            deleted = true;
            break;
        }

    }


    private void handleDisconfirmFromPotentialChauffeur() {
        if (!waitForConfirm || driverAgent.isChauffeur() || deleted)
            return;


        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));

        ACLMessage msg;
        while ((msg = myAgent.receive(mt)) != null) {
            if (msg.getSender().getLocalName().equals(expectedChauffeur.getLocalName())) {
                waitForConfirm = false;
                expectedChauffeur = null;
                LOG.info(" i've handled disconfirm from chauffeur " + msg.getSender().getLocalName());
                return;
            }
        }
    }


    private void handleInformAboutLeaving() {
        if (driverAgent.isChauffeur() || deleted)
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId(Consts.IMGONE_ID));

        ACLMessage message;
        while ((message = myAgent.receive(mt)) != null) {
            if (passengersToAttempt != null && Helper.containsInCollectionByAID(passengersToAttempt, message.getSender())) {
                failToTryChauffeurFlag = true;
                disagreedPassengers.add(Helper.getFromCollectionByAID(passengersToAttempt, message.getSender()));
                Helper.removeFromCollectionByAID(passengersToAttempt, message.getSender());

            }

            LOG.info(" i've(passenger) got message about leaving " + message.getSender().getLocalName());
            driverAgent.addGoneDriver(message.getSender());
            driverAgent.removePotentialPassenger(message.getSender());
            removePotentialChauffeur(message.getSender());

            if (potentialChauffeurs.isEmpty() && passengersToAttempt == null) {
                if (waitForConfirm)
                    throw new IllegalStateException("try to become chauffeur while waiting confirm");

                becomeChaufferAndInformAll();
                LOG.info(" chauffeur because all guys leaving");
            }
        }

        if (passengersToAttempt != null && passengersToAttempt.isEmpty()) {
            failToTryChauffeurFlag = false;
            failTryToBeChauffeur();
        }
    }


    private void sendPassengerStatistic() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(driverAgent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Consts.STATISTICS_ID);
        sd.setName(driverAgent.getName());
        sd.addProperties(new Property("type", "passenger"));
        dfd.addServices(sd);
        try {
            DFService.modify(driverAgent, dfd);
        } catch (FIPAException fe) {
            LOG.error("Failed to register agent in Yellow Pages Service caused {}\n", fe);
        }
    }

    @Override
    public void onTick() {
        driverAgent = (DriverAgent) myAgent;

        if (driverAgent.isChauffeur())
            return;

        if (!sendedRequests) {
            sendRequests();
            return;
        } else {
            checkBecomeChauffeur();
        }


        receiveRequestFromPotentialPassengers();
        handleForAttemptToBeChauffeur();
        handleRefuseFromPotentialPassengers();


        handleInformAboutLeaving();
        receiveRejectFromPotentialChauffeurs();
        handleAcceptFromPotentialChauffeur();
        handleDisconfirmFromPotentialChauffeur();
        handleConfirmFromPotentialChauffeur();


        handleInformAboutChauffeur();
        if (deleted) {
            sendPassengerStatistic();
            driverAgent.doDelete();
        }
    }

}