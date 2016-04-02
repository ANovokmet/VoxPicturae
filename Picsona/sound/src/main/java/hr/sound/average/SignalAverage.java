package hr.sound.average;

/**
 * Utility class for calculating signal average values
 */
public class SignalAverage {

    private SignalAverage() {
    }

    /**
     * Calculates root mean square average value which is proportional with power
     * @param data signal data
     * @return RMS average value
     */
    public static double RMSsignalAverage(short[] data){
        double sum = 0;
        for(int i = 0; i < data.length; i++){
            sum += Math.pow(data[i], 2);
        }
        return Math.sqrt(sum/data.length);
    }
}
