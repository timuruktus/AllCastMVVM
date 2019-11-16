package trelico.ru.allcastmvvm.repositories.audio;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EmptyResultSetException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okhttp3.ResponseBody;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.data_sources.local.AudioPOJODao;
import trelico.ru.allcastmvvm.data_sources.remote.AudioRequestBody;
import trelico.ru.allcastmvvm.data_sources.remote.AudioWebAPI;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState;
import trelico.ru.allcastmvvm.repositories.audio.requests.AudioRequest;
import trelico.ru.allcastmvvm.repositories.audio.requests.PodcastRequest;
import trelico.ru.allcastmvvm.repositories.audio.requests.TTSRequest;
import trelico.ru.allcastmvvm.repositories.local_files.FileStorage;
import trelico.ru.allcastmvvm.services.TTS;
import trelico.ru.allcastmvvm.utils.AndroidUtils;
import trelico.ru.allcastmvvm.utils.StringUtils;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.MyApp.I_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.DEFAULT_EMOTION;

public class AudioRepositoryImpl implements AudioRepository, AudioRequestExecutor{

    private static AudioRepositoryImpl instance;
    private AudioWebAPI audioWebAPI;
    private AudioPOJODao audioPOJODao;
    private FileStorage fileStorage;
    private AudioRequest lastRequest;
    private BehaviorSubject<Integer> requestStateSubject = BehaviorSubject.create();
    private AudioResponseContainer audioResponseContainer = new AudioResponseContainer(requestStateSubject);
    private static final int DEFAULT_TEXT_LENGTH_LIMIT = 1000;
    private static final int MAX_TEXT_LENGTH_LIMIT = 5000;

    private AudioRepositoryImpl(){
        audioWebAPI = AudioWebAPI.getInstance();
        AppDatabase appDatabase = MyApp.INSTANCE.getAppDatabase();
        audioPOJODao = appDatabase.audioPOJODao();
        fileStorage = new FileStorage();
    }

    public static AudioRepositoryImpl getInstance(){
        if(instance == null) instance = new AudioRepositoryImpl();
        return instance;
    }

