package com.example.wordgame.gamemode;

import com.example.wordgame.R;
import com.example.wordgame.models.Board;
import com.example.wordgame.models.HighScoreData;
import com.example.wordgame.gamemode.models.WordFoundReward;

/**
 * Rational game mode. Each word provides the same number of points.
 */
public class GameModeRational extends GameMode {
    private static final GameModeType gameModeType = GameModeType.RATIONAL;
    private static final int SCORE_PER_WORD = 1;

    public GameModeRational(Board board) {
        super(board);
    }

    @Override
    public GameModeType getGameModeType() {
        return gameModeType;
    }

    @Override
    public int getMaxScore() {
        return gameBoard.getWords().size() * SCORE_PER_WORD;
    }

    @Override
    public WordFoundReward getWordReward(String word) {
        return new WordFoundReward(SCORE_PER_WORD, 0, R.raw.snd_found_short);
    }

    @Override
    public HighScoreData getHighScoreData() {
        return new HighScoreData();
    }
}
