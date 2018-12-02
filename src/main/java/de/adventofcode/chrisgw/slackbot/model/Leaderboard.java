package de.adventofcode.chrisgw.slackbot.model;

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
public class Leaderboard implements Iterable<MemberLeaderboardRanking> {

    private int event;
    private long ownerId;

    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PRIVATE)
    @ToString.Exclude
    private Map<Long, MemberLeaderboardRanking> members = new HashMap<>();


    public Instant lastEarnedStarTs() {
        return members().map(MemberLeaderboardRanking::getLastStarTs)
                .max(Comparator.naturalOrder())
                .orElse(Instant.MIN);
    }


    public int memberCount() {
        return members.size();
    }

    public Optional<MemberLeaderboardRanking> getMemberLeaderboardRanking(long memberId) {
        return Optional.ofNullable(members.get(memberId));
    }

    public MemberLeaderboardRanking putMemberLeaderboardRanking(MemberLeaderboardRanking memberLeaderboardRanking) {
        return members.put(memberLeaderboardRanking.getId(), memberLeaderboardRanking);
    }

    public boolean containsMember(long memberId) {
        return members.containsKey(memberId);
    }


    public Stream<MemberLeaderboardRanking> members() {
        return members.values().stream();
    }

    @Override
    public Iterator<MemberLeaderboardRanking> iterator() {
        return members().sorted().iterator();
    }

}
