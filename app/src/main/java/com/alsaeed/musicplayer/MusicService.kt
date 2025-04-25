package com.alsaeed.musicplayer

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log


class MusicService : Service() {

    private var myBinder = MyBinder()
    private var mediaPlayer: MediaPlayer? = MyMediaPlayer.getInstance()
    private var currentSongPath: String? = null
    private var isAlreadyPlaying = false
    private var songsList: List<AudioModel>? = null


    fun setSongLIst(list: List<AudioModel>) {
        songsList = list

    }

    public fun getMusicPlayer(): MediaPlayer? {
        return mediaPlayer
    }


    override fun onBind(intent: Intent?): IBinder? {
        return myBinder

    }


    fun musicPlay(songList: List<AudioModel>) {
        currentSongPath = songList[MyMediaPlayer.currentIndex].path
        mediaPlayer!!.reset()
        mediaPlayer!!.setDataSource(currentSongPath)
        mediaPlayer!!.prepare()
        mediaPlayer!!.start()

    }


    fun pause() {
        mediaPlayer?.pause()
        Log.d("MyService", "Pausing music")
    }

    fun stop() {
        mediaPlayer?.stop()
        Log.d("MyService", "Stopping music")
    }


    fun next() {
        if (MyMediaPlayer.currentIndex == songsList!!.size - 1) {
            return
        }

        MyMediaPlayer.currentIndex += 1
        mediaPlayer!!.reset()
        if (songsList != null) {
            musicPlay(songsList!!)
        }
        isAlreadyPlaying = false


    }


    fun previous() {
        if (MyMediaPlayer.currentIndex == 0) {
            return
        }
        MyMediaPlayer.currentIndex -= 1
        mediaPlayer!!.reset()
        if (songsList != null) {
            musicPlay(songsList!!)
        }
        isAlreadyPlaying = false

    }


    fun getPlayingStatus(): Boolean {
        return isAlreadyPlaying
    }


    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    fun getCurrentPosition(): Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
    }


    override fun onDestroy() {
        super.onDestroy()
    }


    inner class MyBinder : Binder() {
        fun currentService(): MusicService {
            return this@MusicService
        }
    }

}