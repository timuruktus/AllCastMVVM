package trelico.ru.allcastmvvm.repositories.audio;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import trelico.ru.allcastmvvm.repositories.audio.requests.AudioRequest;
import trelico.ru.allcastmvvm.repositories.audio.requests.PodcastRequest;
import trelico.ru.allcastmvvm.repositories.audio.requests.TTSRequest;


/**
 * External API of AudioRepositoryImpl class
 */
public interface AudioRepository{



    /**
     * INNER API
     */
    void replayLastRequest();
    void sendRequest(AudioRequest audioRequest);


    /**
     * OUTER API
     */
    AudioResponseContainer subscribeCurrentAudioUpdates();
}
