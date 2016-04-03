package hr.sound;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundProcessing implements ProcessingThread.OnUpdateListener, PostProcessingThread.OnFinishListener{

    public static int SAMPLE_RATE = 44100;
    public static final int TIME_WINDOW = 1024;  //number of samples
    public static final int POWER_THRESHOLD = 300;
    private static int PROCESSING_THREAD_NUM = 4;

    private OnProcessingUpdateListener listener = null;

    private AudioInputDevice input;
    private AudioOutputDevice output;

    private ReadingThread reading;
    private PlayingThread playing;
    private PostProcessingThread postpro;
    private ArrayList<ProcessingThread> processing = new ArrayList<>();

    public SoundProcessing(AudioInputDevice input, AudioOutputDevice output, int sampleRate){
        this.input = input;
        this.output = output;
        this.SAMPLE_RATE = sampleRate;
    }

    public void start(){
        BlockingQueue<TrackElement> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<TrackElement> outputQueue = new LinkedBlockingQueue<>();
        reading = new ReadingThread(inputQueue, input);
        //playing = new PlayingThread(outputQueue, output);
        postpro = new PostProcessingThread(outputQueue, this);
        for(int i=0; i<PROCESSING_THREAD_NUM; i++){
            processing.add(new ProcessingThread(inputQueue, outputQueue, this));
        }
        new Thread(reading).start();
        for(int i=0; i<PROCESSING_THREAD_NUM; i++){
            new Thread(processing.get(i)).start();
        }
        //new Thread(playing).start();
        new Thread(postpro).start();
    }

    public void stop(){
        reading.stop();
        for(int i=0; i<PROCESSING_THREAD_NUM; i++){
            processing.get(i).stop();
        }
        postpro.stop();
    }

    public OnProcessingUpdateListener getListener() {
        return listener;
    }

    public void setListener(OnProcessingUpdateListener listener) {
        this.listener = listener;
    }

    @Override
    public void onFinish(ProcessingResult result) {
        if(listener != null){
            listener.onFinish(result);
        }
    }

    @Override
    public void onUpdate(double[] soundData) {
        if(listener != null){
            listener.onUpdate(soundData);
        }
    }

    public interface OnProcessingUpdateListener{
        void onUpdate(double[] soundData);
        void onFinish(ProcessingResult result);
    }
}
