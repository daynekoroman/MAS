package com.rida.agents;

import com.rida.tools.FileUtils;
import com.rida.tools.Graph;
import jade.core.Agent;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;


/**
 * Агент-создатель
 * Создает агентов водителей, которые описаны в файле.
 * А так же считывает матрицу смежности графа, который описывает карту города
 */
public class CreatorAgent extends Agent {
    private static final String GRAPH_DESCRIPTION_FILE = "graph.txt";
    private static final String DRIVERS_DESCRIPTION_FILE = "drivers.txt";
    private static final String DRIVER_AGENT_BASE_NAME = "DriverAgent";
    private static final String DRIVER_AGENT_CLASS_NAME = "com.rida.agents.DriverAgent";

    private static final Logger LOG = LoggerFactory.getLogger(CreatorAgent.class);
    private static final long serialVersionUID = 6383145659245328051L;
    private transient Graph mapGraph = null;

    private Graph getGraphFromFile(String fileName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource(fileName);
        LOG.info("I'm trying to get graph from file: {}", fileUrl);
        int[][] matrix;
        try {
            matrix = FileUtils.readSquareIntegerMatrix(fileUrl);
        } catch (IOException e) {
            LOG.error("Failed to read graph matrix caused ", e);
            throw e;
        }
        return (matrix != null) ? new Graph(matrix) : null;
    }

    private void createAgentsFromFile() throws IOException, StaleProxyException {

        ClassLoader classLoader = getClass().getClassLoader();
        URL fileUrl = classLoader.getResource(DRIVERS_DESCRIPTION_FILE);
        int[][] drivers = FileUtils.readSquareIntegerMatrix(fileUrl);
        int driversCount = drivers.length;
        AgentContainer c = getContainerController();
        for (int i = 0; i < driversCount; i++) {
            Object[] params = new Object[3];
            params[0] = mapGraph;
            params[1] = drivers[i][0];
            params[2] = drivers[i][1];
            AgentController a = c.createNewAgent(DRIVER_AGENT_BASE_NAME + i, DRIVER_AGENT_CLASS_NAME, params);
            a.start();
        }
    }

    @Override
    protected void setup() {
        super.setup();
        try {
            mapGraph = getGraphFromFile(GRAPH_DESCRIPTION_FILE);
            LOG.info("I got city graph and now create an Angents");
            createAgentsFromFile();
            LOG.info("I'm done to create an Agents.");
        } catch (IOException e) {
            LOG.error("Failed to get class File", e);
        } catch (StaleProxyException e) {
            LOG.error("Failed create Driver agent caused", e);
        } finally {
            doDelete();
        }
        doDelete();
    }
}
