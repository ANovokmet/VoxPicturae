package hr.probavoice;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import hr.sound.AndroidAudioInput;
import hr.sound.AndroidAudioOutput;
import hr.sound.AudioInputDevice;
import hr.sound.AudioOutputDevice;
import hr.sound.PlayingThread;
import hr.sound.ReadingThread;
import hr.sound.SoundProcessing;
import hr.sound.TrackElement;


public class MainActivity extends ActionBarActivity {


    boolean stop = false;
    int INTERNAL_BUFFER_SIZE = 10000;
    int DATA_SIZE = 524288;
    int TIME_WINDOW = 8192;
    int SAMPLE_RATE = 44100;
    int POJACANJE = 1;
    int START_FREKV = 0;
    int FREKV_RASPON = 1000;

    SoundProcessing processing;

    static {
        System.loadLibrary("proba");
    }

    public native int sumaArray(int[] arej);
    public native String dohvatiString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                processing.stop();
            }
        });
        SeekBar seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                START_FREKV = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        SeekBar seekbar2 = (SeekBar) findViewById(R.id.seekBar2);
        seekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                POJACANJE = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        SeekBar seekbar3 = (SeekBar) findViewById(R.id.seekBar3);
        seekbar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                FREKV_RASPON = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        AudioInputDevice device = null;
        try{
            device = new AndroidAudioInput(44100);
        }catch(Exception e){};
        AudioOutputDevice device2 = new AndroidAudioOutput(44100);

        processing = new SoundProcessing(device, device2);
        processing.start();
        /*Log.e("proba ", "" + sumaArray(new int[]{2, 3, 4, 2}));
        Log.e("proba22 ", dohvatiString());*/
        //NoiseSuppressor.create(record.getAudioSessionId());
        //AcousticEchoCanceler.create(record.getAudioSessionId());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
