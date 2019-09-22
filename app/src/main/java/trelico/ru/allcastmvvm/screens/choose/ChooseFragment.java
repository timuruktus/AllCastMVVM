package trelico.ru.allcastmvvm.screens.choose;


import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import trelico.ru.allcastmvvm.MyApp;
import trelico.ru.allcastmvvm.R;
import trelico.ru.allcastmvvm.screens.player.PlayerActivity;
import trelico.ru.allcastmvvm.utils.AndroidUtils;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.screens.player.PlayerActivity.TEXT;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChooseFragment extends Fragment{


    @BindView(R.id.telegram)
    Button telegram;
    @BindString(R.string.telegram_hint)
    String telegramHint;
    @BindString(R.string.understand)
    String understand;
    @BindString(R.string.error_happened)
    String errorHappened;

    protected static final String TELEGRAM_URI = "org.telegram.messenger";
    private View view;

    private ClipboardManager.OnPrimaryClipChangedListener clipboardListener;
    private ChooseViewModel viewModel;
    private ClipboardManager clipboard;

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
        return view;
    }

    @OnClick(R.id.telegram)
    public void onTelegramClicked(){
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setMessage(telegramHint)
                .setCancelable(true)
                .setPositiveButton(understand, getHintDialogListener())
                .create();
        dialog.show();
    }


    private DialogInterface.OnClickListener getHintDialogListener(){
        return (dialog, which) -> {
            setClipboardListener();
            launchTelegram();
        };
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
                    launchPlayerActivity(TEXT, copiedText);
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
     * @param content - content. Link or raw copied texts.
     * @param tag     - TEXT or LINK_TO_SOURCE in PlayerActivity.
     */
    private void launchPlayerActivity(String tag, String content){
        Intent intent = new Intent(getContext(), PlayerActivity.class);
        intent.putExtra(tag, content);
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
