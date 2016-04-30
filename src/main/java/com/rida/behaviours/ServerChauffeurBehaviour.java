package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.Consts;
import com.rida.tools.DriverDescription;
import com.rida.tools.Helper;
import com.rida.tools.SubsetGenerator;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


public class ServerChauffeurBehaviour extends TickerBehaviour {
    private static final Logger LOG = LoggerFactory.getLogger(ServerChauffeurBehaviour.class);
    private static final long serialVersionUID = -5461756253835984225L;
    private DriverAgent driverAgent;
    private boolean waitForAgree = false;
    private boolean failForAgree = false;
    private ArrayList<AID> expectedPassengers = new ArrayList<>();
    private ArrayList<AID> passengersForConfirm = new ArrayList<>();

    public ServerChauffeurBehaviour(Agent a, long period) {
        super(a, period);
    }




    private void receiveRequestsFromPassengers() {
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        ACLMessage msg;

        while (true) {
            msg = myAgent.receive(mt);
            if (msg == null)
                break;
            AID driverAID = msg.getSender();

            boolean flag = driverAgent.addPotentialPassengerByName(driverAID);
            if (flag)
                LOG.info(" i(chauffeur) add new potential passenger - " +
                        msg.getSender().getLocalName());
        }
    }


    private void checkAndRemovePotentialPassengers() {
        Set<AID> goneDrivers = driverAgent.getGoneDrivers();
        for (AID aid : goneDrivers) {
            for (DriverDescription dd : driverAgent.getPotentialPassengers()) {
                if (dd.getAid().getName().equals(aid.getName())) {
                    driverAgent.removePotentialPassenger(aid);
                    break;
                }
            }
        }
    }


    private void performAcceptProposals() {
        if (waitForAgree)
            return;

        checkAndRemovePotentialPassengers();
        if (driverAgent.getSetPotentialPassengers().isEmpty()) {
            goneAndInformAll();
            driverAgent.doDelete();
            return;
        }
        ACLMessage msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
        msg.setConversationId(Consts.BRINGUP_ID);

        Set<DriverDescription> bestPassengers = Helper.getBestPassengers(driverAgent,
                driverAgent.getSetPotentialPassengers());
        String info = myAgent.getLocalName() + " my(chauffeur) potential passengers:\n";
        for (DriverDescription dd : driverAgent.getSetPotentialPassengers()) {
            info += "\t\t" + dd.getAid().getLocalName() + "\n";
        }
        LOG.info(info);

        for (DriverDescription dd : bestPassengers) {
            msg.addReceiver(dd.getAid());
            expectedPassengers.add(dd.getAid());
            LOG.info(" i send accept to " + dd.getAid().getLocalName());
        }
        driverAgent.send(msg);
        waitForAgree = true;
    }


