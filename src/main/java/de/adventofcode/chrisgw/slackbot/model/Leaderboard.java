package de.adventofcode.chrisgw.slackbot.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;


@Data
@XmlRootElement
@JsonNaming(value = SnakeCaseStrategy.class)
public class Leaderboard implements Iterable<LeaderboardMember> {

    private int event;
    private long ownerId;

    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PRIVATE)
    @ToString.Exclude
    @JsonProperty
    private Map<Long, LeaderboardMember> members = new HashMap<>();


    @ToString.Include
    public Instant lastEarnedStarTs() {
        return members().map(LeaderboardMember::getLastStarTs)
                .max(Comparator.naturalOrder())
                .orElse(Instant.MIN);
    }


    public int memberCount() {
        return members.size();
    }

    public boolean isEmptyLeaderboard() {
        return memberCount() == 0;
    }


    public Optional<LeaderboardMember> getMemberLeaderboardRanking(long memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    public LeaderboardMember addMember(LeaderboardMember leaderboardMember) {
        return members.put(leaderboardMember.getId(), leaderboardMember);
    }

    public boolean containsMember(long memberId) {
        return members.containsKey(memberId);
    }


    public Stream<LeaderboardMember> members() {
        return members.values().stream();
    }

    @Override
    public Iterator<LeaderboardMember> iterator() {
        return members().sorted().iterator();
    }

}
