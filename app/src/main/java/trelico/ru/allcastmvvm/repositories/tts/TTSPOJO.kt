package trelico.ru.allcastmvvm.repositories.tts

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TTSPOJO(@PrimaryKey var hash: String = "",
                   var text: String = "",
                   var durations: ArrayList<Long> = ArrayList(),
                   var texts: ArrayList<String> = ArrayList(),
                   var uris: ArrayList<String> = ArrayList(),
                   var linkToSource: String? = "") {
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