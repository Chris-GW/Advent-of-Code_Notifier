package de.adventofcode.chrisgw.notifier.service;

import de.adventofcode.chrisgw.notifier.model.LeaderboardChange;


public interface LeaderboardMessageService {

    void sendLeaderboardChangeMessage(LeaderboardChange leaderboardChange);

}
