package trelico.ru.allcastmvvm.screens.player;

import android.content.ComponentName;
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
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.services.AudioService;

import static android.support.v4.media.session.PlaybackStateCompat.STATE_BUFFERING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_NONE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_LOCAL;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_WEB;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.IN_PROGRESS;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.LOADING;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.SUCCESS;
import static trelico.ru.allcastmvvm.services.AudioService.REPLAY_LAST_REQUEST_ACTION;

public class PlayerActivity extends AppCompatActivity{

    @BindView(R.id.tryAgainButton) Button tryAgainButton;
    @BindView(R.id.errorLayout) ConstraintLayout errorLayout;
    @BindView(R.id.loadingLayout) ConstraintLayout loadingLayout;
    @BindView(R.id.containerLayout) ConstraintLayout containerLayout;
    public static final String METADATA_MEDIA_TYPE_TTS = "TTS media";
    public static final String METADATA_MEDIA_TYPE_PODCAST = "Podcast media";
    public static final String METADATA_MEDIA_TYPE = "Media type";
    public static final String METADATA_DURATION = "Duration";
    public static final String METADATA_TITLE = "Title";
    public static final String METADATA_TTS_TEXT = "Text";
    public static final String METADATA_DESCRIPTION = "Description";
    public static final String REQUEST_STATE_EXTRA = "Rquest state";
    public static final int REQUEST_STATE_NONE = 0;

    private PlayerViewModel viewModel;
    MediaControllerCompat mediaController;
    private MediaBrowserCompat mediaBrowser;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);
        viewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
        mediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, AudioService.class),
                connectionCallbacks,
                null);
        navController = Navigation.findNavController(this, R.id.navHostFragment);
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
            viewModel.setPlaybackState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata){
            super.onMetadataChanged(metadata);
            Log.d(D_TAG, "onMetadataChanged in PlayerActivity");
            String text = metadata.getString(METADATA_TTS_TEXT);
            viewModel.setTTSText(text);
            String mediaType = metadata.getString(METADATA_MEDIA_TYPE);
            NavDestination currentDestination = navController.getCurrentDestination();
            if(mediaType.equals(METADATA_MEDIA_TYPE_TTS) && currentDestination != null
                    && currentDestination.getId() != R.id.ttsPlayerFragment)
                navController.navigate(R.id.ttsPlayerFragment);
        }

        @Override
        public void onExtrasChanged(Bundle extras){
            super.onExtrasChanged(extras);
            int requestState = extras.getInt(REQUEST_STATE_EXTRA);
            if(requestState != REQUEST_STATE_NONE)
                configureLayoutFromRequestState(extras.getInt(REQUEST_STATE_EXTRA));
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
                viewModel.setMediaController(mediaController);
                Bundle extras = mediaController.getExtras();
                if(extras != null)
                    configureLayoutFromRequestState(extras.getInt(REQUEST_STATE_EXTRA));
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

    private void configureLayoutFromRequestState(long requestState){
        if(requestState == IN_PROGRESS || requestState == SUCCESS){
            errorLayout.setVisibility(GONE);
            loadingLayout.setVisibility(GONE);
            containerLayout.setVisibility(VISIBLE);
        } else if(requestState == LOADING){
            loadingLayout.setVisibility(VISIBLE);
            containerLayout.setVisibility(GONE);
            errorLayout.setVisibility(GONE);
        } else if(requestState == ERROR_WEB && containerLayout.isShown()){
            Toast.makeText(this, R.string.internet_error, Toast.LENGTH_SHORT).show();
        } else if(requestState == ERROR_WEB || requestState == ERROR_LOCAL){
            errorLayout.setVisibility(VISIBLE);
            loadingLayout.setVisibility(GONE);
            containerLayout.setVisibility(GONE);
        }
        viewModel.setRequestState(requestState);
    }

    @OnClick(R.id.tryAgainButton)
    public void onTryAgainButtonClicked(){
        mediaController.getTransportControls().sendCustomAction(REPLAY_LAST_REQUEST_ACTION, null);
    }
}
