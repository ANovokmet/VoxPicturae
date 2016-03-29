package hr.sound;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PlayingThread implements Runnable{

    private final static Logger LOGGER = Logger.getLogger(PlayingThread.class.getName());

    private BlockingQueue<TrackElement> workQueue;
    private AudioOutputDevice device;
    private boolean stop = false;

    public PlayingThread(BlockingQueue<TrackElement> queue, AudioOutputDevice device){
        if(queue==null || device==null){
            throw new RuntimeException("Queue and Device must not be null");
        }
        this.workQueue = queue;
        this.device = device;
    }

    @Override
    public void run(){
        device.start();
        while(!stop){
            try {
                TrackElement a = this.workQueue.poll(5, TimeUnit.SECONDS);
                while(a != null){
                    this.device.write(a.getData(), a.getData().length);
                    a = this.workQueue.poll(5, TimeUnit.SECONDS);
                }
            }catch(Exception e){
                LOGGER.severe(e.getMessage());
            };
        }
        device.stop();
        device.close();
    }

    public void start(){

    }

    public void stop(){
        this.stop = true;
    }
}
