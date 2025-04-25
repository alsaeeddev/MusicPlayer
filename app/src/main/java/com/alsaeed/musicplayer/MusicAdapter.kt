package com.alsaeed.musicplayer

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.alsaeed.musicplayer.databinding.SongItemBinding


class MusicAdapter(
    private val context: Context,
    private val songList: List<AudioModel>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<MusicAdapter.ItemHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        val view = SongItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ItemHolder(view)
    }

    override fun getItemCount(): Int {
        return songList.size
    }


    public fun getSongLIst(): List<AudioModel> {
        return songList
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        val audioSong = songList[position]
        holder.bind(audioSong, position)
    }


    inner class ItemHolder(private val binding: SongItemBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(audioSong: AudioModel, position: Int) {

            if (MyMediaPlayer.currentIndex == position) {
                binding.tvSongTitle.setTextColor(Color.RED)
            } else {
                binding.tvSongTitle.setTextColor(Color.BLACK)
            }
            binding.tvSongTitle.text = audioSong.title
            //   binding.ivMusic.setImageResource(R.drawable.music_ic)

            binding.root.setOnClickListener {
                MyMediaPlayer.getInstance().reset()
                MyMediaPlayer.currentIndex = adapterPosition
                onClick(adapterPosition)


            }

        }

    }
}