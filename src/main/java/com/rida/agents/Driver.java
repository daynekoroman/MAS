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
import jade.util.leap.Comparable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

public class Driver extends Agent {

    private static final Logger LOG = LoggerFactory.getLogger(Driver.class);

    private static final long serialVersionUID = -2126376222227043630L;

    private int n;
    private int[][] graph;
    private int from, to;
    private HashSet<DriverDescription> drivers;
    private int amountTicks = 0;
    private final static int TICK_LIMIT = 2;
    private boolean profitCalculated = false;
    private boolean proposeSended = false;
    private MessageTemplate mt;
    private int sendCount = 0;
    private int rejectCount = 0;
    private int isDriver = 0;
    private ArrayList<DriverDescription> potentialPassengers;
    private List<DriverDescription> passengers;

    public int bfs(int x, int y) {
        // BFS uses Queue data structure
        Queue<Integer> qx = new LinkedList<>();
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

    private void setupGraph() throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("graph.txt");
        File file;
        if (fileUrl != null) {
            file = new File(fileUrl.getFile());
        } else {
            file = null;
        }

        BufferedReader fin;
        if (file != null) {
            try {
                fin = new BufferedReader(new FileReader(file));
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
            } catch (IOException e) {
                doDelete();
                e.printStackTrace();
            }
        }

    }


    private DriverDescription getDriverDescriptionByName(String name) {
        for (DriverDescription dd : drivers) {
            if (dd.name.equals(name)) {
                return dd;
            }
        }

        throw new IllegalStateException("Not found driver");
    }


    private void recalcProfit() {
        Collections.sort(potentialPassengers, Collections.reverseOrder());
        passengers = potentialPassengers;
    }


    private void calculateProfit() {
        for (DriverDescription descr : drivers) {
            descr.calcWayLength();
            descr.calcProfit(from, to);
        }
    }


    static private String parseName(DFAgentDescription stupidName) {
        return stupidName.getName().toString().split(" ")[3].split("@")[0];
    }


    public void setup() {
        try {
            setupGraph();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Object[] args = getArguments();
        from = Integer.parseInt((String) args[0]);
        to = Integer.parseInt((String) args[1]);
        drivers = new HashSet<>();

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
                    if (sendCount == 0) {
                        isDriver = 1;
                    } else {
                        myAgent.addBehaviour(new ResponseReceiver());
                    }
                    if (isDriver == 1) {
                        myAgent.addBehaviour(new DriverBehaviour());
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

                    for (DFAgentDescription aResult : result) {

                        boolean flag = myAgent.getName().equals(aResult.getName().toString().split(" ")[3]);
                        DriverDescription temp = new DriverDescription();
                        temp.name = parseName(aResult);
                        temp.value = aResult.getName();
                        if (!flag) {
                            Iterator<ServiceDescription> iter = aResult.getAllServices();
                            while (true) {
                                if (!iter.hasNext())
                                    break;
                                ServiceDescription s = iter.next();
                                temp.way = s.getName();
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
                isDriver = -1;
                LOG.info("{} I'm passenger now!", new Date());
            }

            MessageTemplate mt_reject = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),
                    MessageTemplate.MatchConversationId("bring-up"));
            ACLMessage msg_reject = myAgent.receive(mt_reject);
            if (msg_reject != null) {
                rejectCount++;
                if (rejectCount == sendCount) {
                    isDriver = 1;
                    LOG.info("{} I'm driver now!", new Date());
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
            potentialPassengers = new ArrayList<>();
            while (msg != null) {
                String driverName = msg.getContent().split("@")[0];
                potentialPassengers.add(getDriverDescriptionByName(driverName));
                msg = myAgent.receive(mt);
            }
            recalcProfit();

            LOG.info("{} I'm driver now!", new Date());

            msg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
            for (int i = 0; i < Math.min(4, passengers.size()); i++) {
                msg.addReceiver(passengers.get(i).value);
            }
            msg.setContent("0");
            msg.setConversationId("bring-up");
            myAgent.send(msg);

            msg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
            for (int i = Math.min(4, passengers.size()); i < passengers.size(); i++) {
                msg.addReceiver(passengers.get(i).value);
            }
            msg.setContent("0");
            msg.setConversationId("bring-up");
            myAgent.send(msg);
            doDelete();
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

    private class DriverDescription implements Comparable {
        String way;
        String name;
        int wayLength;
        int profit;
        int reverseProfit;
        AID value;


        public String toString() {
            return (name.toString() + " " + way);
        }

        void calcWayLength() {
            wayLength = bfs(Integer.parseInt(way.split(" ")[0]), Integer.parseInt(way.split(" ")[1]));
        }

        void calcProfit(int from, int to) {
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
            return this.name.equals(other.name) && this.way.equals(other.way);

        }

        @Override
        public int hashCode() {
            return this.name.hashCode() * this.way.hashCode();
        }

        public int getReverseProfit() {
            return reverseProfit;
        }

        @Override
        public int compareTo(Object o) {
            DriverDescription compDriverDescription = (DriverDescription) o;
            if (reverseProfit == compDriverDescription.reverseProfit) return 0;
            if (reverseProfit > compDriverDescription.reverseProfit) {
                return 1;
            } else {
                return -1;
            }
        }
    }


    private class Trip {

        Trip(AID _driver, int _cost) {
            driver = _driver;
            cost = _cost;
        }

        AID driver;
        int cost;
    }
}
