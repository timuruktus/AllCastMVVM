package trelico.ru.allcastmvvm.screens.player;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.data_sources.remote.NetworkService;
import trelico.ru.allcastmvvm.repositories.AudioPOJO;
import trelico.ru.allcastmvvm.repositories.AudioRepository;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_LOCAL;
import static trelico.ru.allcastmvvm.data_sources.remote.NetworkService.RequestState.ERROR_WEB;
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

    private String contentSource; //LINK_CONTENT_SOURCE or APP_CONTENT_SOURCE
    private String content;
    private String linkToSource;
    private String appName;
    private AudioRepository audioRepository;
    private PlayerViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        ButterKnife.bind(this);
        contentSource = getIntent().getStringExtra(CONTENT_SOURCE);
        if(contentSource.equals(LINK_CONTENT_SOURCE)) linkToSource = content;
        content = getIntent().getStringExtra(CONTENT);
//        appName = getIntent().getStringExtra(APP_NAME);
        audioRepository = AudioRepository.getInstance();
        ViewModelProvider viewModelProvider = new ViewModelProvider(this,
                new SavedStateViewModelFactory(MyApp.INSTANCE, this));
        viewModel = viewModelProvider.get(PlayerViewModel.class);
        viewModel.parseContentIfNeeded(content, contentSource).observe(this, str -> {
            content = str;
            replaceIntentStringExtra(CONTENT, content);
            sendRequest();
        });
        configureLayout();
    }

    private void replaceIntentStringExtra(String tag, String extra){
        getIntent().removeExtra(tag);
        getIntent().putExtra(tag, extra);
    }

    private void configureLayout(){
        if(contentSource.equals(APP_CONTENT_SOURCE)){
            mainTitleText.setText(textFromApp);
        }else if(contentSource.equals(LINK_CONTENT_SOURCE)){
            mainTitleText.setText(textFromLink);
        }
    }

    private void sendRequest(){
        viewModel.sendAudioRequest(content, linkToSource);
        viewModel.getRequestStateLiveData().observe(this, getRequestStateObserver());
        viewModel.getAudioLiveData().observe(this, getAudioObserver());
    }

    private Observer<NetworkService.RequestState> getRequestStateObserver(){
        return requestState -> {
            if(requestState == LOADING){
                loadingLayout.setVisibility(View.VISIBLE);
                playerLayout.setVisibility(View.GONE);
                errorLayout.setVisibility(View.GONE);
            }else if(requestState == ERROR_WEB || requestState == ERROR_LOCAL){
                errorLayout.setVisibility(View.VISIBLE);
                loadingLayout.setVisibility(View.GONE);
                playerLayout.setVisibility(View.GONE);
            }else if(requestState == SUCCESS){
                errorLayout.setVisibility(View.GONE);
                loadingLayout.setVisibility(View.GONE);
                playerLayout.setVisibility(View.VISIBLE);
            }
        };
    }

    private Observer<AudioPOJO> getAudioObserver(){
        return audioPOJO -> {
            if(audioPOJO == null) Log.d(D_TAG, "audioPOJO is null in PlayerActivity");
            else{
                StringBuilder sb = new StringBuilder();
                for(String pieceOfText : audioPOJO.getTexts()) sb.append(pieceOfText);
                String text = sb.toString();
                mainText.setText(text);
            }
            //TODO: launch service and put uris within it
        };
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){

        super.onSaveInstanceState(outState);
    }

    @OnClick(R.id.tryAgainButton)
    public void onTryAgainButtonClicked(){
        viewModel.sendNewAudioRequest(content, linkToSource);
    }

    @OnClick(R.id.previousButton)
    public void onPreviousButtonClicked(){
    }

    @OnClick(R.id.playPauseButton)
    public void onPlayPauseButtonClicked(){
    }
}
