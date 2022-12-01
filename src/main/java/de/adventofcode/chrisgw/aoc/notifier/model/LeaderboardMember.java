package de.adventofcode.chrisgw.aoc.notifier.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.apache.commons.lang3.builder.CompareToBuilder;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Data
@XmlRootElement
@JsonNaming(value = SnakeCaseStrategy.class)
public class LeaderboardMember implements Comparable<LeaderboardMember> {

    private long id;
    private String name;
    private String printName;

    private int localScore;
    private int globalScore;

    @JsonProperty("last_star_ts")
    private int lastStarTsSeconds;
    private int stars;

    @JsonIgnore
    private Map<Integer, AdventOfCodeDayTask> completedDays = new HashMap<>();

    @JsonSetter("completion_day_level")
    public void setCompletedDays(JsonNode completedDayLevelRootNode) {
        completedDayLevelRootNode.fields().forEachRemaining(dayJsonNode -> {
            int day = Integer.parseInt(dayJsonNode.getKey());
            AdventOfCodeDayTask adventOfCodeDayTask = new AdventOfCodeDayTask(day);
            dayJsonNode.getValue().elements().forEachRemaining(completedLevelNode -> {
                Instant completionInstant = Instant.ofEpochSecond(completedLevelNode.path("get_star_ts").asLong());
                adventOfCodeDayTask.addNextCompletedLevel(completionInstant);
            });
            completedDays.put(day, adventOfCodeDayTask);
        });
    }

    public Optional<AdventOfCodeDayTask> getLastFinishedDayTask() {
        return completedDays.values().stream().max(Comparator.comparing(AdventOfCodeDayTask::getLastComplitionTime));
    }


    public Optional<AdventOfCodeDayTask> dayTaskFor(int day) {
        return Optional.ofNullable(completedDays.get(day));
    }


    @Override
    public int compareTo(LeaderboardMember otherMember) {
        return new CompareToBuilder().append(this.getLocalScore(), otherMember.getLocalScore())
                .append(otherMember.getLastStarTs(), this.getLastStarTs()) // smaller first
                .append(this.getPrintName(), otherMember.getPrintName())
                .toComparison() * -1;
    }

    public String getPrintName() {
        if (printName != null) {
            return printName;
        } else if (name != null) {
            return name;
        } else {
            return String.format("(user #%d)", id);
        }
    }

    public Instant getLastStarTs() {
        return Instant.ofEpochSecond(lastStarTsSeconds);
    }

}
