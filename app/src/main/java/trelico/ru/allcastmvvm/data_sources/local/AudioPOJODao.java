package trelico.ru.allcastmvvm.data_sources.local;


import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import trelico.ru.allcastmvvm.services.TTS;

@Dao
public interface AudioPOJODao{


    @Query("SELECT * FROM TTS")
    Single<List<TTS>> getAllSingle();

    @Query("SELECT * FROM TTS WHERE hash = :hash")
    Single<TTS> getByHashSingle(String hash);

    @Query("SELECT * FROM TTS WHERE linkToSource = :linkToSource")
    Single<TTS> getByLinkSingle(String linkToSource);

    @Query("SELECT * FROM TTS")
    Observable<List<TTS>> getAllObservable();

    @Query("SELECT * FROM TTS WHERE hash = :hash")
    Observable<TTS> getByHashObservable(String hash);

    @Query("SELECT * FROM TTS WHERE linkToSource = :linkToSource")
    Observable<TTS> getByLinkObservable(String linkToSource);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TTS TTS);

    @Update
    void update(TTS TTS);

    @Delete
    void delete(TTS TTS);
}
