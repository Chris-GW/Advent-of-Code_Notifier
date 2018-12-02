package de.adventofcode.chrisgw.slackbot.service;


import de.adventofcode.chrisgw.slackbot.model.Leaderboard;
import de.adventofcode.chrisgw.slackbot.model.MemberLeaderboardRanking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Objects;

import static java.util.Objects.requireNonNull;


@Slf4j
@Service
public class AocSlackMessageService implements LeaderboardMessageService {

    private String slackWebhookUrl;
    private Client client;


    @Inject
    public AocSlackMessageService(Client client) {
        this.client = requireNonNull(client);
    }


    @Override
    public void sendNewLeaderboardMessage(Leaderboard leaderboard) {
        try {
            String leaderboardMessage = formatLeaderboardMessage(leaderboard);
            log.debug("sendNewLeaderboardMessage: {}", leaderboardMessage);
            Response postMessageResponse = client.target(slackWebhookUrl)
                    .request()
                    .post(Entity.text(leaderboardMessage));
            log.debug("sendNewLeaderboardMessage {}", postMessageResponse.getStatusInfo());
        } catch (Exception e) {
            log.error("could not send new Leaderboard message", e);
        }
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
            sb.append(String.format("%2d) %" + longestName + "s\t%3d Punkte, %2d Sterne\n", //
                    platzierung++, username, punkte, stars));
        }
        return sb.append("```\"}").toString();
    }


    @Value("${slackWebhookUrl}")
    public void setSlackWebhookUrl(String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
    }

}
