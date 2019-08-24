package trelico.ru.allcastmvvm.screens.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import trelico.ru.allcastmvvm.data_sources.remote.NetworkService;
import trelico.ru.allcastmvvm.repositories.AudioRepository;

public class LinkParserResponse{

    private MutableLiveData<String> parsedTextLiveData = new MutableLiveData<>();
    private MutableLiveData<NetworkService.RequestState> requestStateLiveData = new MutableLiveData<>();


    public LiveData<String> getParsedTextLiveData(){
        return parsedTextLiveData;
    }

    public LiveData<NetworkService.RequestState> getRequestStateLiveData(){

        return requestStateLiveData;
    }


}
