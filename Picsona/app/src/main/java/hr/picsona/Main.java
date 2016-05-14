package hr.picsona;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import hr.image.CameraController;
import hr.image.ImageFilterFactory;
import hr.image.OverlayGenerator;
import hr.sound.AndroidAudioInput;
import hr.sound.AudioInputDevice;
import hr.sound.ProcessingResult;
import hr.sound.SoundProcessing;
import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageView;


public class Main extends AppCompatActivity implements SoundProcessing.OnProcessingUpdateListener {
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

    private boolean statusBarVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private ImageButton switchCameraButton;
    private ImageButton takePictureButton;
    private GPUImage mGPUImage;
    private ImageView takenPictureView;
    private GPUImageFilter mFilter;
    private Uri mSaveImagePath;
    private View mainButtonContainer, afterCaptureContainer;
    private CameraController mCameraController;
    private ProgressBar soundGraph;

    OverlayGenerator emojiOverlayGenerator;
    GPUImageView mGPUImageView;
    ImageFilterFactory imageFilterFactory;
    View popupViewLayout;
    SeekBar genderSB, seekBarEmoji, angerSB, sadnessSB, happinessSB, intensitySB;
    int gender, maxFreq, anger, sadness, happiness, intensity;
    AlertDialog.Builder popDialog;

    int numOfDoubleEmojis = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        statusBarVisible = true;
        mContentView = findViewById(R.id.container);
        soundGraph = (ProgressBar) findViewById(R.id.intensityBar);
        soundGraph.getProgressDrawable().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        InitializeSoundControls();

        InitializeCameraControls();

