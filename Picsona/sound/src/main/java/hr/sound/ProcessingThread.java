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

    private int TIME_WINDOW = SoundProcessing.TIME_WINDOW;
    private int FREQ_RANGE_FOR_POWER_SUM = 50;

    public ProcessingThread(BlockingQueue<TrackElement> inputQueue, BlockingQueue<TrackElement> outputQueue) {
        if (inputQueue == null) {
            throw new RuntimeException("InputQueue must not be null");
        }
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    @Override
    public void run() {
        DoubleFFT_1D fft = new DoubleFFT_1D(TIME_WINDOW);
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

            int min_frekv = (int) PitchDetector.getInstance().getPitch(data);


            //copying the data to double buffer for FFT processing
            double[] processing_buffer = new double[2 * data.length];
            for (int j = 0; j < data.length; j++) {
                processing_buffer[j] = data[j];
            }
            for (int j = data.length; j < 2 * data.length; j++) {  //zero padding
                processing_buffer[j] = 0;
            }
            fft.realForward(processing_buffer);


            //determining the maximal frequency

            double maximal_sum_of_intensities = 0;
            double maximal_frequency = 0;

            int index_range = (int) (FREQ_RANGE_FOR_POWER_SUM * ((double) data.length / SoundProcessing.SAMPLE_RATE));
            for (int k = 0; k < processing_buffer.length / 2; k++) {
                float current_frequency = (float) k * SoundProcessing.SAMPLE_RATE / data.length;
                double sum_of_intensities = 0;
                for (int j = k - index_range; j <= k + index_range; j++) {
                    if (j >= 0 && j < data.length / 2) {
                        int real_index = 2 * j;
                        int imaginary_index = 2 * j + 1;
                        double intensity = Math.sqrt(Math.pow(processing_buffer[real_index], 2) + Math.pow(processing_buffer[imaginary_index], 2));
                        sum_of_intensities += intensity;
                    }
                }

                if (sum_of_intensities > maximal_sum_of_intensities) {
                    maximal_sum_of_intensities = sum_of_intensities;
                    maximal_frequency = current_frequency;
                }
            }

            element.setMaxFrequency(maximal_frequency);


            SpectrumData spectrumData = new SpectrumData(new int[][]{{0, 500}, {500, 1500}, {1500, 3000}, {3000, 6000}, {6000, 10000}});
            for (int k = 0; k < processing_buffer.length / 2; k++) {
                float current_frequency = (float) k * SoundProcessing.SAMPLE_RATE / data.length;
                int real = 2 * k;
                int imaginary = 2 * k + 1;
                double intensity = Math.sqrt(Math.pow(processing_buffer[real], 2) + Math.pow(processing_buffer[imaginary], 2));
                spectrumData.addData(current_frequency, intensity);
            }
            spectrumData.calculateData();


            element.frequency_data = spectrumData.getSpectrumAverages();
            if (element.getPower() > SoundProcessing.POWER_THRESHOLD) {
                //LOGGER.info("frekv podaci  "+element.getPower()+" "+GenderRecognizer.genderFromSpectrum(spectrumData.getSpectrumAverages())+" "+spectrumData);
                double gender_component_1 = GenderRecognizer.genderFromSpectrum(spectrumData.getSpectrumAverages());
                double a = 1 - (element.getMaxFrequency() - 500) / 700;
                double gender_component_2 = a < 0 ? 0 : a > 1 ? 1 : a;
                double b = 0;
                if (element.getPitch() < 150) {
                    b = 1;
                } else if (element.getPitch() > 200) {
                    b = 0;
                } else {
                    b = 1 - (element.getPitch() - 150) / 50;
                }
                double gender_component_3 = b;
                element.gender = (0.6 * gender_component_1 + 0.3 * gender_component_2 + 0.1 * gender_component_3);
            }

            element.setPitch(min_frekv);

            if (this.outputQueue != null) {
                try {
                    this.outputQueue.put(element);
                } catch (Exception e) {
                    LOGGER.severe("Error " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        stop = true;
    }
}
