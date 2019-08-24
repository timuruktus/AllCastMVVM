package trelico.ru.allcastmvvm.screens.choose;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import trelico.ru.allcastmvvm.utils.ClipboardLiveData;

public class ChooseViewModel extends ViewModel{


    private ClipboardLiveData clipboardLiveData;

    ClipboardLiveData getClipboardLiveData(){
        if(clipboardLiveData == null){
            clipboardLiveData = new ClipboardLiveData();
        }        return clipboardLiveData;
    }


}
