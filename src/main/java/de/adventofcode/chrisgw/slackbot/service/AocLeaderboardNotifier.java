package de.adventofcode.chrisgw.slackbot.service;

import de.adventofcode.chrisgw.slackbot.model.Leaderboard;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;


@Slf4j
@Service
public class AocLeaderboardNotifier {

    private AocLeaderboardService leaderboardService;
    private List<LeaderboardMessageService> messageServices = new ArrayList<>();
    private Map<Long, Disposable> registerdLeaderbaords = new ConcurrentHashMap<>();


    @Inject
    public AocLeaderboardNotifier(AocLeaderboardService leaderboardService) {
        this.leaderboardService = requireNonNull(leaderboardService);
    }


    public synchronized Disposable subscribeLeaderboard(long leaderboardId) {
        return subscribeLeaderboard(currentAocDate().getYear(), leaderboardId);
    }

    public synchronized Disposable subscribeLeaderboard(int year, long leaderboardId) {
        LocalDate currentAocDate = currentAocDate();
        final int aocYear;
        if (year > currentAocDate.getYear()) {
            aocYear = currentAocDate.getYear();
        } else {
            aocYear = year;
        }

        return registerdLeaderbaords.computeIfAbsent(leaderboardId, key -> {
            return BehaviorSubject.interval(0L, 10L, TimeUnit.MINUTES)
                    .map(interval -> leaderboardService.fetchAdventOfCodeLeaderboard(aocYear, leaderboardId))
                    .distinctUntilChanged(Leaderboard::lastEarnedStarTs)
                    .doOnEach(leaderboardNotification -> {
                        log.debug("leaderboardNotification for {}: {}", leaderboardId, leaderboardNotification);
                    })
                    .doOnTerminate(() -> registerdLeaderbaords.remove(leaderboardId))
                    .subscribe(this::notifyLeaderboardMessageServices);
        });
    }


    private LocalDate currentAocDate() {
        LocalDate now = LocalDate.now();
        LocalDate thisYearAocStartDate = LocalDate.of(now.getYear(), Month.DECEMBER, 1);
        if (now.isBefore(thisYearAocStartDate)) {
            thisYearAocStartDate = thisYearAocStartDate.minusYears(1);
        }
        return thisYearAocStartDate;
    }


    private void notifyLeaderboardMessageServices(Leaderboard leaderboard) {
        for (LeaderboardMessageService messageService : messageServices) {
            messageService.sendNewLeaderboardMessage(leaderboard);
        }
    }


    @Inject
    public void setMessageServices(List<LeaderboardMessageService> messageServices) {
        this.messageServices = requireNonNull(messageServices);
    }

}
