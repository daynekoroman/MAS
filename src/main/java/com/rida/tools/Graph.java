package com.rida.tools;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by daine on 03.04.2016.
 */
public class Graph {
    private int[][] graph;
    private int[][] waylen;
    private int n;


    public Graph(int[][] graph) {
        this.graph = graph;
        n = graph.length;
        waylen = new int [n][n];
        for (int i = 0; i < n; i ++)
            for (int j = 0; j < n; j ++)
                waylen[i][j] = _bfs(i, j);
    }

    public int bfs(int x, int y){
        return waylen[x][y];
    }

    private int _bfs(int x, int y) {
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
}
