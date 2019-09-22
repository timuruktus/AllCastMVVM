package trelico.ru.allcastmvvm.repositories.tts;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.EmptyResultSetException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.ReplaySubject;
import okhttp3.ResponseBody;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.data_sources.local.AudioPOJODao;
import trelico.ru.allcastmvvm.data_sources.remote.AudioRequestBody;
import trelico.ru.allcastmvvm.data_sources.remote.AudioWebAPI;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState;
import trelico.ru.allcastmvvm.repositories.local_files.FileStorage;
import trelico.ru.allcastmvvm.utils.AndroidUtils;
import trelico.ru.allcastmvvm.utils.StringUtils;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.MyApp.I_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.DEFAULT_EMOTION;

public class TTSRepositoryImpl implements TTSRepository{

    private static TTSRepositoryImpl instance;
    private AudioWebAPI audioWebAPI;
    private AudioPOJODao audioPOJODao;
    private FileStorage fileStorage;
    private PublishSubject<Integer> requestStateSubject = PublishSubject.create();
    private TTSResponseContainer ttsResponseContainer = new TTSResponseContainer(requestStateSubject);
    private static final int DEFAULT_TEXT_LENGTH_LIMIT = 1000;
    private static final int MAX_TEXT_LENGTH_LIMIT = 5000;

    private TTSRepositoryImpl(){
        audioWebAPI = AudioWebAPI.getInstance();
        AppDatabase appDatabase = MyApp.INSTANCE.getAppDatabase();
        audioPOJODao = appDatabase.audioPOJODao();
        fileStorage = new FileStorage();
    }

    public static TTSRepositoryImpl getInstance(){
        if(instance == null) instance = new TTSRepositoryImpl();
        return instance;
    }

    @Override
    public void createTTS(@NonNull String text, @Nullable String linkToSource){
        String hash = StringUtils.getHash(text);
        Log.d(D_TAG, "createTTS in Repo. hash = " + hash);
        audioPOJODao.getByHashSingle(hash)
                .subscribeOn(Schedulers.io())
                .subscribe(new SingleObserver<TTSPOJO>(){
                    @Override
                    public void onSubscribe(Disposable d){
                        requestStateSubject.onNext(RequestState.LOADING);
                    }

                    @Override
                    public void onSuccess(TTSPOJO ttspojo){
                        requestStateSubject.onNext(RequestState.SUCCESS);
                    }

                    @Override
                    public void onError(Throwable e){
                        if(e instanceof EmptyResultSetException){
                            Log.i(I_TAG, "No such audio POJO found in DB");
                            sendWebCreateRequest(text, linkToSource, hash);
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


    private void sendWebCreateRequest(@NonNull String text, @Nullable String linkToSource,
                                      @NonNull String hash){
        Log.d(D_TAG, "sendWebCreateRequest in Repo. hash = " + hash);
        TTSPOJO ttspojo = new TTSPOJO();
        ttspojo.setHash(hash);
        ttspojo.setLinkToSource(linkToSource);
        Observable.fromIterable(splitTextIntoPieces(text, DEFAULT_TEXT_LENGTH_LIMIT))
                .map(splittedText -> {
                    Log.d(D_TAG, "Send web request and add to arraylist in repo");
                    ttspojo.getTexts().add(splittedText);
                    return audioWebAPI.getTTSRawAudioString(new AudioRequestBody(splittedText), DEFAULT_EMOTION);
                })
                .map(responseBody -> {
                    Log.d(D_TAG, "Save file in repo");
                    String fileUri = AndroidUtils.getAudioFilesDir() + File.separator + hash
                            + AndroidUtils.getCurrentSystemTime();
                    return saveTTS(responseBody, fileUri);
                })
                .map(uri -> {
                    Log.d(D_TAG, "uri in TTSRepoImpl = " + uri);
                    Log.d(D_TAG, "Save POJO in DB in repo");
                    ttspojo.getUris().add(uri);
                    audioPOJODao.insert(ttspojo);
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
                        if(!ttspojo.getUris().isEmpty() && !ttspojo.getTexts().isEmpty())
                            audioPOJODao.insert(ttspojo);

                    }

                    @Override
                    public void onComplete(){
                        Log.d(D_TAG, "onComplete in repo");
                        audioPOJODao.insert(ttspojo);
                        requestStateSubject.onNext(RequestState.SUCCESS);
                    }
                });
    }

    /**
     * @param uri     - uri to add to POJO's arrayList
     * @param ttspojo - POJO to save
     * @return observable that shows that update is completed
     */
    private boolean updateTTSPOJO(String uri, TTSPOJO ttspojo){
        ttspojo.getUris().add(uri);
        audioPOJODao.update(ttspojo);
        return true;
    }

//    /**
//     *  This method should not be called from main thread
//     * @param responseBodies - array of bodies to save (must be an audio file)
//     * @param fileNames - names of the saved files
//     * @return observable that emit single uri string with path to saved audio file
//     */
//    private Observable<String> saveManyTTS(ArrayList<ResponseBody> responseBodies,
//                                           ArrayList<String> fileNames){
//        PublishSubject<String> uriObservable = PublishSubject.create();
//        for(int i = 0; i < fileNames.size(); i++){
//            String fileName = fileNames.get(i);
//            ResponseBody responseBody = responseBodies.get(i);
//            fileStorage.saveAudioFile(responseBody, fileName, isSuccessful -> {
//                if(isSuccessful) uriObservable.onNext(fileName);
//                if(!isSuccessful) uriObservable.onError(new IOException());
//            });
//        }
//        return uriObservable;
//    }

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

    private Observable<String> splitTextByLengthLimitRx(String text, int textLengthLimit){
        ReplaySubject<String> texts = ReplaySubject.create();
        for(int i = 0; i < text.length(); i += textLengthLimit){
            texts.onNext(text.substring(i, Math.min(i + textLengthLimit, text.length())));
        }
        return texts;
    }


    private ArrayList<String> splitTextIntoPieces(String text, int textLengthLimit){
        ArrayList<String> textsSplitByCharLimit = new ArrayList<>();
        for(int i = 0; i < text.length(); i += getAddition(textLengthLimit, textsSplitByCharLimit.size())){
            textsSplitByCharLimit.add(
                    text.substring(
                            i, Math.min(
                                    i + getAddition(textLengthLimit, textsSplitByCharLimit.size()),
                                    text.length())));
        }
        return textsSplitByCharLimit;

    }

    /**
     *
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
