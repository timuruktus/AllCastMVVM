package trelico.ru.allcastmvvm.repositories.tts

import androidx.room.Entity
import androidx.room.PrimaryKey
import trelico.ru.allcastmvvm.screens.player.PlayerActivity.APP_CONTENT_SOURCE

@Entity
data class TTSPOJO(@PrimaryKey var hash : String = "",
                   var contentSource : String = APP_CONTENT_SOURCE,
                   var texts : ArrayList<String> = ArrayList(),
                   var uris : ArrayList<String> = ArrayList(),
                   var linkToSource : String? = ""){
    fun isFullyLoaded() : Boolean{
        return texts.size == uris.size
    }
}