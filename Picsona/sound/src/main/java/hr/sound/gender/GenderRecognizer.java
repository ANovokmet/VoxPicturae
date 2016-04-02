package hr.sound.gender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class GenderRecognizer {

    private GenderRecognizer() {
    }

    /**
     * Function assumes following frequency distribution: {{0, 500}, {500, 1500}, {1500, 3000}, {3000, 6000}, {6000, 10000}}
     *
     * @param spectrumData calculated spectrum averages
     * @return probability that speaker is male (female is opposite)
     */
    public static double genderFromSpectrum(double[] spectrumData) {
        double probability = 0;
        Map<Integer, List<Integer>> classes = getIntensityClasses(spectrumData);
        if (classes.size() <= 1) {
            return 0.5;  //we can't estimate with only one class
        }
        double sumation = 0;
        for (int i = 0; i < spectrumData.length; i++) {
            sumation += spectrumData[i];
        }
        double[] coeffitients = new double[spectrumData.length];
        for (int i = 0; i < spectrumData.length; i++) {
            coeffitients[i] = spectrumData[i] / sumation;
        }
        List<Integer> keys = new ArrayList<>(classes.keySet());

        /**
         * Checking weight classes for pattern regarding frequency.
         * We check if top two classes contain relevant frequency ranges, and if they do,
         * total probability is calculated based on predefined probability for that segment
         * (to be in that class) and a percentage of that segment in total energy.
         */

        if (classes.get(keys.get(0)).contains(1)) {
            probability += 1 * coeffitients[0];
        }
        if (classes.get(keys.get(0)).contains(2)) {
            probability += 0.5 * coeffitients[1];
        }
        if (classes.get(keys.get(0)).contains(3)) {
            probability -= 0.3 * coeffitients[2];
        }
        if (classes.get(keys.get(0)).contains(4)) {
            probability -= 0.8 * coeffitients[3];
        }
        if (classes.get(keys.get(1)).contains(1)) {
            probability -= 0.5 * coeffitients[0];
        }
        if (classes.get(keys.get(1)).contains(2)) {
            probability += 0.6 * coeffitients[1];
        }
        if (classes.get(keys.get(1)).contains(3)) {
            probability += 0.5 * coeffitients[2];
        }
        if (classes.get(keys.get(1)).contains(4)) {
            probability -= 0.7 * coeffitients[3];
        }
        probability = probability < 0 ? 0 : probability;
        return probability > 1 ? 1 : probability;
    }

    private static int orderOfMagnitude(double number) {
        int orderOfMagnitude = 0;
        while (number > 1) {
            orderOfMagnitude++;
            number /= 10;
        }
        return orderOfMagnitude;
    }

    private static Map<Integer, List<Integer>> getIntensityClasses(double[] spectrumData) {
        Map<Integer, List<Integer>> classes = new TreeMap<>(new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return o2.compareTo(o1);
            }
        });

        for (int i = 0; i < spectrumData.length; i++) {
            int orderOfMagnitude = orderOfMagnitude(spectrumData[i]);
            if (classes.get(orderOfMagnitude) == null) {
                classes.put(orderOfMagnitude, new ArrayList<Integer>());
            }
            classes.get(orderOfMagnitude).add(i + 1);
        }
        return classes;
    }
}
