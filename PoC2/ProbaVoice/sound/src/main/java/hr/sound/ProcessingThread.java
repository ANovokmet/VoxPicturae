package hr.sound;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import hr.sound.filter.FilterFactory;

public class ProcessingThread implements Runnable {

    private final static Logger LOGGER = Logger.getLogger(ReadingThread.class.getName());

    private BlockingQueue<TrackElement> inputQueue;
    private BlockingQueue<TrackElement> outputQueue;
    private boolean stop = false;

    private int TIME_WINDOW = SoundProcessing.TIME_WINDOW;
    private int FREQ_RANGE_FOR_POWER_SUM = 50;

    public ProcessingThread(BlockingQueue<TrackElement> inputQueue, BlockingQueue<TrackElement> outputQueue){
        if(inputQueue == null){
            throw new RuntimeException("InputQueue must not be null");
        }
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
    }

    @Override
    public void run(){
        DoubleFFT_1D fft = new DoubleFFT_1D(TIME_WINDOW);
        while(!stop){
            TrackElement element = null;
            try {
                element = this.inputQueue.poll(1, TimeUnit.SECONDS);
                if(element == null){
                    continue;
                }
            }catch(Exception e){
                LOGGER.severe(e.getMessage());
            };
            short data[] = element.getData();

            FilterFactory.getDefaultFilter().execute(data);

            //LOGGER.info("avg "+signalAverage(data)+"   "+RMSsignalAverage(data));

            int min_frekv = AMDFBaseFrequency(data);

            element.setPower(RMSsignalAverage(data));

            if(element.getPower()>100){
                if(min_frekv<150){
                    element.gender = 1;
                } else if(min_frekv>200){
                    element.gender = 0;
                } else{
                    element.gender = 1 - (min_frekv-150)/50;
                }
            }


            //LOGGER.info("bazna frekvencija" +" "+RMSsignalAverage(data)+"  "+min_frekv);

            double[] processing_buffer = new double[2*data.length];
            for(int j=0; j<data.length; j++){
                processing_buffer[j] = data[j];
            }
            for(int j=data.length; j<2*data.length; j++){  //zero padding
                processing_buffer[j] = 0;
            }
            fft.realForward(processing_buffer);
            double max = 0;
            double max_frekv = 0;

            int raspon_indexa = (int) ( FREQ_RANGE_FOR_POWER_SUM * ((double) data.length / SoundProcessing.SAMPLE_RATE));
            for (int k = 0; k < data.length / 2; k++) {
                float frekv = (float) k * SoundProcessing.SAMPLE_RATE / data.length;
                int real = 2 * k;
                int imaginary = 2 * k + 1;
                if (frekv < 0 || frekv > 6000) {
                    processing_buffer[real] = 0;
                    processing_buffer[imaginary] = 0;
                }
                double sum = 0;
                for(int j=k-raspon_indexa; j<=k+raspon_indexa; j++){
                    if(j>=0 && j< data.length/2){
                        int real2 = 2 * j;
                        int imaginary2 = 2 * j + 1;
                        double iznos = Math.sqrt(Math.pow(processing_buffer[real2],2) + Math.pow(processing_buffer[imaginary2],2));
                        sum += iznos;
                    }
                }

                if(sum>max){
                    max = sum;
                    max_frekv = frekv;
                }
            }
            //LOGGER.info("max frekv"+Thread.currentThread().getId()+ " "+max+" "+max_frekv);

            //LOGGER.info("vrem domena "+Thread.currentThread().getId()+ ""+max_inten+" "+max_inten_index+" "+max+" "+max_frekv);

            int numofpeaks = 0;
            for(int i=0; i<processing_buffer.length; i++){
                if(0.9*max_frekv<processing_buffer[i]){
                    numofpeaks++;
                }
            }
            element.numofpeaks = numofpeaks;

            element.setMax_frequency(max_frekv);
            element.setPitch(min_frekv);

            if(this.outputQueue != null){
                try {
                    this.outputQueue.put(element);
                }catch(Exception e){
                    //Log.e("Error", e.getMessage());
                }
            }
        }
    }

    private double signalAverage(short[] data){
        double sum = 0;
        for(int i = 0; i < data.length; i++){
            sum += data[i];
        }
        return sum/data.length;
    }

    /**
     * Calculates root mean square average value which is proportional with power
     * @param data signal data
     * @return RMS average value
     */
    private double RMSsignalAverage(short[] data){
        double sum = 0;
        for(int i = 0; i < data.length; i++){
            sum += Math.pow(data[i], 2);
        }
        return Math.sqrt(sum/data.length);
    }

    private int AMDFBaseFrequency(short[] data){
        int start_freq = 90, max_freq = 400;  //expected frequency range for base frequency in voice is 90-400 Hz
        double max_period = (double) TIME_WINDOW / SoundProcessing.SAMPLE_RATE * 1000;  //in ms
        double min_sum = Double.MAX_VALUE;
        int min_freq = 0;
        for(int freq = start_freq; freq < max_freq; freq++){
            double test_period = 1000 / freq;  //period for which test about repetition is conducted
            int number_of_period_windows = (int) Math.floor(max_period / test_period);  //how much test periods fits in max period
            int size_of_window = (int) (test_period * TIME_WINDOW / max_period);  //number of samples in TIME_WINDOW that are part of test period
            double signal_difference = 0;  //total signal difference that is calculated between two consecutive periods
            int number_of_points = 0;  //number of points used for difference calculation
            for(int window_index = 0; window_index < number_of_period_windows - 1; window_index++){
                for(int j=0; j<size_of_window;j++){
                    signal_difference += Math.abs(data[window_index*size_of_window+j]-data[(window_index+1)*size_of_window+j]);
                    number_of_points++;
                }
            }
            if(min_sum > signal_difference/number_of_points){
                min_sum = signal_difference/number_of_points;
                min_freq = freq;
            }
        }
        return min_freq;
    }

    public void stop(){
        stop = true;
    }
}
