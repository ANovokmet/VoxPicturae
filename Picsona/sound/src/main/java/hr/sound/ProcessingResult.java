package hr.sound;

import hr.sound.emotion.EmotionData;

public class ProcessingResult {

    private double genderProbability;
    private double pitch;
    private double maxFrequency;
    private EmotionData emotionData;

    public ProcessingResult(double genderProbability, double pitch, double maxFrequency, EmotionData emotionData){
        this.genderProbability = genderProbability;
        this.pitch = pitch;
        this.maxFrequency = maxFrequency;
        this.emotionData = emotionData;
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
                '}';
    }
}
