package trelico.ru.allcastmvvm.repositories.audio;

import trelico.ru.allcastmvvm.repositories.audio.requests.PodcastRequest;
import trelico.ru.allcastmvvm.repositories.audio.requests.TTSRequest;

public interface AudioRequestExecutor{


    void createTTS(TTSRequest ttsRequest);
    void sendPodcastRequest(PodcastRequest podcastRequest);
}
