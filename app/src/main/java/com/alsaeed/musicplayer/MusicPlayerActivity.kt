package com.alsaeed.musicplayer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alsaeed.musicplayer.databinding.ActivityMusicPlayerBinding
import java.io.Serializable
import java.util.Locale
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION")
class MusicPlayerActivity : AppCompatActivity(), ServiceConnection {
    private lateinit var binding: ActivityMusicPlayerBinding
    private lateinit var songsList: List<AudioModel>
    private lateinit var currentSong: AudioModel


    private var mediaPlayer: MediaPlayer? = null

    private var x = 0
    private var isAlreadyPlaying = false
    private var musicService: MusicService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)


        Log.d("BBBBBB", "onCreate: ")

        window.statusBarColor = Color.parseColor("#DC3500")
        binding.songTitle.isSelected = true

        isAlreadyPlaying = intent.getBooleanExtra("IS", false)
        @Suppress("UNCHECKED_CAST")
        songsList = intent.getSerializableExtra("List") as List<AudioModel>


        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)


        // Set the current song
        if (savedInstanceState == null && isAlreadyPlaying) {
            currentSong = songsList[MyMediaPlayer.currentIndex]
        } else if (savedInstanceState != null) {
            // Restore the state if the activity is being recreated
            songsList = savedInstanceState.getSerializable("Li") as List<AudioModel>
            currentSong = songsList[MyMediaPlayer.currentIndex]
        }


    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("Li", songsList as Serializable)
    }


    private fun updateUI() {
        runOnUiThread {
            musicService?.let {
                val mediaPlayer = it.getMusicPlayer()
                binding.seekBar.progress = mediaPlayer?.currentPosition ?: 0
                binding.currentTime.text = convertToMMSS("${mediaPlayer?.currentPosition}")

                if (mediaPlayer?.isPlaying == true) {
                    binding.pausePlay.setImageResource(R.drawable.pause_ic)
                } else {
                    binding.pausePlay.setImageResource(R.drawable.play_ic)
                }
            }
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        musicService?.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            Handler().postDelayed({
                updateUI()
            }, 100)
        }
    }


    private fun setResMusicSong() {
        currentSong = songsList[MyMediaPlayer.currentIndex]

        binding.songTitle.text = currentSong.title
        binding.totalTime.text = convertToMMSS(currentSong.duration)

        binding.pausePlay.setOnClickListener { pausePlay() }
        binding.next.setOnClickListener { musicNext() }
        binding.previous.setOnClickListener { musicPrevious() }

        musicPlay()
    }


    private fun musicPlay() {
        if (!isAlreadyPlaying) {

            musicService!!.musicPlay(songsList)
            binding.seekBar.progress = 0
            binding.seekBar.max = musicService!!.getMusicPlayer()!!.duration
        } else {
            binding.seekBar.progress = musicService!!.getMusicPlayer()!!.currentPosition
            binding.seekBar.max = mediaPlayer!!.duration
        }

        mediaPlayer?.setOnCompletionListener {
            musicNext() // Move to the next song when the current song is completed.
        }

    }


    private fun musicNext() {

        musicService?.next()
        isAlreadyPlaying = musicService!!.getPlayingStatus()
        setResMusicSong()
    }

    private fun musicPrevious() {
        musicService?.previous()
        isAlreadyPlaying = musicService!!.getPlayingStatus()
        setResMusicSong()
    }

    private fun pausePlay() {
        if (musicService?.isPlaying() == true) {
            musicService?.pause()
        } else {
            //   musicService?.play(currentSong.path)
            musicService!!.getMusicPlayer()!!.start()
        }
    }


    private fun convertToMMSS(duration: String): String {
        val millis = duration.toLongOrNull() ?: return "00:00"
        return String.format(
            Locale.getDefault(), "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)
        )
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val iBinder = service as MusicService.MyBinder
        musicService = iBinder.currentService()
        mediaPlayer = musicService!!.getMusicPlayer()
        musicService!!.setSongLIst(songsList)
        Log.d("BBBBBB", "onServiceConnected: ")

        // Initialize the UI and start the playback
        setResMusicSong()
        updateUI()
        Log.d("BBBBBB", "onServiceConnected: ")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }


}