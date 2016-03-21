package hr.image;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import hr.probavoice.CameraActivity;
import hr.probavoice.MainActivity;
import hr.probavoice.R;

/**
 * Created by Ante on 28.2.2016..
 */
@SuppressWarnings("Deprecation")
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

    public ImageView imageView;

    private Context context;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.Parameters parameters;
    private BitmapImageProcessingThread imageProcessingThread;

    @SuppressWarnings("deprecation")
    public CameraView(Context context, Camera camera){
        super(context);
        this.context = context;

        mCamera = camera;

        setCameraDisplayOrientation((Activity)context, 0, mCamera);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);

        parameters = mCamera.getParameters();

        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = sizes.get(2);//ovdje uzimam pocetnu rezoluciju

        setPreviewSize(previewSize.width, previewSize.height);





        //testiranje maske, array unutar native koda ne ostane inicijaliziran ako se loadMask pozove iz druge klase
        // maska se iscrtava na vrh imageviewa
        // trebalo bi jednostavno u photoshopu uredit masku da ima prozirnost i sve
        Bitmap bitmapMask = BitmapFactory.decodeResource(getResources(), R.drawable.mask2);
        bitmapMask = Bitmap.createScaledBitmap(bitmapMask, previewSize.height, previewSize.width, false);
        bitmapMask.setHasAlpha(true);
        for (int x = 0; x < bitmapMask.getWidth(); x++) {
            for (int y = 0; y < bitmapMask.getHeight(); y++) {

                int pixel = bitmapMask.getPixel(x,y);

                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.green(pixel);

                int a = r>=128 ? 0 : (int)((255-r*2)*0.75f);

                bitmapMask.setPixel(x, y, Color.argb(a,r,g,b));
            }
        }
        int[] intArray = new int[previewSize.height*previewSize.width];
        bitmapMask.getPixels(intArray, 0, previewSize.height, 0, 0, previewSize.height, previewSize.width);




        imageProcessingThread = new BitmapImageProcessingThread(this, intArray, previewSize.height, previewSize.width);
        imageProcessingThread.setPreviewSize(previewSize);
    }

    public void resumeViewWithCamera(Camera camera){
        mCamera = camera;
        setCameraDisplayOrientation((Activity)context, 0, mCamera);

        try{
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);

            mCamera.setParameters(parameters);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public List<Camera.Size> getPreviewSizes(){
        return parameters.getSupportedPreviewSizes();
    }

    public void setPreviewSize(int width, int height){
        mCamera.stopPreview();
        parameters.setPreviewSize(width, height);
        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    public Camera.Size getCurrentPreviewSize(){
        return parameters.getPreviewSize();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try{

            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.setPreviewCallback(this);

            mCamera.setParameters(parameters);
            mCamera.startPreview();

        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceCreated " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        //u slucaju rotiranja uredjaja
        if(mHolder.getSurface() == null){
            return;
        }

        try{
            mCamera.stopPreview();
        } catch (Exception e){
            Log.d("ERROR", "Camera error on surfaceChanged (Camera not running) " + e.getMessage());
        }

        try{
            mCamera.setPreviewDisplay(mHolder);
            mCamera.setPreviewCallback(this);

            mCamera.setParameters(parameters);
            imageProcessingThread.setPreviewSize(parameters.getPreviewSize());
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.stopPreview();
        //mCamera.release();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if ( !imageProcessingThread.isRunning() )
        {
            //Log.d("Adress",data+"");
            imageProcessingThread.setData(data);
            new Thread(imageProcessingThread).start();
        }
    }

    public void setImageViewImage(Bitmap bitmap){
        imageView.setImageBitmap(bitmap);
    }

}
