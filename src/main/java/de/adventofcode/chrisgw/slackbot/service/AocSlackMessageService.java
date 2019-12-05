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
import java.time.Instant;
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

    public static final DateTimeFormatter DAY_TASK_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern(
            "E. dd.MM. 'um' HH:mm");

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
            String leaderboardMessage = formatLeaderboardMessage(leaderboardChange);
            String leaderboardChangeMessage = formatLeaderboardChangeMessage(leaderboardChange);
            String message = "{\"text\": \"" + leaderboardMessage + "\n" + leaderboardChangeMessage + "\"}";
            log.debug("sendLeaderboardChangeMessage: {}", message);
            Response postMessageResponse = client.target(slackWebhookUrl).request().post(Entity.text(message));
            log.debug("sendLeaderboardChangeMessage {}", postMessageResponse.getStatusInfo());
        } catch (Exception e) {
            log.error("could not send new Leaderboard message", e);
        }
    }

    private int findLongestName(Leaderboard leaderboard) {
        return leaderboard.members()
                .map(LeaderboardMember::getPrintName)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .max()
                .orElse(6);
    }


    private String formatLeaderboardMessage(LeaderboardChange leaderboardChange) {
        Leaderboard leaderboard = leaderboardChange.getCurrentLeaderboard();
        int longestName = findLongestName(leaderboard);

        StringBuilder sb = new StringBuilder();
        int platzierung = 1;
        sb.append(String.format("AoC y=%04d       1111111111222222\n", leaderboard.getEvent()));
        sb.append("##)     1234567890123456789012345\n");
        for (LeaderboardMember member : leaderboard) {
            String printName = member.getPrintName();
            int punkte = member.getLocalScore();
            CharSequence completedStarsStr = formatCompletedDayTasks(leaderboardChange, member);
            String letzteAufgabeStr = member.getLastFinishedDayTask().map(this::formatLastFinishedDayTask).orElse("");
            String neuerungPlatzierungStr = formatNeuerungPlatzierung(leaderboardChange, member);
            sb.append(String.format("%2d) %3d %25s \t%" + longestName + "s %s %s\n", //
                    platzierung++, punkte, completedStarsStr, printName, letzteAufgabeStr, neuerungPlatzierungStr));
        }
        return sb.insert(0, "```").append("```").toString();
    }

    private CharSequence formatCompletedDayTasks(LeaderboardChange leaderboardChange, LeaderboardMember member) {
        StringBuilder sb = new StringBuilder(25);
        for (int day = 1; day <= 25; day++) {
            if (!isUnlockedDayTask(leaderboardChange, day)) {
                sb.append(" ");
            } else {
                AdventOfCodeDayTask dayTask = member.dayTaskFor(day).orElse(new AdventOfCodeDayTask(day));
                sb.append(asLevelStar(dayTask));
            }
        }
        return sb;
    }

    private boolean isUnlockedDayTask(LeaderboardChange leaderboardChange, int day) {
        Leaderboard currentLeaderboard = leaderboardChange.getCurrentLeaderboard();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime unlockTime = currentLeaderboard.unlockTimeForDayTask(day);
        return now.isAfter(unlockTime);
    }

    private String formatNeuerungPlatzierung(LeaderboardChange leaderboardChange, LeaderboardMember member) {
        int currentRanking = leaderboardChange.getCurrentLeaderboard().rankingFor(member);
        int previousRanking = currentRanking;
        if (leaderboardChange.getPreviousLeaderboard() != null) {
            previousRanking = leaderboardChange.getPreviousLeaderboard().rankingFor(member);
        }
        if (currentRanking == previousRanking) {
            return "";
        } else if (currentRanking > previousRanking) {
            return "\u2193"; // downward arrow ↓
        } else {
            return "\u2191"; // upwards arrow ↑
        }
    }

    private String formatLastFinishedDayTask(AdventOfCodeDayTask dayTask) {
        String formattedComplitionTime = formatLastComplitionTime(dayTask);
        return String.format("Tag %2d. %s am %s", //
                dayTask.getDay(), asLevelStar(dayTask), formattedComplitionTime);
    }

    private String asLevelStar(AdventOfCodeDayTask dayTask) {
        int completedLevels = dayTask.completedLevels();
        if (completedLevels == 0) {
            return "-";
        } else if (completedLevels == 1) {
            return "\u2606"; // white star ☆
        } else {
            return "\u2605"; // black star ★
        }
    }


    private String formatLeaderboardChangeMessage(LeaderboardChange leaderboardChange) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Neu beendete Aufgaben bis ")
                .append(DAY_TASK_DATE_TIME_PATTERN.format(LocalDateTime.now()))
                .append(":*\n");

        leaderboardChange.getCurrentLeaderboard()
                .members()
                .filter(member -> leaderboardChange.newFinishedDayTasks(member.getId()).findAny().isPresent())
                .sorted(Comparator.comparing(LeaderboardMember::getPrintName))
                .forEachOrdered(member -> {
                    sb.append("*").append(member.getPrintName()).append("* beendeten Aufgaben:\n");
                    sb.append(fomartMemberChanges(leaderboardChange.newFinishedDayTasks(member.getId()))).append("\n");
                });
        return sb.toString().trim();
    }

    private String fomartMemberChanges(Stream<AdventOfCodeDayTask> newFinishedDayTasks) {
        StringBuilder sb = new StringBuilder();
        newFinishedDayTasks.sorted().forEachOrdered(dayTask -> {
            String formattedComplitionTime = formatLastComplitionTime(dayTask);
            sb.append("- ").append(dayTask.getDay()).append(". ").append(asLevelStar(dayTask));
            sb.append(" am ").append(formattedComplitionTime).append("\n");
        });
        return sb.toString().trim();
    }


    private static String formatLastComplitionTime(AdventOfCodeDayTask dayTask) {
        Instant lastComplitionTime = dayTask.getLastComplitionTime();
        LocalDateTime complitionTime = LocalDateTime.ofInstant(lastComplitionTime, ZoneId.systemDefault());
        return DAY_TASK_DATE_TIME_PATTERN.format(complitionTime);
    }


    @Value("${slackWebhookUrl}")
    public void setSlackWebhookUrl(String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
    }

}
