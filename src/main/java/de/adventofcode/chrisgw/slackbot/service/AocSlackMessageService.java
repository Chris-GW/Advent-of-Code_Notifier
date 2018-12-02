package de.adventofcode.chrisgw.slackbot.service;


import de.adventofcode.chrisgw.slackbot.model.AdventOfCodeDayTask;
import de.adventofcode.chrisgw.slackbot.model.Leaderboard;
import de.adventofcode.chrisgw.slackbot.model.MemberLeaderboardRanking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static java.util.Objects.requireNonNull;


@Slf4j
@Service
public class AocSlackMessageService implements LeaderboardMessageService {

    public static final DateTimeFormatter DAY_TASK_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("E. dd.MM. HH:mm");

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
                .map(MemberLeaderboardRanking::getPrintName)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(6);

        StringBuilder sb = new StringBuilder("{\"text\": \"```");
        int platzierung = 1;
        for (MemberLeaderboardRanking memberLeaderboardRanking : leaderboard) {
            String username = memberLeaderboardRanking.getPrintName();
            int punkte = memberLeaderboardRanking.getLocalScore();
            int stars = memberLeaderboardRanking.getStars();
            String letzteAufgabeStr = memberLeaderboardRanking.getLastFinishedDayTask()
                    .map(this::formatLastFinishedDayTask)
                    .orElse("");
            sb.append(String.format("%2d) %3d Punkte, %2d Sterne %" + longestName + "s\t%s\n", //
                    platzierung++, punkte, stars, username, letzteAufgabeStr));
        }
        return sb.append("```\"}").toString();
    }

    private String formatLastFinishedDayTask(AdventOfCodeDayTask dayTask) {
        LocalDateTime complitionTime = LocalDateTime.ofInstant(dayTask.getLastComplitionTime(), ZoneId.systemDefault());
        String formattedComplitionTime = DAY_TASK_DATE_TIME_PATTERN.format(complitionTime);
        char levelChar = (char) ('a' + dayTask.completedLevels() - 1);
        return String.format("Tag %d. %c) am %s", //
                dayTask.getDay(), levelChar, formattedComplitionTime);
    }


    @Value("${slackWebhookUrl}")
    public void setSlackWebhookUrl(String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
    }

}
