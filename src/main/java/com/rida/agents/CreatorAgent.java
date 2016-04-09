package com.rida.agents;

import com.rida.tools.Graph;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

/**
 * Created by daine on 03.04.2016.
 */
public class CreatorAgent extends Agent {
    private static final Logger LOG = LoggerFactory.getLogger(CreatorAgent.class);
    private Graph mapGraph;


    private Graph getGraphFromFile() {
        int n;
        int[][] graph;

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
                return new Graph(graph);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void createAgentsFromFile() {

        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource("drivers.txt");
        File file;
        int driversCount;

        if (fileUrl != null) {
            file = new File(fileUrl.getFile());
        } else {
            file = null;
        }

        BufferedReader fin;
        if (file != null) {
            try {
                fin = new BufferedReader(new FileReader(file));
                String size = fin.readLine();
                driversCount = Integer.parseInt(size);
                AgentContainer c = getContainerController();
                String BASE_NAME = "DriverAgent";
                for (int i = 0; i < driversCount; i++) {
                    String[] verteties = fin.readLine().split(" ");
                    try {
                        Object[] params = new Object[3];
                        params[0] = mapGraph;
                        params[1] = verteties[0];
                        params[2] = verteties[1];
                        AgentController a = c.createNewAgent(BASE_NAME + i, "com.rida.agents.DriverAgent", params);
                        a.start();
                    } catch (Exception e) {

                    }
                }
                fin.close();
                LOG.info("I create {} driver Agents", driversCount);
            } catch (IOException e) {
                doDelete();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void setup() {
//        super.setup();
        mapGraph = getGraphFromFile();
        createAgentsFromFile();
        LOG.info("I'm done to create  an Agents.");
        doDelete();
    }
}
