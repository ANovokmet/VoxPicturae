package hr.sound.spectrum;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SpectrumData {

    private int[][] frequencyRanges;
    private List<List<Double>> buffer = new ArrayList<>();
    private boolean mutable = true;
    private double[] spectrumAverages;

    /**
     * Creates new mutable SpectrumData instance.
     * @param frequencyRanges array of frequency ranges. Each sub-array must have only two elements,
     * bottom and top border. Comparison is done with >= for bottom and < for top border.
     */
    public SpectrumData(int[][] frequencyRanges) {
        this.frequencyRanges = frequencyRanges;
        for (int i = 0; i < frequencyRanges.length; i++) {
            buffer.add(new LinkedList<Double>());
        }
        spectrumAverages = new double[frequencyRanges.length];
        for (int i = 0; i < frequencyRanges.length; i++) {
            if (frequencyRanges[i].length == 2 && frequencyRanges[i][0] < frequencyRanges[i][1]) {
                continue;
            }
            throw new RuntimeException("Wrong frequency range format.");
        }
    }

    /**
     * Adds spectrum information to object.
     * @param frequency
     * @param intensity
     */
    public void addData(double frequency, double intensity) {
        if (this.mutable == false) {
            throw new RuntimeException("No modifications are allowed on this object");
        }
        for (int i = 0; i < this.frequencyRanges.length; i++) {
            if (this.frequencyRanges[i][0] <= frequency && this.frequencyRanges[i][1] > frequency) {
                buffer.get(i).add(intensity);
                break;
            }
        }
    }

    /**
     * Calculates spectrum averages based on data added. After calling this method object becomes "read-only" -
     * you can only read spectrum averages.
     */
    public void calculateData() {
        if (this.mutable == false) {
            return;
        }
        this.mutable = false;
        for (int i = 0; i < this.frequencyRanges.length; i++) {
            double sum = 0;
            for (int j = 0; j < this.buffer.get(i).size(); j++) {
                sum += this.buffer.get(i).get(j);
            }
            this.spectrumAverages[i] = sum / Math.log(this.buffer.get(i).size());
        }
        buffer = null;
    }

    public double[] getSpectrumAverages() {
        return spectrumAverages;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpectrumData{ ");
        for (int i = 0; i < this.frequencyRanges.length; i++) {
            builder.append("[" + this.frequencyRanges[i][0] + ", " + this.frequencyRanges[i][1] + ">" + " : " + this.spectrumAverages[i] + ", ");
        }
        builder.append("}");
        return builder.toString();
    }
}
