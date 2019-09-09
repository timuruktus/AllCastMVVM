package trelico.ru.allcastmvvm.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.media.session.MediaButtonReceiver;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;
import trelico.ru.allcastmvvm.screens.player.PlayerActivity;
import trelico.ru.allcastmvvm.utils.MediaStyleHelper;

import static android.media.AudioManager.AUDIOFOCUS_GAIN;
import static android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK;
import static android.support.v4.media.MediaMetadataCompat.Builder;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.CONTENT;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.CONTENT_SOURCE;

public class AudioService extends Service{


    public static final String AUDIO_SERVICE_LOG = "Audio Service Log";
    public static final String NOTIFICATION_DEFAULT_CHANNEL_ID = "default_cast_channel";
    public static final String MY_EMPTY_MEDIA_ROOT_ID = "empty media root id";
    public static final String UPDATE_URIS_EVENT = "Need to update uris";
    private final int NOTIFICATION_ID = 404;

    private MediaSessionCompat mediaSession;
    private AudioFocusRequest audioFocusRequest;
    private SimpleExoPlayer exoPlayer;
    private MyApp application;
    private AppDatabase appDatabase;
    private TTSPOJO currentTTSPOJO;
    private Disposable ttsPOJOSubscription;
    private Disposable askingForUpdateSubscription;
    private AudioManager audioManager;
    private boolean audioFocusRequested = false;

