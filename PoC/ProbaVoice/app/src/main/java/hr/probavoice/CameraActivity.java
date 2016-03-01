package hr.probavoice;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.List;

import hr.image.CameraView;

public class CameraActivity extends ActionBarActivity {

    private Camera mCamera = null;
    private CameraView mCameraView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        try{
            mCamera = Camera.open();
        } catch (Exception e){
            Log.d("ERROR", "Failed to open camera: " + e.getMessage());
        }

        if(mCamera != null) {
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.cameraView);
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            mCameraView.imageView = (ImageView)findViewById(R.id.imageView);
            camera_view.addView(mCameraView);//add the SurfaceView to the layout


            SurfaceHolder camHolder = mCameraView.getHolder();
            camHolder.addCallback(mCameraView);
            camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            sizeList = mCameraView.getPreviewSizes();
        }
    }



    List<Camera.Size> sizeList;

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
        Camera.Size selectedSize = sizeList.get(itemId-Menu.FIRST);
        mCameraView.setPreviewSize(selectedSize.width, selectedSize.height);

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

}
