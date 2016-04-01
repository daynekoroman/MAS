package pac;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;



public class SalerAgent extends Agent {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2126376279427043630L;
	private String Target;	
	private Integer Price,Whole_Sale_Price,wsq,Queue_Lenght,Sum_Quantity_Needsities_Queue;
	private AID[] queue, temp;
	private Integer [] Quantity_Needs, Temp_Quantity_Needsity, Agents_Time, Temp_Agents_Time;
	public void setup() {
		Object[] args = getArguments();
		if (args != null && args.length > 3) {
			Target = (String) args[0];
			Price = Integer.parseInt((String) args[1]);
			Whole_Sale_Price = Integer.parseInt((String) args[2]);
			wsq = Integer.parseInt((String) args[3]);
			queue = new AID [1];
			Quantity_Needs = new Integer [1];
			Sum_Quantity_Needsities_Queue = Integer.valueOf(0);
			Queue_Lenght = Integer.valueOf(0);
			Agents_Time = new Integer [1];
			System.out.println(getLocalName()+" is ready to sell" + Target);
		}else{
			doDelete();
		}
		
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType(Target + "-selling");
		sd.setName("JADE" + Target + "-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		addBehaviour(new OfferRequestsServer());
		addBehaviour(new PurchaseOrdersServer());
		addBehaviour(new TickerBehaviour(this, 10) {
			protected void onTick() {
				int i;
				for (i = 0; i < Queue_Lenght.intValue(); i ++){
					Agents_Time[i] = Integer.valueOf(Agents_Time[i].intValue()- 10);
					if (Agents_Time[i].intValue() <= 0){
						DeleteFromQueue(queue[i]);
					}
				}
				
			}
		} );
			//catalogue.put(Target, Integer.parseInt(Price1));
			/*addBehaviour(new CyclicBehaviour(this) {
				public void action() {
					ACLMessage MSG = receive();
					if (MSG != null) {
						// Вывод на экран локального имени агента и полученного
						// сообщения	
						if (MSG.getContent().equals((String)"what you sale")){
							ACLMessage reply = MSG.createReply();
		                    reply.setPerformative(ACLMessage.INFORM); // устанавливаем перформатив сообщения
		                    reply.setContent((String)Target); // содержимое сообщения
		                    send(reply);
						}
						System.out.println(" – " + myAgent.getLocalName() + " received: " + MSG.getContent());
					} 
					// Блокируем поведение, пока в очереди сообщений агента
					// не появится хотя бы одно сообщение
					block();
				}
			});*/
		/*}
		else{
			doDelete();
		}*/
	}

	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	
	public void DeleteFromQueue(AID a){
		int i;
		temp = new AID [Queue_Lenght.intValue() - 1];
		Temp_Quantity_Needsity = new Integer[Queue_Lenght.intValue() - 1];
		Temp_Agents_Time = new Integer[Queue_Lenght.intValue() - 1];
		int j = 0;
		for (i = 0; i < Queue_Lenght.intValue(); i++){
			if (a.getName().equals(queue[i].getName())){
				Sum_Quantity_Needsities_Queue = Integer.valueOf(Sum_Quantity_Needsities_Queue.intValue() - Quantity_Needs[i].intValue());
				ACLMessage MSG = new ACLMessage(ACLMessage.FAILURE);
				System.out.println(getName()+": Removing "+queue[i].getName()+" from wholesale queue");
				MSG.addReceiver(a);
				MSG.setConversationId(Target + "-trade");
				MSG.setContent("just not-available");
				MSG.setReplyWith("order"+System.currentTimeMillis());
				send(MSG);
			}
			else{
				temp[j] = queue[i];
				Temp_Quantity_Needsity[j] = Integer.valueOf(Quantity_Needs[j].intValue());
				Temp_Agents_Time[j] = Integer.valueOf(Agents_Time[j].intValue());
				j++;
			}
		}
		queue = temp;
		Quantity_Needs = Temp_Quantity_Needsity;
		Agents_Time = Temp_Agents_Time;
		Queue_Lenght = Integer.valueOf(Queue_Lenght.intValue() - 1);
	}
	
	private class OfferRequestsServer extends CyclicBehaviour {
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage MSG = myAgent.receive(mt);
			if (MSG != null) {
				//System.out.println(" – " + myAgent.getLocalName() + " received: " + MSG.getContent());
				// CFP Message received. Process it
				String str = MSG.getContent();
				String Title;
				String[] Splitedstr = str.split(" ");
				Title = Splitedstr[0];
				Integer Quantity_Needsity = Integer.parseInt(Splitedstr[1]);
				Integer time = Integer.parseInt(Splitedstr[2]); 
				ACLMessage reply = MSG.createReply();
				Integer price;
				if (Quantity_Needsity.intValue() >= wsq.intValue()){
					price = Whole_Sale_Price;
					
				}
				else{
					if (time.intValue() == 0){
						price = Price;
					}
					else{
						price = Whole_Sale_Price;
					}
				}
				//if (price != null) {
					// The requested book is available for sale. Reply with the price
				reply.setPerformative(ACLMessage.PROPOSE);
				reply.setContent(String.valueOf(price.intValue()));
				/*}
				else {
					// The requested book is NOT available for sale.
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("not-available");
				}*/
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	} 
	private class PurchaseOrdersServer extends CyclicBehaviour {
		
		public void AddToQueue(AID a){
			int i;
			Temp_Quantity_Needsity = new Integer[Queue_Lenght.intValue() + 1];
			Temp_Agents_Time = new Integer[Queue_Lenght.intValue() + 1];
			temp = new AID [Queue_Lenght.intValue() + 1];
			for (i = 0; i < Queue_Lenght.intValue(); i++){
				temp[i] = queue[i];
				Temp_Quantity_Needsity[i] = Integer.valueOf(Quantity_Needs[i].intValue());
				Temp_Agents_Time[i] = Integer.valueOf(Agents_Time[i].intValue());
			}
			temp[Queue_Lenght.intValue()] = a;
			queue = temp;
			Quantity_Needs = Temp_Quantity_Needsity;
			Agents_Time = Temp_Agents_Time;
			Queue_Lenght = Integer.valueOf(Queue_Lenght.intValue() + 1);
		}
		
		public boolean QueueAreFull(){
			if (Sum_Quantity_Needsities_Queue.intValue() >= wsq.intValue()){
				return true;
			}
			else{
				return false;
			}
		}
		
		void EraseQueue(){
			int i;
			System.out.println(myAgent.getLocalName()+": Wholesale");
			ACLMessage MSG = new ACLMessage(ACLMessage.INFORM);
			MSG.setPerformative(ACLMessage.INFORM);
			for (i = 0; i < Queue_Lenght.intValue(); i++){
				Integer price = Whole_Sale_Price;
				MSG.addReceiver(queue[i]);
				System.out.println(String.valueOf(Quantity_Needs[i].intValue())+ " " +Target+" sold to agent "+queue[i].getName());
			}
			myAgent.send(MSG);
			queue = new AID [1];
			Queue_Lenght = Integer.valueOf(0);
			Sum_Quantity_Needsities_Queue = Integer.valueOf(0);
		}
	
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage MSG = myAgent.receive(mt);
			if (MSG != null) {
				//System.out.println(" – " + myAgent.getLocalName() + " received: " + MSG.getContent());
				// ACCEPT_PROPOSAL Message received. Process it
				String str = MSG.getContent();
				String Title;
				String[] Splitedstr = str.split(" ");
				Title = Splitedstr[0];
				Integer Quantity_Needsity = Integer.parseInt(Splitedstr[1]);
				Integer time = Integer.parseInt(Splitedstr[2]); 
				ACLMessage reply = MSG.createReply();
				if (time.intValue() > 0){
					AddToQueue(MSG.getSender());
					Agents_Time[Queue_Lenght.intValue() - 1] = Integer.valueOf(time.intValue());
					Quantity_Needs[Queue_Lenght.intValue() - 1] = Integer.valueOf(Quantity_Needsity.intValue());
					Sum_Quantity_Needsities_Queue = Integer.valueOf(Sum_Quantity_Needsities_Queue.intValue() + Quantity_Needsity.intValue());
					if (QueueAreFull()){
						EraseQueue();
					}
				}
				else{
					if (Quantity_Needsity.intValue() >= wsq.intValue()){
						System.out.println(myAgent.getLocalName()+": Retail");
						Integer price = Whole_Sale_Price;
						reply.setPerformative(ACLMessage.INFORM);
						System.out.println(String.valueOf(Quantity_Needsity.intValue())+ " " +Title+" sold to agent "+MSG.getSender().getName());
						myAgent.send(reply);
					}
					else{
						Integer price = Price;
						reply.setPerformative(ACLMessage.INFORM);
						System.out.println(String.valueOf(Quantity_Needsity.intValue())+ " " +Title+" sold to agent "+MSG.getSender().getName());
						myAgent.send(reply);
					}
				}
				//if (price != null) {
				/*}
				else {
					// just sale.
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("not-available");
				}*/
			}
			else {
				block();
			}
		}
	}
}
