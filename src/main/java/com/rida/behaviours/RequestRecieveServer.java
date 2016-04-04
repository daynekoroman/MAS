package com.rida.behaviours;

import com.rida.agents.DriverAgent;
import com.rida.tools.DriverDescription;
import com.rida.tools.Graph;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by daine on 03.04.2016.
 */
public class RequestRecieveServer extends CyclicBehaviour {
    private DriverAgent driverAgent;
    private int amountPotentialPassengers = -1;
    private int countReceivedMsg = 0;

    private static final Logger LOG = LoggerFactory.getLogger(RequestRecieveServer.class);


    private int getMaxProfit(Set<DriverDescription> cont) {
        DriverAgent driverAgent = (DriverAgent) myAgent;
        Graph g = driverAgent.getMapGraph();

        Set<Integer> vert = new HashSet<>();
        Map<Integer, Boolean> end = new HashMap<Integer, Boolean>();
        for (DriverDescription dd : cont) {
            vert.add(dd.getFrom());
        }

        int cur = 0;
        int sum = 0;
        int min = Integer.MAX_VALUE;
        for (Integer t : vert) {
            if (min > g.bfs(driverAgent.getFrom(), t)) {
                min = g.bfs(driverAgent.getFrom(), t);
                cur = t;
            }
        }

        vert.remove(cur);
        sum += min;
        end.put(cur, true);
        for (DriverDescription dd : cont) {
            if (dd.getFrom() == cur)
                vert.add(dd.getTo());
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
                if (dd.getFrom() == cur)
                    vert.add(dd.getTo());
            }
            end.put(cur, true);
        }
        sum += g.bfs(cur, driverAgent.getTo());

        int maxway = 0;
        for (DriverDescription dd : cont) {
            maxway += g.bfs(dd.getFrom(), dd.getTo());
        }
        maxway += g.bfs(driverAgent.getFrom(), driverAgent.getTo());

        //System.out.println(maxway - sum);
        return maxway - sum;
    }

    private int calcMaxPassengersProfit(Set<DriverDescription> best, Set<DriverDescription> q, Set<DriverDescription> inq) {
        DriverAgent driverAgent = (DriverAgent) myAgent;
        Set<DriverDescription> result = new HashSet<>();
        Graph g = driverAgent.getMapGraph();

        int max = Integer.MIN_VALUE;
        for (DriverDescription p1 : q) {
            inq.add(p1);
            int temp_res1 = getMaxProfit(inq);
            if (max < temp_res1) {
                max = temp_res1;
                result = new HashSet<>(inq);
            }
            if (inq.size() < 4) {
                Set<DriverDescription> temp_result2 = new HashSet<>();
                Set<DriverDescription> qq = new HashSet<>(q);
                qq.remove(p1);
                int temp_res2 = calcMaxPassengersProfit(temp_result2, qq, inq);
                if (max < temp_res2) {
                    max = temp_res2;
                    result = new HashSet<>(temp_result2);
                }
                qq.add(p1);
            }
            inq.remove(p1);
        }
        for (DriverDescription dd : result)
            best.add(dd);
        //best = new HashSet<>(result);

        return max;
    }


    @Override
    public void action() {
        DriverAgent driverAgent = (DriverAgent) myAgent;

        if (amountPotentialPassengers < 0) {
            amountPotentialPassengers = driverAgent.calcAmountPotentialPassengers();
        }

        if (countReceivedMsg >= amountPotentialPassengers) {
            return;
        }

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                MessageTemplate.MatchConversationId("bring-up"));
        ACLMessage msg = myAgent.receive(mt);

        if (msg != null) {
            countReceivedMsg++;
            String driverName = msg.getContent().split("@")[0];
            driverAgent.addPassenger(driverAgent.getDriverDescriptionByName(driverName));
        }

//        if (countReceivedMsg == amountPotentialPassengers) {
            Set<DriverDescription> result = new HashSet<>();

        Set<DriverDescription> q = new HashSet<>(driverAgent.getPassengers());
//
//            for (DriverDescription dd : driverAgent.getPassengers())
//                q.add(new DriverDescription(dd));

            Set<DriverDescription> inq = new HashSet<>();
            calcMaxPassengersProfit(result, q, inq);
            // if (driverAgent.getMapGraph().bfs(driverAgent.getFrom(), driverAgent.getTo()) < getMaxProfit(result)){
        String s = "";
            for (DriverDescription dd : result)
                s += "\r\n\t\t" + dd.toString();

        LOG.info("{}", s);


        LOG.info("all messages from potential passengers received");
//        }
    }
}
