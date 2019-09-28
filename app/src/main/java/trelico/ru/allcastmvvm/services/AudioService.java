package trelico.ru.allcastmvvm.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.TransportControls;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;
import trelico.ru.allcastmvvm.screens.player.PlayerActivity;
import trelico.ru.allcastmvvm.utils.MediaStyleHelper;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
import static android.support.v4.media.MediaBrowserCompat.MediaItem;
import static android.support.v4.media.MediaMetadataCompat.Builder;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PAUSED;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_STOPPED;
import static com.google.android.exoplayer2.Player.STATE_ENDED;
import static com.google.android.exoplayer2.Timeline.*;
import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.TEXT;

public class AudioService extends MediaBrowserServiceCompat{


    public static final String AUDIO_SERVICE_LOG = "Audio Service Log";
    public static final String NOTIFICATION_DEFAULT_CHANNEL_ID = "default_cast_channel";
    public static final String LINK_TO_SOURCE = "Link to source";
    public static final String REQUEST_STATE_EXTRA = "metadata request state";
    public static final String DURATION_EXTRA = "duration";
    private final int NOTIFICATION_ID = 404;

    private MediaSessionCompat mediaSession;
    private AudioFocusRequest audioFocusRequest;
    private SimpleExoPlayer exoPlayer;
    private MyApp application;
    private AppDatabase appDatabase;
    private TTSPOJO currentTTSPOJO;
    private Disposable ttsDisposable;
    private Disposable stateDisposable;
    private Disposable progressDisposable;
    private AudioManager audioManager;
    private DefaultDataSourceFactory dataSourceFactory;

