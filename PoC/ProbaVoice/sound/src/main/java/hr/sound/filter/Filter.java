package hr.sound.filter;


public interface Filter {

    /**
     * Executes the filter over data. Result is stored in same array.
     * @param data signal data
     */
    void execute(short[] data);

}
