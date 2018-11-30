package de.adventofcode.chrisgw.slackbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static java.util.Objects.requireNonNull;


public class AdventOfCodeLeaderboardService {

    public static final String ADVENT_OF_CODE_LEADERBIARD_URL = "https://adventofcode.com/2017/leaderboard/private/view/{leaderboardId}.json";

    static final Logger LOG = LoggerFactory.getLogger(AdventOfCodeLeaderboardService.class);

    private Client client;
    private Cookie sessionCookie;


    public AdventOfCodeLeaderboardService(Client client) {
        this.client = requireNonNull(client);
    }


    public Leaderboard fetchAdventOfCodeLeaderboard(long leaderboardId) {
        LOG.debug("fetchAdventOfCodeLeaderboard with leaderboardId={}", leaderboardId);
        Leaderboard leaderboard = client.target(ADVENT_OF_CODE_LEADERBIARD_URL)
                .resolveTemplate("leaderboardId", leaderboardId)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.COOKIE, sessionCookie)
                .get(Leaderboard.class);
        LOG.debug("fetchAdventOfCodeLeaderboard success: {}", leaderboard);
        return leaderboard;
    }

    public void setSessionIdCookie(String sessionId) {
        this.sessionCookie = new Cookie("session", sessionId);
    }

}
