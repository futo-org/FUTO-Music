package com.futo.music.logic.shuffles

import com.futo.music.states.StateDatabase

class PriortiyScoreSmartShuffle(scores: ScoreContainer): SmartShuffle(scores) {
    override fun getTrackIds(count: Int): List<Long> {
        return StateDatabase.instance.getTrackIdsWeighted(count);
    }
}