    @Override
    public void createTTS(TTSRequest ttsRequest){
        String text = ttsRequest.getText();
        String linkToSource = ttsRequest.getLinkToSource();
        String hash = StringUtils.getHash(text);
        Log.d(D_TAG, "createTTS in Repo. hash = " + hash);
        audioPOJODao.getByHashSingle(hash)
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleObserver<TTS>(){
                    @Override
                    public void onSubscribe(Disposable d){
                        requestStateSubject.onNext(RequestState.LOADING);
                    }

                    @Override
                    public void onSuccess(TTS TTS){
                        requestStateSubject.onNext(RequestState.SUCCESS);
                    }

                    @Override
                    public void onError(Throwable e){
                        if(e instanceof EmptyResultSetException){
                            Log.i(I_TAG, "No such audio POJO found in DB");
                            sendTTSWebCreateRequest(text, linkToSource, hash);
                        }
                        e.printStackTrace();
                    }
                });
        audioResponseContainer.setResponseObservable(audioPOJODao.getByHashObservable(hash));
    }

    @Override
    public void sendPodcastRequest(PodcastRequest podcastRequest){
        //TODO
    }

    @Override
    public void replayLastRequest(){
        lastRequest.executeRequest();
    }

    @Override
    public void sendRequest(AudioRequest audioRequest){
        lastRequest = audioRequest;
        audioRequest.injectRepository(this);
        audioRequest.executeRequest();
    }

    @Override
    public AudioResponseContainer subscribeCurrentAudioUpdates(){
        return audioResponseContainer;
    }


    private void sendTTSWebCreateRequest(@NonNull String text, @Nullable String linkToSource,
                                         @NonNull String hash){
        Log.d(D_TAG, "sendTTSWebCreateRequest in Repo. hash = " + hash);
        TTS TTS = new TTS();
        TTS.setHash(hash);
        TTS.setText(text);
        TTS.setLinkToSource(linkToSource);
        TTS.setTexts(splitTextIntoPieces(text, DEFAULT_TEXT_LENGTH_LIMIT));
        audioPOJODao.insert(TTS);
        Observable.fromIterable(TTS.getTexts())
                .map(splittedText -> {
                    Log.d(D_TAG, "Send web request in repo");
                    return audioWebAPI.getTTSRawAudioString(new AudioRequestBody(splittedText), DEFAULT_EMOTION);
                })
                .map(responseBody -> {
                    Log.d(D_TAG, "Save file in repo");
                    String fileUri = AndroidUtils.getAudioFilesDir() + File.separator + hash
                            + AndroidUtils.getCurrentSystemTime();
                    return saveTTS(responseBody.byteStream(), fileUri);
                })
                .map(uri -> {
                    Log.d(D_TAG, "uri in TTSRepoImpl = " + uri);
                    Log.d(D_TAG, "Save POJO in DB in repo");
                    TTS.getUris().add(uri);
                    audioPOJODao.insert(TTS);
                    return new Object();
                })
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<Object>(){
                    @Override
                    public void onSubscribe(Disposable d){
                    }

                    @Override
                    public void onNext(Object o){
                        Log.d(D_TAG, "onNext in repo");
                        requestStateSubject.onNext(RequestState.IN_PROGRESS);
                    }

                    @Override
                    public void onError(Throwable e){
                        Log.d(D_TAG, "onError in repo");
                        e.printStackTrace();
                        if(e instanceof IOException)
                            requestStateSubject.onNext(RequestState.ERROR_LOCAL);
                        else requestStateSubject.onNext(RequestState.ERROR_WEB);
                        if(!TTS.getUris().isEmpty() && !TTS.getTexts().isEmpty())
                            audioPOJODao.insert(TTS);

                    }

                    @Override
                    public void onComplete(){
                        Log.d(D_TAG, "onComplete in repo");
                        audioPOJODao.insert(TTS);
                        requestStateSubject.onNext(RequestState.SUCCESS);
                    }
                });
    }

    /**
     * @param uri     - uri to add to POJO's arrayList
     * @param TTS - POJO to save
     * @return observable that shows that update is completed
     */
    private boolean updateTTSPOJO(String uri, TTS TTS){
        TTS.getUris().add(uri);
        audioPOJODao.update(TTS);
        return true;
    }

    /**
     * This method should not be called from main thread
     *
     * @param responseBody - body to save (must be an audio file)
     * @param fileName     - name of the saved file
     * @return observable that emit single uri string with path to saved audio file
     */
    private String saveTTS(ResponseBody responseBody,
                           String fileName) throws IOException{
        boolean isSuccessful = fileStorage.saveAudioFile(responseBody, fileName);
        if(isSuccessful) return fileName;
        else throw new IOException();
    }

    private String saveTTS(InputStream inputStream,
                           String fileName) throws IOException{
        boolean isSuccessful = fileStorage.saveAudioFile(inputStream, fileName);
        if(isSuccessful) return fileName;
        else throw new IOException();
    }

    private ArrayList<String> splitTextIntoPieces(String text, int textLengthLimit){
        ArrayList<String> textsSplitByCharLimit = new ArrayList<>();
        for(int i = 0; i < text.length(); i += getAddition(textLengthLimit, textsSplitByCharLimit.size() - 1)){
            textsSplitByCharLimit.add(
                    text.substring(
                            i, Math.min(
                                    i + getAddition(textLengthLimit, textsSplitByCharLimit.size()),
                                    text.length())));
        }
        return textsSplitByCharLimit;
    }

    /**
     * @param textLengthLimit - initial text length limit
     * @param arraySize - size of already parsed pieces of text
     * @return calculated addition which is arithmetic progression with step = 250
     * or MAX_TEXT_LENGTH_LIMIT - 1 if calculated addition was over it
     */
    private int getAddition(int textLengthLimit, int arraySize){
        int addition = 250;
        int calculatedAddition = textLengthLimit + arraySize * addition;
        if(calculatedAddition < MAX_TEXT_LENGTH_LIMIT) return calculatedAddition;
        else return MAX_TEXT_LENGTH_LIMIT - 1;
    }
}