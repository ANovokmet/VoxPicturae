package hr.image;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import hr.probavoice.R;
import jp.co.cyberagent.android.gpuimage.GPUImage;

/**
 * Created by Ante on 28.3.2016..
 */
public class CameraController implements BaseCameraController {

    private Activity activity;

    private int mCurrentCameraId;
    private Camera mCamera;
    private GPUImage mGPUImage;
    private Camera.Parameters mParameters;

    private int numberOfCameras;
    private int screenWidth;
    private int screenHeight;
    private float screenAspectRatio;

    public CameraController(Activity activity, GPUImage gpuImage){
        mGPUImage = gpuImage;
        numberOfCameras = Camera.getNumberOfCameras();
        mCurrentCameraId = 0;
        this.activity = activity;
    }

    public void setAreaSize(int width, int height){
        Log.d("Setting optimal size to", width+"x"+height);
        screenWidth = width;
        screenHeight = height;
        screenAspectRatio = (float)height / width;
    }

    @Override
    public void takePicture() {
        if (mParameters.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            doCapture();
        } else {
            mCamera.autoFocus(new Camera.AutoFocusCallback() {

                @Override
                public void onAutoFocus(final boolean success, final Camera camera) {
                    doCapture();
                }

            });
        }
    }

    private void doCapture(){
        mParameters.setRotation(getDisplayOrientation());
        setupPictureSize();
        mCamera.setParameters(mParameters);
        mCamera.takePicture(null, null, pictureCallback);
    }

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data,final Camera camera) {
            final File pictureFile = PictureFileManager.getSaveFile();

            if(pictureFile == null){
                Log.d("Picture Callback error","Picture file was not created");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("ASDF", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("ASDF", "Error accessing file: " + e.getMessage());
            }

            data = null;
            Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
            final GLSurfaceView view = (GLSurfaceView) activity.findViewById(R.id.surfaceView);
            view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            mGPUImage.saveToPictures(bitmap, "PicSona",
                    "PicSona_"+PictureFileManager.createFileName() + ".jpg",
                    new GPUImage.OnPictureSavedListener() {

                        @Override
                        public void onPictureSaved(final Uri uri) {
                            pictureFile.delete();
                            camera.startPreview();
                            view.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                        }
                    });


        }
    };


    @Override
    public void cycleCamera() {
        releaseCamera();
        mGPUImage.deleteImage();
        mCurrentCameraId = (mCurrentCameraId + 1) % numberOfCameras;
        setupCamera(mCurrentCameraId);
    }

    @Override
    public void reSetupCamera() {
        setupCamera(mCurrentCameraId);
    }

    @Override
    public void stopCamera() {
        releaseCamera();
    }

    private void setupCamera(int cameraId) {
        mCamera = startCamera(cameraId);
        mParameters = mCamera.getParameters();
        setupPreviewSize();

        if(mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCamera.setParameters(mParameters);

        //orientation


        //flip vertical
        //flip horizontal
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, cameraInfo);
        boolean flipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        mGPUImage.setUpCamera(mCamera, getCameraDisplayOrientation(), flipHorizontal, false);
    }

    private void setupPreviewSize() {
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
        Camera.Size optimalPreviewSize = getOptimalSize(sizes);
        mParameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
    }

    private void setupPictureSize() {
        List<Camera.Size> sizes = mParameters.getSupportedPictureSizes();
        Camera.Size optimalPreviewSize = getOptimalSize(sizes);
        mParameters.setPictureSize(optimalPreviewSize.width, optimalPreviewSize.height);
    }


    private Camera.Size getOptimalSize(List<Camera.Size> sizes){
        Camera.Size optimalSizeUnrationed = null;
        Camera.Size optimalSizeRationed = null;

        int currentDifference = Integer.MAX_VALUE;

        for(Camera.Size size : sizes){
            float previewRatio = (float) size.width / size.height;

            int difference = Math.abs(size.height - screenHeight);

            if(difference < currentDifference){
                currentDifference = difference;

                if(previewRatio == screenAspectRatio){
                    optimalSizeRationed = size;
                }
                else{
                    optimalSizeUnrationed = size;
                }
            }
        }

        if(optimalSizeRationed != null){
            return optimalSizeRationed;
        }
        else{
            return optimalSizeUnrationed;
        }
    }

    private void releaseCamera() {
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    private Camera startCamera(int cameraId) {
        try{
            return Camera.open(cameraId);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private int getCameraDisplayOrientation(){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, cameraInfo);

        int rotation = getDisplayOrientation();

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            return (cameraInfo.orientation + rotation) % 360;
        }
        else{
            return (cameraInfo.orientation - rotation + 360) % 360;
        }
    }

    private int getDisplayOrientation(){
        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotation = 0;
        switch (displayRotation) {
            case Surface.ROTATION_0: rotation = 0; break;
            case Surface.ROTATION_90: rotation = 90; break;
            case Surface.ROTATION_180: rotation = 180; break;
            case Surface.ROTATION_270: rotation = 270; break;
        }
        return rotation;
    }
}
