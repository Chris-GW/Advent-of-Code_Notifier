package de.adventofcode.chrisgw.slackbot;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

import static java.util.Objects.requireNonNull;


public class AdventOfCodeSlackBot {

    static final Logger LOG = LoggerFactory.getLogger(AdventOfCodeSlackBot.class);

    private String slackWebhookUrl;
    private Client client;

    private Instant previousLastEarnedStarTs = Instant.MIN;


    public AdventOfCodeSlackBot(Client client, String slackWebhookUrl) {
        this.client = requireNonNull(client);
        this.slackWebhookUrl = requireNonNull(slackWebhookUrl);
    }


    public void sendNewLeaderboardMessage(Leaderboard leaderboard) {
        if (!leaderboard.lastEarnedStarTs().isAfter(previousLastEarnedStarTs)) {
            LOG.debug("sendNewLeaderboardMessage unchanged leaderboard: {}", leaderboard.lastEarnedStarTs());
            return; // nothing changed
        }
        String leaderboardMessage = formatLeaderboardMessage(leaderboard);

        LOG.debug("sendNewLeaderboardMessage: {}", leaderboardMessage);
        Response postMessageResponse = client.target(slackWebhookUrl).request().post(Entity.text(leaderboardMessage));
        LOG.debug("sendNewLeaderboardMessage {}", postMessageResponse.getStatusInfo());
    }

    private String formatLeaderboardMessage(Leaderboard leaderboard) {
        int longestName = leaderboard.members()
                .map(MemberLeaderboardRanking::getName)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(6);

        StringBuilder sb = new StringBuilder("{\"text\": \"```");
        int platzierung = 1;
        for (MemberLeaderboardRanking memberLeaderboardRanking : leaderboard) {
            String username = memberLeaderboardRanking.getName();
            int punkte = memberLeaderboardRanking.getLocalScore();
            int stars = memberLeaderboardRanking.getStars();
            sb.append(String.format("%2d. %" + longestName + "s\t%3d Punkte, %2d Sterne\n", //
                    platzierung++, username, punkte, stars));
        }
        return sb.append("```\"}").toString();
    }


    public static void main(String[] args) {
        try {
            Properties properties = loadProperties(args);
            long leaderboardId = Long.parseLong(properties.getProperty("leaderboardId"));
            String sessionId = properties.getProperty("sessionId");
            String slackWebhookUrl = properties.getProperty("slackWebhookUrl");
            Client client = newClient();

            AdventOfCodeSlackBot adventOfCodeSlackBot = new AdventOfCodeSlackBot(client, slackWebhookUrl);
            AdventOfCodeLeaderboardService leaderboardService = new AdventOfCodeLeaderboardService(client);
            leaderboardService.setSessionIdCookie(sessionId);

            Leaderboard leaderboard = leaderboardService.fetchAdventOfCodeLeaderboard(leaderboardId);
            adventOfCodeSlackBot.sendNewLeaderboardMessage(leaderboard);
        } catch (Exception e) {
            LOG.error("exception happend", e);
        }
    }

    private static Properties loadProperties(String[] args) throws IOException {
        Path propertiesPath;
        if (args.length < 1) {
            propertiesPath = Paths.get("advent-of-code.properties");
        } else {
            propertiesPath = Paths.get(args[0]);
        }
        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(propertiesPath));
        return properties;
    }

    private static Client newClient() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
        om.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return ClientBuilder.newClient()
                .register(new JacksonJaxbJsonProvider(om, JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS));
    }

}
