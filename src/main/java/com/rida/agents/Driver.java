/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.rida.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.io.*;
import java.util.*;

/**
 * @author daine
 */
public class Driver extends Agent {

    private static final long serialVersionUID = -2126376222227043630L;

    int n;
    int[][] graph;
    int from, to;
    String name;
    //    private DriverDescription[] drivers;
    private HashSet<DriverDescription> drivers;
    int amountTicks = 0;
    final static int TICK_LIMIT = 2;
    boolean profitCalculated = false;
    boolean proposeSended = false, proposeRecieved = false;
    private int repliesCount = 0; // The counter of replies from Saler agents
    private MessageTemplate mt; // The template to receive repli
    private int sendCount = 0;
    private int rejectCount = 0;
    private int isDriver = 0;
    private int summaryProfit = 0;
    private ArrayList<DriverDescription> potentialPassengers;
    private List<DriverDescription> passengers;
    private ArrayList<Trip> trips;

    public int bfs(int x, int y) {
        // BFS uses Queue data structure
        Queue<Integer> qx = new LinkedList<Integer>();
        int[] res = new int[n];
        for (int i = 0; i < n; i++)
            res[i] = -1;
        qx.add(x);
        res[x] = 0;
        while (!qx.isEmpty()) {
            int cx = qx.remove();
            for (int i = 0; i < n; i++) {
                if (graph[cx][i] != 0 && (res[i] == -1 || res[i] > res[cx] + graph[cx][i])) {
                    qx.add(i);
                    res[i] = res[cx] + graph[cx][i];
                }
            }
        }
        return res[y];
    }

    private void setupGraph() {
        File f = new File("input.txt");
        BufferedReader fin;
        try {
            fin = new BufferedReader(new FileReader(f));
            String[] size = fin.readLine().split(" ");
            n = Integer.parseInt(size[0]);
            graph = new int[n][n];
            for (int i = 0; i < n; i++) {
                String[] matr = fin.readLine().split(" ");
                for (int j = 0; j < matr.length; j++) {
                    graph[i][j] = Integer.parseInt(matr[j]);
                }
            }
            fin.close();
        } catch (Exception e) {
            doDelete();
        }
    }


    private int getReverseProfitByDriver(String driverName) {
        for (DriverDescription dd : drivers) {
            if (driverName.equals(dd.name)) {
                return dd.reverseProfit;
            }
        }

        throw new IllegalStateException("Driver not found");
    }


    private DriverDescription getDriverDescriptionByName(String name) {
        for (DriverDescription dd : drivers) {
            if (dd.name.equals(name)) {
                return dd;
            }
        }

        throw new IllegalStateException("Not found driver");
    }


    private int getMaxProfitByDriver() {
        int max = -1000000000;
        for (DriverDescription dd : drivers) {
            if (dd.profit > max) {
                max = dd.profit;
            }
        }
        return max;
    }


    private void recalcProfit() {
        Collections.sort(potentialPassengers, Collections.reverseOrder());/*potentialPassegengers.stream().sorted(Comparator.comparing(DriverDescription::getReverseProfit).reversed()).collect(Collectors.toList());*/
//        for (int i = 0; i < Math.min(4, passengers.size()); i++)
//            summaryProfit += getReverseProfitByDriver(passengers.get(i).name);
//
//        System.out.println("summaryProfit vs maxProfit: " + summaryProfit + " " + getMaxProfitByDriver());
//        return (summaryProfit > getMaxProfitByDriver());
    }


    private void calculateProfit() {

        for (DriverDescription descr : drivers) {
            descr.calcWayLength();
            descr.calcProfit(from, to);
            //System.out.println(descr.name + " " + descr.profit + " " + descr.reverseProfit + " " + descr.value);
        }
    }


    static private String parseName(DFAgentDescription stupidName) {
        return stupidName.getName().toString().split(" ")[3].split("@")[0];
    }


