package hr.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.Image;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import hr.probavoice.CameraActivity;
import hr.probavoice.MainActivity;

/**
 * Created by Ante on 28.2.2016..
 */
public class CameraView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback{

    public ImageView imageView;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Camera.Parameters parameters;
    private BitmapImageProcessingThread imageProcessingThread;

    @SuppressWarnings("deprecation")
    public CameraView(Context context, Camera camera){
        super(context);
        mCamera = camera;
        mCamera.setDisplayOrientation(90);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);

        parameters = mCamera.getParameters();

        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size previewSize = sizes.get(sizes.size() - 3);//ovdje uzimam pocetnu rezoluciju (3. najveca)

        setPreviewSize(previewSize.width, previewSize.height);

        imageProcessingThread = new BitmapImageProcessingThread(this);
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
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.stopPreview();
        mCamera.release();
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if ( !imageProcessingThread.isRunning() )
        {
            imageProcessingThread.setData(data);
            imageProcessingThread.setPreviewSize(camera.getParameters().getPreviewSize());
            new Thread(imageProcessingThread).start();
        }
    }

    public void setImageViewImage(Bitmap bitmap){
        imageView.setImageBitmap(bitmap);
    }

}
