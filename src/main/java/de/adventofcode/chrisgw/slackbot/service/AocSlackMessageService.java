package de.adventofcode.chrisgw.slackbot.service;


import de.adventofcode.chrisgw.slackbot.model.AdventOfCodeDayTask;
import de.adventofcode.chrisgw.slackbot.model.Leaderboard;
import de.adventofcode.chrisgw.slackbot.model.LeaderboardChange;
import de.adventofcode.chrisgw.slackbot.model.LeaderboardMember;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;


@Slf4j
@Service
public class AocSlackMessageService implements LeaderboardMessageService {

    public static final DateTimeFormatter DAY_TASK_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("E. dd.MM. HH:mm");

    private String slackWebhookUrl;
    private Client client;
    private Map<Long, LeaderboardChange> leaderboardChanges = new HashMap<>();


    @Inject
    public AocSlackMessageService(Client client) {
        this.client = requireNonNull(client);
    }


    @Override
    public void sendLeaderboardChangeMessage(LeaderboardChange leaderboardChange) {
        try {
            String leaderboardMessage = formatLeaderboardMessage(leaderboardChange.getCurrentLeaderboard());
            String leaderboardChangeMessage = formatLeaderboardChangeMessage(leaderboardChange);
            String message = "{\"text\": \"" + leaderboardMessage + "\n" + leaderboardChangeMessage + "\"}";
            log.debug("sendLeaderboardChangeMessage: {}", message);
            Response postMessageResponse = client.target(slackWebhookUrl).request().post(Entity.text(message));
            log.debug("sendLeaderboardChangeMessage {}", postMessageResponse.getStatusInfo());
        } catch (Exception e) {
            log.error("could not send new Leaderboard message", e);
        }
    }

    private String formatLeaderboardMessage(Leaderboard leaderboard) {
        int longestName = leaderboard.members()
                .map(LeaderboardMember::getPrintName)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(6);

        StringBuilder sb = new StringBuilder();
        int platzierung = 1;
        for (LeaderboardMember leaderboardMember : leaderboard) {
            String username = leaderboardMember.getPrintName();
            int punkte = leaderboardMember.getLocalScore();
            int stars = leaderboardMember.getStars();
            String letzteAufgabeStr = leaderboardMember.getLastFinishedDayTask()
                    .map(this::formatLastFinishedDayTask)
                    .orElse("");
            sb.append(String.format("%2d) %3d Punkte, %2d Sterne %" + longestName + "s\t%s\n", //
                    platzierung++, punkte, stars, username, letzteAufgabeStr));
        }
        return sb.insert(0, "```").append("```").toString();
    }

    private String formatLastFinishedDayTask(AdventOfCodeDayTask dayTask) {
        LocalDateTime complitionTime = LocalDateTime.ofInstant(dayTask.getLastComplitionTime(), ZoneId.systemDefault());
        String formattedComplitionTime = DAY_TASK_DATE_TIME_PATTERN.format(complitionTime);
        char levelChar = (char) ('a' + dayTask.completedLevels() - 1);
        return String.format("Tag %d. %c) am %s", //
                dayTask.getDay(), levelChar, formattedComplitionTime);
    }


    private String formatLeaderboardChangeMessage(LeaderboardChange leaderboardChange) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Neu beendete Aufgaben seit ")
                .append(DAY_TASK_DATE_TIME_PATTERN.format(LocalDateTime.now()))
                .append(":*\n");

        leaderboardChange.getCurrentLeaderboard()
                .members()
                .sorted(Comparator.comparing(LeaderboardMember::getPrintName))
                .filter(member -> leaderboardChange.newFinishedDayTasks(member.getId()).findAny().isPresent())
                .forEachOrdered(member -> {
                    sb.append("*").append(member.getPrintName()).append("* beendeten Aufgaben:\n");
                    sb.append(fomartMemberChanges(leaderboardChange.newFinishedDayTasks(member.getId()))).append("\n");
                });
        return sb.toString().trim();
    }

    private String fomartMemberChanges(Stream<AdventOfCodeDayTask> newFinishedDayTasks) {
        StringBuilder sb = new StringBuilder();
        newFinishedDayTasks.sorted().forEachOrdered(dayTask -> {
            LocalDateTime complitionTime = LocalDateTime.ofInstant(dayTask.getLastComplitionTime(),
                    ZoneId.systemDefault());
            String formattedComplitionTime = DAY_TASK_DATE_TIME_PATTERN.format(complitionTime);
            char levelChar = (char) ('a' + dayTask.completedLevels() - 1);

            sb.append("- ").append(dayTask.getDay()).append(". ").append(levelChar);
            sb.append(") am ").append(formattedComplitionTime).append("\n");
        });
        return sb.toString().trim();
    }


    @Value("${slackWebhookUrl}")
    public void setSlackWebhookUrl(String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
    }

}
