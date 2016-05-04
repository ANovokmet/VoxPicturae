package hr.sound.emotion;


public class EmotionData {

    private double angerProbability, sadnessProbability, happinessProbability;

    private double speechIntensity;

    public EmotionData(double angerProbability, double sadnessProbability, double happinessProbability, double speechIntensity){
        this.angerProbability = angerProbability;
        this.sadnessProbability = sadnessProbability;
        this.happinessProbability = happinessProbability;
        this.speechIntensity = speechIntensity;
    }

    public double getSpeechIntensity() {
        return speechIntensity;
    }

    public double getHappinessProbability() {
        return happinessProbability;
    }

    public double getSadnessProbability() {
        return sadnessProbability;
    }

    public double getAngerProbability() {
        return angerProbability;
    }

    @Override
    public String toString() {
        return "EmotionData{" +
                "angerProbability=" + angerProbability +
                ", sadnessProbability=" + sadnessProbability +
                ", happinessProbability=" + happinessProbability +
                ", speechIntensity=" + speechIntensity +
                '}';
    }
}
