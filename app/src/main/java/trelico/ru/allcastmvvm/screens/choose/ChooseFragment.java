package trelico.ru.allcastmvvm.screens.choose;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.repositories.audio.AudioRepository;
import trelico.ru.allcastmvvm.repositories.audio.AudioRepositoryImpl;
import trelico.ru.allcastmvvm.repositories.audio.requests.TTSRequest;
import trelico.ru.allcastmvvm.screens.player.PlayerActivity;
import trelico.ru.allcastmvvm.utils.AndroidUtils;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChooseFragment extends Fragment{


    @BindView(R.id.telegramViewBackground) ImageView telegramViewBackground;
    @BindView(R.id.telegramTryButton) Button telegramTryButton;
    @BindString(R.string.understand)
    String understand;
    @BindString(R.string.error_happened)
    String errorHappened;

    protected static final String TELEGRAM_URI = "org.telegram.messenger";
    private View view;

    private ClipboardManager.OnPrimaryClipChangedListener clipboardListener;
    private ChooseViewModel viewModel;
    private ClipboardManager clipboard;
    private AudioRepository audioRepository;

    public ChooseFragment(){
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        view = inflater.inflate(R.layout.fragment_choose, container, false);
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        viewModel = viewModelProvider.get(ChooseViewModel.class);
        ButterKnife.bind(this, view);
        telegramViewBackground.setOutlineProvider(tryButtonOutlineProvider);
        audioRepository = AudioRepositoryImpl.getInstance();
        return view;
    }

    ViewOutlineProvider tryButtonOutlineProvider = new ViewOutlineProvider(){
        @Override
        public void getOutline(View view, Outline outline){
            // Or read size directly from the view's width/height
            int size = getResources().getDimensionPixelSize(R.dimen.telegramIconBackgroundSize);
            outline.setOval(0, 0, size, size);
        }
    };


    @OnClick(R.id.telegramTryButton)
    public void onViewClicked(){
        setClipboardListener();
        launchTelegram();
    }

    private void setClipboardListener(){
        clipboard = (ClipboardManager) MyApp.INSTANCE.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardListener = getListener();
        clipboard.addPrimaryClipChangedListener(clipboardListener);
    }


    private ClipboardManager.OnPrimaryClipChangedListener getListener(){
        return () -> {
            if(clipboard == null) return;
            if(!clipboard.hasPrimaryClip()) return;
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            String copiedText = item.getText().toString();
            try{
                if(!copiedText.isEmpty()){
                    clipboard.removePrimaryClipChangedListener(clipboardListener);
                    Log.d(D_TAG, "getListener in ChooseFragment. Text size = " + copiedText.length());
                    launchPlayerActivity(copiedText, null);
                    AndroidUtils.bringActivityToForeground(getContext(), PlayerActivity.class);
                }
            } catch(NullPointerException ex){
                Toast.makeText(getContext(), errorHappened, Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        clipboard = null;
        clipboardListener = null;
    }

    /**
     * @param text         - copied text.
     * @param linkToSource - link to source where text was given of.
     */
    private void launchPlayerActivity(String text, String linkToSource){
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        TTSRequest ttsRequest = new TTSRequest(text, linkToSource);
        audioRepository.sendRequest(ttsRequest);
        startActivity(intent);
    }


    private void launchTelegram(){
        final boolean isAppInstalled = AndroidUtils.isAppAvailable(MyApp.INSTANCE, TELEGRAM_URI);
        if(isAppInstalled){
            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_URI));
            appIntent.setData(Uri.parse("https://t.me"));//TODO
            startActivity(appIntent);
        } else{
            Toast.makeText(getContext(), R.string.app_is_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

}
