package com.futo.music.logic.shuffles

import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBTrack

abstract class SmartShuffle(val scoresRoot: ScoreContainer) {
    val random = java.util.Random();

    abstract fun getTrackIds(count: Int): List<RecommendationTrack>;

    fun getTracks(count: Int): List<DBTrack> {
        val ids = getTrackIds(count);
        return StateDatabase.instance.getTracks(ids.map { it.id });
    }
    fun getTracksWithReason(count: Int): Pair<List<DBTrack>, List<String?>> {
        val ids = getTrackIds(count);
        var reasonMap = mutableListOf<String>()
        for(id in ids)
            reasonMap.add(id.reason);

        val foundTracks = StateDatabase.instance.getTracks(ids.map { it.id }).groupBy { it.id };

        return Pair(ids.mapNotNull { foundTracks.getOrDefault(it.id, null)?.firstOrNull() }, reasonMap);
    }


    fun selectScore(chance1: Int, chance2: Int, chance3: Int, chance4: Int, chance5: Int): Int {
        val total = chance1 + chance2 + chance3 + chance4 + chance5;
        val index = random.nextInt(total);

        if(chance1 > 0 && index < chance1)
            return starToScore(1);
        if(chance2 > 0 && index < chance2 + chance1)
            return starToScore(2);
        if(chance3 > 0 && index < chance3 + chance2 + chance1)
            return starToScore(3);
        if(chance4 > 0 && index < chance4 + chance3 + chance2 + chance1)
            return starToScore(4);
        return starToScore(5);
    }

    fun starToScore(star: Int): Int {
        return when(star) {
            0 -> 0
            1 -> 20
            2 -> 40
            3 -> 60
            4 -> 80
            5 -> 100
            else -> 0
        }
    }
    fun scoreToStar(score: Int): Int {
        return when(score) {
            0 -> 0
            20 -> 1
            40 -> 2
            60 -> 3
            80 -> 4
            100 -> 5
            else -> 0
        }
    }

    open class RecommendationTrack(
        val id: Long,
        val reason: String
    )
}