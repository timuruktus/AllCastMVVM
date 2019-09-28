package trelico.ru.allcastmvvm.screens.player;

import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.services.AudioService;

import static android.support.v4.media.session.PlaybackStateCompat.*;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.MyApp.E_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_LOCAL;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_WEB;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.IN_PROGRESS;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.LOADING;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.SUCCESS;
import static trelico.ru.allcastmvvm.services.AudioService.DURATION_EXTRA;
import static trelico.ru.allcastmvvm.services.AudioService.REQUEST_STATE_EXTRA;

public class PlayerActivity extends AppCompatActivity{

    public static final String LINK_TO_SOURCE = "Link to source";
    public static final String TEXT = "Text";

    @BindView(R.id.tryAgainButton) Button tryAgainButton;
    @BindView(R.id.errorLayout) ConstraintLayout errorLayout;
    @BindView(R.id.loadingLayout) ConstraintLayout loadingLayout;
    @BindView(R.id.mainTitleText) TextView mainTitleText;
    @BindView(R.id.mainText) TextView mainText;
    @BindView(R.id.titleText) TextView titleText;
    @BindView(R.id.authorText) TextView authorText;
    @BindView(R.id.buttonLeft) ImageButton previousButton;
    @BindView(R.id.playPauseButton) ImageButton playPauseButton;
    @BindView(R.id.seekBar) AppCompatSeekBar seekBar;
    @BindView(R.id.currentTime) TextView currentTime;
    @BindView(R.id.totalTime) TextView totalTime;
    @BindView(R.id.playerLayout) ConstraintLayout playerLayout;
    @BindString(R.string.text_from_app) String textFromApp;
    @BindString(R.string.text_from_link) String textFromLink;
    @BindView(R.id.inProgressBar) ProgressBar inProgressBar;
    @BindDrawable(R.drawable.ic_play_black) Drawable playDrawable;
    @BindDrawable(R.drawable.ic_pause_black) Drawable pauseDrawable;

    private String linkToSource;
    private String text;
    private String hash;

