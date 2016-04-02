package hr.sound;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Logger;

public class ReadingThread implements Runnable{

    private final static Logger LOGGER = Logger.getLogger(ReadingThread.class.getName());

    private BlockingQueue<TrackElement> workQueue;
    private AudioInputDevice device;
    private boolean stop = false;

    private int TIME_WINDOW = SoundProcessing.TIME_WINDOW;

    public ReadingThread(BlockingQueue<TrackElement> queue, AudioInputDevice device){
        if(queue==null || device==null){
            throw new RuntimeException("Queue and Device must not be null");
        }
        this.workQueue = queue;
        this.device = device;
    }

    @Override
    public void run(){
        device.start();
        short[] buffer = new short[TIME_WINDOW];
        while(!stop){
            int read = device.read(buffer, TIME_WINDOW);
            if(read<=0){
                break;
            }
            try {
                this.workQueue.put(new TrackElement(buffer, read));
            }catch(Exception e){
                LOGGER.severe(e.getMessage());
            }
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