        InitializeParameterSeekbars();
        getProgresses();
    }

    private void InitializeSoundControls() {
        switchCameraButton = (ImageButton) findViewById(R.id.buttonSwitchCamera);
        takePictureButton = (ImageButton) findViewById(R.id.buttonTakePicture);

        findViewById(R.id.buttonRecordSound).setOnClickListener(new View.OnClickListener() {

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
                    ((ImageButton) v).setImageResource(R.drawable.record_sound_button_fg_d);
                    takePictureButton.setEnabled(false);
                    processing = new SoundProcessing(device, null, SoundProcessing.SAMPLE_RATE);
                    processing.setListener(Main.this);
                    processing.start();
                    recording = true;
                } else {
                    ((ImageButton) v).setImageResource(R.drawable.record_sound_button_fg);
                    processing.stop();
                    takePictureButton.setEnabled(true);
                    recording = false;
                }
            }
        });
    }

    private void InitializeCameraControls() {
        mainButtonContainer = findViewById(R.id.buttonContainer);
        afterCaptureContainer = findViewById(R.id.afterCaptureContainer);


        mGPUImage = new GPUImage(this);

        GLSurfaceView glSurfaceView = (GLSurfaceView) findViewById(R.id.surfaceView);
        mGPUImage.setGLSurfaceView(glSurfaceView);

        takenPictureView = (ImageView) findViewById(R.id.takenPictureView);

        switchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePicture();
            }
        });

        ImageButton editParamsButton = (ImageButton) findViewById(R.id.buttonEditParams);
        editParamsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFilter == null) {
                    showToast(getString(R.string.noFilterString));
                } else {
                    showEditParamsDialog();
                }
            }
        });

        mCameraController = new CameraController(this, mGPUImage, glSurfaceView, new GPUImage.OnPictureSavedListener() {
            @Override
            public void onPictureSaved(Uri uri) {
                mSaveImagePath = uri;
                takenPictureView.setImageURI(uri);
                takenPictureView.setVisibility(View.VISIBLE);
                setCapturedPictureInterface();
            }
        });

        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        int height43 = (int) Math.round((double) width / 3 * 4);

        mCameraController.setDesiredPreviewSize(height43, width);//height i width moraju biti obrnuti zbog orijentacije ekrana
        mCameraController.setDesiredPictureSize(height43, width);

        mGPUImageView = (GPUImageView) findViewById(R.id.gpuimageView);

        glSurfaceView.getLayoutParams().height = height43;
        mGPUImageView.getLayoutParams().height = height43;
        takenPictureView.getLayoutParams().height = height43;

        emojiOverlayGenerator = new OverlayGenerator(this);
        imageFilterFactory = new ImageFilterFactory(emojiOverlayGenerator);


        mCameraController.setOverlayGenerator(emojiOverlayGenerator);

        findViewById(R.id.buttonSharePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, mSaveImagePath);
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, getString(R.string.shareString)));
            }
        });

        findViewById(R.id.buttonDeletePicture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int deleted = getContentResolver().delete(mSaveImagePath, null, null);
                if (deleted != 1) {
                    showToast(getString(R.string.deleteImageErrorString));
                }
                restoreStandardInterface();
            }
        });
    }

    private void setCapturedPictureInterface() {
        mainButtonContainer.setVisibility(View.GONE);
        afterCaptureContainer.setVisibility(View.VISIBLE);
    }

    private void restoreStandardInterface() {
        afterCaptureContainer.setVisibility(View.GONE);
        mainButtonContainer.setVisibility(View.VISIBLE);
        takenPictureView.setImageDrawable(null);
        takenPictureView.setVisibility(View.GONE);
        takenPictureView.setVisibility(View.VISIBLE);
        mCameraController.reSetupCamera();
        mSaveImagePath = null;
    }


    private void InitializeParameterSeekbars() {
        if (popupViewLayout != null && popupViewLayout instanceof ViewGroup) {
            return;
        }
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        popupViewLayout = inflater.inflate(R.layout.parameter_popup, null);

        genderSB = (SeekBar) popupViewLayout.findViewById(R.id.seekBarGender);
        genderSB.getProgressDrawable().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);
        genderSB.getThumb().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);

        seekBarEmoji = (SeekBar) popupViewLayout.findViewById(R.id.seekBarEmoji);
        seekBarEmoji.getProgressDrawable().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);
        seekBarEmoji.getThumb().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);

        angerSB = (SeekBar) popupViewLayout.findViewById(R.id.seekBarAnger);
        angerSB.getProgressDrawable().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);
        angerSB.getThumb().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);

        sadnessSB = (SeekBar) popupViewLayout.findViewById(R.id.seekBarSadness);
        sadnessSB.getProgressDrawable().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);
        sadnessSB.getThumb().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);

        happinessSB = (SeekBar) popupViewLayout.findViewById(R.id.seekBarHappiness);
        happinessSB.getProgressDrawable().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);
        happinessSB.getThumb().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);

        intensitySB = (SeekBar) popupViewLayout.findViewById(R.id.seekBarIntensity);
        intensitySB.getProgressDrawable().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);
        intensitySB.getThumb().setColorFilter(getResources().getColor(R.color.buttonEnabled), PorterDuff.Mode.SRC_IN);
    }

    private void getProgresses() {
        gender = genderSB.getProgress();
        numOfDoubleEmojis = seekBarEmoji.getProgress();
        anger = angerSB.getProgress();
        sadness = sadnessSB.getProgress();
        happiness = happinessSB.getProgress();
        intensity = intensitySB.getProgress();
    }

    private void showEditParamsDialog() {

        InitializeParameterSeekbars();
        popDialog = new AlertDialog.Builder(this);
        popDialog.setView(popupViewLayout);

        genderSB.setProgress(gender);
        seekBarEmoji.setProgress(numOfDoubleEmojis);
        angerSB.setProgress(anger);
        sadnessSB.setProgress(sadness);
        happinessSB.setProgress(happiness);
        intensitySB.setProgress(intensity);

        popDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {

                getProgresses();
                if (popupViewLayout instanceof ViewGroup) {//recycling the view
                    ((ViewGroup) popupViewLayout.getParent()).removeView(popupViewLayout);
                }

                mFilter = imageFilterFactory.calculateFilter((float) gender / 100, (float) 420, (float) maxFreq, (float) anger / 100, (float) sadness / 100, (float) happiness / 100, (float) intensity / 400);

                Bitmap bitmap = imageFilterFactory.calculateOverlay((float) gender / 100, (float) anger / 100, (float) sadness / 100, (float) happiness / 100, numOfDoubleEmojis);
                mGPUImageView.setImage(bitmap);
                mGPUImage.setFilter(mFilter);
            }
        });
        popDialog.show();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        delayedHide(100);
    }

    private void toggle() {
        if (statusBarVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        statusBarVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        statusBarVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((TextView) findViewById(R.id.debugView)).setText("" + result);
            }
        });

        soundGraph.setProgress(0);

        setProgresses(result);
        getProgresses();

        mFilter = imageFilterFactory.calculateFilter(result);
        Bitmap bitmap = imageFilterFactory.calculateOverlay(result, numOfDoubleEmojis);
        mGPUImageView.setImage(bitmap);
        mGPUImage.setFilter(mFilter);
    }


    private void setProgresses(ProcessingResult result) {
        genderSB.setProgress((int) (result.getGenderProbability() * 100));
        seekBarEmoji.setProgress(numOfDoubleEmojis);
        maxFreq = (int) result.getMaxFrequency();
        angerSB.setProgress((int) (result.getEmotionData().getAngerProbability() * 100));
        sadnessSB.setProgress((int) (result.getEmotionData().getSadnessProbability() * 100));
        happinessSB.setProgress((int) (result.getEmotionData().getHappinessProbability() * 100));
        intensitySB.setProgress((int) (result.getEmotionData().getSpeechIntensity() * 100));
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(Main.this, message, Toast.LENGTH_SHORT);
        toast.getView().setBackgroundColor(getResources().getColor(R.color.buttonEnabled));
        ((TextView) toast.getView().findViewById(android.R.id.message)).setTextColor(getResources().getColor(R.color.white));
        toast.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraController.reSetupCamera();

        emojiOverlayGenerator.setInitializationParams(mCameraController.getPreviewHeight(), mCameraController.getPreviewWidth(), 6, 8);//treba biti ovdje zbog nuznosti inicijalizacije kamere

        mGPUImageView.setImage(emojiOverlayGenerator.getLastOverlay());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraController.stopCamera();
    }

    @Override
    public void onBackPressed() {
        if (mSaveImagePath != null) {
            restoreStandardInterface();
        } else {
            super.onBackPressed();
        }
    }

    private void switchCamera() {
        mCameraController.cycleCamera();
    }

    private void takePicture() {
        mCameraController.takePicture();
    }

}
