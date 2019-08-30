package trelico.ru.allcastmvvm.repositories.tts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.*;

public class TTSResponseContainer{

    private MutableLiveData<RequestState> requestState;
    private Observable<TTSPOJO> response = Observable.empty();

    public TTSResponseContainer(MutableLiveData<RequestState> requestState){
        this.requestState = requestState;
    }

    public TTSResponseContainer(MutableLiveData<RequestState> requestState, Observable<TTSPOJO> response){
        this.requestState = requestState;
        this.response = response;
    }

    public TTSResponseContainer(){
    }

    public LiveData<RequestState> getRequestStateLiveData(){
        return requestState;
    }

    public void setRequestStateLiveData(MutableLiveData<RequestState> requestState){
        this.requestState = requestState;
    }

    public Observable<TTSPOJO> getResponseObservable(){
        return response;
    }

    public void setResponseObservable(Observable<TTSPOJO> response){
        this.response = response;
    }
}
