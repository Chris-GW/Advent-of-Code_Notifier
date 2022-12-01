package de.adventofcode.chrisgw.aoc.notifier.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;


@Value
@XmlRootElement
@JsonNaming(value = SnakeCaseStrategy.class)
public class LeaderboardChange {

    private final Leaderboard previousLeaderboard;

    @NotNull
    private final Leaderboard currentLeaderboard;


    public Stream<AdventOfCodeDayTask> newFinishedDayTasks(long memberId) {
        if (previousLeaderboard == null || previousLeaderboard.isEmptyLeaderboard()) {
            return Stream.empty();
        }
        return currentLeaderboard.getMemberForId(memberId)
                .map(LeaderboardMember::getCompletedDays)
                .map(Map::values)
                .map(Collection::stream)
                .orElse(Stream.empty())
                .filter(dayTask -> isNewSolvedDayTask(memberId, dayTask));
    }

    private boolean isNewSolvedDayTask(long memberId, AdventOfCodeDayTask dayTask) {
        if (previousLeaderboard == null) {
            return false;
        }
        Instant previousLastStarTs = previousLeaderboard.getMemberForId(memberId)
                .map(LeaderboardMember::getLastStarTs)
                .orElse(Instant.MIN);
        return dayTask.getLastComplitionTime().isAfter(previousLastStarTs);
    }

    public boolean hasChanged() {
        return previousLeaderboard == null || !currentLeaderboard.lastEarnedStarTs()
                .equals(previousLeaderboard.lastEarnedStarTs());
    }

    public boolean hasChanged(long memberId) {
        if (previousLeaderboard == null) {
            return false;
        }
        Instant lastChange = currentLeaderboard.getMemberForId(memberId)
                .map(LeaderboardMember::getLastStarTs)
                .orElse(Instant.MIN);
        Instant previousLastChange = previousLeaderboard.getMemberForId(memberId)
                .map(LeaderboardMember::getLastStarTs)
                .orElse(Instant.MIN);
        return !lastChange.equals(previousLastChange);
    }

}
