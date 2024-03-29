package trelico.ru.allcastmvvm.repositories.local_files;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import trelico.ru.allcastmvvm.MyApp;

import static trelico.ru.allcastmvvm.MyApp.I_TAG;

public class FileStorage{

    private Context context = MyApp.INSTANCE;

    public void saveAudioFile(ResponseBody body, String fileName,
                              FileSavingCallback callback){
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
                callback.onFileSaved(true);
            } catch(IOException e){
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
            callback.onFileSaved(false);
        }
    }

    public boolean saveAudioFile(ResponseBody body, String fileName){
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
                return true;
            } catch(IOException e){
                return false;
            } finally{
                if(inputStream != null){
                    inputStream.close();
                }

                if(outputStream != null){
                    outputStream.close();
                }
            }
        } catch(IOException e){
            return false;
        }
    }

    public boolean saveAudioFile(InputStream inputStream, String fileName){
        try{
            File audioFile = new File(fileName);
            OutputStream outputStream = null;
            try{
                byte[] fileReader = new byte[4096];
                outputStream = new FileOutputStream(audioFile);
                while(true){
                    int read = inputStream.read(fileReader);
                    if(read == -1){
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                }
                outputStream.flush();
                return true;
            } catch(IOException e){
                return false;
            } finally{
                if(inputStream != null){
                    inputStream.close();
                }

                if(outputStream != null){
                    outputStream.close();
                }
            }
        } catch(IOException e){
            return false;
        }
    }

}
