package trelico.ru.allcastmvvm.repositories.tts;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.room.EmptyResultSetException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import okhttp3.ResponseBody;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.data_sources.local.AudioPOJODao;
import trelico.ru.allcastmvvm.data_sources.local.FileSavingCallback;
import trelico.ru.allcastmvvm.data_sources.local.FileStorage;
import trelico.ru.allcastmvvm.data_sources.remote.AudioRequestBody;
import trelico.ru.allcastmvvm.data_sources.remote.AudioWebAPI;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState;
import trelico.ru.allcastmvvm.utils.AndroidUtils;
import trelico.ru.allcastmvvm.utils.ConnectionMonitor;
import trelico.ru.allcastmvvm.utils.HashUtils;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.MyApp.E_TAG;
import static trelico.ru.allcastmvvm.MyApp.I_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.DEFAULT_EMOTION;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.APP_CONTENT_SOURCE;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.LINK_CONTENT_SOURCE;

public class TTSRepositoryImpl implements TTSRepository{

    private static TTSRepositoryImpl instance;
    private AudioWebAPI audioWebAPI;
    private AudioPOJODao audioPOJODao;
    private FileStorage fileStorage;
    private ConnectionMonitor connectionMonitor;
    private MutableLiveData<RequestState> requestStateLiveData = new MutableLiveData<>();
    private TTSResponseContainer ttsResponseContainer = new TTSResponseContainer(requestStateLiveData);
    private static final int DEFAULT_TEXT_LENGTH_LIMIT = 1000;
    private static final int MAX_TEXT_LENGTH_LIMIT = 5000;

    private TTSRepositoryImpl(){
        audioWebAPI = AudioWebAPI.getInstance();
        AppDatabase appDatabase = MyApp.getAppDatabase();
        audioPOJODao = appDatabase.audioPOJODao();
        fileStorage = new FileStorage();
        connectionMonitor = MyApp.getConnectionMonitor();
    }

    public static TTSRepositoryImpl getInstance(){
        if(instance == null) instance = new TTSRepositoryImpl();
        return instance;
    }

