package trelico.ru.allcastmvvm.screens.player;


import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.ads.AdView;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import trelico.ru.allcastmvvm.R;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.IN_PROGRESS;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.LOADING;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.SUCCESS;

/**
 * A simple {@link Fragment} subclass.
 */
public class TTSPlayerFragment extends Fragment{

    @BindView(R.id.buttonSeekBackwards) ImageButton buttonSeekBackwards;
    @BindView(R.id.ttsPlayPauseButton) ImageButton ttsPlayPauseButton;
    @BindView(R.id.ttsProgressBar) ProgressBar ttsProgressBar;
    @BindView(R.id.ttsText) TextView ttsText;
    @BindView(R.id.ttsAd) AdView ttsAd;
    @BindDrawable(R.drawable.ic_play_black) Drawable playDrawable;
    @BindDrawable(R.drawable.ic_pause_black) Drawable pauseDrawable;
    private PlayerViewModel playerViewModel;
    public static final int DEFAULT_SEEK_BACKWARDS_VALUE = -10;
    private MediaControllerCompat mediaController;


    public TTSPlayerFragment(){
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ttsplayer, container, false);
        ButterKnife.bind(view);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        playerViewModel = new ViewModelProvider(getActivity()).get(PlayerViewModel.class);
        mediaController = playerViewModel.getMediaControllerLiveData().getValue();
        playerViewModel.getMediaControllerLiveData().observe(this, mediaControllerObserver);
        playerViewModel.getPlaybackStateLiveData().observe(this, playbackStateObserver);
        playerViewModel.getTtsTextLiveData().observe(this, ttsTextObserver);
    }

    private Observer<PlaybackStateCompat> playbackStateObserver = playbackStateCompat -> {
        int playbackState = playbackStateCompat.getState();
        if(playbackState == STATE_PLAYING){
            ttsPlayPauseButton.setBackground(pauseDrawable);
        } else if(playbackState == STATE_BUFFERING
                || playbackState == STATE_NONE
                || playbackState == STATE_PAUSED){
            ttsPlayPauseButton.setBackground(playDrawable);
        }
    };

    private Observer<String> ttsTextObserver = ttsText -> this.ttsText.setText(ttsText);

    private Observer<MediaControllerCompat> mediaControllerObserver = mediaController ->
            this.mediaController = mediaController;

    private Observer<Long> requestStateObserver = requestState -> {
        if(requestState == IN_PROGRESS || requestState == SUCCESS){
            ttsProgressBar.setVisibility(GONE);
        } else if(requestState == LOADING){
            ttsProgressBar.setVisibility(VISIBLE);
        }
    };

    @OnClick(R.id.buttonSeekBackwards)
    public void onButtonSeekBackwardsClicked(){
        mediaController.getTransportControls().seekTo(DEFAULT_SEEK_BACKWARDS_VALUE);
    }

    @OnClick(R.id.ttsPlayPauseButton)
    public void onTTSPlayPauseButtonClicked(){
        int state = mediaController.getPlaybackState().getState();
        if(state == STATE_PLAYING){
            mediaController.getTransportControls().pause();
        } else if(state == STATE_STOPPED || state == STATE_PAUSED || state == STATE_NONE){
            mediaController.getTransportControls().play();
        }
//        setPlayPauseButton(state);
    }
}
