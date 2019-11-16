package trelico.ru.allcastmvvm.repositories.audio;

import java.util.ArrayList;

import trelico.ru.allcastmvvm.repositories.audio.requests.AudioRequest;

public class AudioPlaylist implements AudioResponse{

    private String id;
    private ArrayList<AudioRequest> audioRequests;

    public AudioPlaylist(){
    }

    public AudioPlaylist(String id, ArrayList<AudioRequest> audioRequests){
        this.id = id;
        this.audioRequests = audioRequests;
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    public ArrayList<AudioRequest> getAudioRequests(){
        return audioRequests;
    }

    public void setAudioRequests(ArrayList<AudioRequest> audioRequests){
        this.audioRequests = audioRequests;
    }

    @Override
    public String getIdentificator(){
        return id;
    }
}