    private void handleAgreeExpectedPassengers() {
        if (!waitForAgree)
            return;

        ACLMessage msg;
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(
                ACLMessage.AGREE), MessageTemplate.MatchConversationId(Consts.BRINGUP_ID));
        while ((msg = myAgent.receive(mt)) != null) {
            if (!expectedPassengers.contains(msg.getSender()))
                continue;

            LOG.info(" yeaah, this guy(" +
                    msg.getSender().getLocalName() + ") agree to go with me");
            expectedPassengers.remove(msg.getSender());
            passengersForConfirm.add(msg.getSender());
            if (expectedPassengers.isEmpty()) {
                if (failForAgree) {
                    failForAgree = false;
                    informAboutDisconfirm();
                }
                else {
                    goneAndInformAll();
                    driverAgent.doDelete();
                    return;
                }
            }
        }

    }


    private void handleOtherBadNewsFromPassengers() {
        ACLMessage msg;
        MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative( ACLMessage.REFUSE),
                MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                                   MessageTemplate.MatchPerformative(ACLMessage.CANCEL)));

        while ((msg = myAgent.receive(mt)) != null) {
            AID senderAID = msg.getSender();
            if (msg.getPerformative() == ACLMessage.INFORM && msg.getConversationId().equals(Consts.IMGONE_ID)) {
                driverAgent.addGoneDriver(msg.getSender());
            }


            if (driverAgent.removePotentialPassenger(senderAID)) {
                LOG.info(" success remove potential passenger - " +
                        senderAID.getLocalName());
            } else {
                LOG.info(" fail to remove potential passenger - " +
                        senderAID.getLocalName());
            }

            switch (msg.getPerformative()) {
                case ACLMessage.REFUSE: {
                    LOG.info(" this guy(" + msg.getSender().getLocalName() + ") don't agree go with me(REFUSE)");
                    break;
                }
                case ACLMessage.CANCEL: {
                    LOG.info("receive cancel from " + msg.getSender().getLocalName());
                    break;
                }
                case ACLMessage.INFORM: {
                    if (msg.getConversationId().equals(Consts.IMGONE_ID))
                        LOG.info("receive GONE from " + msg.getSender().getLocalName());
                    else if (msg.getConversationId().equals(Consts.IMCHAUFFER_ID))
                        LOG.info("receive 'become chauffer' from " + msg.getSender().getLocalName());
                    else
                        throw new IllegalStateException("conversationID: " + msg.getConversationId() +
                                " sender: " + msg.getSender().getLocalName());
                    break;
                }
            }

            String info = myAgent.getLocalName() + " my potential passengers after removing:\n";
            for (DriverDescription dd : driverAgent.getSetPotentialPassengers())
                info += "\t\t" + dd.getAid().getLocalName() + "\n";

            LOG.info(info);

            if (waitForAgree && expectedPassengers.contains(msg.getSender())) {
                failForAgree = true;
                expectedPassengers.remove(msg.getSender());
                passengersForConfirm.add(msg.getSender());

            }

            if (expectedPassengers.isEmpty())
                informAboutDisconfirm();

        }

        ACLMessage msgTrash;
        MessageTemplate mtTrash = MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);

        while (true) {
            msgTrash = myAgent.receive(mtTrash);
            if (msgTrash == null)
                break;
        }
    }


    private void informAboutDisconfirm() {
        waitForAgree = false;
        ACLMessage msg = new ACLMessage(ACLMessage.DISCONFIRM);
        msg.setConversationId(Consts.BRINGUP_ID);

        for (AID dd : passengersForConfirm) {
            msg.addReceiver(dd);
            LOG.info(" disconfirm to " + dd.getLocalName());
        }
        driverAgent.send(msg);
        passengersForConfirm.clear();
    }


    private void goneAndInformAll() {
        ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
        msg.setConversationId(Consts.BRINGUP_ID);

        String info = driverAgent.getLocalName() + " i'm gone like chauffeur with:\n";
        for (AID aid : passengersForConfirm) {
            msg.addReceiver(aid);
            info += "\t\t" + aid.getLocalName() + "\n";
        }

        driverAgent.send(msg);
        LOG.info(info);

        ACLMessage msgInfo = new ACLMessage(ACLMessage.INFORM);
        msgInfo.setConversationId(Consts.IMGONE_ID);
        for (DriverDescription dd : driverAgent.getDrivers()) {
            AID currentAID = dd.getAid();
            if (!passengersForConfirm.contains(currentAID) && !currentAID.equals(myAgent.getAID())) {
                msgInfo.addReceiver(currentAID);
                LOG.info(" i(chauffeur) notificate that i gone " + currentAID.getLocalName());
            }
        }
        driverAgent.send(msgInfo);
        sendChaufferStatistic();
        LOG.info("I public statistic about my ride in Yellow Pages");
    }

    private void sendChaufferStatistic() {
        Set<DriverDescription> passangers = new HashSet<>();
        for (AID aid : passengersForConfirm) {
            passangers.add(driverAgent.getPotentionalPassngerByAID(aid));
        }
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(driverAgent.getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Consts.STATISTICS_ID);
        sd.setName(driverAgent.getName());
        sd.addProperties(new Property("type", "chauffer"));
        sd.addProperties(new Property("way-length", Helper.calcBestSetProfit(passangers, driverAgent
                .getMapGraph(), driverAgent.getDescription().getTrip())));
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
        if (!driverAgent.isChauffeur())
            return;

        receiveRequestsFromPassengers();
        handleOtherBadNewsFromPassengers();
        handleAgreeExpectedPassengers();
        performAcceptProposals();
    }
}
