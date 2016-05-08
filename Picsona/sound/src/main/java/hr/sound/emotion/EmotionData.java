package hr.sound.emotion;


public class EmotionData {

    private double angerProbability, sadnessProbability, happinessProbability;

    private double speechIntensity;

    private Status status;

    public EmotionData(double angerProbability, double sadnessProbability, double happinessProbability, double speechIntensity){
        this.angerProbability = angerProbability;
        this.sadnessProbability = sadnessProbability;
        this.happinessProbability = happinessProbability;
        this.speechIntensity = speechIntensity;
        status = Status.STATUS_OK;
    }

    public EmotionData(){
        this(0, 0, 0, 0);
        status = Status.STATUS_NOT_ENOUGH_DATA;
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
