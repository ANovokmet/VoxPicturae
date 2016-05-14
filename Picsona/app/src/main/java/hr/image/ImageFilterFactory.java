package hr.image;

import android.graphics.Bitmap;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Random;

import hr.sound.ProcessingResult;
import jp.co.cyberagent.android.gpuimage.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.GPUImageLevelsFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageRGBFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageSaturationFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageVignetteFilter;

/**
 * Created by Ante on 6.5.2016..
 */
public class ImageFilterFactory {

    private OverlayGenerator emojiOverlayGenerator;
    Random rand = new Random();
    int distinctEmojis = 3;

    public ImageFilterFactory(OverlayGenerator generator) {
        emojiOverlayGenerator = generator;
    }

    public GPUImageFilter calculateFilter(float gender, float pitch, float maxFreq, float anger, float sadness, float happiness, float intensity) {
        ArrayList<GPUImageFilter> filters = new ArrayList<GPUImageFilter>();

        float redmid = gender > 0.5f ? 1 - (gender - 0.5f) * 2f : 1 + (1 - gender - 0.5f) * 3f * 2f;
        float greenmid = 1f;
        if (Math.abs(gender - 0.5f) <= 0.2f) {
            greenmid += (gender - 0.5f) * 5f * rand.nextFloat();
        }
        float bluemid = gender > 0.75f ? 1 + ((gender - 0.75f) * 4f) * 2f : gender > 0.25f && gender < 0.5f ? (gender - 0.25f) * 4f : 1f;

        //red = 1-2 za zensko, 0-1 za musko
        //green = 1-2 zelena nijansa, 0-1 ljubicasta
        //blue = 1-3 plava, 0-1 žuta

        if (maxFreq < 300) {
            float amount = maxFreq / 300f;
            GPUImageVignetteFilter vignetteFilter = new GPUImageVignetteFilter(new PointF(0.5f, 0.5f), new float[]{0.0f, 0.0f, 0.0f}, 0.5f * amount, 0.88f);
            filters.add(vignetteFilter);
        } else if (maxFreq > 1000) {
            float amount = (maxFreq - 1000f) / 500;
            if (amount > 1f) amount = 1f;
            amount = 1 - amount;

            float redamount = redmid / 2 > 1f ? 1f : redmid / 2;
            float greenamount = greenmid / 2 > 1f ? 1f : greenmid / 2;
            float blueamount = bluemid / 2 > 1f ? 1f : bluemid / 2;


            GPUImageVignetteFilter vignetteFilter = new GPUImageVignetteFilter(new PointF(0.5f, 0.5f), new float[]{redamount, greenamount, blueamount}, 0.3f * amount, 0.88f);
            filters.add(vignetteFilter);
        }


        float contrastmin = 0.2f;//pitch/600
        float contrastmax = 0.9f;//maxfreq/1000

        GPUImageLevelsFilter levelsFilter = new GPUImageLevelsFilter();
        levelsFilter.setRedMin(contrastmin, redmid, contrastmax);
        levelsFilter.setGreenMin(contrastmin, greenmid, contrastmax);
        levelsFilter.setBlueMin(contrastmin, bluemid, contrastmax);//dobar pristup, min treba generalno biti veći od 0 nego max manji od 1
        filters.add(levelsFilter);

        if (intensity < 0.5f) {
            filters.add(new GPUImageSaturationFilter(intensity * 2f));
        } else if (intensity > 1.5f) {
            filters.add(new GPUImageContrastFilter(1f + intensity / 4));
        }

        if (anger > 0.5f && happiness < 0.5f && sadness < 0.5f) {

            float amount = 1 - (anger - 0.5f);
            filters.add(new GPUImageRGBFilter(1.0f, amount, amount));
        } else if (anger < 0.5f && happiness > 0.5f && sadness < 0.5f) {

            float amount = 1 - (happiness - 0.5f);
            filters.add(new GPUImageRGBFilter(1.0f, amount, 1.0f));
        } else if (anger < 0.5f && happiness < 0.5f && sadness > 0.5f) {

            float amount = 1 - (sadness - 0.5f);
            filters.add(new GPUImageRGBFilter(amount, amount, 1.0f));
        } else {
            if (anger > 0 && happiness > 0 && sadness > 0)
                filters.add(new GPUImageRGBFilter(1.0f + anger, 1.0f + happiness, 1.0f + sadness));
        }

        return new GPUImageFilterGroup(filters);
    }


