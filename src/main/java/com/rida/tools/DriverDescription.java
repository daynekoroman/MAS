package com.rida.tools;

import jade.core.AID;

/**
 * Created by daine on 04.04.2016.
 */
public class DriverDescription implements Comparable {
    private String way;

    public String getName() {
        return name;
    }

    private String name;

    public AID getValue() {
        return value;
    }

    private AID value;
    private int from, to;
    private int wayLength;
    private int profit;
    private int reverseProfit;
    private Graph mapGraph;


    public DriverDescription(String way, String name, AID a, Graph g) {
        this.way = way;
        this.name = name;
        this.value = a;
        from = Integer.parseInt(way.split(" ")[0]);
        to = Integer.parseInt(way.split(" ")[1]);
        mapGraph = g;
    }

    public DriverDescription(DriverDescription dd){
        this.way = dd.way;
        this.name = dd.name;
        this.value = dd.value;
        this.from = dd.from;
        this.to = dd.to;
        this.wayLength = dd.wayLength;
        this.profit = dd.profit;
        this.reverseProfit = dd.reverseProfit;
        this.mapGraph = dd.mapGraph;
    }

    public String toString() {
        return (name.toString() + " " + way);
    }

    public void calcWayLength() {
        wayLength = mapGraph.bfs(Integer.parseInt(way.split(" ")[0]), Integer.parseInt(way.split(" ")[1]));
    }

    public void calcProfit(int from, int to) {
        int myFrom = Integer.parseInt(way.split(" ")[0]);
        int myTo = Integer.parseInt(way.split(" ")[1]);

        reverseProfit = mapGraph.bfs(myFrom, myTo) + mapGraph.bfs(from, to) -
                (mapGraph.bfs(from, myFrom) + mapGraph.bfs(myFrom, myTo) + mapGraph.bfs(myTo, to));

        profit = mapGraph.bfs(myFrom, myTo) + mapGraph.bfs(from, to) -
                (mapGraph.bfs(myFrom, from) + mapGraph.bfs(from, to) + mapGraph.bfs(to, myTo));
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

    public String getname() {
        return name;
    }

    public String getway() {
        return way;
    }

    public int getProfit() {
        return profit;
    }

    public int getWayLength() {
        return wayLength;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }
}