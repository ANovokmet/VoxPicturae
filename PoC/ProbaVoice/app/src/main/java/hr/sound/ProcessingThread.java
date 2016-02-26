package hr.sound;


import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ProcessingThread implements Runnable {

    private BlockingQueue<TrackElement> inputQueue;
    private BlockingQueue<TrackElement> outputQueue;
    private boolean stop = false;

    private int TIME_WINDOW = 1024;
    private int SAMPLE_RATE = 44100;

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
                Log.e("Error", e.getMessage());
            };
            short data[] = element.getData();
            int LENGTH = element.getData().length;
            for(int j=0; j<2; j++) {  //Savitzky-Golay filter
                for (int i = 3; i < LENGTH-3 ; i++) {
                    data[i] = (short)((-2*data[i - 3] + 3*data[i - 2] + 6*data[i - 1]
                            + 7*data[i] + 6*data[i + 1] + 3*data[i + 2]
                            + -2*data[i + 3]) / (-2 + 3 + 6 + 7 + 6 + 3 - 2));
                }
                //processing_buffer[TIME_WINDOW - 1] = (processing_buffer[TIME_WINDOW - 2] + processing_buffer[TIME_WINDOW - 1] + 0) / 3;
            }
            Log.e("avg", ""+signalAverage(data));

            double[] processing_buffer = new double[data.length];
            for(int j=0; j<data.length; j++){
                processing_buffer[j] = data[j];
            }
            fft.realForward(processing_buffer);
            double max = 0;
            double max_frekv = 0;

            int raspon_indexa = (int) ( 10 * ((double) data.length / SAMPLE_RATE));  //raspon 10 Hz
            for (int k = 0; k < data.length / 2; k++) {
                float frekv = (float) k * SAMPLE_RATE / data.length;
                int real = 2 * k;
                int imaginary = 2 * k + 1;
                if (frekv < 0 || frekv > 6000) {
                    data[real] = 0;
                    data[imaginary] = 0;
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
            Log.e("max frekv", ""+max+" "+max_frekv);

            if(this.outputQueue != null){
                try {
                    this.outputQueue.put(element);
                }catch(Exception e){
                    Log.e("Error", e.getMessage());
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

    public void stop(){
        stop = true;
    }
}
