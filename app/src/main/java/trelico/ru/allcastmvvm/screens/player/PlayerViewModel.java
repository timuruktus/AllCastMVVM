package trelico.ru.allcastmvvm.screens.player;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService;
import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;
import trelico.ru.allcastmvvm.repositories.tts.TTSRepository;
import trelico.ru.allcastmvvm.repositories.tts.TTSRepositoryImpl;
import trelico.ru.allcastmvvm.repositories.tts.TTSResponseContainer;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;

public class PlayerViewModel extends ViewModel{


    private LinkParser linkParser = new LinkParser();

    private LiveData<NetworkService.RequestState> requestStateLiveData;
    private MutableLiveData<TTSPOJO> ttsLiveData = new MutableLiveData<>();
    private TTSResponseContainer ttsResponseContainer;
    private TTSRepository ttsRepository = TTSRepositoryImpl.getInstance();

    private String savedText;
    private String savedLinkToSource;



    Single<String> parseLinkToText(String link){
        return linkParser.stub();
    }
}
