package com.alsaeed.musicplayer

import android.media.MediaPlayer

object MyMediaPlayer {

    private var mediaPlayer: MediaPlayer? = null

    var currentIndex: Int = -1

    fun getInstance(): MediaPlayer {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        }
        return mediaPlayer!!
    }
}