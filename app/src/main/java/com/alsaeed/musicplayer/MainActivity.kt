package com.alsaeed.musicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alsaeed.musicplayer.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Serializable


class MainActivity : AppCompatActivity(), ServiceConnection {
    private var musicService: MusicService? = null
    private var isServiceBound = false
    private lateinit var binding: ActivityMainBinding
    private val songsList: MutableList<AudioModel> = mutableListOf()
    private lateinit var adapter: MusicAdapter
    private var isPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = Color.WHITE

        if (!checkPermission()) {
            requestPermission()

        } else {
            initializeApp()
        }

    }


    private fun initializeApp() {
        // Launch coroutine on Main thread
        CoroutineScope(Dispatchers.Main).launch {

            // Show progress bar
            binding.progressBar.visibility = View.VISIBLE

            val songs = withContext(Dispatchers.IO) {
                val songs = ArrayList<AudioModel>()
                val projection = arrayOf(
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
                )
                val selection = MediaStore.Audio.Media.IS_MUSIC + " !=0"

                contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val audioModel = AudioModel(
                            cursor.getString(1),
                            cursor.getString(0),
                            cursor.getString(2)
                        )
                        if (File(audioModel.path).exists()) {
                            songs.add(audioModel)
                        }
                    }
                }
                songs
            }

            // Hide progress bar
            binding.progressBar.visibility = View.GONE

            songsList.clear()
            songsList.addAll(songs)

            adapter = MusicAdapter(this@MainActivity, songsList) { position ->
                if (isPlaying) {
                    musicService?.musicPlay(songsList)
                    binding.songTitle.text = songsList[MyMediaPlayer.currentIndex].title
                    adapter.notifyDataSetChanged()
                } else {

                    if (songsList.isNotEmpty()) {
                        val intent = Intent(this@MainActivity, MusicPlayerActivity::class.java)
                        intent.putExtra("List", songsList as Serializable)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@MainActivity, "No songs to play", Toast.LENGTH_SHORT)
                            .show()
                    }

                }
            }

            binding.recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
            binding.recyclerView.adapter = adapter
            binding.noSongFound.visibility = if (songsList.isEmpty()) View.VISIBLE else View.GONE

            setupControls()
        }
    }


    private fun setupControls() {
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, Context.BIND_AUTO_CREATE)

        binding.pausePlay.setOnClickListener {
            musicService?.getMusicPlayer()?.let {
                if (it.isPlaying) {
                    it.pause()
                    binding.pausePlay.setImageResource(R.drawable.play_ic)
                } else {
                    it.start()
                    binding.pausePlay.setImageResource(R.drawable.pause_ic)
                }
            }
        }



        binding.next.setOnClickListener { musicNext() }
        binding.previous.setOnClickListener { musicPrevious() }

        binding.controls.setOnClickListener {
            val intent1 = Intent(this@MainActivity, MusicPlayerActivity::class.java)
            intent1.putExtra("IS", true)
            intent1.putExtra("List", songsList as Serializable)
            startActivity(intent1)
        }
    }


    private fun musicNext() {

        musicService?.next()
        binding.songTitle.text = songsList[MyMediaPlayer.currentIndex].title
        adapter.notifyDataSetChanged()

    }


    private fun musicPrevious() {
        musicService?.previous()
        binding.songTitle.text = songsList[MyMediaPlayer.currentIndex].title
        adapter.notifyDataSetChanged()

    }


    private fun checkPermission(): Boolean {

        val result = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (result == PackageManager.PERMISSION_GRANTED) {
            return true

        } else {
            return false
        }
    }


    private fun requestPermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+ (Tiramisu), use READ_MEDIA_AUDIO instead
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                92
            )
        } else {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(permission),
                92
            )

        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 92 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
            initializeApp() // Or load your music files
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            requestPermission()
        }
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        val binder = service as MusicService.MyBinder
        musicService = binder.currentService()
        isServiceBound = true
        musicService!!.setSongLIst(songsList)

        // Update the controls based on the current state of the media player
        updateControls()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
        isServiceBound = false
    }


    private fun updateControls() {
        musicService?.getMusicPlayer()?.let { mediaPlayer ->
            if (mediaPlayer.isPlaying) {
                binding.controls.visibility = View.VISIBLE
                binding.songTitle.text = songsList[MyMediaPlayer.currentIndex].title
                binding.pausePlay.setImageResource(R.drawable.pause_ic)
                isPlaying = true
            } else {
                isPlaying = false
                binding.controls.visibility = View.GONE
            }

            mediaPlayer.setOnCompletionListener {
                musicNext() // Move to the next song when the current song is completed.
            }
        }

    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
            updateControls()
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            unbindService(this)
            isServiceBound = false
        }
    }
}