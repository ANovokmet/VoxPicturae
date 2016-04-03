package hr.sound.pitch;

import hr.sound.SoundProcessing;

/**
 * Calculates pitch using AMDF algorithm.
 */
public class AMDFPitchDetector extends PitchDetector {

    @Override
    public double getPitch(short[] data) {
        int startFreq = 90, maxFreq = 400;  //expected frequency range for base frequency in voice is 90-400 Hz
        double maxPeriod = (double) SoundProcessing.TIME_WINDOW / SoundProcessing.SAMPLE_RATE * 1000;  //in ms
        double minSum = Double.MAX_VALUE;
        int minFreq = 0;
        for (int freq = startFreq; freq < maxFreq; freq++) {
            double testPeriod = 1000 / freq;  //period for which test about repetition is conducted
            int numberOfPeriodWindows = (int) Math.floor(maxPeriod / testPeriod);  //how much test periods fits in max period
            int sizeOfWindow = (int) (testPeriod * SoundProcessing.TIME_WINDOW / maxPeriod);  //number of samples in TIME_WINDOW that are part of test period
            double signalDifference = 0;  //total signal difference that is calculated between two consecutive periods
            int numberOfPoints = 0;  //number of points used for difference calculation
            for (int windowIndex = 0; windowIndex < numberOfPeriodWindows - 1; windowIndex++) {
                for (int j = 0; j < sizeOfWindow; j++) {
                    signalDifference += Math.abs(data[windowIndex * sizeOfWindow + j] - data[(windowIndex + 1) * sizeOfWindow + j]);
                    numberOfPoints++;
                }
            }
            if (minSum > signalDifference / numberOfPoints) {
                minSum = signalDifference / numberOfPoints;
                minFreq = freq;
            }
        }
        return minFreq;
    }
}
