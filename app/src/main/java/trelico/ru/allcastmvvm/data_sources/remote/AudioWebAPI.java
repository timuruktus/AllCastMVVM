package trelico.ru.allcastmvvm.data_sources.remote;

import android.util.Log;

import io.reactivex.Observable;
import okhttp3.ResponseBody;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;

public class AudioWebAPI{


    private static AudioWebAPI audioWebApi;
    private static NetworkService networkService;
    private static YandexRetrofit yandexRetrofit; //replaceable


    private AudioWebAPI(){}

    public static AudioWebAPI getInstance(){
        if(audioWebApi == null){
            audioWebApi = new AudioWebAPI();
            networkService = NetworkService.getInstance();
            yandexRetrofit = networkService.getYandexService().create(YandexRetrofit.class);
        }
        return audioWebApi;
    }

    public Observable<ResponseBody> getTTSRawAudioString(AudioRequestBody body, String emotion){
        Log.d(D_TAG, "getTTSRawAudioString in AudioWebAPI");
        return yandexRetrofit.getSpeechOgg(body.getText(), emotion);
    }
}
