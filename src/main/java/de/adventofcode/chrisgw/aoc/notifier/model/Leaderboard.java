package de.adventofcode.chrisgw.aoc.notifier.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import javax.xml.bind.annotation.XmlRootElement;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

import static java.time.Month.DECEMBER;


@Data
@XmlRootElement
@JsonNaming(value = SnakeCaseStrategy.class)
public class Leaderboard implements Iterable<LeaderboardMember> {

    public static final LocalTime DAY_TASK_START_TIME = LocalTime.of(6, 0);

    private int event;
    private long ownerId;

    @Getter(value = AccessLevel.PRIVATE)
    @Setter(value = AccessLevel.PRIVATE)
    @ToString.Exclude
    @JsonProperty
    private Map<Long, LeaderboardMember> members = new HashMap<>();


    @ToString.Include
    public Instant lastEarnedStarTs() {
        return members().map(LeaderboardMember::getLastStarTs).max(Comparator.naturalOrder()).orElse(Instant.MIN);
    }


    public int rankingFor(LeaderboardMember member) {
        Iterator<LeaderboardMember> memberIterator = iterator();
        for (int rank = 0; memberIterator.hasNext(); rank++) {
            LeaderboardMember nextMember = memberIterator.next();
            if (member.getId() == nextMember.getId()) {
                return rank;
            }
        }
        return Integer.MAX_VALUE;
    }


    public LocalDateTime unlockTimeForDayTask(int day) {
        return LocalDate.of(event, DECEMBER, day).atTime(DAY_TASK_START_TIME);
    }


    public int memberCount() {
        return members.size();
    }

    public boolean isEmptyLeaderboard() {
        return memberCount() == 0;
    }


    public Optional<LeaderboardMember> getMemberForId(long memberId) {
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
