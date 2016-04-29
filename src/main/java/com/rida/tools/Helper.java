package com.rida.tools;

import com.rida.agents.DriverAgent;
import jade.core.AID;

import java.util.HashSet;
import java.util.Set;

/**
 * Различные вычисления
 * Created by shmagrinsky on 09.04.16.
 */
public class Helper {
    private Helper() {
        throw new AssertionError();
    }

    /**
     * Расчитать длину пути
     */
    public static int calcProfit(Trip trip1, Trip trip2, Graph graph) {
        int from1 = trip1.getFrom();
        int to1 = trip1.getTo();
        int from2 = trip2.getFrom();
        int to2 = trip2.getTo();
        return graph.bfs(from1, to1) + graph.bfs(from2, to2) -
                (graph.bfs(from1, from2) + graph.bfs(from2, to2) + graph.bfs(to2, to1));
    }


    public static Set<DriverDescription> getBestPassengers(DriverAgent driverAgent, Set<DriverDescription> passengers) {
        Set<DriverDescription> bestPassengers = null;
        int passengersCount = passengers.size();
        int limitSize = (passengersCount > 4) ? 4 : passengersCount;
        Set<Set<DriverDescription>> passengerCombinations = new
                SubsetGenerator<DriverDescription>().generateSubSets(passengers, limitSize);
        double maxProfit = Double.NEGATIVE_INFINITY;

        for (Set<DriverDescription> passengerCombination : passengerCombinations) {
            int size = passengerCombination.size();
            if (size > 0 && size <= 4) {
                double profit = Helper.calcBestSetProfit(passengerCombination, driverAgent.getMapGraph(),
                        driverAgent.getDescription().getTrip());
                if (maxProfit < profit) {
                    maxProfit = profit;
                    bestPassengers = passengerCombination;
                }
            }
        }
        return bestPassengers;
    }

    public static double calcBestSetProfit(Set<DriverDescription> cont, Graph g, Trip driverTrip) {

        Set<Integer> vert = new HashSet<>();
        for (DriverDescription dd : cont) {
            vert.add(dd.getTrip().getFrom());
        }

        int currentVertex = nearestVertex(g, driverTrip.getFrom(), vert);
        int sum = 0;
        int min = g.bfs(driverTrip.getFrom(), currentVertex);
        vert.remove(currentVertex);
        sum += min;
        for (DriverDescription dd : cont) {
            Trip trip = dd.getTrip();
            if (trip.getFrom() == currentVertex)
                vert.add(trip.getTo());
        }

        while (vert.size() > 1) {
            int nearestVertex = nearestVertex(g, currentVertex, vert);
            min = g.bfs(currentVertex, nearestVertex);
            vert.remove(nearestVertex);
            sum += min;
            currentVertex = nearestVertex;
            for (DriverDescription dd : cont) {
                if (dd.getTrip().getFrom() == currentVertex)
                    vert.add(dd.getTrip().getTo());
            }
        }
        sum += g.bfs(currentVertex, driverTrip.getTo());

        double sumCost = 0.0;
        for (DriverDescription dd : cont) {
//            sumCost += g.bfs(dd.getTrip().getFrom(), dd.getTrip().getTo());
            sumCost += dd.getTrip().getCost();
        }
//        sumCost += g.bfs(driverTrip.getFrom(), driverTrip.getTo());

        return sumCost - sum;
    }

    private static int nearestVertex(Graph g, int from, Set<Integer> vertices) {
        Integer min = Integer.MAX_VALUE;
        int currentVertex = -1;
        for (Integer t : vertices) {
            int shortDistance = g.bfs(from, t);
            if (min > shortDistance) {
                min = shortDistance;
                currentVertex = t;
            }
        }
        return currentVertex;
    }


    public static boolean removeFromCollectionByAID(Set<DriverDescription> set, AID aid) {
        for (DriverDescription dd : set) {
            if (dd.getAid().getName().equals(aid.getName())) {
                set.remove(dd);
                return true;
            }
        }

        return false;
    }


    public static boolean containsInCollectionByAID(Set<DriverDescription> set, AID aid) {
        for (DriverDescription dd : set) {
            if (dd.getAid().getName().equals(aid.getName())) {
                return true;
            }
        }

        return false;
    }

    public static DriverDescription getFromCollectionByAID(Set<DriverDescription> set, AID aid) {
        for (DriverDescription dd : set) {
            if (dd.getAid().getName().equals(aid.getName())) {
                return dd;
            }
        }

        return null;
    }
}
