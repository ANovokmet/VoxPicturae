package hr.sound.emotion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import hr.sound.SoundProcessing;

public class EmotionRecognizer {

    private final static Logger LOGGER = Logger.getLogger(EmotionRecognizer.class.getName());

    public static EmotionData emotionFromSpeech(List<Double> elementPowers) {
        double angerProbability = 0, sadnessProbability = 0, happinessProbability = 0;
        double harmonicMean = harmonicMean(elementPowers);
        double standardDeviation = standardDeviation(elementPowers, harmonicMean);

        LOGGER.info("emotion " + harmonicMean + "  " + standardDeviation);

        List<Integer> silencePeriodLengths = new ArrayList<>();
        List<Integer> speechPeriodLengths = new ArrayList<>();
        int speechPeriodLength = 0, silencePeriodLength = 0;
        boolean isFirstSpeechPeriod = true;
        List<Double> firstSpeechPeriod = new ArrayList<>();

        for (double elementPower : elementPowers) {
            if (elementPower > harmonicMean) {
                if (silencePeriodLength > 0) {  //if switch happened
                    silencePeriodLengths.add(silencePeriodLength);
                    silencePeriodLength = 0;
                }
                if (isFirstSpeechPeriod) {
                    firstSpeechPeriod.add(elementPower);
                }
                speechPeriodLength++;
            } else {
                if (speechPeriodLength > 0) {
                    speechPeriodLengths.add(speechPeriodLength);
                    speechPeriodLength = 0;
                }
                if (isFirstSpeechPeriod && firstSpeechPeriod.size() != 0) {
                    isFirstSpeechPeriod = false;
                }
                silencePeriodLength++;
            }
        }

        if (speechPeriodLengths.isEmpty()) {
            return new EmotionData();
        }

        LOGGER.info("prvih nekoliko elemanata " + firstSpeechPeriod);

        int minimalPeriodLength = listMinimum(speechPeriodLengths);
        //remove minimal periods of speech from list because of probability to record very short speech spikes in silence
        speechPeriodLengths.removeAll(Collections.singletonList(minimalPeriodLength));

        minimalPeriodLength = listMinimum(silencePeriodLengths);
        //remove minimal periods of silence from list because of probability to record very short silence spikes during speech
        silencePeriodLengths.removeAll(Collections.singletonList(minimalPeriodLength));
        //remove first and last element of silence because analysis should only consider interval during which speech occurs
        silencePeriodLengths.remove(0);
        if (silencePeriodLengths.size() - 1 >= 0) {
            silencePeriodLengths.remove(silencePeriodLengths.size() - 1);
        }

        double sumOfSpeechPeriodLengths = 0;
        for (int periodLength : speechPeriodLengths) {
            sumOfSpeechPeriodLengths += periodLength;
        }
        double sumOfSilencePeriodLengths = 0;
        for (int periodLength : silencePeriodLengths) {
            sumOfSilencePeriodLengths += periodLength;
        }

        double speechPeriodLengthsAvg = sumOfSpeechPeriodLengths / speechPeriodLengths.size();
        double silencePeriodLengthsAvg = sumOfSilencePeriodLengths / silencePeriodLengths.size();
        if (Double.isNaN(silencePeriodLengthsAvg)) {
            silencePeriodLengthsAvg = 0;
        }
        LOGGER.info("govor tisina " + speechPeriodLengths + " " + silencePeriodLengths + " " + speechPeriodLengthsAvg + " " + silencePeriodLengthsAvg);

        double periodLengthsDiff = 0.15 * (speechPeriodLengthsAvg + silencePeriodLengthsAvg);
        double neutralSadnessDampening = 1;
        if (Math.abs(speechPeriodLengthsAvg - silencePeriodLengthsAvg) < periodLengthsDiff) {
            //neutral emotion
            neutralSadnessDampening = 0.3;
        } else if (speechPeriodLengthsAvg - periodLengthsDiff > silencePeriodLengthsAvg) {
            //anger and happines
            double coef = (speechPeriodLengthsAvg - silencePeriodLengthsAvg) / silencePeriodLengthsAvg;
            coef = coef > 1 ? 1 : coef;
            angerProbability += coef * 0.3333;
            happinessProbability += coef * 0.3333;
        } else if (speechPeriodLengthsAvg + periodLengthsDiff < silencePeriodLengthsAvg) {
            //sadness
            double coef = (silencePeriodLengthsAvg - speechPeriodLengthsAvg) / speechPeriodLengthsAvg;
            coef = coef > 1 ? 1 : coef;
            sadnessProbability += coef * 0.5;
        }

        if (standardDeviation > 3600) {
            angerProbability += 0.6666 * neutralSadnessDampening;
        } else if (standardDeviation < 1600) {
            sadnessProbability += 0.5 * neutralSadnessDampening;
        } else {
            //expected possible sadness range is 1000-2600, lower than 1600 is certain that is sadness
            double sadnessLocalProbability = (1000 - standardDeviation + 1600) / 1000 * 0.5;
            sadnessProbability += sadnessLocalProbability < 0 ? 0 : sadnessLocalProbability * neutralSadnessDampening;

            double firstSpeak = listMaximum(firstSpeechPeriod);
            double tenseEmotionsLocalProbability = firstSpeak / (harmonicMean * 10);
            double angerAdjustment = tenseEmotionsLocalProbability > 0.5 ? (1.5 * tenseEmotionsLocalProbability - 0.25) : 0.5;
            double happinessAdjustment = tenseEmotionsLocalProbability < 0.5 ? (-1.5 * tenseEmotionsLocalProbability + 1.25) : 0.5;
            angerProbability += tenseEmotionsLocalProbability * 0.6666 * angerAdjustment;
            happinessProbability += (1 - tenseEmotionsLocalProbability) * 0.6666 * happinessAdjustment;
        }

        LOGGER.info("calculated a h s " + angerProbability + " " + happinessProbability + " " + sadnessProbability);
        return new EmotionData(adjustProbability(angerProbability), adjustProbability(sadnessProbability), adjustProbability(happinessProbability), averageMean(elementPowers) / 2000);
    }

