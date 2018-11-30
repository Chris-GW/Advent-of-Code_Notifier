package de.adventofcode.chrisgw.slackbot;

import lombok.Data;
import org.apache.commons.lang3.builder.CompareToBuilder;

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


    public Optional<Instant> getCompletedTsForLevel(int level) {
        if (hasCompletedLevel(level)) {
            return Optional.of(completedLevel.get(level - 1));
        } else {
            return Optional.empty();
        }
    }

    public Optional<Instant> getLastComplitionTime() {
        return getCompletedTsForLevel(completedLevels());
    }


    public int addNextCompletedLevel(Instant completionTs) {
        completedLevel.add(completionTs);
        return completedLevels();
    }


    @Override
    public Iterator<Instant> iterator() {
        return completedLevel.iterator();
    }


    @Override
    public int compareTo(AdventOfCodeDayTask otherDayTask) {
        return new CompareToBuilder().append(this.getDay(), otherDayTask.getDay())
                .append(this.completedLevels(), otherDayTask.completedLevels())
                .append(this.getLastComplitionTime().orElse(Instant.MIN),
                        otherDayTask.getLastComplitionTime().orElse(Instant.MIN))
                .toComparison();
    }

    @Override
    public String toString() {
        int latestLevel = completedLevels();
        return String.format("%2d:%1d@%s", day, latestLevel,
                getCompletedTsForLevel(latestLevel).orElseThrow(RuntimeException::new));
    }

}
