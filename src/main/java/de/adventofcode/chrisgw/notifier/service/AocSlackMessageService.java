package de.adventofcode.chrisgw.notifier.service;


import de.adventofcode.chrisgw.notifier.model.AdventOfCodeDayTask;
import de.adventofcode.chrisgw.notifier.model.Leaderboard;
import de.adventofcode.chrisgw.notifier.model.LeaderboardChange;
import de.adventofcode.chrisgw.notifier.model.LeaderboardMember;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Slf4j
@Service
public class AocSlackMessageService implements LeaderboardMessageService {

    public static final DateTimeFormatter DAY_TASK_DATE_TIME_PATTERN = DateTimeFormatter.ofPattern(
            "E. dd.MM. 'um' HH:mm");

    private final Client client;
    private String slackWebhookUrl;


    @Inject
    public AocSlackMessageService(Client client) {
        this.client = requireNonNull(client);
    }


    @Override
    public void sendLeaderboardChangeMessage(LeaderboardChange leaderboardChange) {
        Response postMessageResponse = null;
        try {
            String leaderboardMessage = formatLeaderboardMessage(leaderboardChange);
            String leaderboardChangeMessage = formatLeaderboardChangeMessage(leaderboardChange);
            postMessageResponse = postSlackMessage(leaderboardMessage + "\n" + leaderboardChangeMessage);
            StatusType statusInfo = postMessageResponse.getStatusInfo();
            if (Family.SUCCESSFUL.equals(statusInfo.getFamily())) {
                log.debug("sendLeaderboardChangeMessage {}", statusInfo);
            } else {
                log.error("sendLeaderboardChangeMessage {}", statusInfo);
            }
        } catch (Exception e) {
            log.error("could not send new Leaderboard message", e);
        } finally {
            if (postMessageResponse != null) {
                postMessageResponse.close();
            }
        }
    }

    private Response postSlackMessage(String slackMessage) {
        return postSlackMessage(new SlackPostMessageDto(slackMessage));
    }

    private Response postSlackMessage(SlackPostMessageDto slackPostMessageDto) {
        log.debug("sendLeaderboardChangeMessage: {}", slackPostMessageDto);
        return client.target(slackWebhookUrl).request().post(entity(slackPostMessageDto, APPLICATION_JSON));
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
        sb.append(String.format("AoC y=%04d", leaderboard.getEvent()));
        sb.append("       5      10       15      20       25\t");
        sb.append(StringUtils.rightPad("Benutzer", longestName)).append("  Letzte bearbeitete Aufgabe\n");
        for (LeaderboardMember member : leaderboard) {
            String printName = StringUtils.rightPad(member.getPrintName(), longestName);
            int punkte = member.getLocalScore();
            CharSequence completedStarsStr = formatCompletedDayTasks(leaderboardChange, member);
            String letzteAufgabeStr = member.getLastFinishedDayTask().map(this::formatLastFinishedDayTask).orElse("");
            String neuerungPlatzierungStr = formatNeuerungPlatzierung(leaderboardChange, member);
            sb.append(String.format("%2d) %3d %25s\t%s %s %s%n", //
                    platzierung++, punkte, completedStarsStr, printName, letzteAufgabeStr, neuerungPlatzierungStr));
        }
        return sb.insert(0, "```").append("```").toString();
    }

    private CharSequence formatCompletedDayTasks(LeaderboardChange leaderboardChange, LeaderboardMember member) {
        StringBuilder sb = new StringBuilder(25);
        sb.append("|");
        for (int day = 1; day <= 25; day++) {
            if (!isUnlockedDayTask(leaderboardChange, day)) {
                sb.append("\u1680\u1680");
            } else {
                AdventOfCodeDayTask dayTask = member.dayTaskFor(day).orElse(new AdventOfCodeDayTask(day));
                sb.append(asLevelStar(dayTask));
            }
            if (day % 5 == 0) {
                sb.append("|");
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
        if (currentRanking == previousRanking && leaderboardChange.hasChanged(member.getId())) {
            return "*";
        } else if (currentRanking == previousRanking || previousRanking == Integer.MAX_VALUE) {
            return " ";
        } else if (currentRanking > previousRanking) {
            return "\u2193"; // downward arrow ↓
        } else {
            return "\u2191"; // upwards arrow ↑
        }
    }

    private String formatLastFinishedDayTask(AdventOfCodeDayTask dayTask) {
        String formattedComplitionTime = formatLastComplitionTime(dayTask);
        return String.format("%2d. %s am %s", //
                dayTask.getDay(), asLevelStar(dayTask), formattedComplitionTime);
    }

    private String asLevelStar(AdventOfCodeDayTask dayTask) {
        int completedLevels = dayTask.completedLevels();
        if (completedLevels == 0) {
            return "\u1680\u1680";
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
        if (StringUtils.isEmpty(slackWebhookUrl)) {
            throw new IllegalArgumentException("Expect non empty slackWebhookUrl: " + slackWebhookUrl);
        }
        this.slackWebhookUrl = slackWebhookUrl;
    }

    @Data
    @XmlRootElement
    public static class SlackPostMessageDto {

        private final String text;

    }
}
