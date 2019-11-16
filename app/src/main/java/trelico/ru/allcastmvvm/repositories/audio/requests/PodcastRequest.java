package trelico.ru.allcastmvvm.repositories.audio.requests;

import trelico.ru.allcastmvvm.repositories.audio.AudioRepository;
import trelico.ru.allcastmvvm.repositories.audio.AudioRequestExecutor;

public class PodcastRequest implements AudioRequest{

    private String id;
    private AudioRequestExecutor audioRepository;

    @Override
    public void executeRequest(){
        audioRepository.sendPodcastRequest(this);
    }

    public PodcastRequest(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    @Override
    public void injectRepository(AudioRequestExecutor audioRequestExecutor){
        this.audioRepository = audioRequestExecutor;
    }
}
