package hr.sound;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundProcessing {

    public static final int SAMPLE_RATE = 44100;
    public static final int TIME_WINDOW = 1024;
    private static int PROCESSING_THREAD_NUM = 4;

    private AudioInputDevice input;
    private AudioOutputDevice output;

    private ReadingThread reading;
    private PlayingThread playing;
    private PostProcessingThread postpro;
    private ArrayList<ProcessingThread> processing = new ArrayList<>();

    public SoundProcessing(AudioInputDevice input, AudioOutputDevice output){
        this.input = input;
        this.output = output;
    }

    public void start(){
        BlockingQueue<TrackElement> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<TrackElement> outputQueue = new LinkedBlockingQueue<>();
        reading = new ReadingThread(inputQueue, input);
        //playing = new PlayingThread(outputQueue, output);
        postpro = new PostProcessingThread(outputQueue);
        for(int i=0; i<PROCESSING_THREAD_NUM; i++){
            processing.add(new ProcessingThread(inputQueue, outputQueue));
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
}
