package hr.sound.pitch;

import hr.sound.SoundProcessing;

/**
 * Calculates pitch using AMDF algorithm.
 */
public class AMDFPitchDetector extends PitchDetector {

    @Override
    public double getPitch(short[] data) {
        int start_freq = 90, max_freq = 400;  //expected frequency range for base frequency in voice is 90-400 Hz
        double max_period = (double) SoundProcessing.TIME_WINDOW / SoundProcessing.SAMPLE_RATE * 1000;  //in ms
        double min_sum = Double.MAX_VALUE;
        int min_freq = 0;
        for (int freq = start_freq; freq < max_freq; freq++) {
            double test_period = 1000 / freq;  //period for which test about repetition is conducted
            int number_of_period_windows = (int) Math.floor(max_period / test_period);  //how much test periods fits in max period
            int size_of_window = (int) (test_period * SoundProcessing.TIME_WINDOW / max_period);  //number of samples in TIME_WINDOW that are part of test period
            double signal_difference = 0;  //total signal difference that is calculated between two consecutive periods
            int number_of_points = 0;  //number of points used for difference calculation
            for (int window_index = 0; window_index < number_of_period_windows - 1; window_index++) {
                for (int j = 0; j < size_of_window; j++) {
                    signal_difference += Math.abs(data[window_index * size_of_window + j] - data[(window_index + 1) * size_of_window + j]);
                    number_of_points++;
                }
            }
            if (min_sum > signal_difference / number_of_points) {
                min_sum = signal_difference / number_of_points;
                min_freq = freq;
            }
        }
        return min_freq;
    }
}
