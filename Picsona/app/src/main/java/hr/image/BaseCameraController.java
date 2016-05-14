package hr.image;

import android.hardware.Camera;

/**
 * Created by Ante on 28.3.2016..
 */
public interface BaseCameraController {
    void takePicture();
    void cycleCamera();
    void reSetupCamera();
    void stopCamera();
}
