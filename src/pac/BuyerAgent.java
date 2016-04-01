package pac;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BuyerAgent extends Agent {

		
	/**
	 * 
	 */
	private static final long serialVersionUID = -7107932459085960207L;
	
	private String Target;
	private AID[] Saler_Agents;
	private Integer Quantity,Time;
	int Times = 0;
	public void setup() {
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			Target = (String) args[0];
			Quantity = Integer.parseInt((String) args[1]);
			Time = Integer.parseInt((String) args[2]);
			addBehaviour(new TickerBehaviour(this, 5000) {
				protected void onTick() {
					System.out.println(getLocalName() + " going to buy " + Target);
					// Update the list of Saler agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType(Target + "-selling");
					template.addServices(sd);
					Times++;
					if (Times == 5){
						System.out.println(myAgent.getLocalName()+": I will buy "+ Target + " from another service!!!");
						doDelete();
					}
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Found the following Saler agents:");
						Saler_Agents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							Saler_Agents[i] = result[i].getName();
							System.out.println(Saler_Agents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}
		else{
			doDelete();
		}
	}
	private class RequestPerformer extends Behaviour {
		private AID Best_Saler; // The agent who provides the best offer 
		private int Best_Price;  // The best offered price
		private int Replies_Count = 0; // The counter of replies from Saler agents
		private MessageTemplate mt; // The template to receive replies
		private int Step = 0;

			public void action() {
				switch (Step) {
				case 0:
					// Send the cfp to all Salers
					ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
					for (int i = 0; i < Saler_Agents.length; ++i) {
						cfp.addReceiver(Saler_Agents[i]);
					} 
					cfp.setContent(Target+ " " + String.valueOf(Quantity.intValue())+" "+ String.valueOf(Time.intValue()));
					cfp.setConversationId(Target + "-trade");
					cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
					myAgent.send(cfp);
					// Prepare the template to get proposals
					mt = MessageTemplate.and(MessageTemplate.MatchConversationId(Target + "-trade"),
							MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
					Step = 1;
					break;
				case 1:
					// Receive all proposals/refusals from Saler agents
					ACLMessage reply = myAgent.receive(mt);
					if (reply != null) {
						 
						// Reply received
						if (reply.getPerformative() == ACLMessage.PROPOSE) {
							// This is an offer 
							int price = Integer.parseInt(reply.getContent());
							if (Best_Saler == null || price < Best_Price) {
								// This is the best offer at present
								Best_Price = price;
								Best_Saler = reply.getSender();
							}
						}
						Replies_Count++;
						if (Replies_Count >= Saler_Agents.length) {
							// We received all replies
							Step = 2; 
						}
					}
					else {
						block();
					}
					break;
				case 2:
					// Send the purchase order to the saler that provided the best offer
					ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
					order.addReceiver(Best_Saler);
					System.out.println(" – " + myAgent.getLocalName()+ ": Try to buy "+ Target + " from " + Best_Saler.getName());
					order.setContent(Target+ " " + String.valueOf(Quantity.intValue())+" "+ String.valueOf(Time.intValue()));
					order.setConversationId(Target + "-trade");
					order.setReplyWith("order"+System.currentTimeMillis());
					myAgent.send(order);
					// Prepare the template to get the purchase order reply
					Step = 3;
					break;
				case 3:      
					// Receive the purchase order reply
					reply = myAgent.receive();
					if (reply != null) {
						// Purchase order reply received
						if (reply.getPerformative() == ACLMessage.INFORM) {
							// Purchase successful. We can terminate
;
							System.out.println(myAgent.getName()+": "+String.valueOf(Quantity.intValue())+" "+Target+" successfully purchased from agent "+reply.getSender().getName());
							System.out.println("Price = "+Best_Price*Quantity);
							myAgent.doDelete();
						}
						else {
							System.out.println(myAgent.getLocalName() + ": I can't wait anymore");
							Time = 0;
						}
	
						Step = 4;
					}
					else {
						block();
					}
					break;
				}        
			}
	
			public boolean done() {
				if (Step == 2 && Best_Saler == null) {
					System.out.println("Attempt failed: "+Target+" not available for sale");
				}
				return ((Step == 2 && Best_Saler == null) || Step == 4);
			}
	}
		
}
