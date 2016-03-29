package hr.probavoice;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Visibility;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

import hr.image.CameraView;

public class CameraActivity extends ActionBarActivity {

    private Camera mCamera = null;
    private CameraView mCameraView = null;

    private int cameraId = 0;

    List<Camera.Size> sizeList;

    public native boolean loadMask(int[] pixels, int width, int height);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        newOpenCamera();
        initializeCameraView();



        Camera.Size currentSize = mCameraView.getCurrentPreviewSize();


        //testiranje maske/okvira
        // maska se iscrtava na vrh imageviewa
        // trebalo bi jednostavno u photoshopu uredit masku da ima prozirnost i sve
        // zbog ovoga stucne na 0.3s pri loadanju
        // isti kod u cameraview


        ImageView imageViewMask = (ImageView)findViewById(R.id.imageViewMask);

        Bitmap bitmapMask = BitmapFactory.decodeResource(getResources(), R.drawable.mask2);
        bitmapMask = Bitmap.createScaledBitmap(bitmapMask, currentSize.height, currentSize.width, false);
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
        //imageViewMask.setRotation(90);//nekad sam rotirao cijeli imageview
        imageViewMask.setImageBitmap(bitmapMask);
        imageViewMask.setVisibility(View.VISIBLE);//micanje/stavljanje maske

        /*int[] intArray = new int[288*352];
        bitmapMask.getPixels(intArray, 0, 288, 0, 0, 288, 352);
        loadMask(intArray, 288, 352);*/
    }

    public void initializeCamera() {
        try {
            mCamera = Camera.open(cameraId);
        } catch (Exception e) {
            Log.d("ERROR", "Failed to open camera: " + e.getMessage());
        }
    }

    public void initializeCameraView(){
        if(mCamera != null) {
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.cameraView);
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            mCameraView.imageView = (ImageView)findViewById(R.id.imageView);
            //mCameraView.imageView.setRotation(90);
            //mCameraView.imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            camera_view.addView(mCameraView);//add the SurfaceView to the layout

            sizeList = mCameraView.getPreviewSizes();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCameraView.getHolder().removeCallback(mCameraView);
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        // Get the Camera instance as the activity achieves full user focus
        if (mCamera == null) {
            newOpenCamera(); // Local method to handle camera init
            mCameraView.resumeViewWithCamera(mCamera);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera_resolutions, menu);
        Camera.Size currentSize = mCameraView.getCurrentPreviewSize();

        for ( int i = 0; i<sizeList.size();i++ ) {
            MenuItem item = menu.add(0, Menu.FIRST+i, Menu.NONE, sizeList.get(i).width+"x"+sizeList.get(i).height);
            item.setCheckable(true);
            if(sizeList.get(i).width == currentSize.width && sizeList.get(i).height == currentSize.height){
                item.setChecked(true);
            }
        }

        menu.setGroupCheckable(0, true, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        item.setChecked(true);

        int itemId = item.getItemId();
        Camera.Size selectedSize = sizeList.get(itemId - Menu.FIRST);
        mCameraView.setPreviewSize(selectedSize.width, selectedSize.height);

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //mCameraView.getHolder().removeCallback(mCameraView);
    }

    private void newOpenCamera() {
        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }
    }



    private CameraHandlerThread mThread = null;
    private class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    initializeCamera();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            }
            catch (InterruptedException e) {
                Log.w("ds", "wait was interrupted");
            }
        }
    }


}