    private boolean audioFocusRequested = false;
    private AudioHelper audioHelper = new AudioHelper();
    private ConcatenatingMediaSource playlistMediaSource = new ConcatenatingMediaSource(true);
    private final Builder metadataBuilder = new Builder();
    private final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    );

    @Override
    public void onCreate(){
        super.onCreate();
        configureMediaSession();

        application = (MyApp) getApplicationContext();
        dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(),
                Util.getUserAgent(this, getString(R.string.app_name)));
        appDatabase = application.getAppDatabase();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        stateDisposable = audioHelper.getRequestStateObservable().subscribe(requestStateConsumer);
        configureExoPlayer();
        configureNotificationChannel();
    }

    private void configureExoPlayer(){
        exoPlayer = ExoPlayerFactory.newSimpleInstance(application,
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(),
                new DefaultLoadControl());
        exoPlayer.setPlayWhenReady(false);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        exoPlayer.addListener(new Player.EventListener(){
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState){
                if(playbackState == STATE_ENDED){
                    Log.d(D_TAG, "playback state ended");
                    TransportControls transportControls = mediaSession.getController().getTransportControls();
                    exoPlayer.setPlayWhenReady(false);
                    transportControls.seekTo(0);
                    transportControls.pause();
                }
            }
        });

    }

    private void configureMediaSession(){
        mediaSession = new MediaSessionCompat(getApplicationContext(), AUDIO_SERVICE_LOG);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setActive(true);
        mediaSession.setCallback(mediaSessionCallback);
        setSessionToken(mediaSession.getSessionToken());
    }

    private void configureActivityIntent(TTSPOJO ttspojo){
        Intent activityIntent = new Intent(application, PlayerActivity.class);
        StringBuilder sb = new StringBuilder();
        for(String pieceOfText : ttspojo.getTexts()) sb.append(pieceOfText);
        activityIntent.putExtra(TEXT, sb.toString());
        activityIntent.putExtra(LINK_TO_SOURCE, currentTTSPOJO.getLinkToSource());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(activityIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mediaSession.setSessionActivity(resultPendingIntent);
    }

    private void configureNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel =
                    new NotificationChannel(NOTIFICATION_DEFAULT_CHANNEL_ID,
                            getString(R.string.allcast),
                            NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(notificationChannel);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setAcceptsDelayedFocusGain(true)
                    .setWillPauseWhenDucked(true)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }
    }

    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback(){

        @Override
        public void onPlay(){
            super.onPlay();
            Log.d(D_TAG, "onPlay in service. playlist size = " + playlistMediaSource.getSize());
            startService(new Intent(getApplicationContext(), AudioService.class));
            setPlaybackState(STATE_PLAYING);
            if(!exoPlayer.getPlayWhenReady()){
                Log.d(D_TAG, "onPlay2 in service");
                if(!audioFocusRequested){
                    audioFocusRequested = true;

                    int audioFocusResult;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusRequest);
                    } else{
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusChangeListener,
                                AudioManager.STREAM_MUSIC, AUDIOFOCUS_GAIN);
                        Log.d(D_TAG, "requesting audiofocus in service");
                    }
                    if(audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                        audioFocusRequested = false;
                        Log.d(D_TAG, "lost audiofocus in service");
                        return;
                    }
                }

                mediaSession.setActive(true); // Сразу после получения фокуса

                registerReceiver(becomingNoisyReceiver,
                        new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

                exoPlayer.setPlayWhenReady(true);
            }

            if(progressDisposable == null || progressDisposable.isDisposed()){
                progressDisposable = Observable.interval(1, TimeUnit.SECONDS)
                        .subscribe(aLong -> {
                            setPlaybackState(STATE_PLAYING);
                            //TODO: check and call updates
                        });
            }
            setNotification(mediaSession.getController().getPlaybackState().getState());
        }

        /**
         * Send new audio request only when text is new
         * @param text
         * @param extras
         */
        @Override
        public void onPlayFromMediaId(String text, Bundle extras){
            super.onPlayFromMediaId(text, extras);
            String linkToSource = extras.getString(LINK_TO_SOURCE);
            Log.d(D_TAG, "onPlayFromMediaID in AudioService. Text = " + text);
            if(ttsDisposable != null && !ttsDisposable.isDisposed()) ttsDisposable.dispose();
            audioHelper.sendAudioRequest(text, linkToSource);
            ttsDisposable = audioHelper.getTTSObservable()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(TTSConsumer, getTTSErrorConsumer);
        }

        /**
         * Always send new audio request
         * @param text
         * @param extras
         */
        @Override
        public void onPrepareFromMediaId(String text, Bundle extras){
            super.onPrepareFromMediaId(text, extras);
            String linkToSource = extras.getString(LINK_TO_SOURCE);
            Log.d(D_TAG, "onPrepareFromMediaId in AudioService. Text = " + text);
            if(ttsDisposable != null && !ttsDisposable.isDisposed()) ttsDisposable.dispose();
            audioHelper.sendNewAudioRequest(text, linkToSource);
            ttsDisposable = audioHelper.getTTSObservable()
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(TTSConsumer, getTTSErrorConsumer);
        }

        @Override
        public void onPause(){
            super.onPause();
            if(progressDisposable != null && !progressDisposable.isDisposed())
                progressDisposable.dispose();
            Log.d(D_TAG, "onPause in service");
            if(exoPlayer.getPlayWhenReady()){
                exoPlayer.setPlayWhenReady(false);
                unregisterReceiver(becomingNoisyReceiver);
            }
            setPlaybackState(STATE_PAUSED);

            setNotification(mediaSession.getController().getPlaybackState().getState());
        }

        @Override
        public void onSkipToPrevious(){
            super.onSkipToPrevious();
            exoPlayer.seekTo(0);
        }

        @Override
        public void onStop(){
            progressDisposable.dispose();
            super.onStop();
            stopSelf();
            if(exoPlayer.getPlayWhenReady()){
                exoPlayer.setPlayWhenReady(false);
                unregisterReceiver(becomingNoisyReceiver);
            }

            if(audioFocusRequested){
                audioFocusRequested = false;

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                } else{
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                }
            }

            mediaSession.setActive(false);
            setPlaybackState(STATE_STOPPED);

            setNotification(mediaSession.getController().getPlaybackState().getState());
            exoPlayer.stop(true);
            stopSelf();
        }

        @Override
        public void onSeekTo(long pos){
            super.onSeekTo(pos);
            exoPlayer.seekTo(pos);
        }
    };

    private void setPlaybackState(int state){
        long currentPlayedTimeInMs = exoPlayer.getCurrentPosition();
        mediaSession.setPlaybackState(stateBuilder.setState(state, currentPlayedTimeInMs,
                1).build());
    }

    private void setNotification(int playbackState){
        switch(playbackState){
            case STATE_PLAYING:{
                startForeground(NOTIFICATION_ID, getNotification(playbackState));
                break;
            }
            case STATE_PAUSED:{
                NotificationManagerCompat.from(AudioService.this)
                        .notify(NOTIFICATION_ID, getNotification(playbackState));
                stopForeground(false);
                break;
            }
            default:{
                stopForeground(true);
                break;
            }
        }
    }

    private void updateRequestState(int requestState){
        Bundle bundle = new Bundle();
        bundle.putInt(REQUEST_STATE_EXTRA, requestState);
        mediaSession.setExtras(bundle);
    }

    private void updateMetadataFromTrack(long duration, String title, String text){
        if(title == null) title = "AllCast";
        metadataBuilder.putLong(METADATA_KEY_DURATION, duration);
        metadataBuilder.putString(METADATA_KEY_TITLE, title);
        metadataBuilder.putString(METADATA_KEY_DISPLAY_DESCRIPTION, text);
        mediaSession.setMetadata(metadataBuilder.build());
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints){
        return new BrowserRoot(getString(R.string.app_name), null);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaItem>> result){

    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId){
//        MediaButtonReceiver.handleIntent(mediaSession, intent);
//        return super.onStartCommand(intent, flags, startId);
//    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mediaSession.release();
        exoPlayer.release();
        stateDisposable.dispose();
        ttsDisposable.dispose();
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        switch(focusChange){
            case AUDIOFOCUS_GAIN:
                Log.d(D_TAG, "audiofocus gained in AudioService");
                exoPlayer.setVolume(1f); //MAY BE A BUG
                mediaSession.getController().getTransportControls().play();
//                mediaSessionCallback.onPlay(); // Не очень красиво (вызов метода колбэка)
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
//                mediaSessionCallback.onPause();
                break;
            case AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
//                exoPlayer.setVolume(exoPlayer.getVolume() - 0.2f); //MAY BE A BUG
                break;
            default:
                Log.d(D_TAG, "audiofocus lost in AudioService");
                audioFocusRequested = false;
                mediaSessionCallback.onPause();
                break;
        }
    };

    private Consumer<Integer> requestStateConsumer = this::updateRequestState;

    private Consumer<TTSPOJO> TTSConsumer = ttspojo -> {
        if(currentTTSPOJO != null && !currentTTSPOJO.getHash().equals(ttspojo.getHash())){
            exoPlayer.prepare(playlistMediaSource, true, true);
            playlistMediaSource.clear();
        }
        currentTTSPOJO = ttspojo;
        int urisArraySize = ttspojo.getUris().size();
        Log.d(D_TAG, "urisArraySize in Service = " + urisArraySize);
        Log.d(D_TAG, "playlistMediaSource.getSize() in Service = " + playlistMediaSource.getSize());
        for(int i = playlistMediaSource.getSize(); i < urisArraySize; i++){
            Uri currentUri = Uri.parse(ttspojo.getUris().get(i));
            Log.d(D_TAG, "add uri in AudioService = " + currentUri.toString());
            ProgressiveMediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .setTag(currentUri).createMediaSource(currentUri);
            playlistMediaSource.addMediaSource(mediaSource);
            exoPlayer.prepare(playlistMediaSource, false, false);
        }
        long duration = 0;
        exoPlayer.getCurrentPosition();
        Timeline timeline = exoPlayer.getCurrentTimeline();
        Log.d(D_TAG, "Windows count in Service = " + timeline.getWindowCount());
        for(int i = 0; i < timeline.getWindowCount(); i++){
            duration += timeline.getWindow(i, new Window()).getDurationMs();
        }
        StringBuilder sb = new StringBuilder();
        for(String splittedText : ttspojo.getTexts()) sb.append(splittedText);
        String text = sb.toString();
        updateMetadataFromTrack(duration, null, text);
    };

    private Consumer<Throwable> getTTSErrorConsumer = Throwable::printStackTrace;

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
                mediaSessionCallback.onPause();
            }
        }
    };

    private Notification getNotification(int playbackState){
        MediaControllerCompat controller = mediaSession.getController();
        NotificationCompat.Builder builder = MediaStyleHelper.from(application, mediaSession);
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_back_black,
                getString(R.string.previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(application, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

        if(playbackState == STATE_PLAYING)
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_pause_black,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(application, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        else
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_play_black,
                    getString(R.string.play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(application, PlaybackStateCompat.ACTION_PLAY_PAUSE)));

        builder.addAction(new NotificationCompat.Action(R.drawable.ic_close_black,
                getString(R.string.close),
                MediaButtonReceiver.buildMediaButtonPendingIntent(application, PlaybackStateCompat.ACTION_STOP)))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver
                                .buildMediaButtonPendingIntent(application, PlaybackStateCompat.ACTION_STOP))
                        .setMediaSession(mediaSession.getSessionToken()))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setColor(ContextCompat.getColor(application, R.color.notificationBackgroundColor))
//                .setShowWhen(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(controller.getSessionActivity())
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(application,
                        PlaybackStateCompat.ACTION_STOP))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID);

        configureActivityIntent(currentTTSPOJO);

        return builder.build();
    }


    private class MediaPlayer{



    }
}
