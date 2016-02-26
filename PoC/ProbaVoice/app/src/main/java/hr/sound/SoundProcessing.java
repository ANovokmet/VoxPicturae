package hr.sound;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundProcessing {

    private AudioInputDevice input;
    private AudioOutputDevice output;

    private ReadingThread reading;
    private PlayingThread playing;
    private ArrayList<ProcessingThread> processing = new ArrayList<>();

    public SoundProcessing(AudioInputDevice input, AudioOutputDevice output){
        this.input = input;
        this.output = output;
    }

    public void start(){
        BlockingQueue<TrackElement> inputQueue = new LinkedBlockingQueue<>();
        BlockingQueue<TrackElement> outputQueue = new LinkedBlockingQueue<>();
        reading = new ReadingThread(inputQueue, input);
        playing = new PlayingThread(outputQueue, output);
        for(int i=0; i<4; i++){
            processing.add(new ProcessingThread(inputQueue, outputQueue));
        }
        new Thread(reading).start();
        for(int i=0; i<4; i++){
            new Thread(processing.get(i)).start();
        }
        new Thread(playing).start();
    }

    public void stop(){
        reading.stop();
        for(int i=0; i<4; i++){
            processing.get(i).stop();
        }
        playing.stop();
    }
}
