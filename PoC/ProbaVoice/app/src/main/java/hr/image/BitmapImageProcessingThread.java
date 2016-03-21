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

import java.nio.IntBuffer;
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
    private int[] rgb8888_data;
    private int[] rgb8888_help_data;

    @SuppressWarnings("JNI")
    public native Bitmap loadBitmap(Bitmap bitmap);
    public native boolean convertYUV420_NV21toRGB8888(byte[] N21FrameData, int width, int height, int[] pixels, int[] helppixels);
    public native boolean loadMask(int[] pixels, int width, int height);

    public BitmapImageProcessingThread(CameraView cameraView, int[] pixels, int w, int h){
        this.cameraView = cameraView;
        uiHandler = new Handler(Looper.getMainLooper());
        loadMask(pixels, w, h);
    }

    public void setData(byte[] data){
        this.data = data;
    }

    public void setPreviewSize(Camera.Size previewSize){
        this.previewSize = previewSize;


        //originally previewSize.width, previewSize.height
        result = Bitmap.createBitmap(previewSize.height, previewSize.width, Bitmap.Config.ARGB_8888);
        rgb8888_data = new int[previewSize.width * previewSize.height];
        rgb8888_help_data = new int[previewSize.width * previewSize.height];
    }

    @Override
    public void run()
    {
        processing = true;

        try {

            convertYUV420_NV21toRGB8888(data, previewSize.width, previewSize.height, rgb8888_data, rgb8888_help_data);
            result.copyPixelsFromBuffer(IntBuffer.wrap(rgb8888_data));

            //Log.d("rgb8888_data.length",rgb8888_data.length+"");

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
