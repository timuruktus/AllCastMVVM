package trelico.ru.allcastmvvm.repositories.audio;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;

public class AudioResponseContainer{

    private BehaviorSubject<Integer> requestState;
    private Observable<? extends AudioResponse> response = Observable.empty();

    public AudioResponseContainer(BehaviorSubject<Integer> requestState){
        this.requestState = requestState;
    }

    public AudioResponseContainer(BehaviorSubject<Integer> requestState, Observable<? extends AudioResponse> response){
        this.requestState = requestState;
        this.response = response;
    }

    public AudioResponseContainer(){
    }

    public Observable<Integer> getRequestStateObservable(){
        return requestState;
    }

    public void setRequestStateLiveData(BehaviorSubject<Integer> requestState){
        this.requestState = requestState;
    }

    public Observable<? extends AudioResponse> getResponseObservable(){
        return response;
    }

    public void setResponseObservable(Observable<? extends AudioResponse> response){
        this.response = response;
    }

    public void clear(){
        response = null;
    }
}
