package de.adventofcode.chrisgw.slackbot.service;

import de.adventofcode.chrisgw.slackbot.model.Leaderboard;


public interface LeaderboardMessageService {

    void sendNewLeaderboardMessage(Leaderboard leaderboard);
}
