package trelico.ru.allcastmvvm.repositories.tts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.reactivex.Observable;


/**
 * External API of TTSRepositoryImpl class
 */
public interface TTSRepository{

    /**
     * INNER API
     */
    void createTTS(@NonNull String text, @Nullable String linkToSource);
    void updateExistingTTS(String text);

    /**
     * OUTER API
     */
    TTSResponseContainer subscribeCurrentTTSUpdates();
}
