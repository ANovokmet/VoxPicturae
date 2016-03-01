package hr.image;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.ByteArrayOutputStream;

import java.util.logging.LogRecord;

import hr.probavoice.CameraActivity;
import hr.probavoice.MainActivity;

/**
 * Created by Ante on 29.2.2016..
 */
public class BitmapImageProcessingThread implements Runnable {

    private boolean processing = false;
    private byte[] data;
    private Camera.Size previewSize;
    private Bitmap result;
    private CameraView cameraView;
    private Handler uiHandler;

    @SuppressWarnings("JNI")
    public native Bitmap loadBitmap(Bitmap bitmap);

    public BitmapImageProcessingThread(CameraView cameraView){
        this.cameraView = cameraView;
        uiHandler = new Handler(Looper.getMainLooper());
    }

    public void setData(byte[] data){
        this.data = data;
    }

    public void setPreviewSize(Camera.Size previewSize){
        this.previewSize = previewSize;
    }

    @Override
    public void run()
    {
        processing = true;

        try {
            YuvImage yuvimage = new YuvImage(data, ImageFormat.NV21, previewSize.width, previewSize.height, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 80, baos);
            byte[] jdata = baos.toByteArray();

            // Convert to Bitmap
            Bitmap bmp = BitmapFactory.decodeByteArray(jdata, 0, jdata.length);

            result = loadBitmap(bmp);

            onFinished();
        }
        catch (Exception e){
            Log.d("ERROR", "Error processing " + e.getMessage());
        }

        processing = false;
    }

    public void onFinished(){
        uiHandler.post(publishImage);
    }

    private Runnable publishImage = new Runnable(){
        @Override
        public void run() {
            cameraView.setImageViewImage(result);
        }
    };

    public boolean isRunning(){
        return processing;
    }
}
