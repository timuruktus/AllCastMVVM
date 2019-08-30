package trelico.ru.allcastmvvm.data_sources.local;


import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;

@Dao
public interface AudioPOJODao{


    @Query("SELECT * FROM TTSPOJO")
    Single<List<TTSPOJO>> getAllSingle();

    @Query("SELECT * FROM TTSPOJO WHERE hash = :hash")
    Single<TTSPOJO> getByHashSingle(String hash);

    @Query("SELECT * FROM TTSPOJO WHERE linkToSource = :linkToSource")
    Single<TTSPOJO> getByLinkSingle(String linkToSource);

    @Query("SELECT * FROM TTSPOJO")
    Observable<List<TTSPOJO>> getAllObservable();

    @Query("SELECT * FROM TTSPOJO WHERE hash = :hash")
    Observable<TTSPOJO> getByHashObservable(String hash);

    @Query("SELECT * FROM TTSPOJO WHERE linkToSource = :linkToSource")
    Observable<TTSPOJO> getByLinkObservable(String linkToSource);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TTSPOJO TTSPOJO);

    @Update
    void update(TTSPOJO TTSPOJO);

    @Delete
    void delete(TTSPOJO TTSPOJO);
}
