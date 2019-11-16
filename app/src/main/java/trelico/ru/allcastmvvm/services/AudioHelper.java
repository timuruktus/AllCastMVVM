package trelico.ru.allcastmvvm.services;

import io.reactivex.Observable;
import trelico.ru.allcastmvvm.repositories.audio.AudioRepository;
import trelico.ru.allcastmvvm.repositories.audio.AudioResponse;
import trelico.ru.allcastmvvm.repositories.audio.AudioRepositoryImpl;
import trelico.ru.allcastmvvm.repositories.audio.AudioResponseContainer;
import trelico.ru.allcastmvvm.repositories.audio.requests.AudioRequest;
import trelico.ru.allcastmvvm.repositories.audio.requests.TTSRequest;

public class AudioHelper{

    private AudioResponseContainer audioResponseContainer;
    private AudioRepository audioRepository;
    private Observable<Integer> requestStateObservable;
    private String savedText;

    public AudioHelper(){
        audioRepository = AudioRepositoryImpl.getInstance();
        audioResponseContainer = audioRepository.subscribeCurrentAudioUpdates();
        requestStateObservable = audioResponseContainer.getRequestStateObservable();
    }

    protected void replayLastRequest(){
        audioRepository.replayLastRequest();
    }

    protected void sendAudioRequest(AudioRequest audioRequest){
        audioRepository.sendRequest(audioRequest);
    }

    public Observable<Integer> getRequestStateObservable(){
        return requestStateObservable;
    }

    public Observable<? extends AudioResponse> getTTSObservable(){
        return audioResponseContainer.getResponseObservable();
    }

}
