package de.adventofcode.chrisgw.slackbot.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonNaming(value = SnakeCaseStrategy.class)
public class MemberLeaderboardRanking implements Comparable<MemberLeaderboardRanking> {

    private long id;
    private String name;

    private int localScore;
    private int globalScore;

    private Instant lastStarTs;
    private int stars;

    @JsonIgnore
    private Map<Integer, AdventOfCodeDayTask> completedDays = new HashMap<>();

    @JsonSetter("completion_day_level")
    public void setCompletedDays(JsonNode completedDayLevelRootNode) {
        completedDayLevelRootNode.elements().forEachRemaining(completedDayLevelNode -> {
            int day = Integer.parseInt(completedDayLevelNode.fieldNames().next());
            AdventOfCodeDayTask adventOfCodeDayTask = new AdventOfCodeDayTask(day);
            completedDayLevelNode.elements().forEachRemaining(completedLevelNode -> {
                Instant completionInstant = Instant.ofEpochSecond(completedLevelNode.path("get_star_ts").asLong());
                adventOfCodeDayTask.addNextCompletedLevel(completionInstant);
            });
            completedDays.put(day, adventOfCodeDayTask);
        });
    }

    public Optional<AdventOfCodeDayTask> getLastFinishedDayTask() {
        return completedDays.values().stream().max(Comparator.comparing(AdventOfCodeDayTask::getLastComplitionTime));
    }

    private Optional<Instant> getCompletionDayLevel(int day, int level) {
        JsonNode completionDayLevel = completionDayLevelPath(day, level).path("get_star_ts");
        if (completionDayLevel.isValueNode()) {
            Instant completionInstant = Instant.ofEpochSecond(completionDayLevel.asLong());
            return Optional.of(completionInstant);
        } else {
            return Optional.empty();
        }
    }

    private JsonNode completionDayLevelPath(int day, int level) {
        JsonNode completionDayLevel = null;
        return completionDayLevel.path(String.valueOf(day)).path(String.valueOf(level));
    }


    @Override
    public int compareTo(MemberLeaderboardRanking otherMember) {
        return new CompareToBuilder().append(this.getLocalScore(), otherMember.getLocalScore())
                .append(this.getLastStarTs(), otherMember.getLastStarTs())
                .append(this.getGlobalScore(), otherMember.getGlobalScore())
                .toComparison() * -1;
    }

    public String getPrintName() {
        if (name != null) {
            return name;
        } else {
            return String.format("(user #%d)", id);
        }
    }

}
