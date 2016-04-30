package com.rida.behaviours;

import com.rida.agents.CreatorAgent;
import com.rida.tools.Consts;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.Property;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.util.leap.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Слушаем сервис желтых страниц для получения и обработки стастики поездок
 * Created by shmgrinsky on 28.04.16.
 */
public class StatisticsBehaviour extends TickerBehaviour {
    private static final Logger LOG = LoggerFactory.getLogger(StatisticsBehaviour.class);
    private int agentsAmount = 0;
    private CreatorAgent creatorAgent;

    public StatisticsBehaviour(Agent a, long period) {
        super(a, period);
        this.creatorAgent = (CreatorAgent) myAgent;
        this.agentsAmount = creatorAgent.getAgentsAmount();
    }

    @Override
    protected void onTick() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Consts.STATISTICS_ID);
        template.addServices(sd);

        try {
            DFAgentDescription[] yellowPageResults;
            yellowPageResults = DFService.search(myAgent, template);
            if (yellowPageResults.length == agentsAmount) {
                LOG.info("All agents sent me statistic!");
                List<Statistic> statistics = new ArrayList<>();
                for (DFAgentDescription page : yellowPageResults) {
                    statistics.add(parseStatistic(page));
                }
                analyzeStatistics(statistics);
            }
        } catch (FIPAException e) {
            LOG.error("Error in FIPA Protocol", e);
        }
    }

    private enum AgentType {
        CHAUFFER, PASSANGER
    }

    private void analyzeStatistics(List<Statistic> statistics) {
        int chaufferCount = 0;
        int carpoolingDistance = 0;
        for (Statistic statistic : statistics) {
            if (statistic.getType() == AgentType.CHAUFFER) {
                chaufferCount++;
                carpoolingDistance += statistic.getWayLength();
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Chauffeurs: %d, Passengers: %d%n", chaufferCount, agentsAmount - chaufferCount));
        builder.append(String.format("\t carpooling distance: %d %n", carpoolingDistance));
        builder.append(String.format("\t alone distance %d", creatorAgent.getAloneDistance()));
        LOG.info(builder.toString());
        creatorAgent.doDelete();
    }

    private class Statistic {
        private AgentType type;
        private int wayLength;

        Statistic(AgentType type, int wayLength) {
            this.type = type;
            this.wayLength = wayLength;
        }

        AgentType getType() {
            return type;
        }

        int getWayLength() {
            return wayLength;
        }
    }

    private Statistic parseStatistic(Iterator properties) {
        AgentType type = null;
        int wayLength = 0;
        while (properties.hasNext()) {
            Property property = (Property) properties.next();
            Object o = property.getValue();
            String propertyName = property.getName();
            switch (propertyName) {
                case "type":
                    String typeString = (String) o;
                    if (typeString.equals("passenger")) {
                        type = AgentType.PASSANGER;
                    } else {
                        type = AgentType.CHAUFFER;
                    }
                    break;
                case "way-length":
                    wayLength = Integer.parseInt(o.toString());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
        return new Statistic(type, wayLength);

    }

    private Statistic parseStatistic(DFAgentDescription page) {
        Statistic statistic = null;
        Iterator iter = page.getAllServices();
        while (iter.hasNext()) {
            ServiceDescription s = (ServiceDescription) iter.next();
            Iterator properties = s.getAllProperties();
            statistic = parseStatistic(properties);
        }
        return statistic;
    }
}

