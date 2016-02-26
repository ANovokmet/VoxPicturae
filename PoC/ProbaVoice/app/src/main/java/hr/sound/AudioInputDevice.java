package hr.sound;

public interface AudioInputDevice {

    /**
     * Method that starts recording
     */
    void start();

    /**
     * Method that reads audio data to buffer. Implementations may vary in decision to try read length or just currently available data
     * @param buffer
     * @param length
     * @return number of actually read shorts
     */
    int read(short[] buffer, int length);

    /**
     * Method that stops recording
     */
    void stop();

    /**
     * Method that closes audio device
     */
    void close();
}
