package trelico.ru.allcastmvvm.data_sources.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import trelico.ru.allcastmvvm.services.TTS;

@Database(entities = {TTS.class}, version = 6, exportSchema = false)
@TypeConverters({trelico.ru.allcastmvvm.utils.TypeConverters.class})
public abstract class AppDatabase extends RoomDatabase{
    public abstract AudioPOJODao audioPOJODao();
}
