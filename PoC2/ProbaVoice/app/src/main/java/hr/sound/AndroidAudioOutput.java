package hr.sound;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

public class AndroidAudioOutput implements AudioOutputDevice {

    private AudioTrack player;

    public AndroidAudioOutput(int sampleRate){
        int minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        this.player = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
                10*minBuffer, AudioTrack.MODE_STREAM);
    }

    @Override
    public void start(){
        player.play();
    }

    @Override
    public void write(short[] buffer, int length){
        player.write(buffer, 0, length);
    }

    @Override
    public void stop(){
        player.stop();
    }

    @Override
    public void close(){
        player.stop();
        player.release();
    }
}
