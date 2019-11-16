package trelico.ru.allcastmvvm.services

import androidx.room.Entity
import androidx.room.PrimaryKey
import trelico.ru.allcastmvvm.repositories.audio.AudioResponse

@Entity
data class TTS(@PrimaryKey var hash: String = "",
               var text: String = "",
               var durations: ArrayList<Long> = ArrayList(),
               var texts: ArrayList<String> = ArrayList(),
               var uris: ArrayList<String> = ArrayList(),
               var linkToSource: String? = ""): AudioResponse {
    override fun getIdentificator(): String {
        return hash;
    }

    fun isFullyLoaded(): Boolean {
        return texts.size == uris.size
    }

    fun hasLinkSource(): Boolean{
        return linkToSource != null
    }

    fun getDownloadedDuration(): Long{
        return durations.sum()
    }
}