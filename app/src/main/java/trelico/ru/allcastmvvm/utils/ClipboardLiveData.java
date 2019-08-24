package trelico.ru.allcastmvvm.utils;

import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import trelico.ru.allcastmvvm.MyApp;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;

public class ClipboardLiveData extends LiveData<String>{

    private ClipboardManager clipboard;
    private ClipboardManager.OnPrimaryClipChangedListener listener;


    @Override
    protected void onActive(){
        super.onActive();
        clipboard = (ClipboardManager) MyApp.INSTANCE.getSystemService(Context.CLIPBOARD_SERVICE);
        listener = getListener();
        clipboard.addPrimaryClipChangedListener(listener);
    }

    @Override
    protected void onInactive(){
        super.onInactive();
        if(listener != null){
            clipboard.removePrimaryClipChangedListener(listener);
        }
        Log.d(D_TAG, "onInactive in ClipboardLiveData");
        clipboard = null;
    }

    private ClipboardManager.OnPrimaryClipChangedListener getListener(){
        return () -> {
            String text =  (String) clipboard.getPrimaryClip().getItemAt(0).getText();
            if(text != null && !text.isEmpty()) setValue(text);
        };
    }
}
