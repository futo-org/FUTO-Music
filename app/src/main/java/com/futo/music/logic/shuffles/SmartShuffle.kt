package com.futo.music.logic.shuffles

import com.futo.music.states.StateDatabase
import com.futo.music.storage.db.DBTrack

abstract class SmartShuffle(val scoresRoot: ScoreContainer) {
    val random = java.util.Random();

    abstract fun getTrackIds(count: Int): List<Long>;

    fun getTracks(count: Int): List<DBTrack> {
        val ids = getTrackIds(count);
        val tracks = StateDatabase.instance.getTracks(ids);
        return tracks.sortedBy { ids.indexOf(it.id) };
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
}