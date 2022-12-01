package de.adventofcode.chrisgw.aoc.notifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.adventofcode.chrisgw.aoc.notifier.model.Leaderboard;
import de.adventofcode.chrisgw.aoc.notifier.model.LeaderboardMember;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;


@Slf4j
@Service
public class AocLeaderboardService {

    public static final String ADVENT_OF_CODE_LEADERBIARD_URL = "https://adventofcode.com/{year}/leaderboard/private/view/{leaderboardId}.json";

    private Client client;
    private ObjectMapper om;
    private Cookie sessionCookie;
    private Map<String, String> memberNames = new HashMap<>();


    @Inject
    public AocLeaderboardService(Client client, ObjectMapper om) {
        this.client = requireNonNull(client);
        this.om = requireNonNull(om);
    }


    public Leaderboard fetchAdventOfCodeLeaderboard(int year, long leaderboardId) {
        log.debug("fetchAdventOfCodeLeaderboard with leaderboardId={}", leaderboardId);
        Leaderboard leaderboard = client.target(ADVENT_OF_CODE_LEADERBIARD_URL)
                .resolveTemplate("leaderboardId", leaderboardId)
                .resolveTemplate("year", year)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, sessionCookie)
                .get(Leaderboard.class);
        leaderboard.members().forEach(this::setPrintName);
        log.debug("fetchAdventOfCodeLeaderboard success: {}", leaderboard);
        return leaderboard;
    }

    private void setPrintName(LeaderboardMember member) {
        String name = member.getName();
        String printName = memberNames.getOrDefault(name, null);
        member.setPrintName(printName);
    }


    @Value("${sessionId}")
    public void setSessionIdCookie(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new IllegalArgumentException("Expect non empty sessionId: " + sessionId);
        }
        this.sessionCookie = new Cookie("session", sessionId);
    }


    @Value( "${memberNamesJsonFilePath}")
    public void setMemberNamesResource(Resource memberNamesResource) {
        if (!memberNamesResource.isReadable()) {
            log.warn("could not read memberNames from: " + memberNamesResource);
            return;
        }
        try (InputStream inputStream = memberNamesResource.getInputStream()) {
            JsonNode rootNode = om.readTree(inputStream);
            for (JsonNode jsonNode : rootNode) {
                String userName = jsonNode.path("userName").asText();
                String name = jsonNode.path("name").asText();
                memberNames.put(userName, name);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
