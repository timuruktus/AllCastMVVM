package trelico.ru.allcastmvvm.data_sources.local;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.subjects.PublishSubject;
import okhttp3.ResponseBody;
import trelico.ru.allcastmvvm.MyApp;

import static trelico.ru.allcastmvvm.MyApp.D_TAG;
import static trelico.ru.allcastmvvm.MyApp.I_TAG;

public class FileStorage{

    private Context context = MyApp.INSTANCE;

//    @SuppressLint("CheckResult")
//    public void saveStringToFile(String fileName, String stringToSave){
//        Observable.empty()
//                .onErrorReturn(error -> Log.e(E_TAG, error.getMessage()))
//                .subscribe(o -> {
//                    Context context = MyApp.INSTANCE;
//                    FileOutputStream outputStream;
//                    outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
//                    outputStream.write(stringToSave.getBytes());
//                    outputStream.close();
//        });
//
//    }

//    /**
//     * @param fileName     - filename to new file
//     * @param stringToSave - string to save in new file
//     * @return - emits false in LiveData if error happened, otherwise true
//     */
//    public LiveData<Boolean> saveStringToFile(String fileName, String stringToSave){
//        MutableLiveData<Boolean> completableLiveData = new MutableLiveData<>();
//        ExecutorService executorService = Executors.newSingleThreadExecutor();
//        executorService.execute(() -> {
//            FileOutputStream outputStream;
//            try{
//                outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
//                outputStream.write(stringToSave.getBytes());
//                outputStream.close();
//                completableLiveData.postValue(true);
//            } catch(FileNotFoundException ex){
//                ex.printStackTrace();
//                completableLiveData.postValue(false);
//            } catch(IOException ex){
//                ex.printStackTrace();
//                completableLiveData.postValue(false);
//            }
//        });
//        return completableLiveData;
//    }

    public void saveAudioFile(ResponseBody body, String fileName,
                                            FileSavingCallback callback){
        Log.d(D_TAG, "saveAudioFile in FileStorage");
        try{
            File audioFile = new File(fileName);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try{
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(audioFile);

                while(true){
                    int read = inputStream.read(fileReader);

                    if(read == -1){
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(I_TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                Log.d(D_TAG, "near first onNext() in FileStorage()");
                callback.onFileSaved(true);

            } catch(IOException e){
                Log.d(D_TAG, "near second onNext() in FileStorage()");
                callback.onFileSaved(false);
            } finally{
                if(inputStream != null){
                    inputStream.close();
                }

                if(outputStream != null){
                    outputStream.close();
                }
            }
        } catch(IOException e){
            Log.d(D_TAG, "near third onNext() in FileStorage()");
            callback.onFileSaved(false);
        }
    }

}