    private BehaviorSubject<Long> seekObservable = BehaviorSubject.create();
    private DefaultDataSourceFactory dataSourceFactory;
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
        application = (MyApp) getApplicationContext();
        dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(),
                Util.getUserAgent(this, getString(R.string.app_name)));
        appDatabase = application.getAppDatabase();
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        configureNotificationChannel();
        configureMediaSession();
        configureExoPlayer();
    }

    private void configureExoPlayer(){
        exoPlayer = ExoPlayerFactory.newSimpleInstance(application,
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(),
                new DefaultLoadControl());
        exoPlayer.setPlayWhenReady(false);
        exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
        exoPlayer.prepare(playlistMediaSource);
    }

    private void configureMediaSession(){
        mediaSession = new MediaSessionCompat(getApplicationContext(), AUDIO_SERVICE_LOG);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setPlaybackState(stateBuilder.build());
        mediaSession.setCallback(mediaSessionCallback);
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

            if(!exoPlayer.getPlayWhenReady()){
                Log.d(D_TAG, "onPlay2 in service");
                startService(new Intent(getApplicationContext(), AudioService.class));
                if(!audioFocusRequested){
                    audioFocusRequested = true;

                    int audioFocusResult;
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusRequest);
                    } else{
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusChangeListener,
                                AudioManager.STREAM_MUSIC, AUDIOFOCUS_GAIN);
                    }
                    if(audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
                        audioFocusRequested = false;
                        return;
                    }
                }

                mediaSession.setActive(true); // Сразу после получения фокуса

                registerReceiver(becomingNoisyReceiver,
                        new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

                exoPlayer.setPlayWhenReady(true);
            }

                long currentPlayedTimeInMs = exoPlayer.getContentPosition();
                mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                        currentPlayedTimeInMs, 1).build());
                refreshNotificationAndForegroundStatus(mediaSession.getController().getPlaybackState().getState());
        }

        @Override
        public void onPause(){
            super.onPause();
            Log.d(D_TAG, "onPause in service");
            if(exoPlayer.getPlayWhenReady()){
                exoPlayer.setPlayWhenReady(false);
                unregisterReceiver(becomingNoisyReceiver);
            }
            long currentPlayedTimeInMs = exoPlayer.getContentPosition();
            mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    currentPlayedTimeInMs, 1).build());

            refreshNotificationAndForegroundStatus(mediaSession.getController().getPlaybackState().getState());
        }

        @Override
        public void onSkipToPrevious(){
            super.onSkipToPrevious();
            exoPlayer.seekTo(0);
        }

        @Override
        public void onStop(){
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
            long currentPlayedTimeInMs = exoPlayer.getContentPosition();
            mediaSession.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_STOPPED,
                    currentPlayedTimeInMs, 1).build());

            refreshNotificationAndForegroundStatus(mediaSession.getController().getPlaybackState().getState());
            exoPlayer.stop(true);
            stopSelf();
        }

        @Override
        public void onSeekTo(long pos){
            super.onSeekTo(pos);
            seekObservable.onNext(pos);
        }
    };

    private void refreshNotificationAndForegroundStatus(int playbackState){
        switch(playbackState){
            case PlaybackStateCompat.STATE_PLAYING:{
                startForeground(NOTIFICATION_ID, getNotification(playbackState));
                break;
            }
            case PlaybackStateCompat.STATE_PAUSED:{
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

    private void updateMetadataFromTrack(long duration, String title, String text){
        if(title == null) title = "AllCast";
        metadataBuilder.putLong(METADATA_KEY_DURATION, duration);
        metadataBuilder.putString(METADATA_KEY_TITLE, title);
        metadataBuilder.putString(METADATA_KEY_DISPLAY_DESCRIPTION, text);
        mediaSession.setMetadata(metadataBuilder.build());
    }

    @Nullable
    @Override
    public AudioServiceBinder onBind(Intent intent){
        return new AudioServiceBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mediaSession.release();
        exoPlayer.release();
        if(askingForUpdateSubscription != null && !askingForUpdateSubscription.isDisposed())
            askingForUpdateSubscription.dispose();
        if(ttsPOJOSubscription != null && !ttsPOJOSubscription.isDisposed())
            ttsPOJOSubscription.dispose();
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = focusChange -> {
        switch(focusChange){
            case AUDIOFOCUS_GAIN:
                exoPlayer.setVolume(1f); //MAY BE A BUG
                mediaSessionCallback.onPlay(); // Не очень красиво (вызов метода колбэка)
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mediaSessionCallback.onPause();
                break;
            case AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                exoPlayer.setVolume(exoPlayer.getVolume() - 0.2f); //MAY BE A BUG
                break;
            default:
                mediaSessionCallback.onPause();
                break;
        }
    };

    private Observer<TTSPOJO> ttspojoObserver = new Observer<TTSPOJO>(){
        @Override
        public void onSubscribe(Disposable d){
            ttsPOJOSubscription = d;
            playlistMediaSource.clear();
        }

        @Override
        public void onNext(TTSPOJO ttspojo){
            currentTTSPOJO = ttspojo;
            int urisArraySize = ttspojo.getUris().size();
            String hash = ttspojo.getHash();
            for(int i = playlistMediaSource.getSize(); i < urisArraySize; i++){
                playlistMediaSource.addMediaSource(new ProgressiveMediaSource.Factory(dataSourceFactory).setTag(hash + i)
                        .createMediaSource(Uri.parse(ttspojo.getUris().get(i))));
            }
            exoPlayer.prepare(playlistMediaSource, false, false);
            long duration = exoPlayer.getDuration();
            StringBuilder sb = new StringBuilder();
            for(String splittedText : ttspojo.getTexts()) sb.append(splittedText);
            String text = sb.toString();
            updateMetadataFromTrack(duration, null, text);
            configureActivityIntent(ttspojo);
        }

        @Override
        public void onError(Throwable e){
            e.printStackTrace();
        }

        @Override
        public void onComplete(){

        }
    };

    private void configureActivityIntent(TTSPOJO ttspojo){
        Intent activityIntent = new Intent(application, PlayerActivity.class);
        StringBuilder sb = new StringBuilder();
        for(String pieceOfText : ttspojo.getTexts()) sb.append(pieceOfText);
        activityIntent.putExtra(CONTENT, sb.toString());
        activityIntent.putExtra(CONTENT_SOURCE, currentTTSPOJO.getContentSource());
        mediaSession.setSessionActivity(PendingIntent.getActivity(application,
                0, activityIntent, 0));
    }



    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            // Disconnecting headphones - stop playback
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())){
                mediaSessionCallback.onPause();
            }
        }
    };

    private Notification getNotification(int playbackState){
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSession);
        builder.addAction(new NotificationCompat.Action(R.drawable.ic_back_black,
                getString(R.string.previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

        if(playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_pause_black,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        else
            builder.addAction(new NotificationCompat.Action(R.drawable.ic_play_black,
                    getString(R.string.play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));

        builder.addAction(new NotificationCompat.Action(R.drawable.ic_close_black,
                getString(R.string.close),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)));

        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver
                        .buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(mediaSession.getSessionToken())); // setMediaSession требуется для Android Wear
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark)); // The whole background (in MediaStyle), not just icon background
        builder.setShowWhen(false);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setOnlyAlertOnce(true);
        builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID);

        return builder.build();
    }

    public class AudioServiceBinder extends Binder{

        public MediaSessionCompat.Token getMediaSessionToken(){
            return mediaSession.getSessionToken();
        }

        public void setNewAudioPOJOHash(String hash){
            if(ttsPOJOSubscription != null && !ttsPOJOSubscription.isDisposed())
                ttsPOJOSubscription.dispose();
            playlistMediaSource.clear();
            appDatabase.audioPOJODao().getByHashObservable(hash)
                    .subscribeOn(Schedulers.io())
                    .subscribe(ttspojoObserver);
        }

        public boolean isPlaying(){
            return mediaSession.getController().getPlaybackState().getState()
                    == PlaybackStateCompat.STATE_PLAYING;
        }

        public long getCurrentPlaybackPosition(){
            return exoPlayer.getCurrentPosition();
        }

        /**
         * @return observable that start emitting item when ttsPOJO update
         * is required
         */
        public Observable getAskingForUpdatesObservable(){
            return Observable.interval(3, TimeUnit.SECONDS)
                    .filter(aLong -> {
                        if(currentTTSPOJO != null){
                            long totalDuration = exoPlayer.getDuration();
                            long currentDuration = exoPlayer.getCurrentPosition();
                            boolean isNearToEnd = totalDuration - currentDuration < 1000;
                            int currentPosition = exoPlayer.getCurrentPeriodIndex();
                            int totalUrisPositions = currentTTSPOJO.getUris().size();
                            int totalPositions = currentTTSPOJO.getTexts().size();
                            return totalUrisPositions - currentPosition < 2
                                    && totalPositions != totalUrisPositions
                                    && isNearToEnd
                                    && totalPositions > 1;
                        }else return false;
                    });
        }

        public Observable<Long> onSeekObservable(){
            return seekObservable;
        }
    }


}
