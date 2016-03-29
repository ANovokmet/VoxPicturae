package hr.sound;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/**
 * Class that represents a proxy towards concrete Android implementation of sound recording.
 */
public class AndroidAudioInput implements AudioInputDevice {

    private AudioRecord record;

    public AndroidAudioInput(int sampleRate) throws Exception{
        int minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        this.record = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 10*minBuffer);
        int i = 0;
        while(this.record.getState() == AudioRecord.STATE_UNINITIALIZED){
            if(i==50){
                throw new Exception("Unable to initialize recording device");
            }
            i++;
            Thread.sleep(100);
        }
    }

    @Override
    public void start(){
        this.record.startRecording();
    }

    /**
     * Method for reading sound data from device. Method will always try to read length shorts.
     * Less can be read because of hardware device unavailability.
     * @param buffer short buffer where data will be written
     * @param length how much shorts
     * @return number of read shorts
     */
    @Override
    public int read(short[] buffer, int length){
        int full = 0;
        while(full < length){
            int read = this.record.read(buffer, full, length - full);
            if(read < 0){
                break;
            }
            full += read;
        }
        return full;
    }

    @Override
    public void stop(){
        this.record.stop();
    }

    /**
     * Method finalizes audio input and releases underlying resources. No method should be called on this object after this one.
     */
    @Override
    public void close(){
        this.record.stop();
        this.record.release();
    }
}
