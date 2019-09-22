package trelico.ru.allcastmvvm.repositories.tts;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState;

public class TTSResponseContainer{

    private PublishSubject<Integer> requestState;
    private Observable<TTSPOJO> response = Observable.empty();

    public TTSResponseContainer(PublishSubject<Integer> requestState){
        this.requestState = requestState;
    }

    public TTSResponseContainer(PublishSubject<Integer> requestState, Observable<TTSPOJO> response){
        this.requestState = requestState;
        this.response = response;
    }

    public TTSResponseContainer(){
    }

    public Observable<Integer> getRequestStateObservable(){
        return requestState;
    }

    public void setRequestStateLiveData(PublishSubject<Integer> requestState){
        this.requestState = requestState;
    }

    public Observable<TTSPOJO> getResponseObservable(){
        return response;
    }

    public void setResponseObservable(Observable<TTSPOJO> response){
        this.response = response;
    }

    public void clear(){
        response = null;
    }
}
