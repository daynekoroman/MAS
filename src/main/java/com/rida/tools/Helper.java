package com.rida.tools;

/**
 * Created by shmagrinsky on 09.04.16.
 */
public class Helper {
    /**
     * Расчитать длину пути
     */
    public static int calcWayLength(Trip trip, Graph graph) {
        return graph.bfs(trip.getFrom(), trip.getTo());
    }

    public static int calcProfit(Trip trip1, Trip trip2, Graph graph) {
        int from1 = trip1.getFrom();
        int to1 = trip1.getTo();
        int from2 = trip2.getFrom();
        int to2 = trip2.getTo();
        return graph.bfs(from1, to1) + graph.bfs(from2, to2) -
                (graph.bfs(from1, from2) + graph.bfs(from2, to2) + graph.bfs(to2, to1));
    }
}