    public void setup() {
        setupGraph();
        Object[] args = getArguments();
        from = Integer.parseInt((String) args[0]);
        to = Integer.parseInt((String) args[1]);
        drivers = new HashSet<DriverDescription>();

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("bring-up");
        sd.setName(String.valueOf(from) + " " + String.valueOf(to));
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new TickerBehaviour(this, 500) {

            @Override
            protected void onTick() {
                if (amountTicks >= TICK_LIMIT) {
                    if (!profitCalculated) {
                        calculateProfit();
                        profitCalculated = true;
                    }
                    if (!proposeSended) {
                        myAgent.addBehaviour(new RequestPerformer());
                        proposeSended = true;
                        return;
                    }
                    if (proposeSended) {
                        if (sendCount == 0) {
                            isDriver = 1;
                        } else {
                            myAgent.addBehaviour(new ResponseReceiver());
                        }
                    }
                    if (isDriver == 1) {
                        myAgent.addBehaviour(new DriverBehaviour());

                        //proposeRecieved = true;
                    }
                    if (isDriver == -1) {
                        myAgent.addBehaviour(new PassengerBehaviour());
                    }
                    return;
                }
                amountTicks++;

                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("bring-up");
                template.addServices(sd);

                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);

                    for (int i = 0; i < result.length; i++) {

                        boolean flag = myAgent.getName().equals(result[i].getName().toString().split(" ")[3]);
                        DriverDescription temp = new DriverDescription();
                        temp.name = parseName(result[i]);
                        temp.value = result[i].getName();
                        if (!flag) {
                            Iterator<ServiceDescription> iter = result[i].getAllServices();
                            while (true) {
                                if (!iter.hasNext())
                                    break;
                                ServiceDescription s = iter.next();
                                temp.way = s.getName();
                                // System.out.println(temp);
                            }
                            drivers.add(temp);
                        }
                    }
                } catch (FIPAException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private class ResponseReceiver extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt_accept = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                    MessageTemplate.MatchConversationId("bring-up"));
            ACLMessage msg_accept = myAgent.receive(mt_accept);
            if (msg_accept != null) {
//                if (trips == null) {
//                    trips = new ArrayList<>();
//                }
//
//                trips.add(new Trip(msg_accept.getSender(), Integer.parseInt(msg_accept.getContent())));
                isDriver = -1;
                System.out.println(myAgent.getName() + " passenger now!");
            }

            MessageTemplate mt_reject = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
                    MessageTemplate.MatchConversationId("bring-up"));
            ACLMessage msg_reject = myAgent.receive(mt_reject);
            if (msg_reject != null) {
                rejectCount++;
                if (rejectCount == sendCount) {
                    isDriver = 1;
                    System.out.println(myAgent.getName() + " driver now!");
                    //myAgent.addBehaviour(new DriverBehaviour());
                }
            }
        }
    }


    private class RequestPerformer extends OneShotBehaviour {
        @Override
        public void action() {
            ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
            for (DriverDescription driver : drivers) {
                if (driver.profit > 0 && driver.reverseProfit <= driver.profit) {
                    cfp.addReceiver(driver.value);
                    sendCount++;
                }
            }
            cfp.setContent(myAgent.getName());
            cfp.setConversationId("bring-up");
            cfp.setReplyWith("cfp" + System.currentTimeMillis()); // Unique value
            myAgent.send(cfp);
            // Prepare the template to get proposals
            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("bring-up"),
                    MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
        }
    }


    private class DriverBehaviour extends OneShotBehaviour {
        @Override
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("bring-up"));
            ACLMessage msg = myAgent.receive(mt);
            potentialPassengers = new ArrayList<DriverDescription>();
            while (msg != null) {
                //System.out.println(myAgent.getName() + " recieved : " + msg.getContent());
                String driverName = msg.getContent().split("@")[0];
                potentialPassengers.add(getDriverDescriptionByName(driverName));
                msg = myAgent.receive(mt);
            }
            recalcProfit();

            System.out.println(myAgent.getName() + " is Driver now!");

            msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            for (int i = 0; i < Math.min(4, passengers.size()); i++) {
                msg.addReceiver(passengers.get(i).value);
            }
            msg.setContent("0");
            msg.setConversationId("bring-up");
            //msg.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
            myAgent.send(msg);

            msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            for (int i = Math.min(4, passengers.size()); i < passengers.size(); i++) {
                msg.addReceiver(passengers.get(i).value);
            }
            msg.setContent("0");
            msg.setConversationId("bring-up");
            //msg.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
            myAgent.send(msg);
            doDelete();
