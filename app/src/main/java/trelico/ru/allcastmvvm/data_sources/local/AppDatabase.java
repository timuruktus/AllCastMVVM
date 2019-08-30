package trelico.ru.allcastmvvm.data_sources.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import trelico.ru.allcastmvvm.repositories.tts.TTSPOJO;

@Database(entities = {TTSPOJO.class}, version = 3)
@TypeConverters({trelico.ru.allcastmvvm.utils.TypeConverters.class})
public abstract class AppDatabase extends RoomDatabase{
    public abstract AudioPOJODao audioPOJODao();
}
