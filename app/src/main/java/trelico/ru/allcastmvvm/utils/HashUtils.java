package trelico.ru.allcastmvvm.utils;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static trelico.ru.allcastmvvm.MyApp.I_TAG;

public class HashUtils{

    private static MessageDigest messageDigest;


    private static String bytesToHexString(byte[] bytes){
        // http://stackoverflow.com/questions/332079
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < bytes.length; i++){
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if(hex.length() == 1){
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    // generate a hash
    public static String getHash(String text){
        String hash;
        try{
            if(messageDigest == null) messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(text.getBytes());
            hash = bytesToHexString(messageDigest.digest());
            Log.d(I_TAG, "Hashing result is " + hash);
            return hash;
        } catch(NoSuchAlgorithmException ex){
            ex.printStackTrace();
            Log.e(I_TAG, "Error in hashing " + ex.getMessage());
            return "";
        }
    }
}
