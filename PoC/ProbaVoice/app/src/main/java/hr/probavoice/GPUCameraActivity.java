package hr.probavoice;

import android.app.Activity;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;

import hr.image.CameraController;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSwirlFilter;
import jp.co.cyberagent.android.gpuimage.ProbaVoiceFilter;

public class GPUCameraActivity extends Activity {

    private GPUImage mGPUImage;
    private GPUImageFilter mFilter;
    private CameraController mCameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpucamera);

        mGPUImage = new GPUImage(this);

        GLSurfaceView glSurfaceView = (GLSurfaceView)findViewById(R.id.surfaceView);
        mGPUImage.setGLSurfaceView(glSurfaceView);

        switchCameraButton = (Button)findViewById(R.id.buttonSwitchCamera);
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        Button takePictureButton = (Button)findViewById(R.id.buttonTakePicture);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        mCameraController = new CameraController(mGPUImage);

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        mCameraController.setAreaSize(width, height);

        mGPUImage.setFilter(new ProbaVoiceFilter());

    }

    Button switchCameraButton;

    @Override
    protected void onResume() {
        super.onResume();
        mCameraController.reSetupCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraController.stopCamera();
    }

    private void switchCamera(){
        switchCameraButton.setClickable(false);
        mCameraController.cycleCamera();
        switchCameraButton.setClickable(true);
    }

    private void takePicture(){
        mCameraController.takePicture();
    }

}
