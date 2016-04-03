package hr.image;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Ante on 29.3.2016..
 */
public class PictureFileManager {


    public static File getSaveFile(){

        boolean isExternalStorageAvailable = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);

        if(isExternalStorageAvailable){
            File storageDirectory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "PicSona");

            if(!storageDirectory.exists()){
                if(!storageDirectory.mkdirs()){
                    Log.e("Storage directory", "Failed to create it");
                    return null;
                }
            }

            return new File(storageDirectory, createFileName() + ".jpg");



        }
        else {
            Log.e("External storage", "Not available");
            return null;
        }
    }

    public static String createFileName(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hhmmss_ddMMyyyy");
        return simpleDateFormat.format(new Date());
    }
}
