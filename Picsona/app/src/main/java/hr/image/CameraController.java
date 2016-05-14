package hr.image;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import hr.picsona.R;
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

    private float optimalAspectRatio = 4.f / 3;

    private GLSurfaceView mGlSurfaceView;

    int pictureWidth, pictureHeight;
    float pictureAspectRatio;
    private boolean mFlipHorizontal;

    OverlayGenerator emojiOverlayGenerator;
    int optimalPreviewWidth, optimalPreviewHeight;

    public CameraController(Activity activity, GPUImage gpuImage, GLSurfaceView glSurfaceView, GPUImage.OnPictureSavedListener clientCallback) {
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
     *
     * @param width
     * @param height
     */
    public void setDesiredPreviewSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        screenAspectRatio = (float) height / width;
    }


    public void setDesiredPictureSize(int width, int height) {
        pictureWidth = width;
        pictureHeight = height;
        pictureAspectRatio = (float) height / width;
    }

    public int getPreviewWidth() {
        return optimalPreviewWidth;
    }

    public int getPreviewHeight() {
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

    private void doCapture() {
        mParameters.setRotation(getDisplayOrientation());
        setupPictureSize();
        mCamera.setParameters(mParameters);
        mCamera.takePicture(null, null, pictureCallback);
    }


    private Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, final Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            final GLSurfaceView view = mGlSurfaceView;
            view.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


            PictureSaveExecutor pictureSaveExecutor = new PictureSaveExecutor(activity, mGPUImage);

            pictureSaveExecutor.saveToPicturesWithOverlay(bitmap, emojiOverlayGenerator, getImageNeededRotation(), mFlipHorizontal, activity.getString(R.string.saveImageFolder),
                    activity.getString(R.string.fileNamePrefix) + PictureFileManager.createFileName() + ".jpg",
                    new GPUImage.OnPictureSavedListener() {

                        @Override
                        public void onPictureSaved(final Uri uri) {
                            String path = mGPUImage.getPath(uri);
                            releaseCamera();
                            if (uri.getPath() == null) {
                                showToast(activity.getString(R.string.saveImageErrorString));
                                reSetupCamera();
                            } else {
                                mClientCallback.onPictureSaved(uri);
                                showToast(activity.getString(R.string.saveImageSuccess) + path);
                            }
                        }
                    });


        }
    };

    private void showToast(String message) {
        Toast toast = Toast.makeText(activity, message, Toast.LENGTH_SHORT);
        toast.getView().setBackgroundColor(activity.getResources().getColor(R.color.buttonEnabled));
        ((TextView) toast.getView().findViewById(android.R.id.message)).setTextColor(activity.getResources().getColor(R.color.white));
        toast.show();
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
        if (mCamera != null) {
            return;
        }
        mCamera = startCamera(cameraId);
        mParameters = mCamera.getParameters();
        setupPreviewSize();

        if (mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCamera.setParameters(mParameters);

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, cameraInfo);
        mFlipHorizontal = cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT;
        mGPUImage.setUpCamera(mCamera, getCameraDisplayOrientation(), mFlipHorizontal, false);

    }

    private void setupPreviewSize() {
        List<Camera.Size> sizes = mParameters.getSupportedPreviewSizes();

        for (Camera.Size size : sizes) {
            Log.d("prew supp", size.width + "x" + size.height + " r:" + ((float) size.width / size.height));
        }

        Camera.Size optimalPreviewSize = getOptimalSize(sizes, screenHeight, true);
        Log.d("prew opti", optimalPreviewSize.width + "x" + optimalPreviewSize.height + " r:" + ((float) optimalPreviewSize.width / optimalPreviewSize.height));

        optimalPreviewWidth = optimalPreviewSize.width;
        optimalPreviewHeight = optimalPreviewSize.height;

        mParameters.setPreviewSize(optimalPreviewSize.width, optimalPreviewSize.height);
    }

    private void setupPictureSize() {
        List<Camera.Size> sizes = mParameters.getSupportedPictureSizes();
        for (Camera.Size size : sizes) {
            Log.d("pict supp", size.width + "x" + size.height + " r:" + ((float) size.width / size.height));
        }
        Camera.Size optimalPictureSize = getOptimalSize(sizes, pictureHeight, true);
        Log.d("pict opti", optimalPictureSize.width + "x" + optimalPictureSize.height + " r:" + ((float) optimalPictureSize.width / optimalPictureSize.height));

        mParameters.setPictureSize(optimalPictureSize.width, optimalPictureSize.height);
    }

    private Camera.Size getBiggestSize(List<Camera.Size> sizes) {
        Camera.Size optimalSizeUnrationed = null;
        Camera.Size optimalSizeRationed = null;

        int biggestWidth = 0;

        for (Camera.Size size : sizes) {
            float previewRatio = (float) size.width / size.height;


            if (biggestWidth < size.width) {
                biggestWidth = size.width;

                if (previewRatio == optimalAspectRatio) {
                    optimalSizeRationed = size;
                } else {
                    optimalSizeUnrationed = size;
                }
            }
        }

        if (optimalSizeRationed != null) {
            return optimalSizeRationed;
        } else {
            return optimalSizeUnrationed;
        }
    }

    private Camera.Size getOptimalSize(List<Camera.Size> sizes, int optimalSize, boolean compareToHeight) {
        Camera.Size optimalSizeUnrationed = null;
        Camera.Size optimalSizeRationed = null;

        int currentDifference = Integer.MAX_VALUE;

        for (Camera.Size size : sizes) {
            float previewRatio = (float) size.width / size.height;

            int difference;
            if (compareToHeight) {
                difference = Math.abs(size.height - optimalSize);
            } else {
                difference = Math.abs(size.width - optimalSize);
            }

            if (difference < currentDifference) {
                currentDifference = difference;

                if (previewRatio == optimalAspectRatio) {
                    optimalSizeRationed = size;
                } else {
                    optimalSizeUnrationed = size;
                }
            }
        }

        if (optimalSizeRationed != null) {
            return optimalSizeRationed;
        } else {
            return optimalSizeUnrationed;
        }
    }

    private void releaseCamera() {
        if (mCamera == null) {
            return;
        }
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    private Camera startCamera(int cameraId) {
        try {
            return Camera.open(cameraId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getCameraDisplayOrientation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, cameraInfo);

        int rotation = getDisplayOrientation();

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            return (cameraInfo.orientation + rotation) % 360;
        } else {
            return (cameraInfo.orientation - rotation + 360) % 360;
        }
    }

    private int getDisplayOrientation() {
        int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int rotation = 0;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                rotation = 0;
                break;
            case Surface.ROTATION_90:
                rotation = 90;
                break;
            case Surface.ROTATION_180:
                rotation = 180;
                break;
            case Surface.ROTATION_270:
                rotation = 270;
                break;
        }
        return rotation;
    }

    private int getImageNeededRotation() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCurrentCameraId, cameraInfo);

        int rotation = getDisplayOrientation();

        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            rotation = (cameraInfo.orientation + rotation + 180) % 360;
        } else {
            rotation = (cameraInfo.orientation - rotation + 360) % 360;
        }
        return rotation;
    }

    public void setOverlayGenerator(OverlayGenerator overlayGenerator) {
        emojiOverlayGenerator = overlayGenerator;
    }
}
