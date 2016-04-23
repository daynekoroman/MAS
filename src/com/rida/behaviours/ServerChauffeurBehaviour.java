package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import com.rida.tools.SubsetGenerator;
import com.rida.tools.Trip;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import jade.lang.acl.MessageTemplate;

import java.awt.IllegalComponentStateException;
import java.lang.annotation.Documented;
import java.util.*;



public class ServerChauffeurBehaviour extends TickerBehaviour {

	private static final long serialVersionUID = -5461756253835984225L;
	private DriverAgent driverAgent;
	private boolean waitForAgree = false;
	private ArrayList<AID> expectedPassengers = new ArrayList<>();
	private ArrayList<AID> passengersForConfirm = new ArrayList<>();
    
	public ServerChauffeurBehaviour(Agent a, long period) {
		super(a, period);
	}


    private Set<DriverDescription> getBestPassengers(Set<DriverDescription> passengers) {
        Set<DriverDescription> bestPassengeres;
        int passengersCount = passengers.size();
        int limitSize = (passengersCount > 4) ? 4 : passengersCount;
        Set<Set<DriverDescription>> passengerCombinations = new 
        		SubsetGenerator<DriverDescription>().powerSet(passengers, limitSize);
        int maxProfit = Integer.MIN_VALUE;
        bestPassengeres = null;
        for (Set<DriverDescription> passengerCombination : passengerCombinations) {
            int size = passengerCombination.size();
            if (size > 0 && size <= 4) {
                int profit = getMaxProfit(passengerCombination);
                if (maxProfit < profit) {
                    maxProfit = profit;
                    bestPassengeres = passengerCombination;
                }
            }
        }
        return bestPassengeres;
    }


