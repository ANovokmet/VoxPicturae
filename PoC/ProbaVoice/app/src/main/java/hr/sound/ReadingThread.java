package hr.sound;

import android.util.Log;

import java.util.concurrent.BlockingQueue;

public class ReadingThread implements Runnable{

    private BlockingQueue<TrackElement> workQueue;
    private AudioInputDevice device;
    private boolean stop = false;

    private int TIME_WINDOW = 1024;

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
                Log.e("Error", e.getMessage());
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
