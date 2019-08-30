package trelico.ru.allcastmvvm.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;

import static trelico.ru.allcastmvvm.utils.ConnectionMonitor.ConnectionState.*;

public class ConnectionMonitor{

    private Context context;
    private BehaviorSubject<ConnectionState> source = BehaviorSubject.create();
    private IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);

    public ConnectionMonitor(Context context){
        this.context = context;
    }

    public Disposable subscribe(Consumer<ConnectionState> consumer){
        if(!source.hasObservers()) context.registerReceiver(networkReceiver, filter);
        return source.subscribe(consumer);
    }

    public void unsubscribeAll(){
        source.onComplete();
        context.unregisterReceiver(networkReceiver);
    }

    private BroadcastReceiver networkReceiver = new BroadcastReceiver(){
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent){
            if(intent.getExtras() != null){
                NetworkInfo activeNetwork = (NetworkInfo) intent.getExtras().get(ConnectivityManager.EXTRA_NETWORK_INFO);
                boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
                if(isConnected){
                    switch(activeNetwork.getType()){
                        case ConnectivityManager.TYPE_WIFI:
                            source.onNext(new ConnectionState(WIFI));
                            break;
                        case ConnectivityManager.TYPE_MOBILE:
                            source.onNext(new ConnectionState(MOBILE));
                            break;
                    }
                } else{
                    source.onNext(new ConnectionState(DISCONNECTED));
                }
            }
        }
    };

    public class ConnectionState{

        private int type;

        public static final int MOBILE = 2;
        public static final int WIFI = 1;
        public static final int DISCONNECTED = 0;

        public ConnectionState(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }
    }
}
