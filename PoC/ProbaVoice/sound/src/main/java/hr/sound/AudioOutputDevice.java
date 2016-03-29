package hr.sound;

public interface AudioOutputDevice {

    /**
     * Starts playing
     */
    void start();

    /**
     * Writes audio data to buffer
     * @param buffer
     * @param length
     */
    void write(short[] buffer, int length);

    /**
     * Stops playing
     */
    void stop();

    /**
     * Closes audio device
     */
    void close();
}
