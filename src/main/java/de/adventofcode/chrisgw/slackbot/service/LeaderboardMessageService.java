package de.adventofcode.chrisgw.slackbot.service;

import de.adventofcode.chrisgw.slackbot.model.LeaderboardChange;


public interface LeaderboardMessageService {

    void sendLeaderboardChangeMessage(LeaderboardChange leaderboardChange);

}
