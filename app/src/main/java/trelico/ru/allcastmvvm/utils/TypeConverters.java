package trelico.ru.allcastmvvm.utils;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class TypeConverters{

    @TypeConverter
    public static ArrayList<String> convertToStringArrayList(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String convertFromStringArrayList(ArrayList<String> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }

    @TypeConverter
    public static ArrayList<Long> convertToLongArrayList(String value) {
        Type listType = new TypeToken<ArrayList<Long>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String convertFromLongArrayList(ArrayList<Long> list) {
        Gson gson = new Gson();
        return gson.toJson(list);
    }
}
