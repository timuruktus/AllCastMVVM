package trelico.ru.allcastmvvm.screens.player;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import trelico.ru.allcastmvvm.data_sources.remote.NetworkService;
import trelico.ru.allcastmvvm.repositories.AudioPOJO;
import trelico.ru.allcastmvvm.repositories.AudioRepository;
import trelico.ru.allcastmvvm.repositories.AudioResponse;

import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.APP_CONTENT_SOURCE;

public class PlayerViewModel extends ViewModel{


    private LinkParser linkParser = new LinkParser();
    private SavedStateHandle state;

    private LiveData<NetworkService.RequestState> requestStateLiveData;
    private LiveData<AudioPOJO> audioLiveData;

    private String savedText;
    private String savedLinkToSource;

    void sendAudioRequest(String text, String linkToSource){
        if(!text.equals(savedText)) sendNewAudioRequest(text, linkToSource);
    }

    void sendNewAudioRequest(String text, String linkToSource){
        savedText = text;
        savedLinkToSource = linkToSource;
        AudioRepository audioRepository = AudioRepository.getInstance();
        AudioResponse audioResponse = audioRepository.getTTSAudio(text, linkToSource);
        requestStateLiveData = audioResponse.getRequestState();
        audioLiveData = audioResponse.getResponse();
    }

    LiveData<String> parseContentIfNeeded(String content, String contentSource){
        MutableLiveData<String> parsedContent = new MutableLiveData<>();
        if(contentSource.equals(APP_CONTENT_SOURCE)) parsedContent.setValue(content); //Cause content - already parsed texts
        else return linkParser.stub(); //TODO: remove stub
        return parsedContent;
    }

    public LiveData<NetworkService.RequestState> getRequestStateLiveData(){
        return requestStateLiveData;
    }

    public LiveData<AudioPOJO> getAudioLiveData(){
        return audioLiveData;
    }
}
