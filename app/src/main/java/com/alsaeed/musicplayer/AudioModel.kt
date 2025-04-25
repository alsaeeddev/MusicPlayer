package com.alsaeed.musicplayer

import java.io.Serializable

data class AudioModel(
    val path: String = "",
    val title: String = "",
    val duration: String = ""

) : Serializable
