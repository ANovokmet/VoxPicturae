package hr.sound;

public class ProcessingResult {

    private double genderProbability;
    private double pitch;
    private double maxFrequency;

    public ProcessingResult(double genderProbability, double pitch, double maxFrequency){
        this.genderProbability = genderProbability;
        this.pitch = pitch;
        this.maxFrequency = maxFrequency;
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

    @Override
    public String toString() {
        return "ProcessingResult{" +
                "genderProbability=" + genderProbability +
                ", pitch=" + pitch +
                ", maxFrequency=" + maxFrequency +
                '}';
    }
}
