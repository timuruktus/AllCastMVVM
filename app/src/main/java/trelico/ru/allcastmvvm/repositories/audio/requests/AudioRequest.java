package trelico.ru.allcastmvvm.repositories.audio.requests;

import trelico.ru.allcastmvvm.repositories.audio.AudioRepository;
import trelico.ru.allcastmvvm.repositories.audio.AudioRequestExecutor;

public interface AudioRequest{

    void executeRequest();
    void injectRepository(AudioRequestExecutor audioRequestExecutor);
}
