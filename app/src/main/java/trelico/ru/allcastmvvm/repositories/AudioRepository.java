package trelico.ru.allcastmvvm.repositories;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import okhttp3.ResponseBody;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.data_sources.local.AudioPOJODao;
import trelico.ru.allcastmvvm.data_sources.local.FileSavingCallback;
import trelico.ru.allcastmvvm.data_sources.local.FileStorage;
import trelico.ru.allcastmvvm.data_sources.remote.AudioRequestBody;
import trelico.ru.allcastmvvm.data_sources.remote.AudioWebAPI;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState;
import trelico.ru.allcastmvvm.utils.AndroidUtils;
import trelico.ru.allcastmvvm.utils.HashUtils;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.DEFAULT_EMOTION;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.APP_CONTENT_SOURCE;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.LINK_CONTENT_SOURCE;

public class AudioRepository{

    private static AudioRepository instance;
    private AudioWebAPI audioWebAPI;
    private AudioPOJODao audioPOJODao;
    private FileStorage fileStorage;
    private MutableLiveData<RequestState> requestStateLiveData;
    private static final int DEFAULT_TEXT_LENGTH_LIMIT = 1000;

    private AudioRepository(){
        audioWebAPI = AudioWebAPI.getInstance();
        AppDatabase appDatabase = MyApp.getAppDatabase();
        audioPOJODao = appDatabase.audioPOJODao();
        fileStorage = new FileStorage();
    }

    public static AudioRepository getInstance(){
        if(instance == null) instance = new AudioRepository();
        return instance;
    }

    public AudioResponse getTTSAudio(@NonNull String text, @Nullable String linkToSource){
        requestStateLiveData = new MutableLiveData<>();
        requestStateLiveData.setValue(RequestState.LOADING);
        String hash = HashUtils.getHash(text);
        if(audioPOJODao.getByHash(hash).getValue() == null){
            Log.d(D_TAG, "getTTSAudio in AudioRepository. Value is null");
            sendTTSWebRequest(text, linkToSource, hash);
        }
        return new AudioResponse(requestStateLiveData, audioPOJODao.getByHash(hash));
    }

    private void sendTTSWebRequest(String text, String linkToSource, String hash){
        ArrayList<String> splittedText = splitTextByLengthLimit(text, DEFAULT_TEXT_LENGTH_LIMIT);
        ArrayList<Observable<ResponseBody>> requests = new ArrayList<>();
        for(String textPart : splittedText){
            requests.add(audioWebAPI.getTTSRawAudioString(new AudioRequestBody(textPart), DEFAULT_EMOTION));
        }
        Observable<ResponseBody> responses = Observable.concat(requests);
        responses.subscribeOn(Schedulers.io())
                .subscribe(getTTSWebObserver(text, linkToSource, hash));
    }

    private Observer<ResponseBody> getTTSWebObserver(@NonNull String text,
                                                     @Nullable String linkToSource,
                                                     @NonNull String hash){
        return new Observer<ResponseBody>(){

            private AudioPOJO audioPOJO = new AudioPOJO();
            private ArrayList<String> uris = new ArrayList<>();

            @Override
            public void onSubscribe(Disposable d){
                Log.d(D_TAG, "onSubscribe in AudioRepo");
            }

            @SuppressLint("CheckResult")
            @Override
            public void onNext(ResponseBody response){
                Log.d(D_TAG, "onNext in AudioRepo");
                requestStateLiveData.postValue(RequestState.IN_PROGRESS);
                String contentSource = linkToSource == null ? APP_CONTENT_SOURCE : LINK_CONTENT_SOURCE;
                String fileUri = AndroidUtils.getAudioFilesDir() + File.separator + hash;
                fileStorage.saveAudioFile(
                        response,
                        fileUri,
                        isSuccessful -> {
                            if(!isSuccessful){
                                Log.d(D_TAG, "!isSuccessful in AudioRepo");
                                requestStateLiveData.postValue(RequestState.ERROR_LOCAL);
                            }
                            else{
                                Log.d(D_TAG, "isSuccessful in AudioRepo");
                                AudioPOJO audioPOJO = new AudioPOJO(hash, contentSource, text, fileUri, linkToSource);
                                Log.d(D_TAG, "AudioRepo. audioPOJO = " + audioPOJO.toString());
                                audioPOJODao.insert(audioPOJO);

                            }
                        });
            }

            @Override
            public void onError(Throwable e){
                requestStateLiveData.postValue(RequestState.ERROR_WEB);
                Log.d(D_TAG, "onError in AudioRepo. Error = " + e.getMessage());
                e.printStackTrace();
            }

            @Override
            public void onComplete(){
                requestStateLiveData.postValue(RequestState.SUCCESS);
                Log.d(D_TAG, "onComplete in AudioRepo");
            }
        };
    }

    private ArrayList<String> splitTextByLengthLimit(String text, int textLengthLimit){
        ArrayList<String> textsSplitByCharLimit = new ArrayList<>();
        for(int i = 0; i < text.length(); i += textLengthLimit){
            textsSplitByCharLimit.add(text.substring(i, Math.min(i + textLengthLimit, text.length())));
        }
        return textsSplitByCharLimit;

    }

}
