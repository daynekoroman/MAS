package com.rida.agents;

import com.rida.tools.FileUtils;
import com.rida.tools.Graph;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

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
    private static final String GRAPH_DESCRIPTION_FILE = "graph.txt";
//    private static final Logger LOG = LoggerFactory.getLogger(CreatorAgent.class);
    private Graph mapGraph;

    private Graph getGraphFromFile(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource(fileName);
//        LOG.info("I'm trying to get graph from file: {}", fileUrl);
        System.out.println("I'm trying to get graph from file: " + fileUrl);
        int[][] matrix = null;
        try {
            matrix = FileUtils.readSquareIntegerMatrix(fileUrl);
        } catch (IOException e) {
//            LOG.error("Failed to read graph matrix caused: \n {}", e.getMessage());
        	System.out.println("Failed to read graph matrix caused: \n " +e.getMessage());
            throw e;
        }
        return (matrix != null) ? new Graph(matrix) : null;
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
//                LOG.info("{} I create {} driver Agents", new Date(), driversCount);
                System.out.println(new Date() + "I create " + driversCount + " driver Agents");
            } catch (IOException e) {
                doDelete();
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void setup() {
        super.setup();
        try {
            mapGraph = getGraphFromFile(GRAPH_DESCRIPTION_FILE);
//            LOG.info("I got city graph and now create an Angents");
            System.out.println("I got city graph and now create an Angents");
            createAgentsFromFile();
            System.out.println("I'm done to create an Agents.");
//            LOG.info("I'm done to create an Agents.");
        } catch (IOException e) {

        } finally {
            doDelete();
        }
        doDelete();
    }
}