//                    passengers = new ArrayList<>(potentialPassengers);
//                    potentialPassengers = null;

//                    System.out.println("passengers:");
//                    passengers.stream().forEach((dd) -> {
//                        System.out.println("\t" + dd.name + " " + dd.reverseProfit);
//                    });
//
//
//
//                    System.out.println("after sort passengers:");
//                    collect.stream().forEach((dd) -> {
//                        System.out.println("\t" + dd.name + " " + dd.reverseProfit);
//                    });

        }
    }

    private class PassengerBehaviour extends OneShotBehaviour {
        @Override
        public void action() {

            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("bring-up"));
            ACLMessage msg = myAgent.receive(mt);
            potentialPassengers = new ArrayList<DriverDescription>();
            while (msg != null) {
                //System.out.println(myAgent.getName() + " recieved : " + msg.getContent());
                String driverName = msg.getContent().split("@")[0];
                potentialPassengers.add(getDriverDescriptionByName(driverName));
                msg = myAgent.receive(mt);
            }
            recalcProfit();

            //System.out.println(myAgent.getName() + " is Driver now!");

            msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            for (int i = 0; i < passengers.size(); i++) {
                msg.addReceiver(passengers.get(i).value);
            }
            msg.setContent("0");
            msg.setConversationId("bring-up");
            //msg.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
            myAgent.send(msg);
            doDelete();
//                    passengers = new ArrayList<>(potentialPassengers);
//                    potentialPassengers = null;

//                    System.out.println("passengers:");
//                    passengers.stream().forEach((dd) -> {
//                        System.out.println("\t" + dd.name + " " + dd.reverseProfit);
//                    });
//
//
//
//                    System.out.println("after sort passengers:");
//                    collect.stream().forEach((dd) -> {
//                        System.out.println("\t" + dd.name + " " + dd.reverseProfit);
//                    });

        }

        private void sendResponse(ACLMessage msg) {

        }
    }


//    private class DriverBehaviour extends Behaviour {
//
//        @Override
//        public void action() {
//
//        }
//
//        @Override
//        public boolean done() {
//            return true;
//        }
//
//    }


    private class DriverDescription {
        public String way;
        public String name;
        public int wayLength;
        public int profit;
        public int reverseProfit;
        public AID value;


        public String toString() {
            return (name.toString() + " " + way);
        }

        public void calcWayLength() {
            wayLength = bfs(Integer.parseInt(way.split(" ")[0]), Integer.parseInt(way.split(" ")[1]));
        }

        public void calcProfit(int from, int to) {
            int myFrom = Integer.parseInt(way.split(" ")[0]);
            int myTo = Integer.parseInt(way.split(" ")[1]);

            reverseProfit = bfs(myFrom, myTo) + bfs(from, to) -
                    (bfs(from, myFrom) + bfs(myFrom, myTo) + bfs(myTo, to));

            profit = bfs(myFrom, myTo) + bfs(from, to) -
                    (bfs(myFrom, from) + bfs(from, to) + bfs(to, myTo));
        }


        @Override
        public boolean equals(Object another) {
            if (another.getClass() != this.getClass()) {
                return false;
            }

            DriverDescription other = (DriverDescription) another;
            if (this.name.equals(other.name) && this.way.equals(other.way)) {
                return true;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode() * this.way.hashCode();
        }

        public int getReverseProfit() {
            return reverseProfit;
        }
    }


    private class Trip {

        Trip(AID _driver, int _cost) {
            driver = _driver;
            cost = _cost;
        }

        public AID driver;
        int cost;
    }
}