    @Override
    public void createTTS(@NonNull String text, @Nullable String linkToSource){
        String hash = HashUtils.getHash(text);
        audioPOJODao.getByHashSingle(hash)
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleObserver<TTSPOJO>(){
                    @Override
                    public void onSubscribe(Disposable d){
                        requestStateLiveData.postValue(RequestState.LOADING);
                    }

                    @Override
                    public void onSuccess(TTSPOJO ttspojo){
                        requestStateLiveData.postValue(RequestState.SUCCESS);
                    }

                    @Override
                    public void onError(Throwable e){
                        if(e instanceof EmptyResultSetException){
                            Log.i(I_TAG, "There is no such audioPOJO in DB");
                            sendWebCreateRequest(text, linkToSource);
                        }
                    }
                });
        ttsResponseContainer.setResponseObservable(audioPOJODao.getByHashObservable(hash));
    }

    @Override
    public void updateExistingTTS(String text){

    }

    @Override
    public TTSResponseContainer subscribeCurrentTTSUpdates(){
        return ttsResponseContainer;
    }


    private void sendWebCreateRequest(@NonNull String text, @Nullable String linkToSource){
        String hash = HashUtils.getHash(text);
        TTSPOJO ttspojo = new TTSPOJO();
        ttspojo.setHash(hash);
        ttspojo.setLinkToSource(linkToSource);
        if(linkToSource != null && !linkToSource.isEmpty()) ttspojo.setContentSource(LINK_CONTENT_SOURCE);
        else ttspojo.setContentSource(APP_CONTENT_SOURCE);
        splitTextByLengthLimitRx(text, DEFAULT_TEXT_LENGTH_LIMIT)
                .flatMap(splittedText -> {
                    Log.d(D_TAG, "Step one in repo");
                    ttspojo.getTexts().add(splittedText);
                    return audioWebAPI
                            .getTTSRawAudioString(new AudioRequestBody(splittedText), DEFAULT_EMOTION);
                })
                .flatMap(responseBody ->{
                    Log.d(D_TAG, "Step two in repo");
                    String fileUri = AndroidUtils.getAudioFilesDir() + File.separator + hash;
                    return saveTTS(responseBody, fileUri);
                })
                .flatMap(uri -> {
                    Log.d(D_TAG, "Step three in repo");
                    ttspojo.getUris().add(uri);
                    return null;
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Object>(){
                    @Override
                    public void onSubscribe(Disposable d){
                        Log.d(D_TAG, "onSubscribe in repo");
                        requestStateLiveData.postValue(RequestState.LOADING);
                    }

                    @Override
                    public void onNext(Object o){
                        Log.d(D_TAG, "onNext in repo");
                        requestStateLiveData.postValue(RequestState.IN_PROGRESS);
                    }

                    @Override
                    public void onError(Throwable e){
                        Log.d(D_TAG, "onError in repo");
                        if(e instanceof IOException) requestStateLiveData.postValue(RequestState.ERROR_LOCAL);
                        else requestStateLiveData.postValue(RequestState.ERROR_WEB);
                        if(!ttspojo.getUris().isEmpty() && !ttspojo.getTexts().isEmpty())
                            audioPOJODao.insert(ttspojo);

                    }

                    @Override
                    public void onComplete(){
                        Log.d(D_TAG, "onComplete in repo");
                        requestStateLiveData.postValue(RequestState.SUCCESS);
                        audioPOJODao.insert(ttspojo);
                    }
                });
    }

    /**
     *
     * @param uri - uri to add to POJO's arrayList
     * @param ttspojo - POJO to save
     * @return observable that shows that update is completed
     */
    private Observable<Boolean> updateTTSPOJO(String uri, TTSPOJO ttspojo){
        PublishSubject<Boolean> completedObservable = PublishSubject.create();
        ttspojo.getUris().add(uri);
        audioPOJODao.update(ttspojo);
        completedObservable.onNext(true);
        return completedObservable;
    }

    /**
     *  This method should not be called from main thread
     * @param responseBodies - array of bodies to save (must be an audio file)
     * @param fileNames - names of the saved files
     * @return observable that emit single uri string with path to saved audio file
     */
    private Observable<String> saveManyTTS(ArrayList<ResponseBody> responseBodies,
                                           ArrayList<String> fileNames){
        PublishSubject<String> uriObservable = PublishSubject.create();
        for(int i = 0; i < fileNames.size(); i++){
            String fileName = fileNames.get(i);
            ResponseBody responseBody = responseBodies.get(i);
            fileStorage.saveAudioFile(responseBody, fileName, isSuccessful -> {
                if(isSuccessful) uriObservable.onNext(fileName);
                if(!isSuccessful) uriObservable.onError(new IOException());
            });
        }
        return uriObservable;
    }

    /**
     *  This method should not be called from main thread
     * @param responseBody - body to save (must be an audio file)
     * @param fileName - name of the saved file
     * @return observable that emit single uri string with path to saved audio file
     */
    private ObservableSource<String> saveTTS(ResponseBody responseBody,
                                       String fileName){
        Log.d(D_TAG, "saveTTS in repo. Step 1");
        PublishSubject<String> uriObservable = PublishSubject.create();
        Log.d(D_TAG, "saveTTS in repo. Step 2");
        fileStorage.saveAudioFile(responseBody, fileName, isSuccessful -> {
                if(isSuccessful){
                    uriObservable.onNext(fileName);
                    Log.d(D_TAG, "saveTTS in repo. Step 3");
                }
                if(!isSuccessful){
                    uriObservable.onError(new IOException());
                    Log.d(D_TAG, "saveTTS in repo. Step 4");
                }
        });
        return uriObservable;
    }

    private Observable<String> splitTextByLengthLimitRx(String text, int textLengthLimit){
        ReplaySubject<String> texts = ReplaySubject.create();
        for(int i = 0; i < text.length(); i += textLengthLimit){
            texts.onNext(text.substring(i, Math.min(i + textLengthLimit, text.length())));
        }
        return texts;
    }

    private ArrayList<String> splitTextByLengthLimit(String text, int textLengthLimit){
        ArrayList<String> textsSplitByCharLimit = new ArrayList<>();
        for(int i = 0; i < text.length(); i += textLengthLimit){
            textsSplitByCharLimit.add(text.substring(i, Math.min(i + textLengthLimit, text.length())));
        }
        return textsSplitByCharLimit;

    }
}
