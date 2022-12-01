package de.adventofcode.chrisgw.aoc.notifier.service;

import de.adventofcode.chrisgw.aoc.notifier.model.LeaderboardChange;


public interface LeaderboardMessageService {

    void sendLeaderboardChangeMessage(LeaderboardChange leaderboardChange);

}
