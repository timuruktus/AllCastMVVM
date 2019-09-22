package trelico.ru.allcastmvvm.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import trelico.ru.allcastmvvm.MyApp;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class AndroidUtils{

    public static boolean isAppAvailable(Context context, String appName){
        PackageManager pm = context.getPackageManager();
        try{
            pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch(PackageManager.NameNotFoundException e){
            return false;
        }
    }


    public static void bringActivityToForeground(Context context, Class className){
        Intent foregroundIntent = new Intent(context, className);
        foregroundIntent.setFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        context.startActivity(foregroundIntent);
    }

    public static String getAudioFilesDir(){
        return MyApp.INSTANCE.getFilesDir().getAbsolutePath();
    }

    public static long getCurrentSystemTime(){
        return System.currentTimeMillis();
    }
}
