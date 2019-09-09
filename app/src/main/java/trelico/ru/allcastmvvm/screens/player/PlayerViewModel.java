package trelico.ru.allcastmvvm.screens.player;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService;
import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;
import trelico.ru.allcastmvvm.repositories.tts.TTSRepository;
import trelico.ru.allcastmvvm.repositories.tts.TTSRepositoryImpl;
import trelico.ru.allcastmvvm.repositories.tts.TTSResponseContainer;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.APP_CONTENT_SOURCE;

public class PlayerViewModel extends ViewModel{


    private LinkParser linkParser = new LinkParser();

    private LiveData<NetworkService.RequestState> requestStateLiveData;
    private MutableLiveData<TTSPOJO> ttsLiveData = new MutableLiveData<>();
    private TTSResponseContainer ttsResponseContainer;
    private TTSRepository ttsRepository = TTSRepositoryImpl.getInstance();

    private String savedText;
    private String savedLinkToSource;

    void sendAudioRequest(String text, String linkToSource){
        if(!text.equals(savedText)) sendNewAudioRequest(text, linkToSource);
    }

    void sendNewAudioRequest(String text, String linkToSource){
        savedText = text;
        savedLinkToSource = linkToSource;
        ttsResponseContainer = ttsRepository.subscribeCurrentTTSUpdates();
        requestStateLiveData = ttsResponseContainer.getRequestStateLiveData();
        ttsRepository.createTTS(text, linkToSource);
        subscribeToResponse();
    }

    void updateExistingTTS(String text){
        ttsRepository.updateExistingTTS(text);
    }

    public LiveData<NetworkService.RequestState> getRequestStateLiveData(){
        return requestStateLiveData;
    }

    public LiveData<TTSPOJO> getTtsLiveData(){
        return ttsLiveData;
    }

    private void subscribeToResponse(){
        ttsResponseContainer.getResponseObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TTSPOJO>(){
                    @Override
                    public void onSubscribe(Disposable d){
                        Log.d(D_TAG, "onSubscribe in PlayerViewModel");
                    }

                    @Override
                    public void onNext(TTSPOJO ttspojo){
                        Log.d(D_TAG, "onNext in PlayerViewModel");
                        ttsLiveData.setValue(ttspojo);
                    }

                    @Override
                    public void onError(Throwable e){
                        Log.d(D_TAG, "onError in PlayerViewModel");
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete(){
                        Log.d(D_TAG, "onComplete in PlayerViewModel");
                    }
                });
    }

    LiveData<String> parseContentIfNeeded(String content, String contentSource){
        MutableLiveData<String> parsedContent = new MutableLiveData<>();
        if(contentSource != null && !contentSource.isEmpty() && contentSource.equals(APP_CONTENT_SOURCE))
            parsedContent.setValue(content); //Cause content - already parsed texts
        else if((contentSource == null || contentSource.isEmpty()) && content.startsWith("http"))
            linkParser.stub();
        else return linkParser.stub(); //TODO: remove stub
        return parsedContent;
    }
}
