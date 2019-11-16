package trelico.ru.allcastmvvm.screens.player;

import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import trelico.ru.allcastmvvm.MyApp;

public class PlayerViewModel extends ViewModel{


    private MutableLiveData<Long> requestStateLiveData = new MutableLiveData<>();
    private MutableLiveData<MediaControllerCompat> mediaControllerLiveData = new MutableLiveData<>();
    private MutableLiveData<PlaybackStateCompat> playbackStateLiveData = new MutableLiveData<>();
    private MutableLiveData<String> ttsTextLiveData = new MutableLiveData<>();


    public void setMediaController(MediaControllerCompat mediaController){
        mediaControllerLiveData.postValue(mediaController);
    }

    public void setRequestState(long requestState){
        requestStateLiveData.postValue(requestState);
    }

    public void setPlaybackState(PlaybackStateCompat playbackState){
        playbackStateLiveData.postValue(playbackState);
    }

    public void setTTSText(String ttsText){
        ttsTextLiveData.postValue(ttsText);
    }

    public LiveData<String> getTtsTextLiveData(){
        return ttsTextLiveData;
    }

    public LiveData<PlaybackStateCompat> getPlaybackStateLiveData(){
        return playbackStateLiveData;
    }

    public LiveData<Long> getRequestStateLiveData(){
        return requestStateLiveData;
    }

    public LiveData<MediaControllerCompat> getMediaControllerLiveData(){
        return mediaControllerLiveData;
    }
}
