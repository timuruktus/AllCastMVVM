package trelico.ru.allcastmvvm.services;

import java.util.ArrayList;

import trelico.ru.allcastmvvm.repositories.audio.AudioResponse;

public class Playlist{

    private ArrayList<AudioResponse> playlist;

    public Playlist(ArrayList<AudioResponse> playlist){
        this.playlist = playlist;
    }

    public Playlist(){
    }

    public ArrayList<AudioResponse> getPlaylist(){
        return playlist;
    }

    public void setPlaylist(ArrayList<AudioResponse> playlist){
        this.playlist = playlist;
    }
}
