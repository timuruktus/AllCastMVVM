package trelico.ru.allcastmvvm;

import android.app.Application;

import androidx.room.Room;

import com.google.android.gms.ads.MobileAds;

import trelico.ru.allcastmvvm.data_sources.local.AppDatabase;
import trelico.ru.allcastmvvm.utils.ConnectionMonitor;


public class MyApp extends Application{

    public static final String D_TAG = "Debugtag";
    public static final String I_TAG = "Infotag";
    public static final String E_TAG = "Errortag";
    public static MyApp INSTANCE;
    private AppDatabase appDatabase;
    private static ConnectionMonitor connectionMonitor;

    @Override
    public void onCreate(){
        super.onCreate();
        INSTANCE = this;
        MobileAds.initialize(this);
        connectionMonitor = new ConnectionMonitor(this);
        appDatabase =  Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "database")
                .fallbackToDestructiveMigration() //TODO: delete later in production
                .build();
    }

    public static ConnectionMonitor getConnectionMonitor(){
        return connectionMonitor;
    }

    public AppDatabase getAppDatabase(){
        return appDatabase;
    }
}
