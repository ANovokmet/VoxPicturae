package hr.sound.pitch;

public abstract class PitchDetector {

    public abstract double getPitch(short data[]);

    /**
     * @return Default PitchDetector implementation
     */
    public static PitchDetector getInstance() {
        return new AMDFPitchDetector();
    }
}
