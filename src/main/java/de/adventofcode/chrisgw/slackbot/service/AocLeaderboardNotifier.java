package de.adventofcode.chrisgw.slackbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adventofcode.chrisgw.slackbot.model.Leaderboard;
import de.adventofcode.chrisgw.slackbot.model.LeaderboardChange;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
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
    private ObjectMapper objectMapper;
    private List<LeaderboardMessageService> messageServices = new ArrayList<>();

    private Map<Long, Disposable> registerdLeaderbaords = new ConcurrentHashMap<>();


    @Inject
    public AocLeaderboardNotifier(AocLeaderboardService leaderboardService, ObjectMapper objectMapper) {
        this.leaderboardService = requireNonNull(leaderboardService);
        this.objectMapper = requireNonNull(objectMapper);
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
            LeaderboardChange initialLeaderboardChange = loadLeaderboardChange(year, leaderboardId);
            log.debug("loadLeaderboardChange: {}", initialLeaderboardChange);

            return BehaviorSubject.interval(0L, 10L, TimeUnit.MINUTES)
                    .map(interval -> leaderboardService.fetchAdventOfCodeLeaderboard(aocYear, leaderboardId))
                    .distinctUntilChanged(Leaderboard::lastEarnedStarTs)
                    .scan(initialLeaderboardChange, (previousLeaderboardChange, currentLeaderboard) -> {
                        Leaderboard previousLeaderboard = previousLeaderboardChange.getCurrentLeaderboard();
                        return new LeaderboardChange(previousLeaderboard, currentLeaderboard);
                    })
                    .skip(1)
                    .filter(LeaderboardChange::hasChanged)
                    .doOnNext(leaderboardChange -> log.debug("next for {}: {}", leaderboardId, leaderboardChange))
                    .doOnError(error -> log.error("error for " + leaderboardId, error))
                    .doOnDispose(() -> log.debug("dispose for " + leaderboardId))
                    .doOnTerminate(() -> log.debug("terminate for " + leaderboardId))
                    .doOnTerminate(() -> registerdLeaderbaords.remove(leaderboardId))
                    .subscribe(this::notifyAboutLeaderboardChange);
        });
    }


    private void notifyAboutLeaderboardChange(LeaderboardChange leaderboardChange) {
        saveLeaderboard(leaderboardChange.getCurrentLeaderboard());
        for (LeaderboardMessageService messageService : messageServices) {
            messageService.sendLeaderboardChangeMessage(leaderboardChange);
        }
    }


    private void saveLeaderboard(Leaderboard currentLeaderboard) {
        try {
            int year = currentLeaderboard.getEvent();
            File leaderboardSaveFile = leaderboardSaveFile(year, currentLeaderboard.getOwnerId());
            objectMapper.writerFor(Leaderboard.class).writeValue(leaderboardSaveFile, currentLeaderboard);
        } catch (IOException e) {
            log.error("could not save leaderboardChange", e);
        }
    }

    private LeaderboardChange loadLeaderboardChange(int year, final long leaderboardId) {
        File leaderboardSaveFile = leaderboardSaveFile(year, leaderboardId);
        if (!leaderboardSaveFile.exists()) {
            log.info("loadLeaderboardChange file doesn't exists: " + leaderboardSaveFile.getAbsolutePath());
            Leaderboard currentLeaderboard = new Leaderboard();
            currentLeaderboard.setEvent(year);
            currentLeaderboard.setOwnerId(leaderboardId);
            return new LeaderboardChange(null, currentLeaderboard);
        }

        try {
            Leaderboard currentLeaderboard = objectMapper.readerFor(Leaderboard.class).readValue(leaderboardSaveFile);
            return new LeaderboardChange(currentLeaderboard, currentLeaderboard);
        } catch (IOException e) {
            throw new RuntimeException("could not load LeaderboardChange", e);
        }
    }

    private File leaderboardSaveFile(int year, long leaderboardId) {
        File leaderboardSaveFile = new File("aocLeaderboards/AocLeaderboard_" + year + "_" + leaderboardId + ".json");
        leaderboardSaveFile.getParentFile().mkdirs();
        return leaderboardSaveFile;
    }


    private LocalDate currentAocDate() {
        LocalDate now = LocalDate.now();
        LocalDate thisYearAocStartDate = LocalDate.of(now.getYear(), Month.DECEMBER, 1);
        if (now.isBefore(thisYearAocStartDate)) {
            thisYearAocStartDate = thisYearAocStartDate.minusYears(1);
        }
        return thisYearAocStartDate;
    }


    @Inject
    public void setMessageServices(List<LeaderboardMessageService> messageServices) {
        this.messageServices = requireNonNull(messageServices);
    }

}
