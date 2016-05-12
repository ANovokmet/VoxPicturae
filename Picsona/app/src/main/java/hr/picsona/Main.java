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
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

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
        mCameraController.setAreaSize(width * 2, height * 2);

        GPUImageView gpuImageView = (GPUImageView)findViewById(R.id.gpuimageView);

        OverlayGenerator og = new OverlayGenerator(this, mCameraController.getAreaWidth(), mCameraController.getAreaHeight(),4,5);
        Bitmap bitmap = og.createOverlay();

        gpuImageView.setImage(bitmap);

        mCameraController.setOverlayGenerator(og);

    }

    final FakeFilterCalculator fkcalculator = new FakeFilterCalculator();


    View parameterViewLayout;
    SeekBar redSB1,redSB2,greenSB1,greenSB2,blueSB1,blueSB2,compositeSB1,compositeSB2,contrastSB;
    int red1,red2,green1,green2,blue1,blue2,composite1,composite2,contrast;

    AlertDialog.Builder popDialog;

    private void InitializeParameterSeekbars() {
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        parameterViewLayout = inflater.inflate(R.layout.parameter_popup, (ViewGroup) findViewById(R.id.layout_dialog));

        redSB1 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarRed1);
        redSB2 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarRed2);
        greenSB1 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarGreen1);
        greenSB2 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarGreen2);
        blueSB1 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarBlue1);
        blueSB2 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarBlue2);
        compositeSB1 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarComposite1);
        compositeSB2 = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarComposite2);
        contrastSB = (SeekBar) parameterViewLayout.findViewById(R.id.seekBarContrast);
    }

    private void getProgresses(){
        red1 = redSB1.getProgress();
        red2 = redSB2.getProgress();
        green1 = greenSB1.getProgress();
        green2 = greenSB2.getProgress();
        blue1 = blueSB1.getProgress();
        blue2 = blueSB2.getProgress();
        composite1 = compositeSB1.getProgress();
        composite2 = compositeSB2.getProgress();
        contrast = contrastSB.getProgress();
    }

    private void showEditParamsDialog() {

        InitializeParameterSeekbars();
        popDialog = new AlertDialog.Builder(this);
        popDialog.setView(parameterViewLayout);

        redSB1.setProgress(red1);
        redSB2.setProgress(red2);
        greenSB1.setProgress(green1);
        greenSB2.setProgress(green2);
        blueSB1.setProgress(blue1);
        blueSB2.setProgress(blue2);
        compositeSB1.setProgress(composite1);
        compositeSB2.setProgress(composite2);
        contrastSB.setProgress(contrast);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            popDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialogInterface) {

                    getProgresses();

                    mFilter = fkcalculator.calculateFilter(red1,red2,green1,green2,blue1,blue2,composite1,composite2,contrast);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraController.stopCamera();
    }

    private void switchCamera() {
        mCameraController.cycleCamera();
    }

    private void takePicture() {
        mCameraController.takePicture();
    }

}
