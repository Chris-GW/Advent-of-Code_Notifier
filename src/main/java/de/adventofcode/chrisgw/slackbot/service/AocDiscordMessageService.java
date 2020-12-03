package de.adventofcode.chrisgw.slackbot.service;

import de.adventofcode.chrisgw.slackbot.model.AdventOfCodeDayTask;
import de.adventofcode.chrisgw.slackbot.model.Leaderboard;
import de.adventofcode.chrisgw.slackbot.model.LeaderboardChange;
import de.adventofcode.chrisgw.slackbot.model.LeaderboardMember;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.Stream;

import static de.adventofcode.chrisgw.slackbot.service.AocSlackMessageService.DAY_TASK_DATE_TIME_PATTERN;
import static java.util.Objects.requireNonNull;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;


@Slf4j
@Service
public class AocDiscordMessageService implements LeaderboardMessageService {

    private String discordWebhook;
    private Client client;


    @Inject
    public AocDiscordMessageService(Client client) {
        this.client = requireNonNull(client);
    }


    @Override
    public void sendLeaderboardChangeMessage(LeaderboardChange leaderboardChange) {
        try {
            String leaderboardMessage = formatLeaderboardMessage(leaderboardChange);
            String leaderboardChangeMessage = formatLeaderboardChangeMessage(leaderboardChange);
            Response postMessageResponse = postDiscordMessage(leaderboardMessage + "\n" + leaderboardChangeMessage);
            StatusType statusInfo = postMessageResponse.getStatusInfo();
            if (!Status.OK.equals(statusInfo)) {
                log.error("sendLeaderboardChangeMessage {}", statusInfo);
            } else {
                log.debug("sendLeaderboardChangeMessage {}", statusInfo);
            }
        } catch (Exception e) {
            log.error("could not send new Leaderboard message", e);
        }
    }


    private Response postDiscordMessage(String discordPostMessage) {
        return postDiscordMessage(new DiscordPostMessageDto(discordPostMessage));
    }

    private Response postDiscordMessage(DiscordPostMessageDto discordPostMessageDto) {
        log.debug("sendLeaderboardChangeMessage: {}", discordPostMessageDto.getContent());
        return client.target(discordWebhook).request().post(entity(discordPostMessageDto, APPLICATION_JSON));
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
        sb.append("    5     10    15     20    25\t");
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
                sb.append("\u2587"); // LOWER SEVEN EIGHTHS BLOCK
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
            return "\u2587"; // LOWER SEVEN EIGHTHS BLOCK
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


    @Value("${discordWebhookUrl}")
    public void setDiscordToken(String discordWebhookUrl) {
        if (StringUtils.isEmpty(discordWebhookUrl)) {
            throw new IllegalArgumentException("Expect non empty discordToken: " + discordWebhookUrl);
        }
        this.discordWebhook = discordWebhookUrl;
    }


    @Data
    @XmlRootElement
    public static class DiscordPostMessageDto {

        private final String content;

    }

}
