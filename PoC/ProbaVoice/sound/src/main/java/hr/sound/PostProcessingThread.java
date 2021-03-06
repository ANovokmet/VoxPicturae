package hr.sound;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class PostProcessingThread implements Runnable{

    private final static Logger LOGGER = Logger.getLogger(PostProcessingThread.class.getName());

    private BlockingQueue<TrackElement> workQueue;
    private boolean stop = false;

    public PostProcessingThread(BlockingQueue<TrackElement> queue){
        if(queue==null){
            throw new RuntimeException("Queue and Device must not be null");
        }
        this.workQueue = queue;
    }

    @Override
    public void run(){
        double sum = 0, sum2 = 0;
        double br = 0;
        float gender_probability = 0;
        int num_of_executions = 5;
        while(true){
            TrackElement element = null;
            try {
                element = this.workQueue.poll(1, TimeUnit.SECONDS);
                if(element == null){
                    num_of_executions--;
                    if(num_of_executions==0){
                        break;
                    }
                    continue;
                }
            }catch(Exception e){
                LOGGER.severe(e.getMessage());
            };
            if(element.getPower()>100){
                sum += element.getPitch();
                sum2 += element.getMax_frequency();
                gender_probability += element.gender;
                br++;
            }
            LOGGER.info(element.getPitch()+" "+element.getPower()+" "+element.getMax_frequency()+" "+element.numofpeaks);
        }
        LOGGER.info("average pitch "+sum/br+" "+sum2/br+" "+gender_probability/br);
    }

    public void start(){

    }

    public void stop(){
        this.stop = true;
    }
}
