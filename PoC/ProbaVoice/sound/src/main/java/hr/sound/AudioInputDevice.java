package hr.sound;

public interface AudioInputDevice {

    /**
     * Starts recording
     */
    void start();

    /**
     * Reads audio data to buffer. Implementations may vary in decision to try read length or just currently available data
     * @param buffer
     * @param length
     * @return number of actually read shorts
     */
    int read(short[] buffer, int length);

    /**
     * Stops recording
     */
    void stop();

    /**
     * Closes audio device
     */
    void close();
}
