package trelico.ru.allcastmvvm.screens.player;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import butterknife.BindDrawable;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState;
import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;
import trelico.ru.allcastmvvm.services.AudioService;
import trelico.ru.allcastmvvm.utils.HashUtils;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_LOCAL;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_WEB;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.IN_PROGRESS;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.LOADING;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.SUCCESS;

public class PlayerActivity extends AppCompatActivity{


    public static final String LINK_CONTENT_SOURCE = "Link";
    public static final String APP_CONTENT_SOURCE = "App";
    public static final String CONTENT_SOURCE = "Content source";
    public static final String CONTENT = "Content";
    public static final String APP_NAME = "App name";
    @BindView(R.id.tryAgainButton) Button tryAgainButton;
    @BindView(R.id.errorLayout) ConstraintLayout errorLayout;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.loadingLayout) ConstraintLayout loadingLayout;
    @BindView(R.id.mainTitleText) TextView mainTitleText;
    @BindView(R.id.mainText) TextView mainText;
    @BindView(R.id.titleText) TextView titleText;
    @BindView(R.id.authorText) TextView authorText;
    @BindView(R.id.previousButton) ImageButton previousButton;
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

    private String contentSource; //LINK_CONTENT_SOURCE or APP_CONTENT_SOURCE
    private String content;
    private String linkToSource;
    private String appName;
    private PlayerViewModel viewModel;
    AudioService.AudioServiceBinder audioServiceBinder;
    MediaControllerCompat mediaController;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);
        contentSource = getIntent().getStringExtra(CONTENT_SOURCE);
        if(contentSource.equals(LINK_CONTENT_SOURCE)) linkToSource = content;
        content = getIntent().getStringExtra(CONTENT);
//        appName = getIntent().getStringExtra(APP_NAME);
        ViewModelProvider viewModelProvider = new ViewModelProvider(this,
                new SavedStateViewModelFactory(MyApp.INSTANCE, this));
        viewModel = viewModelProvider.get(PlayerViewModel.class);

        configureLayout();
    }

    @Override
    protected void onStart(){
        super.onStart();
        bindService(new Intent(this, AudioService.class),
                serviceConnection,
                BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection(){

        Disposable seekDisposable;
        Disposable updatesDisposable;
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            Log.d(D_TAG, "onServiceConnected in PlayerActivity");
            audioServiceBinder = (AudioService.AudioServiceBinder) service;
            viewModel.parseContentIfNeeded(content, contentSource).observe(PlayerActivity.this, str -> {
                content = str;
                replaceIntentStringExtra(CONTENT, content);
                sendRequest();
            });
            seekDisposable = audioServiceBinder.onSeekObservable().subscribeOn(Schedulers.io())
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(onSeekConsumer);
            updatesDisposable = audioServiceBinder.getAskingForUpdatesObservable()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(askingForUpdatesConsumer);
            try{
                mediaController = new MediaControllerCompat(
                        PlayerActivity.this, audioServiceBinder.getMediaSessionToken());
                mediaController.registerCallback(
                        new MediaControllerCompat.Callback(){
                            @Override
                            public void onPlaybackStateChanged(PlaybackStateCompat state){
                                if(state == null)
                                    return;
                                int playbackState = state.getState();
                                if(playbackState == PlaybackStateCompat.STATE_PLAYING){
                                    playPauseButton.setBackground(pauseDrawable);
                                } else if(playbackState == PlaybackStateCompat.STATE_BUFFERING){
                                    playPauseButton.setBackground(playDrawable);
                                }
                            }

                            @Override
                            public void onMetadataChanged(MediaMetadataCompat metadata){
                                super.onMetadataChanged(metadata);
                                long duration = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
                                String title = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
                                String description = metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION);
                                totalTime.setText(duration + "");
                                mainTitleText.setText(title);
                                mainText.setText(description);
                            }
                        }

                );
            } catch(RemoteException e){
                mediaController = null;
            }
        }


        @Override
        public void onServiceDisconnected(ComponentName name){
            audioServiceBinder = null;
            mediaController = null;
            seekDisposable.dispose();
            updatesDisposable.dispose();
        }
    };

    private Consumer askingForUpdatesConsumer = new Consumer(){
        @Override
        public void accept(Object o) throws Exception{
            viewModel.updateExistingTTS(content);
        }
    };

    private Consumer<Long> onSeekConsumer = aLong -> seekBar.setProgress(aLong.intValue());

    private void replaceIntentStringExtra(String tag, String extra){
        getIntent().removeExtra(tag);
        getIntent().putExtra(tag, extra);
    }

    private void configureLayout(){
        if(contentSource.equals(APP_CONTENT_SOURCE)){
            mainTitleText.setText(textFromApp);
        } else if(contentSource.equals(LINK_CONTENT_SOURCE)){
            mainTitleText.setText(textFromLink);
        }
    }

    private void sendRequest(){
        viewModel.sendAudioRequest(content, linkToSource);
        viewModel.getRequestStateLiveData().observe(this, getRequestStateObserver());
        audioServiceBinder.setNewAudioPOJOHash(HashUtils.getHash(content));
    }

    private Observer<RequestState> getRequestStateObserver(){
        return requestState -> {
            if(requestState == IN_PROGRESS){
                errorLayout.setVisibility(GONE);
                loadingLayout.setVisibility(GONE);
                playerLayout.setVisibility(VISIBLE);
                inProgressBar.setVisibility(VISIBLE);
            } else if(requestState == LOADING){
                loadingLayout.setVisibility(VISIBLE);
                playerLayout.setVisibility(GONE);
                errorLayout.setVisibility(GONE);
            } else if((requestState == ERROR_LOCAL || requestState == ERROR_WEB) && playerLayout.isShown()){
                Toast.makeText(this, "Error stub", Toast.LENGTH_SHORT).show();
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
        };
    }

    @OnClick(R.id.tryAgainButton)
    public void onTryAgainButtonClicked(){
        viewModel.sendNewAudioRequest(content, linkToSource);
    }

    @OnClick(R.id.previousButton)
    public void onPreviousButtonClicked(){
        mediaController.getTransportControls().seekTo(0);
    }

    @OnClick(R.id.playPauseButton)
    public void onPlayPauseButtonClicked(){
        if(audioServiceBinder.isPlaying()){
            mediaController.getTransportControls().pause();
            playPauseButton.setBackground(playDrawable);
        }else{
            mediaController.getTransportControls().play();
            playPauseButton.setBackground(pauseDrawable);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(serviceConnection != null)
            unbindService(serviceConnection);
    }
}
