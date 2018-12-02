package de.adventofcode.chrisgw.slackbot.model;

import lombok.Data;
import org.apache.commons.lang3.builder.CompareToBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;


@Data
public class AdventOfCodeDayTask implements Comparable<AdventOfCodeDayTask>, Iterable<Instant> {

    public static final int DAY_TASK_LEVELS = 2;

    private int day;
    private List<Instant> completedLevel = new ArrayList<>(DAY_TASK_LEVELS);


    public AdventOfCodeDayTask(int day) {
        this.day = day;
    }


    public int completedLevels() {
        return completedLevel.size();
    }

    public boolean hasCompletedLevel(int level) {
        return 1 <= completedLevels() && completedLevels() <= level;
    }

    public boolean hasCompletedAllLevels() {
        return completedLevels() == DAY_TASK_LEVELS;
    }

    public boolean hasCompletedAnyLevels() {
        return completedLevels() > 0;
    }


    public Instant getCompletedTsForLevel(int level) {
        return completedLevel.get(level - 1);
    }

    public Instant getLastComplitionTime() {
        return getCompletedTsForLevel(completedLevels());
    }


    public int addNextCompletedLevel(Instant completionTs) {
        completedLevel.add(completionTs);
        return completedLevels();
    }


    public Optional<Duration> neededDurationForLevel(int level) {
        if (level <= 1 || !hasCompletedLevel(level)) {
            return Optional.empty();
        }
        Instant previousLevelCompletedTs = getCompletedTsForLevel(level - 1);
        Instant levelCompletedTs = getCompletedTsForLevel(level);
        return Optional.ofNullable(Duration.between(previousLevelCompletedTs, levelCompletedTs));
    }


    @Override
    public Iterator<Instant> iterator() {
        return completedLevel.iterator();
    }


    @Override
    public int compareTo(AdventOfCodeDayTask otherDayTask) {
        return new CompareToBuilder().append(this.getDay(), otherDayTask.getDay())
                .append(this.completedLevels(), otherDayTask.completedLevels())
                .append(this.getLastComplitionTime(), otherDayTask.getLastComplitionTime())
                .toComparison();
    }

    @Override
    public String toString() {
        int latestLevel = completedLevels();
        return String.format("%2d:%1d@%s", day, latestLevel, getCompletedTsForLevel(latestLevel));
    }

}
