package com.futo.music.logic.shuffles

import com.futo.music.states.StateDatabase

class PriortiyScoreSmartShuffle(scores: ScoreContainer): SmartShuffle(scores) {
    override fun getTrackIds(count: Int): List<RecommendationTrack> {
        return StateDatabase.instance.getTrackIdsWeighted(count).map { RecommendationTrack(it, "") };
    }
}