package hr.picsona;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import hr.image.CameraController;
import hr.image.FakeFilterCalculator;
import hr.image.FilterCalculator;
import hr.image.OverlayGenerator;
import hr.sound.AndroidAudioInput;
import hr.sound.AudioInputDevice;
import hr.sound.ProcessingResult;
import hr.sound.SoundProcessing;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Main extends AppCompatActivity implements SoundProcessing.OnProcessingUpdateListener {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    final int ACTIVITY_CHOOSE_FILE = 1;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    /*@Override
    public boolean dispatchTouchEvent (MotionEvent ev) {  //dodir ekrana
        Toast.makeText(Main.this, "proba", Toast.LENGTH_SHORT).show();
        return super.dispatchTouchEvent(ev);
    }*/

    private Button switchCameraButton;
    private Button takePictureButton;
    private GPUImage mGPUImage;
    private GPUImageFilter mFilter;
    private FilterCalculator mFilterCalculator;
    private CameraController mCameraController;
    private ProgressBar freqGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        initialiseGraph();

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        freqGraph = (ProgressBar) findViewById(R.id.intensityBar);
        freqGraph.getProgressDrawable().setColorFilter(Color.parseColor("#8400dc"), PorterDuff.Mode.SRC_IN);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.

        switchCameraButton = (Button) findViewById(R.id.buttonSwitchCamera);
        takePictureButton = (Button) findViewById(R.id.buttonTakePicture);
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
        findViewById(R.id.recordSound).setOnClickListener(new View.OnClickListener() {

            SoundProcessing processing;
            boolean recording = false;

            @Override
            public void onClick(View v) {
                if (recording == false) {
                    AudioInputDevice device = null;
                    try {
                        device = new AndroidAudioInput(44100);
                    } catch (Exception e) {
                        return;
                    }
                    initialiseGraph();
                    ((Button) v).setText("Stop recording");
                    switchCameraButton.setVisibility(View.GONE);
                    takePictureButton.setVisibility(View.GONE);
                    freqGraph.setVisibility(View.VISIBLE);
                    processing = new SoundProcessing(device, null, 44100);
                    processing.setListener(Main.this);
                    processing.start();
                    recording = true;
                } else {
                    ((Button) v).setText("Start recording");
                    processing.stop();
                    freqGraph.setVisibility(View.GONE);
                    switchCameraButton.setVisibility(View.VISIBLE);
                    takePictureButton.setVisibility(View.VISIBLE);
                    recording = false;
                }
            }
        });

        findViewById(R.id.loadImage).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent chooseFile;
                Intent chooserWrapper;
                chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("image/*");
                chooserWrapper = Intent.createChooser(chooseFile, "Choose a picture");
                startActivityForResult(chooserWrapper, ACTIVITY_CHOOSE_FILE);
            }
        });

        InitializeCameraControls();

        InitializeParameterSeekbars();
        getProgresses();
    }

    private void InitializeCameraControls(){
        mFilterCalculator = new FilterCalculator();

        mGPUImage = new GPUImage(this);

        GLSurfaceView glSurfaceView = (GLSurfaceView)findViewById(R.id.surfaceView);
        mGPUImage.setGLSurfaceView(glSurfaceView);

        Button switchCameraButton = (Button)findViewById(R.id.buttonSwitchCamera);
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

        Button editParamsButton = (Button)findViewById(R.id.buttonEditParams);
        editParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showEditParamsDialog();

            }
        });

        mCameraController = new CameraController(this, mGPUImage, glSurfaceView);

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        mCameraController.setDesiredPreviewSize(height, width);//height i width moraju biti obrnuti zbog orijentacije ekrana
        mCameraController.setDesiredPictureSize(height, width);

        mGPUImageView = (GPUImageView)findViewById(R.id.gpuimageView);

        mOverlayGenerator = new OverlayGenerator(this);



        mCameraController.setOverlayGenerator(mOverlayGenerator);

        /*ArrayList<GPUImageFilter> filters = new ArrayList<GPUImageFilter>();
        filters.add(new GPUImageContrastFilter(1.5f));
        filters.add(new GPUImageRGBFilter(237/255.f,221/255.f,158/255.f));
            //++levels filter
        mGPUImage.setFilter(new GPUImageFilterGroup(filters));*/
    }

    OverlayGenerator mOverlayGenerator;
    GPUImageView mGPUImageView;

    final FakeFilterCalculator fkcalculator = new FakeFilterCalculator();


    View parameterViewLayout;
    SeekBar genderSB, pitchSB, maxFreqSB, angerSB, sadnessSB, happinessSB, intensitySB;
    int gender,pitch,maxFreq,anger,sadness,happiness,intensity;

    AlertDialog.Builder popDialog;

    private void InitializeParameterSeekbars() {
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        parameterViewLayout = inflater.inflate(R.layout.parameter_popup, (ViewGroup) findViewById(R.id.layout_dialog));

        genderSB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarGender);
        pitchSB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarPitch);
        maxFreqSB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarMaxFreq);
        angerSB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarAnger);
        sadnessSB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarSadness);
        happinessSB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarHappiness);
        intensitySB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarIntensity);
    }

    private void getProgresses(){
        gender = genderSB.getProgress();
        pitch = pitchSB.getProgress();
        maxFreq = maxFreqSB.getProgress();
        anger = angerSB.getProgress();
        sadness = sadnessSB.getProgress();
        happiness = happinessSB.getProgress();
        intensity = intensitySB.getProgress();
    }

    private void showEditParamsDialog() {

        InitializeParameterSeekbars();
        popDialog = new AlertDialog.Builder(this);
        popDialog.setView(parameterViewLayout);

        genderSB.setProgress(gender);
        pitchSB.setProgress(pitch);
        maxFreqSB.setProgress(maxFreq);
        angerSB.setProgress(anger);
        sadnessSB.setProgress(sadness);
        happinessSB.setProgress(happiness);
        intensitySB.setProgress(intensity);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            popDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {

                    getProgresses();

                    mFilter = fkcalculator.calculateFilter((float)gender/100, (float)pitch, (float)maxFreq, (float)anger/100, (float)sadness/100, (float)happiness/100, (float)intensity/400);
                    mGPUImage.setFilter(mFilter);
                }
            });
        }


        popDialog.create();
        popDialog.show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onUpdate(final double[] soundData, final double power) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                freqGraph.setProgress((int) power);
            }
        });
    }

    private void initialiseGraph() {

    }

    @Override
    public void onFinish(final ProcessingResult result) {
        Log.e("rezultati", "" + result);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.debugView)).setText("" + result);
            }
        });
        mFilter = mFilterCalculator.calculateFilter(result);
        mGPUImage.setFilter(mFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraController.reSetupCamera();

        mOverlayGenerator.setInitializationParams(mCameraController.getPreviewHeight(), mCameraController.getPreviewWidth(),6,8);
        mOverlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Angry);
        Bitmap bitmap = mOverlayGenerator.createOverlay(8);
        mGPUImageView.setImage(bitmap);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraController.stopCamera();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACTIVITY_CHOOSE_FILE:
                Uri uri = data.getData();
                String path = uri.toString();
                Log.e("put do slike", path);
                if(path.startsWith("content")){
                    try {
                        path = getRealPathFromURI(uri);
                    }catch(Exception e){
                        Toast.makeText(this, "Supported format is JPG", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                Log.e("put do slike2", path);
                if(!path.endsWith(".jpg") && !path.endsWith(".jpeg")) {
                    Toast.makeText(this, "Supported format is JPG", Toast.LENGTH_SHORT).show();
                    break;
                }
                //mCameraController.stopCamera();  //napraviti "pause" camera
                mGPUImage.setImage(uri);
                break;
            default:
                Toast.makeText(this, "Image loading error", Toast.LENGTH_SHORT).show();
        }
    }

    public String getRealPathFromURI(Uri contentUri) throws Exception{

        String [] proj={MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query( contentUri,
                proj,
                null,
                null,
                null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    private void switchCamera(){
        mCameraController.cycleCamera();
    }

    private void takePicture() {
        mCameraController.takePicture();
    }

}
