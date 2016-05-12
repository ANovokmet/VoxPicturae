package hr.picsona;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
<<<<<<< HEAD
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.opengl.GLSurfaceView;
import android.os.Build;
=======
>>>>>>> refs/remotes/origin/master
import android.content.Intent;
import android.database.Cursor;
<<<<<<< HEAD
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
=======
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
>>>>>>> refs/remotes/origin/master
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
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
     * Code for which the Choose File Activity is started and for which
     * result in onActivityResult is checked
     */
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
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private Button switchCameraButton;
    private Button takePictureButton;
    private GPUImage mGPUImage;
    private GPUImageFilter mFilter;
    private FilterCalculator mFilterCalculator;
    private CameraController mCameraController;
    private ProgressBar soundGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        mVisible = true;
        mContentView = findViewById(R.id.container);
        soundGraph = (ProgressBar) findViewById(R.id.intensityBar);
        soundGraph.getProgressDrawable().setColorFilter(Color.parseColor("#8400dc"), PorterDuff.Mode.SRC_IN);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        InitializeSoundControls();

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

    private void InitializeSoundControls() {
        switchCameraButton = (Button) findViewById(R.id.buttonSwitchCamera);
        takePictureButton = (Button) findViewById(R.id.buttonTakePicture);

        findViewById(R.id.recordSound).setOnClickListener(new View.OnClickListener() {

            SoundProcessing processing;
            boolean recording = false;

            @Override
            public void onClick(View v) {
                if (recording == false) {
                    AudioInputDevice device;
                    try {
                        device = new AndroidAudioInput(SoundProcessing.SAMPLE_RATE);
                    } catch (Exception e) {
                        return;
                    }
                    ((Button) v).setText("Stop recording");
                    switchCameraButton.setVisibility(View.GONE);
                    takePictureButton.setVisibility(View.GONE);
                    soundGraph.setVisibility(View.VISIBLE);
                    processing = new SoundProcessing(device, null, SoundProcessing.SAMPLE_RATE);
                    processing.setListener(Main.this);
                    processing.start();
                    recording = true;
                } else {
                    ((Button) v).setText("Start recording");
                    processing.stop();
                    soundGraph.setVisibility(View.GONE);
                    switchCameraButton.setVisibility(View.VISIBLE);
                    takePictureButton.setVisibility(View.VISIBLE);
                    recording = false;
                }
            }
        });
    }

    private void InitializeCameraControls() {
        mFilterCalculator = new FilterCalculator();

        mGPUImage = new GPUImage(this);

        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceView);
        mGPUImage.setGLSurfaceView(glSurfaceView);

        Button switchCameraButton = (Button) findViewById(R.id.buttonSwitchCamera);
        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        Button takePictureButton = (Button) findViewById(R.id.buttonTakePicture);
        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        Button editParamsButton = (Button) findViewById(R.id.buttonEditParams);
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

<<<<<<< HEAD
        mCameraController.setDesiredPreviewSize(height, width);//height i width moraju biti obrnuti zbog orijentacije ekrana
        mCameraController.setDesiredPictureSize(height, width);

        mGPUImageView = (GPUImageView)findViewById(R.id.gpuimageView);

        mOverlayGenerator = new OverlayGenerator(this);

=======
        mCameraController.setAreaSize(width / 2, height / 2);

        GPUImageView gpuImageView = (GPUImageView) findViewById(R.id.gpuimageView);

        OverlayGenerator og = new OverlayGenerator(this, mCameraController.getAreaWidth(), mCameraController.getAreaHeight(), 4, 5);
        Bitmap bitmap = og.createOverlay();
>>>>>>> refs/remotes/origin/master


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
<<<<<<< HEAD
    SeekBar genderSB, pitchSB, maxFreqSB, angerSB, sadnessSB, happinessSB, intensitySB;
    int gender,pitch,maxFreq,anger,sadness,happiness,intensity;
=======
    SeekBar redSB1, redSB2, greenSB1, greenSB2, blueSB1, blueSB2, compositeSB1, compositeSB2, contrastSB;
    int red1, red2, green1, green2, blue1, blue2, composite1, composite2, contrast;
>>>>>>> refs/remotes/origin/master

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

<<<<<<< HEAD
    private void getProgresses(){
        gender = genderSB.getProgress();
        pitch = pitchSB.getProgress();
        maxFreq = maxFreqSB.getProgress();
        anger = angerSB.getProgress();
        sadness = sadnessSB.getProgress();
        happiness = happinessSB.getProgress();
        intensity = intensitySB.getProgress();
=======
    private void getProgresses() {
        red1 = redSB1.getProgress();
        red2 = redSB2.getProgress();
        green1 = greenSB1.getProgress();
        green2 = greenSB2.getProgress();
        blue1 = blueSB1.getProgress();
        blue2 = blueSB2.getProgress();
        composite1 = compositeSB1.getProgress();
        composite2 = compositeSB2.getProgress();
        contrast = contrastSB.getProgress();
>>>>>>> refs/remotes/origin/master
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

<<<<<<< HEAD
                    mFilter = fkcalculator.calculateFilter((float)gender/100, (float)pitch, (float)maxFreq, (float)anger/100, (float)sadness/100, (float)happiness/100, (float)intensity/400);
=======
                    mFilter = fkcalculator.calculateFilter(red1, red2, green1, green2, blue1, blue2, composite1, composite2, contrast);
>>>>>>> refs/remotes/origin/master
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
                soundGraph.setProgress((int) power);
            }
        });
    }

    @Override
    public void onFinish(final ProcessingResult result) {
        Log.e("results", "" + result);
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
                if (data == null) {
                    Toast.makeText(this, "Image loading error", Toast.LENGTH_SHORT).show();
                    break;
                }
                Uri uri = data.getData();
                String path = uri.toString();
                if (path.startsWith("content")) {
                    try {
                        path = getRealPathFromURI(uri);
                    } catch (Exception e) {
                        Toast.makeText(this, "Supported format is JPG", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                if (!path.endsWith(".jpg") && !path.endsWith(".jpeg")) {
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

    public String getRealPathFromURI(Uri contentUri) throws Exception {

        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getContentResolver().query(contentUri,
                proj,
                null,
                null,
                null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        return cursor.getString(column_index);
    }

    private void switchCamera() {
        mCameraController.cycleCamera();
    }

    private void takePicture() {
        mCameraController.takePicture();
    }

}
