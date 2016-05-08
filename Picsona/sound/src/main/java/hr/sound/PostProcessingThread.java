package hr.sound;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import hr.sound.emotion.EmotionData;
import hr.sound.emotion.EmotionRecognizer;

public class PostProcessingThread implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(PostProcessingThread.class.getName());

    private BlockingQueue<TrackElement> workQueue;
    private boolean stop = false;
    private OnFinishListener listener;

    public PostProcessingThread(BlockingQueue<TrackElement> queue, OnFinishListener listener) {
        if (queue == null) {
            throw new RuntimeException("Queue and Device must not be null");
        }
        this.workQueue = queue;
        this.listener = listener;
    }

    @Override
    public void run() {
        double pitchSum = 0, freqSum = 0;
        double numberOfElements = 0, weightOfElements = 0;
        float genderProbability = 0;
        int numberOfPollExecutions = 2;
        List<Double> snaga = new LinkedList<>();
        while (true) {
            TrackElement element = null;
            try {
                element = this.workQueue.poll(1, TimeUnit.SECONDS);
                if (element == null) {
                    numberOfPollExecutions--;
                    if (numberOfPollExecutions == 0) {
                        break;
                    }
                    continue;
                }
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }

            snaga.add(element.getPower());
            if (element.getPower() > SoundProcessing.POWER_THRESHOLD) {
                pitchSum += element.getPitch();
                freqSum += element.getMaxFrequency();
                genderProbability += element.getGender() * element.getPower();
                numberOfElements++;
                weightOfElements += element.getPower();
            }
        }

        EmotionData emotionData = EmotionRecognizer.emotionFromSpeech(snaga);

        LOGGER.info("average pitch " + pitchSum / numberOfElements + " " + freqSum / numberOfElements + " " + genderProbability / weightOfElements);
        if (listener != null) {
            listener.onFinish(new ProcessingResult(genderProbability / weightOfElements, pitchSum / numberOfElements, freqSum / numberOfElements, emotionData));
        }
    }

    public void start() {

    }

    public void stop() {
        this.stop = true;
    }


    public interface OnFinishListener {
        void onFinish(ProcessingResult result);
    }
}
