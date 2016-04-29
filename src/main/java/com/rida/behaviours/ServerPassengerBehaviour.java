package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.Consts;
import com.rida.tools.DriverDescription;
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

/**
 * Поведение пассажира
 */
public class ServerPassengerBehaviour extends TickerBehaviour {

    private static final Logger LOG = LoggerFactory.getLogger(ServerPassengerBehaviour.class);
    private static final long serialVersionUID = -4274531231778761295L;
    private DriverAgent driverAgent;
    private boolean sendedRequests = false;
    private double bestCost = Double.NEGATIVE_INFINITY;
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
        sendMessage(null, ACLMessage.INFORM, Consts.IMCHAUFFER_ID, potentialChauffeurs);
        LOG.info(" inform about new chauffeur other potential chauffeurs");
        sendMessage(null, ACLMessage.INFORM, Consts.IMCHAUFFER_ID, driverAgent.getSetPotentialPassengers());
        LOG.info("inform about new chauffeur gother potential passengers");
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

        return maxProfitIfChauffeur > Consts.CHAUFFER_COEF_LIMIT * bestCost;
    }


    private void sendRequests() {
        sendedRequests = true;

        potentialChauffeurs = driverAgent.getGoodTrips();
        if (potentialChauffeurs.isEmpty()) {
            becomeChaufferAndInformAll();
            LOG.info("chauffeur because there is no one good driver");
            return;
        }

        double currentCost;
        for (DriverDescription driver : potentialChauffeurs) {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            cfp.addReceiver(driver.getAid());
            currentCost = driver.getTrip().getCost();
            LOG.info(" send a message to " +
                    driver.getAid().getLocalName() + "  with cost " + currentCost);
            cfp.setContent(String.valueOf(currentCost));
            cfp.setConversationId(Consts.BRINGUP_ID);
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
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage msg;

        while ((msg = myAgent.receive(mt)) != null) {
            replyOnRequestFromPotentionalPassanger(msg);
        }

    }


    private void receiveRequestFromPotentialPassengers() {
        if (driverAgent.isChauffeur())
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
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage msg;

        while ((msg = myAgent.receive(mt)) != null) {
            driverAgent.removePotentialPassenger(msg.getSender());
        }
    }


    private void handleInformAboutChauffeur() {
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("i'm chauffeur"));

        ACLMessage msg;
        while ((msg = myAgent.receive(mt)) != null) {
            driverAgent.removePotentialPassenger(msg.getSender());
            LOG.info(" i've got message about new chauffeur - " +
                    msg.getSender().getLocalName());
        }
    }


    private void handleAcceptFromPotentialChauffeur() {
        if (waitForConfirm || driverAgent.isChauffeur())
            return;

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(
                ACLMessage.ACCEPT_PROPOSAL), MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));

        ACLMessage msg;
        while ((msg = myAgent.receive(mt)) != null) {
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
        if (!waitForConfirm || driverAgent.isChauffeur())
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
                    LOG.info(" notificate that i'm gone like passenger " +
                            dd.getAid().getLocalName());
                    sendPassengerStatistic();
                }
            }

            driverAgent.send(newMsg);
            LOG.info(" gone like passenger(with " +
                    senderAID.getLocalName() + " )");
            myAgent.doDelete();
            break;
        }

    }


    private void handleDisconfirmFromPotentialChauffeur() {
        if (!waitForConfirm || driverAgent.isChauffeur())
            return;


        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));

        ACLMessage msg;
        while ((msg = myAgent.receive(mt)) != null) {
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
                MessageTemplate.MatchConversationId(Consts.IMGONE_ID));

        ACLMessage msg;
        while ((msg = myAgent.receive(mt)) != null) {
            replyOnMessgaeAboutLeaving(msg);
        }

    }

    private void replyOnMessgaeAboutLeaving(ACLMessage message) {
        LOG.info(" i've(passenger) got message about leaving " +
                message.getSender().getLocalName());
        driverAgent.addGoneDriver(message.getSender());

        driverAgent.removePotentialPassenger(message.getSender());
        for (DriverDescription dd : potentialChauffeurs) {
            if (dd.getName().equals(message.getSender().getName())) {
                potentialChauffeurs.remove(dd);
                break;
            }
        }
        costsForChauffers.remove(message.getSender().getName());
        if (!driverAgent.isChauffeur() && potentialChauffeurs.isEmpty()) {
            if (waitForConfirm)
                throw new IllegalStateException("try to become chauffeur while waiting confirm");

            becomeChaufferAndInformAll();
            LOG.info(" chauffeur because all guys leaving");
        }

    }

    private void replyOnRequestFromPotentionalPassanger(ACLMessage message) {
        AID senderAID = message.getSender();
        ACLMessage newMsg = message.createReply();
        double newCost = Double.parseDouble(message.getContent());
        newCost *= Consts.PASSANGER_COEF_NEW_COST;

        if (costsForChauffers.get(senderAID.getName()) == null)
            return;


        if (newCost > Consts.PASSANGER_COEF_LIMIT * costsForChauffers.get(senderAID.getName())) {
            newMsg.setContent("Too expensive");
            newMsg.setPerformative(ACLMessage.REFUSE);
            removePotentialChauffeur(senderAID);
            if (!costsForChauffers.containsKey(senderAID.getName()) || (potentialChauffeurs.isEmpty() &&
                    waitForConfirm)) {
                throw new IllegalStateException();
            }
            costsForChauffers.remove(senderAID.getName());
            LOG.info(" refuse because this guy - " +
                    senderAID.getLocalName() + " too expensive");

            if (potentialChauffeurs.isEmpty()) {
                becomeChaufferAndInformAll();
                LOG.info(" chauffeur because other chauffeurs are too expensive or rejected my proposal");
                return;
            }

        } else {
            newMsg.setContent(String.valueOf(newCost));
            newMsg.setPerformative(ACLMessage.CFP);
            LOG.info(" new cost(" + newCost + ") for chauffeur " +
                    senderAID.getLocalName());
        }
        driverAgent.send(newMsg);
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