    public Bitmap calculateOverlay(float gender, float anger, float sadness, float happiness, int doubleemojicount) {
        Bitmap bitmap = emojiOverlayGenerator.clearLastOverlay();

        if (doubleemojicount > 0) {
            if (gender > 0.75f) {
                emojiOverlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Male);
                bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis(1);
                doubleemojicount--;
            } else if (gender < 0.25f) {
                emojiOverlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Female);
                bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis(1);
                doubleemojicount--;
            }
        }

        float angerSqr = anger * anger;
        float sadnessSqr = sadness * sadness;
        float happinessSqr = happiness * happiness;

        float sumEmojiParamsSqr = angerSqr + sadnessSqr + happinessSqr;
        float numAnger = angerSqr / sumEmojiParamsSqr;
        float numSadness = sadnessSqr / sumEmojiParamsSqr;
        float numHappiness = happinessSqr / sumEmojiParamsSqr;

        int numAngEmojis = Math.round(numAnger * doubleemojicount * (float) Math.ceil((double) anger / 0.25f) / 4f);
        int numSadEmojis = Math.round(numSadness * doubleemojicount * Math.round(sadness / 0.25f) / 4f);
        int numHapEmojis = Math.round(numHappiness * doubleemojicount * Math.round(happiness / 0.25f) / 4f);

        if (numAngEmojis > 0 && anger >= 0.25f) {
            emojiOverlayGenerator.prepareEmojis(distinctEmojis, OverlayGenerator.EmojiType.Angry);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis(numAngEmojis);
        }
        if (numHapEmojis > 0 && happiness >= 0.25f) {
            emojiOverlayGenerator.prepareEmojis(distinctEmojis, OverlayGenerator.EmojiType.Happy);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis(numHapEmojis);
        }
        if (numSadEmojis > 0 && sadness >= 0.25f) {
            emojiOverlayGenerator.prepareEmojis(distinctEmojis, OverlayGenerator.EmojiType.Sad);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis(numSadEmojis);
        }
        return bitmap;
    }

    public Bitmap calculateOverlay(float gender, float anger, float sadness, float happiness) {
        Bitmap bitmap = emojiOverlayGenerator.clearLastOverlay();

        if (gender > 0.75f) {
            emojiOverlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Male);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis(1);
        } else if (gender < 0.25f) {
            emojiOverlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Female);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis(1);
        }


        if (anger > 0.5f) {
            emojiOverlayGenerator.prepareEmojis(distinctEmojis, OverlayGenerator.EmojiType.Angry);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis((int) Math.ceil(anger / 0.50f));
        }
        if (happiness > 0.5f) {
            emojiOverlayGenerator.prepareEmojis(distinctEmojis, OverlayGenerator.EmojiType.Happy);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis((int) Math.ceil(happiness / 0.50f));
        }
        if (sadness > 0.5f) {
            emojiOverlayGenerator.prepareEmojis(distinctEmojis, OverlayGenerator.EmojiType.Sad);
            bitmap = emojiOverlayGenerator.reCreateOverlayWithMoreEmojis((int) Math.ceil(sadness / 0.50f));
        }

        return bitmap;
    }

    public GPUImageFilter calculateFilter(ProcessingResult result) {
        return calculateFilter((float) result.getGenderProbability(),
                (float) result.getPitch(),
                (float) result.getMaxFrequency(),
                (float) result.getEmotionData().getAngerProbability(),
                (float) result.getEmotionData().getSadnessProbability(),
                (float) result.getEmotionData().getHappinessProbability(),
                (float) result.getEmotionData().getSpeechIntensity());
    }


    public Bitmap calculateOverlay(ProcessingResult result, int numberOfDoubleEmojis) {
        return calculateOverlay((float) result.getGenderProbability(),
                (float) result.getEmotionData().getAngerProbability(),
                (float) result.getEmotionData().getSadnessProbability(),
                (float) result.getEmotionData().getHappinessProbability(), numberOfDoubleEmojis);

    }
}
