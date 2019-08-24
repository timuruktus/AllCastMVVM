package trelico.ru.allcastmvvm.data_sources.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import trelico.ru.allcastmvvm.BuildConfig;

public class NetworkService{

    private static NetworkService instance;
    private static final String YANDEX_TTS_URL = "https://tts.api.cloud.yandex.net/";
    private Retrofit yandexService;
    public static final String DEFAULT_EMOTION = "good";
    public static final String GOOD_EMOTION = "good";
    public static final String EVIT_EMOTION = "evil";
    public static final String NEUTRAL_EMOTION = "neutral";

    private NetworkService() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .build();

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        yandexService = new Retrofit.Builder()
                .baseUrl(YANDEX_TTS_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    public static NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }


    public Retrofit getYandexService(){
        return yandexService;
    }


    public enum RequestState{
        LOADING, SUCCESS, ERROR_WEB, ERROR_LOCAL, IN_PROGRESS
    }
}
