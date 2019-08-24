package trelico.ru.allcastmvvm.repositories

import androidx.room.Entity
import androidx.room.PrimaryKey
import trelico.ru.allcastmvvm.screens.player.PlayerActivity.APP_CONTENT_SOURCE

@Entity
data class AudioPOJO(@PrimaryKey val hash : String = "",
                     val contentSource : String = APP_CONTENT_SOURCE,
                     val texts : ArrayList<String> = ArrayList(),
                     val uris : ArrayList<String> = ArrayList(),
                     val linkToSource : String? = "")