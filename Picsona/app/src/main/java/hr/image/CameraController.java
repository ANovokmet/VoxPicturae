package hr.image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.widget.Toast;

import java.util.List;

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
    private GPUImage.OnPictureSavedListener mClientCallback;

    private int numberOfCameras;
    private int screenWidth;
    private int screenHeight;
    private float screenAspectRatio;

    private float optimalAspectRatio = 4.f/3;

    private GLSurfaceView mGlSurfaceView;

    public CameraController(Activity activity, GPUImage gpuImage, GLSurfaceView glSurfaceView, GPUImage.OnPictureSavedListener clientCallback){
        mGPUImage = gpuImage;
        numberOfCameras = Camera.getNumberOfCameras();
        mCurrentCameraId = 0;
        mGlSurfaceView = glSurfaceView;
        this.activity = activity;
        mClientCallback = clientCallback;

    }

    /**
     * This value is used for choosing the optimal preview resolution. The one with the
     * closest size will be chosen.
     * @param width
     * @param height
     */
    public void setDesiredPreviewSize(int width, int height){
        Log.d("Set opti prev size to", width+"x"+height);
        screenWidth = width;
        screenHeight = height;
        screenAspectRatio = (float)height / width;
    }


    int pictureWidth, pictureHeight;
    float pictureAspectRatio;

    public void setDesiredPictureSize(int width, int height){
        Log.d("Set opti pict size to", width+"x"+height);
        pictureWidth = width;
        pictureHeight = height;
        pictureAspectRatio = (float)height / width;
    }


    public int getAreaWidth(){
        return screenWidth;
    }
    public int getAreaHeight(){
        return screenHeight;
    }

    public int getPreviewWidth(){
        return optimalPreviewWidth;
    }

    public int getPreviewHeight(){
        return optimalPreviewHeight;
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
        mParameters.setRotation(getDisplayOrientation());//nema utjecaja >>gpuimage.java
        setupPictureSize();
        mCamera.setParameters(mParameters);
        mCamera.takePicture(null, null, pictureCallback);
    }

    //public Bitmap mOverlayBitmap;
    private boolean mFlipHorizontal;

    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data,final Camera camera) {
            /*final File pictureFile = PictureFileManager.getSaveFile();

            if(pictureFile == null){
                Log.d("Picture Callback error","Picture file was not created");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("Picture Callback error", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("Picture Callback error", "Error accessing file: " + e.getMessage());
            }

            data = null;*/
            //Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
            int format = camera.getParameters().getPictureFormat();
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            final GLSurfaceView view = mGlSurfaceView;
            view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


            SaverTasker saverTasker = new SaverTasker(activity,mGPUImage);

            saverTasker.saveToPicturesWithOverlay(bitmap, mOverlayGenerator, getImageNeededRotation(), mFlipHorizontal, "Picsona",
                    "Picsona_" + PictureFileManager.createFileName() + ".jpg",
                    new GPUImage.OnPictureSavedListener() {

                        @Override
                        public void onPictureSaved(final String path) {
                            if (path == null) {
                                Toast.makeText(activity, "Error while saving image", Toast.LENGTH_SHORT).show();
                                resumeCamera();
                            } else {
                                mClientCallback.onPictureSaved(path);
                                Toast.makeText(activity, "Picture saved at " + path, Toast.LENGTH_SHORT).show();
                            }
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
    public void resumeCamera() {
        mCamera.startPreview();
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
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
        mFlipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        mGPUImage.setUpCamera(mCamera, getCameraDisplayOrientation(), mFlipHorizontal, false);

    }

    private void setupPreviewSize() {
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();

        for(Camera.Size size : sizes){
            Log.d("prew supp",size.width+"x"+size.height+" r:"+((float)size.width/size.height));
        }

        Camera.Size optimalPreviewSize = getOptimalSize(sizes, screenHeight, true);
        Log.d("prew opti",optimalPreviewSize.width+"x"+optimalPreviewSize.height+" r:"+((float)optimalPreviewSize.width/optimalPreviewSize.height));

        optimalPreviewWidth = optimalPreviewSize.width;
        optimalPreviewHeight = optimalPreviewSize.height;

        mParameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
    }

    int optimalPreviewWidth, optimalPreviewHeight;

    private void setupPictureSize() {
        List<Camera.Size> sizes = mParameters.getSupportedPictureSizes();
        for(Camera.Size size : sizes){
            Log.d("pict supp",size.width+"x"+size.height+" r:"+((float)size.width/size.height));
        }
        Camera.Size optimalPictureSize = getOptimalSize(sizes, pictureHeight, true);
        Log.d("pict opti",optimalPictureSize.width+"x"+optimalPictureSize.height+" r:"+((float)optimalPictureSize.width/optimalPictureSize.height));

        mParameters.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);

        //mOverlayBitmap = mOverlayGenerator.reCreateOverlayForSize(optimalPreviewSize.height, optimalPreviewSize.width);
    }

    private Camera.Size getBiggestSize(List<Camera.Size> sizes){
        Camera.Size optimalSizeUnrationed = null;
        Camera.Size optimalSizeRationed = null;

        int biggestWidth = 0;

        for(Camera.Size size : sizes){
            float previewRatio = (float) size.width / size.height;


            if(biggestWidth < size.width){
                biggestWidth = size.width;

                if(previewRatio == optimalAspectRatio){
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


    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int optimalSize, boolean compareToHeight){
        Camera.Size optimalSizeUnrationed = null;
        Camera.Size optimalSizeRationed = null;

        int currentDifference = Integer.MAX_VALUE;

        for(Camera.Size size : sizes){
            float previewRatio = (float) size.width / size.height;

            int difference;
            if(compareToHeight){
                difference = Math.abs(size.height - optimalSize);
            }
            else {
                difference = Math.abs(size.width - optimalSize);
            }


            if(difference < currentDifference){
                currentDifference = difference;

                if(previewRatio == optimalAspectRatio){
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

    /**
     * empirical data
     */
    private int getImageNeededRotation(){
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, cameraInfo);

        int rotation = getDisplayOrientation();

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            rotation = (cameraInfo.orientation + rotation + 180) % 360;
        }
        else{
            rotation = (cameraInfo.orientation - rotation + 360) % 360;
        }
        return rotation;
    }


    OverlayGenerator mOverlayGenerator;
    public void setOverlayGenerator(OverlayGenerator overlayGenerator) {
        mOverlayGenerator = overlayGenerator;
    }
}
