package trelico.ru.allcastmvvm.repositories.audio.requests;

import androidx.annotation.Nullable;

import trelico.ru.allcastmvvm.repositories.audio.AudioRepository;
import trelico.ru.allcastmvvm.repositories.audio.AudioRequestExecutor;

public class TTSRequest implements AudioRequest{

    private String text;
    @Nullable private String linkToSource;
    private AudioRequestExecutor audioRepository;

    @Override
    public void executeRequest(){
        audioRepository.createTTS(this);
    }

    public TTSRequest(String text){
        this.text = text;
    }

    public TTSRequest(String text, @Nullable String linkToSource){
        this.text = text;
        this.linkToSource = linkToSource;
    }

    public TTSRequest(){
    }

    @Nullable
    public String getLinkToSource(){
        return linkToSource;
    }

    public void setLinkToSource(@Nullable String linkToSource){
        this.linkToSource = linkToSource;
    }

    public String getText(){
        return text;
    }

    public void setText(String text){
        this.text = text;
    }

    @Override
    public void injectRepository(AudioRequestExecutor audioRequestExecutor){
        this.audioRepository = audioRequestExecutor;
    }
}
