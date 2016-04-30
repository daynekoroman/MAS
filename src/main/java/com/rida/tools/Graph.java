package com.rida.tools;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Класс структуры графа
 */
public class Graph implements Serializable {
    private static final long serialVersionUID = 6130960523989033158L;
    private int[][] adjacencyMatrix;
    private int[][] waylen;
    private int n;

    /**
     * Конструктор класса граф
     *
     * @param adjacencyMatrix - матрица смежности
     */
    public Graph(int[][] adjacencyMatrix) {
        this.adjacencyMatrix = adjacencyMatrix;
        n = adjacencyMatrix.length;
        waylen = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                waylen[i][j] = calcShortestDistanceMatrix(i, j);
    }

    public int bfs(int x, int y) {
        return waylen[x][y];
    }

    private int calcShortestDistanceMatrix(int x, int y) {
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
                if (adjacencyMatrix[cx][i] != 0 && (res[i] == -1 || res[i] > res[cx] + adjacencyMatrix[cx][i])) {
                    qx.add(i);
                    res[i] = res[cx] + adjacencyMatrix[cx][i];
                }
            }
        }
        return res[y];
    }
}
