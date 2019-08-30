package trelico.ru.allcastmvvm;

import android.app.Application;

import androidx.room.Room;

import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.utils.ConnectionMonitor;


public class MyApp extends Application{

    public static final String D_TAG = "Debug tag";
    public static final String I_TAG = "Info tag";
    public static final String E_TAG = "Error tag";
    public static MyApp INSTANCE;
    private static AppDatabase appDatabase;
    private static ConnectionMonitor connectionMonitor;

    @Override
    public void onCreate(){
        super.onCreate();
        INSTANCE = this;
        connectionMonitor = new ConnectionMonitor(this);
        appDatabase =  Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database")
                .fallbackToDestructiveMigration() //TODO: delete later in production
                .build();
    }

    public static ConnectionMonitor getConnectionMonitor(){
        return connectionMonitor;
    }

    public static AppDatabase getAppDatabase(){
        return appDatabase;
    }
}
