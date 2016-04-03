package hr.sound;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import hr.sound.average.SignalAverage;
import hr.sound.filter.FilterFactory;
import hr.sound.gender.GenderRecognizer;
import hr.sound.pitch.PitchDetector;
import hr.sound.spectrum.SpectrumData;

public class ProcessingThread implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(ProcessingThread.class.getName());

    private BlockingQueue<TrackElement> inputQueue;
    private BlockingQueue<TrackElement> outputQueue;
    private boolean stop = false;
    private OnUpdateListener listener;

    private int FREQ_RANGE_FOR_POWER_SUM = 50;

    public ProcessingThread(BlockingQueue<TrackElement> inputQueue, BlockingQueue<TrackElement> outputQueue, OnUpdateListener listener) {
        if (inputQueue == null) {
            throw new RuntimeException("InputQueue must not be null");
        }
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.listener = listener;
    }

    @Override
    public void run() {
        DoubleFFT_1D fft = new DoubleFFT_1D(SoundProcessing.TIME_WINDOW);
        while (!stop) {
            TrackElement element = null;
            try {
                element = this.inputQueue.poll(1, TimeUnit.SECONDS);
                if (element == null) {
                    continue;
                }
            } catch (Exception e) {
                LOGGER.severe(e.getMessage());
            }
            ;
            short data[] = element.getData();

            FilterFactory.getDefaultFilter().execute(data);

            element.setPower(SignalAverage.RMSsignalAverage(data));

            int minFreq = (int) PitchDetector.getInstance().getPitch(data);


            //copying the data to double buffer for FFT processing
            double[] processingBuffer = new double[2 * data.length];
            for (int j = 0; j < data.length; j++) {
                processingBuffer[j] = data[j];
            }
            for (int j = data.length; j < 2 * data.length; j++) {  //zero padding
                processingBuffer[j] = 0;
            }
            fft.realForward(processingBuffer);


            //determining the maximal frequency

            double maximalSumOfIntensities = 0;
            double maximalFrequency = 0;

            int index_range = (int) (FREQ_RANGE_FOR_POWER_SUM * ((double) data.length / SoundProcessing.SAMPLE_RATE));
            for (int k = 0; k < processingBuffer.length / 2; k++) {
                float currentFrequency = (float) k * SoundProcessing.SAMPLE_RATE / data.length;
                double sumOfIntensities = 0;
                for (int j = k - index_range; j <= k + index_range; j++) {
                    if (j >= 0 && j < data.length / 2) {
                        int realIndex = 2 * j;
                        int imaginaryIndex = 2 * j + 1;
                        double intensity = Math.sqrt(Math.pow(processingBuffer[realIndex], 2) + Math.pow(processingBuffer[imaginaryIndex], 2));
                        sumOfIntensities += intensity;
                    }
                }

                if (sumOfIntensities > maximalSumOfIntensities) {
                    maximalSumOfIntensities = sumOfIntensities;
                    maximalFrequency = currentFrequency;
                }
            }

            element.setMaxFrequency(maximalFrequency);


            SpectrumData spectrumData = new SpectrumData(new int[][]{{0, 500}, {500, 1500}, {1500, 3000}, {3000, 6000}, {6000, 10000}});
            for (int k = 0; k < processingBuffer.length / 2; k++) {
                float currentFrequency = (float) k * SoundProcessing.SAMPLE_RATE / data.length;
                int real = 2 * k;
                int imaginary = 2 * k + 1;
                double intensity = Math.sqrt(Math.pow(processingBuffer[real], 2) + Math.pow(processingBuffer[imaginary], 2));
                spectrumData.addData(currentFrequency, intensity);
            }
            spectrumData.calculateData();


            element.frequency_data = spectrumData.getSpectrumAverages();
            if (element.getPower() > SoundProcessing.POWER_THRESHOLD) {
                //LOGGER.info("frekv podaci  "+element.getPower()+" "+GenderRecognizer.genderFromSpectrum(spectrumData.getSpectrumAverages())+" "+spectrumData);
                double genderComponent1 = GenderRecognizer.genderFromSpectrum(spectrumData.getSpectrumAverages());
                double temp = 1 - (element.getMaxFrequency() - 500) / 700;
                double genderComponent2 = temp < 0 ? 0 : temp > 1 ? 1 : temp;
                temp = 0;
                if (element.getPitch() < 150) {
                    temp = 1;
                } else if (element.getPitch() > 200) {
                    temp = 0;
                } else {
                    temp = 1 - (element.getPitch() - 150) / 50;
                }
                double genderComponent3 = temp;
                element.gender = (0.6 * genderComponent1 + 0.3 * genderComponent2 + 0.1 * genderComponent3);
            }

            element.setPitch(minFreq);

            if (this.outputQueue != null) {
                try {
                    this.outputQueue.put(element);
                } catch (Exception e) {
                    LOGGER.severe("Error " + e.getMessage());
                }
            }
            if (listener != null) {
                listener.onUpdate(spectrumData.getSpectrumAverages());
            }
        }
    }

    public void stop() {
        stop = true;
    }

    public interface OnUpdateListener{
        void onUpdate(double[] soundData);
    }
}
