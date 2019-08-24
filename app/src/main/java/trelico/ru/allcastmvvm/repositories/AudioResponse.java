package trelico.ru.allcastmvvm.repositories;

import androidx.lifecycle.LiveData;

import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.*;

public class AudioResponse{

    private LiveData<RequestState> requestState;
    private LiveData<AudioPOJO> response;

    public AudioResponse(LiveData<RequestState> requestState, LiveData<AudioPOJO> response){
        this.requestState = requestState;
        this.response = response;
    }

    public AudioResponse(){
    }

    public LiveData<RequestState> getRequestState(){
        return requestState;
    }

    public void setRequestState(LiveData<RequestState> requestState){
        this.requestState = requestState;
    }

    public LiveData<AudioPOJO> getResponse(){
        return response;
    }

    public void setResponse(LiveData<AudioPOJO> response){
        this.response = response;
    }
}
