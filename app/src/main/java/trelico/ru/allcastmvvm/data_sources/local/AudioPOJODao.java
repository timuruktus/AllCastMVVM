package trelico.ru.allcastmvvm.data_sources.local;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Observable;
import trelico.ru.allcastmvvm.repositories.AudioPOJO;

@Dao
public interface AudioPOJODao{


    @Query("SELECT * FROM AudioPOJO")
    LiveData<List<AudioPOJO>> getAll();

    @Query("SELECT * FROM AudioPOJO WHERE hash = :hash")
    LiveData<AudioPOJO> getByHash(String hash);

    @Query("SELECT * FROM AudioPOJO WHERE linkToSource = :linkToSource")
    LiveData<AudioPOJO> getByLink(String linkToSource);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AudioPOJO audioPOJO);

    @Update
    void update(AudioPOJO audioPOJO);

    @Delete
    void delete(AudioPOJO audioPOJO);
}
