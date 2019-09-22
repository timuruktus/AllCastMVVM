package trelico.ru.allcastmvvm.services;

import io.reactivex.Observable;
import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;
import trelico.ru.allcastmvvm.repositories.tts.TTSRepository;
import trelico.ru.allcastmvvm.repositories.tts.TTSRepositoryImpl;
import trelico.ru.allcastmvvm.repositories.tts.TTSResponseContainer;

public class AudioHelper{

    private TTSResponseContainer ttsResponseContainer;
    private TTSRepository ttsRepository;
    private Observable<Integer> requestStateObservable;
    private String savedText;

    public AudioHelper(){
        ttsRepository = TTSRepositoryImpl.getInstance();
        ttsResponseContainer = ttsRepository.subscribeCurrentTTSUpdates();
        requestStateObservable = ttsResponseContainer.getRequestStateObservable();
    }

    protected void sendAudioRequest(String text, String linkToSource){
        if(savedText != null && savedText.equals(text)) return;
        sendNewAudioRequest(text, linkToSource);
    }

    protected void sendNewAudioRequest(String text, String linkToSource){
        ttsRepository.createTTS(text, linkToSource);
        savedText = text;
    }

    public Observable<Integer> getRequestStateObservable(){
        return requestStateObservable;
    }

    public Observable<TTSPOJO> getTTSObservable(){
        return ttsResponseContainer.getResponseObservable();
    }

}