    private PlayerViewModel viewModel;
    MediaControllerCompat mediaController;
    private MediaBrowserCompat mediaBrowser;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);
        linkToSource = getIntent().getStringExtra(LINK_TO_SOURCE);
        text = getIntent().getStringExtra(TEXT);
        ViewModelProvider viewModelProvider = new ViewModelProvider(this,
                new SavedStateViewModelFactory(MyApp.INSTANCE, this));
        viewModel = viewModelProvider.get(PlayerViewModel.class);
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, AudioService.class),
                connectionCallbacks,
                null);
        configureLayoutInit();
    }

    @Override
    protected void onStart(){
        super.onStart();
        mediaBrowser.connect();
    }

    @Override
    protected void onResume(){
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onStop(){
        super.onStop();
        if(MediaControllerCompat.getMediaController(PlayerActivity.this) != null){
            MediaControllerCompat.getMediaController(PlayerActivity.this).unregisterCallback(controllerCallback);
        }
        mediaBrowser.disconnect();
    }

    private final MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback(){
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state){
            if(state == null)
                return;
            Log.d(D_TAG, "onPlaybackStateChanged in PlayerActivity");
            setPlayPauseButton(state.getState());
            setProgress(state.getPosition());
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata){
            super.onMetadataChanged(metadata);
            Log.d(D_TAG, "onMetadataChanged in PlayerActivity");
            setMetadata(metadata);
        }

        @Override
        public void onExtrasChanged(Bundle extras){
            super.onExtrasChanged(extras);
            int requestState = extras.getInt(REQUEST_STATE_EXTRA);
            int duration = extras.getInt(DURATION_EXTRA);
            if(requestState != 0)
                configureLayoutFromRequestState(extras.getInt(REQUEST_STATE_EXTRA));
            if(duration != 0)
                setDuration(duration);
        }
    };

    private final MediaBrowserCompat.ConnectionCallback connectionCallbacks = new MediaBrowserCompat.ConnectionCallback(){

        @Override
        public void onConnected(){
            super.onConnected();
            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
            try{
                mediaController =
                        new MediaControllerCompat(PlayerActivity.this, token);
                MediaControllerCompat.setMediaController(PlayerActivity.this, mediaController);
                mediaController.registerCallback(controllerCallback);
                Bundle extras = mediaController.getExtras();
                if(text == null){
                    viewModel.parseLinkToText(linkToSource)
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<String>(){
                                @Override
                                public void onSubscribe(Disposable d){

                                }

                                @Override
                                public void onSuccess(String parsedText){
                                    text = parsedText;
                                    getIntent().putExtra(TEXT, parsedText);
                                    sendRequestToPlay();
                                }

                                @Override
                                public void onError(Throwable e){
                                    e.printStackTrace();
                                }
                            });
                } else sendRequestToPlay();
                if(extras != null)
                    configureLayoutFromRequestState(extras.getInt(REQUEST_STATE_EXTRA));
                setMetadata(mediaController.getMetadata());
                setPlayPauseButton(mediaController.getPlaybackState().getState());
                setProgress(mediaController.getPlaybackState().getPosition());

                seekBar.setOnSeekBarChangeListener(seekBarChangeListener);
            } catch(RemoteException e){
                e.printStackTrace();
            }

        }

        @Override
        public void onConnectionFailed(){
            super.onConnectionFailed();
            finish();
        }

        @Override
        public void onConnectionSuspended(){
            super.onConnectionSuspended();
            finish();
        }
    };

    private SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener(){
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
            if(fromUser)
                mediaController.getTransportControls().seekTo(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar){

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar){

        }
    };

    private void sendRequestToPlay(){
        if(mediaController == null || text == null){
            Log.e(E_TAG, "Error on sendRequestToPlay in " + getLocalClassName());
            return;
        }
        Bundle linkToSourceBundle = new Bundle();
        linkToSourceBundle.putString(AudioService.LINK_TO_SOURCE, linkToSource);
        mediaController.getTransportControls().playFromMediaId(text, linkToSourceBundle);
    }

    private void sendNewRequestToPlay(){
        if(mediaController == null || text == null){
            Log.e(E_TAG, "Error on sendNewRequestToPlay in " + getLocalClassName());
            return;
        }
        Bundle linkToSourceBundle = new Bundle();
        linkToSourceBundle.putString(AudioService.LINK_TO_SOURCE, linkToSource);
        mediaController.getTransportControls().prepareFromMediaId(text, linkToSourceBundle);
    }

    private void setPlayPauseButton(int playbackState){
        if(playbackState == STATE_PLAYING){
            playPauseButton.setBackground(pauseDrawable);
        } else if(playbackState == STATE_BUFFERING
                || playbackState == STATE_NONE
                || playbackState == STATE_PAUSED){
            playPauseButton.setBackground(playDrawable);
        }
    }

    private void setProgress(long progress){
        seekBar.setProgress((int) progress);
        currentTime.setText(progress / 1000 + "");
    }

    private void setMetadata(MediaMetadataCompat metadata){
        if(metadata != null){
//            long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
            String description = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION);
//            totalTime.setText(duration + "");
//            seekBar.setMax((int) duration);
            mainTitleText.setText(title);
            mainText.setText(description);
        }
    }

    private void configureLayoutInit(){
        if(linkToSource != null){
            mainTitleText.setText(textFromApp);
        } else{
            mainTitleText.setText(textFromLink);
        }
//        configureLayoutFromRequestState(LOADING);
    }

    private void configureLayoutFromRequestState(long requestState){
        if(requestState == IN_PROGRESS){
            errorLayout.setVisibility(GONE);
            loadingLayout.setVisibility(GONE);
            playerLayout.setVisibility(VISIBLE);
            inProgressBar.setVisibility(VISIBLE);
        } else if(requestState == LOADING){
            loadingLayout.setVisibility(VISIBLE);
            playerLayout.setVisibility(GONE);
            errorLayout.setVisibility(GONE);
        } else if(requestState == ERROR_WEB && playerLayout.isShown()){
            Toast.makeText(this, R.string.internet_error, Toast.LENGTH_SHORT).show();
        } else if(requestState == ERROR_WEB || requestState == ERROR_LOCAL){
            errorLayout.setVisibility(VISIBLE);
            loadingLayout.setVisibility(GONE);
            playerLayout.setVisibility(GONE);
        } else if(requestState == SUCCESS){
            errorLayout.setVisibility(GONE);
            loadingLayout.setVisibility(GONE);
            playerLayout.setVisibility(VISIBLE);
            inProgressBar.setVisibility(GONE);
        }
    }

    private void setDuration(long duration){
        totalTime.setText(duration + "");
        seekBar.setMax((int) duration);
    }

    @OnClick(R.id.tryAgainButton)
    public void onTryAgainButtonClicked(){
        sendNewRequestToPlay();
    }

    @OnClick(R.id.buttonLeft)
    public void onPreviousButtonClicked(){
        mediaController.getTransportControls().seekTo(0);
    }

    @OnClick(R.id.playPauseButton)
    public void onPlayPauseButtonClicked(){
        int state = mediaController.getPlaybackState().getState();
        if(state == STATE_PLAYING){
            mediaController.getTransportControls().pause();
        } else if(state == STATE_STOPPED || state == STATE_PAUSED || state == STATE_NONE){
            mediaController.getTransportControls().play();
        }
//        setPlayPauseButton(state);
    }
}
