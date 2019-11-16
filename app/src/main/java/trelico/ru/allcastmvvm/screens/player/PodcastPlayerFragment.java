package trelico.ru.allcastmvvm.screens.player;


import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import trelico.ru.allcastmvvm.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class PodcastPlayerFragment extends Fragment{


    public PodcastPlayerFragment(){
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_podcast_player, container, false);
    }

}
