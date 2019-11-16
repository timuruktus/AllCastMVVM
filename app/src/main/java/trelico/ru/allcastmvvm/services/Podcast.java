package trelico.ru.allcastmvvm.services;

import androidx.annotation.Nullable;

import trelico.ru.allcastmvvm.repositories.audio.AudioResponse;

public class Podcast implements AudioResponse{

    private String id;
    private String description;
    private long duration;
    private String author;
    private String authorId;
    @Nullable private String imageUrl;

    public Podcast(){
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }

    @Override
    public String getIdentificator(){
        return id;
    }
}
