package hr.image;

import android.content.Context;
import android.hardware.Camera;
import android.util.DisplayMetrics;

import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImage;

/**
 * Created by Ante on 28.3.2016..
 */
public class CameraController implements BaseCameraController {

    private int mCurrentCameraId;
    private Camera mCamera;
    private GPUImage mGPUImage;
    private Camera.Parameters mParameters;

    private int numberOfCameras;
    private int screenWidth;
    private int screenHeight;
    private float screenAspectRatio;

    public CameraController(GPUImage gpuImage){
        mGPUImage = gpuImage;
        numberOfCameras = Camera.getNumberOfCameras();
        mCurrentCameraId = 0;
    }

    public void setAreaSize(int width, int height){
        screenWidth = width;
        screenHeight = height;
        screenAspectRatio = (float)height / width;
    }

    @Override
    public void takePicture() {

    }

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

        //flip horizontal

        //flip vertical

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, cameraInfo);
        boolean flipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        mGPUImage.setUpCamera(mCamera, 90, flipHorizontal, false);
    }

    private void setupPreviewSize() {
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();
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
            mParameters.setPreviewSize(optimalSizeRationed.width, optimalSizeRationed.height);
        }
        else{
            mParameters.setPreviewSize(optimalSizeUnrationed.width, optimalSizeUnrationed.height);
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
}
