package hr.sound;

public interface AudioOutputDevice {

    /**
     * Method that starts playing
     */
    void start();

    /**
     * Method that writes audio data to buffer
     * @param buffer
     * @param length
     */
    void write(short[] buffer, int length);

    /**
     * Method that stops playing
     */
    void stop();

    /**
     * Method that closes audio device
     */
    void close();
}
