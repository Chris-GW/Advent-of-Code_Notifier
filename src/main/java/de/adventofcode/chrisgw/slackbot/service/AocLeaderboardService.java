package de.adventofcode.chrisgw.slackbot.service;

import de.adventofcode.chrisgw.slackbot.model.Leaderboard;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static java.util.Objects.requireNonNull;


@Slf4j
@Service
public class AocLeaderboardService {

    public static final String ADVENT_OF_CODE_LEADERBIARD_URL = "https://adventofcode.com/{year}/leaderboard/private/view/{leaderboardId}.json";

    static final Logger LOG = LoggerFactory.getLogger(AocLeaderboardService.class);

    private Client client;
    private Cookie sessionCookie;


    @Inject
    public AocLeaderboardService(Client client) {
        this.client = requireNonNull(client);
    }


    public Leaderboard fetchAdventOfCodeLeaderboard(int year, long leaderboardId) {
        LOG.debug("fetchAdventOfCodeLeaderboard with leaderboardId={}", leaderboardId);
        Leaderboard leaderboard = client.target(ADVENT_OF_CODE_LEADERBIARD_URL)
                .resolveTemplate("leaderboardId", leaderboardId)
                .resolveTemplate("year", year)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, sessionCookie)
                .get(Leaderboard.class);
        LOG.debug("fetchAdventOfCodeLeaderboard success: {}", leaderboard);
        return leaderboard;
    }


    @Value("${sessionId}")
    public void setSessionIdCookie(String sessionId) {
        this.sessionCookie = new Cookie("session", sessionId);
    }


}