    private int getMaxProfit(Set<DriverDescription> cont) {
        //DriverAgent driverAgent = (DriverAgent) myAgent;
        Graph g = driverAgent.getMapGraph();

        Set<Integer> vert = new HashSet<>();
        Map<Integer, Boolean> end = new HashMap<Integer, Boolean>();
        for (DriverDescription dd : cont) {
            vert.add(dd.getTrip().getFrom());
        }

        int cur = 0;
        int sum = 0;
        int min = Integer.MAX_VALUE;
        for (Integer t : vert) {
            if (min > g.bfs(driverAgent.getDescription().getTrip().getFrom(), t)) {
                min = g.bfs(driverAgent.getDescription().getTrip().getFrom(), t);
                cur = t;
            }
        }

        vert.remove(cur);
        sum += min;
        end.put(cur, true);
        for (DriverDescription dd : cont) {
            if (dd.getTrip().getFrom() == cur)
                vert.add(dd.getTrip().getTo());
        }

        while (vert.size() > 1) {
            min = Integer.MAX_VALUE;
            int curcur = 0;
            for (Integer t : vert) {
                if (min > g.bfs(cur, t)) {
                    min = g.bfs(cur, t);
                    curcur = t;
                }
            }
            vert.remove(curcur);
            sum += min;
            cur = curcur;
            for (DriverDescription dd : cont) {
                if (dd.getTrip().getFrom() == cur)
                    vert.add(dd.getTrip().getTo());
            }
            end.put(cur, true);
        }
        sum += g.bfs(cur, driverAgent.getDescription().getTrip().getTo());

        int maxway = 0;
        for (DriverDescription dd : cont) {
            maxway += g.bfs(dd.getTrip().getFrom(), dd.getTrip().getTo());
        }
        maxway += g.bfs(driverAgent.getDescription().getTrip().getFrom(), driverAgent.getDescription().getTrip().getTo());

        //System.out.println(maxway - sum);
        return maxway - sum;
    }

    
    private void receiveRequestsFromPassengers() { 
    	MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = null; 
        double currentCost = 0;
        
        while (true) {
        	msg = myAgent.receive(mt);
        	if (msg == null)
        		break;
            AID driverAID = msg.getSender();
            
            boolean flag = driverAgent.addPotentialPassengerByName(driverAID);
            if (flag)
            	System.out.println(myAgent.getLocalName() + ": i(chauffeur) add new potential passenger - " +
            		msg.getSender().getLocalName());
        }
    }
    
    
    private void checkAndRemovePotentialPassengers() {
    	for(AID aid : driverAgent.goneDrivers) {
    		for(DriverDescription dd : driverAgent.getPotentialPassengers()) {
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
    	msg.setConversationId("bring-up");
    	
    	Set<DriverDescription> bestPassengers = getBestPassengers(driverAgent.getSetPotentialPassengers());
    	String info = myAgent.getLocalName() + ": my(chauffeur) potential passengers:\n";
    	for(DriverDescription dd : driverAgent.getSetPotentialPassengers()) {
    		info += "\t\t" + dd.getAid().getLocalName() + "\n";
    	}
    	System.out.println(info);
    	
    	for(DriverDescription dd : bestPassengers) {
    		msg.addReceiver(dd.getAid());
    		expectedPassengers.add(dd.getAid());
    		System.out.println(driverAgent.getLocalName() + ": i send accept to " + dd.getAid().getLocalName());
    	}
    	driverAgent.send(msg);
    	waitForAgree = true;
    }
    
    
    private void handleAgreeExpectedPassengers() {
    	if (!waitForAgree)
    		return;
    	
    	ACLMessage msg;
    	MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(
    			ACLMessage.AGREE), MessageTemplate.MatchConversationId("bring-up"));
    	while (true) {
         	msg = myAgent.receive(mt);
         	if (msg == null)
         		break;
         	
         	if (!expectedPassengers.contains(msg.getSender())) 
         		continue;
         	
         	System.out.println(driverAgent.getLocalName() + ": yeaah, this guy(" + 
 					msg.getSender().getLocalName() + ") agree to go with me");
         	expectedPassengers.remove(msg.getSender());
         	passengersForConfirm.add(msg.getSender());
         	if (expectedPassengers.isEmpty()) {
         		goneAndInformAll();
         		driverAgent.doDelete();
         		return;
         	}
    	}
         	
    }
    
    
    private void handleOtherBadNewsFromPassengers() {
    	ACLMessage msg;
    	MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchPerformative(
    			ACLMessage.REFUSE), MessageTemplate.MatchPerformative(ACLMessage.INFORM));
    	
    	while (true) {
         	msg = myAgent.receive(mt);
         	if (msg == null)
         		break;
         	
         	if (msg.getPerformative() == ACLMessage.INFORM && msg.getConversationId().equals("i'm gone")) {
         		driverAgent.goneDrivers.add(msg.getSender());
         	}
         	
         	boolean flag = driverAgent.removePotentialPassenger(msg.getSender());
         	if (flag)
         		System.out.println(driverAgent.getLocalName() + ": success remove potential passenger - " + 
         				msg.getSender().getLocalName());
         	else
         		System.out.println(driverAgent.getLocalName() + ": fail to remove potential passenger - " + 
         				msg.getSender().getLocalName());
         	
         	String info = myAgent.getLocalName() + ": my potential passengers after removing:\n";
         	for(DriverDescription dd : driverAgent.getSetPotentialPassengers())
         		info += "\t\t" + dd.getAid().getLocalName() + "\n";
         		
         	System.out.println(info);
         	
         	if (waitForAgree) {
         		if (expectedPassengers.contains(msg.getSender())) {
         			waitForAgree = false;
         			expectedPassengers.remove(msg.getSender());
         			passengersForConfirm.addAll(expectedPassengers);
         			expectedPassengers.clear();
         			
         			informAboutDisconfirm();
         			System.out.println(driverAgent.getLocalName() + ": this guy(" + 
         					msg.getSender().getLocalName() + ") don't agree go with me");
         			System.out.println(driverAgent.getLocalName() + ": go to begin");
         		}
         	}
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
    	ACLMessage msg = new ACLMessage(ACLMessage.DISCONFIRM);
    	msg.setConversationId("bring-up");
    	
    	for(AID dd : passengersForConfirm) {
    		msg.addReceiver(dd);
    		System.out.println(driverAgent.getLocalName() + ": disconfirm to " + dd.getLocalName());
    	}
    	driverAgent.send(msg);
    	passengersForConfirm.clear();
    }
    
    
    private void goneAndInformAll() {
    	ACLMessage msg = new ACLMessage(ACLMessage.CONFIRM);
    	msg.setConversationId("bring-up");
    	
    	String info = driverAgent.getLocalName() + ": i'm gone like chauffeur with:\n";
    	for(AID aid : passengersForConfirm) {
    		msg.addReceiver(aid);
    		info += "\t\t" + aid.getLocalName() + "\n";
    	}
    	
    	driverAgent.send(msg);
    	System.out.println(info);
    	
    	ACLMessage msgInfo = new ACLMessage(ACLMessage.INFORM);
    	msgInfo.setConversationId("i'm gone");
    	for(DriverDescription dd : driverAgent.getDrivers()) {
    		if (!passengersForConfirm.contains(dd.getAid()) && !dd.getAid().getLocalName().equals(myAgent.getAID().getLocalName())) {
    			msgInfo.addReceiver(dd.getAid());
    			System.out.println(myAgent.getLocalName() + ": i(chauffeur) notificate that i gone " + dd.getAid().getLocalName());
    		}
    	}
    	driverAgent.send(msgInfo);
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
