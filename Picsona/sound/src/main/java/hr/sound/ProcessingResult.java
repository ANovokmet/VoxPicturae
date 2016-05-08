package hr.sound;

import hr.sound.emotion.EmotionData;

public class ProcessingResult {

    private double genderProbability;
    private double pitch;
    private double maxFrequency;
    private EmotionData emotionData;
    private Status status;

    public ProcessingResult(double genderProbability, double pitch, double maxFrequency, EmotionData emotionData){
        this.emotionData = emotionData;
        double checkSum = genderProbability + pitch + maxFrequency;
        if(Double.isNaN(checkSum)){
            status = Status.STATUS_NOT_ENOUGH_DATA;
            return;
        }
        this.genderProbability = genderProbability;
        this.pitch = pitch;
        this.maxFrequency = maxFrequency;
        status = Status.STATUS_OK;
    }

    public double getGenderProbability() {
        return genderProbability;
    }

    public double getPitch() {
        return pitch;
    }

    public double getMaxFrequency() {
        return maxFrequency;
    }

    public EmotionData getEmotionData() {
        return emotionData;
    }

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "genderProbability=" + genderProbability +
                ", pitch=" + pitch +
                ", maxFrequency=" + maxFrequency +
                ", emotionData=" + emotionData +
                ", status=" + status +
                '}';
    }

    public Status getStatus() {
        return status;
    }

    public enum Status{
        STATUS_OK,
        STATUS_NOT_ENOUGH_DATA
    }
}