    private static double adjustProbability(double probability) {
        if (probability > 1) {
            return 1;
        } else if (probability < 0) {
            return 0;
        }
        return probability;
    }

    private static double harmonicMean(List<Double> data) {
        double sum = 0;
        double numberOfElements = 0;
        for (double broj : data) {
            if (broj > SoundProcessing.POWER_THRESHOLD) {
                sum += 1 / broj;
                numberOfElements++;
            }
        }
        return numberOfElements / sum;
    }

    private static double averageMean(List<Double> data) {
        double sum = 0;
        double numberOfElements = 0;
        for (double broj : data) {
            if (broj > SoundProcessing.POWER_THRESHOLD) {
                sum += broj;
                numberOfElements++;
            }
        }
        return sum / numberOfElements;
    }

    private static double standardDeviation(List<Double> data, double threshold) {
        double sum = 0;
        double numberOfElements = 0;
        for (double number : data) {
            if (number > threshold) {
                sum += number;
                numberOfElements++;
            }
        }
        double average = sum / numberOfElements;
        sum = 0;
        numberOfElements = 0;
        for (double number : data) {
            if (number > threshold) {
                sum += Math.pow(average - number, 2);
                numberOfElements++;
            }
        }
        return Math.sqrt(sum / numberOfElements);
    }

    private static int listMinimum(List<Integer> list) {
        int minimum = Integer.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) < minimum) {
                minimum = list.get(i);
            }
        }
        return minimum;
    }

    private static double listMaximum(List<Double> list) {
        double maximum = Integer.MIN_VALUE;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) > maximum) {
                maximum = list.get(i);
            }
        }
        int br = 0;
        for (double element : list) {
            if (maximum != element && element > maximum * 0.9) {
                br++; //counting the local maximums
            }
        }
        if (br < 2) {  //maximum is just a spike in intensity
            return maximum * 0.7;
        }
        return maximum;
    }
}
