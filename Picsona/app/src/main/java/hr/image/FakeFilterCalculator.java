package hr.image;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;
import java.util.Random;

import hr.image.filters.PicsonaToneCurveFilter;
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
public class FakeFilterCalculator {

    private OverlayGenerator overlayGenerator;
    public FakeFilterCalculator(OverlayGenerator generator){
        overlayGenerator = generator;
    }


    public GPUImageFilter calculateFilter(int red1, int red2, int green1, int green2, int blue1, int blue2, int composite1, int composite2, int contrast) {
        ArrayList<GPUImageFilter> filters = new ArrayList<GPUImageFilter>();

        PicsonaToneCurveFilter ITCFfilter = new PicsonaToneCurveFilter();



        Point[] redCurve = new Point[]{new Point(0, 0), new Point(64, 64 + (-64 + red1)), new Point(192, 192 + (-64 + red2)), new Point(255, 255)};
        Point[] greenCurve = new Point[]{new Point(0, 0), new Point(64, 64 + (-64 + green1)), new Point(192, 192 + (-64 + green2)), new Point(255, 255)};
        Point[] blueCurve = new Point[]{new Point(0, 0), new Point(64, 64 + (-64 + blue1)), new Point(192, 192 + (-64 + blue2)), new Point(255, 255)};
        Point[] compositeCurve = new Point[]{new Point(0, 0), new Point(64, 64 + (-64 + composite1)), new Point(192, 192 + (-64 + composite2)), new Point(255, 255)};

        ITCFfilter.setCompositeCurveFromArray(compositeCurve);
        ITCFfilter.setRedCurveFromArray(redCurve);
        ITCFfilter.setGreenCurveFromArray(greenCurve);
        ITCFfilter.setBlueCurveFromArray(blueCurve);

        filters.add(ITCFfilter);


        filters.add(new GPUImageContrastFilter(2.0f * (float)contrast/100.0f));

        return new GPUImageFilterGroup(filters);
    }

    public GPUImageFilter calculateFilter(float gender, float pitch, float maxFreq, float anger, float sadness, float happiness, float intensity) {
        ArrayList<GPUImageFilter> filters = new ArrayList<GPUImageFilter>();

        float redmid = gender > 0.5f ? 1-(gender-0.5f)*2f : 1+(1-gender-0.5f)*3f*2f;
        Log.d("redmid",redmid+"");

        float greenmid = 1f;
        if(Math.abs(gender-0.5f) <= 0.2f){
            greenmid += (gender-0.5f)*5f*rand.nextFloat();
        }
        Log.d("greenmid",greenmid+"");

        float bluemid = gender > 0.75f ? 1+((gender-0.75f)*4f)*2f : gender > 0.25f && gender < 0.5f ? (gender-0.25f)*4f : 1f;
        Log.d("bluemid",bluemid+"");

        float[] min = new float[] {0.0f, 0.0f ,0.0f};

        final float[] mid = new float[] {gender*10,2.0f,2.0f};
        //red = 1-2 za zensko, 0-1 za musko
        //green = 1-2 zelena nijansa, 0-1 ljubicasta
        //blue = 1-3 plava, 0-1 žuta

        final float[] max = new float[] {1.0f,1.0f,1.0f};


        if(maxFreq < 300){
            float amount = maxFreq / 300f;
            GPUImageVignetteFilter vignetteFilter = new GPUImageVignetteFilter(new PointF(0.5f,0.5f),new float[] {0.0f, 0.0f, 0.0f}, 0.5f * amount , 0.88f);
            filters.add(vignetteFilter);
            Log.d("vignette black",amount+"");
        }
        else if(maxFreq > 1000){
            float amount = (maxFreq - 1000f) / 500;
            if(amount>1f) amount = 1f;
            amount = 1 - amount;

            float redamount = redmid/2 > 1f ? 1f : redmid/2;
            float greenamount = greenmid/2>1f ? 1f : greenmid/2;
            float blueamount = bluemid/2 > 1f ? 1f : bluemid/2;



            GPUImageVignetteFilter vignetteFilter = new GPUImageVignetteFilter(new PointF(0.5f,0.5f),new float[] {redamount, greenamount, blueamount}, 0.3f * amount , 0.88f);
            Log.d("vignette white",amount+"");
            filters.add(vignetteFilter);
        }





        final float[] minOut = new float[] {0.0f, 0.0f ,0.0f};
        final float[] maxOut = new float[] {1.0f,1.0f,1.0f};


        float contrastmin = 0.2f;//pitch/600
        float contrastmax = 0.9f;//maxfreq/1000

        GPUImageLevelsFilter levelsFilter = new GPUImageLevelsFilter();
        //levelsFilter.setMin(min[0], mid[0], max[0], minOut[0], maxOut[0]);
        levelsFilter.setRedMin(contrastmin, redmid, contrastmax);
        levelsFilter.setGreenMin(contrastmin, greenmid, contrastmax);
        levelsFilter.setBlueMin(contrastmin, bluemid, contrastmax);//dobar pristup, min treba generalno biti veći od 0 nego max manji od 1
        filters.add(levelsFilter);

        if(intensity < 0.5f) {
            filters.add(new GPUImageSaturationFilter(intensity * 2f));
        }

        if(anger > 0.5f && happiness < 0.5f && sadness < 0.5f){

            float amount = 1-(anger-0.5f);
            filters.add(new GPUImageRGBFilter(1.0f, amount, amount));
        }
        else if(anger < 0.5f && happiness > 0.5f && sadness < 0.5f){

            float amount = 1-(happiness-0.5f);
            filters.add(new GPUImageRGBFilter(1.0f,amount,1.0f));
        }
        else if(anger < 0.5f && happiness < 0.5f && sadness > 0.5f){

            float amount = 1-(sadness-0.5f);
            filters.add(new GPUImageRGBFilter(amount,amount,1.0f));
        }

        //GPUImageRGBFilter rgbFilter = new GPUImageRGBFilter(1-gender,221.f/255,gender);
        //filters.add(rgbFilter);

        return new GPUImageFilterGroup(filters);
    }


    Random rand = new Random();


    public Bitmap calculateOverlay(float gender, float anger, float sadness, float happiness){
        Bitmap bitmap = overlayGenerator.clearLastOverlay();;


        if(gender > 0.75f){
            overlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Male);
            bitmap = overlayGenerator.reCreateOverlayWithMoreEmojis(1);
        }else if(gender < 0.25f){
            overlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Female);
            bitmap = overlayGenerator.reCreateOverlayWithMoreEmojis(1);
        }


        if(anger > 0.5f){
            overlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Angry);
            bitmap = overlayGenerator.reCreateOverlayWithMoreEmojis((int) Math.ceil(anger / 0.25f));
        }
        if(happiness > 0.5f){
            overlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Happy);
            bitmap = overlayGenerator.reCreateOverlayWithMoreEmojis((int)Math.ceil(happiness/0.25f));
        }
        if(sadness > 0.5f){
            overlayGenerator.prepareEmojis(3, OverlayGenerator.EmojiType.Sad);
            bitmap = overlayGenerator.reCreateOverlayWithMoreEmojis((int)Math.ceil(sadness/0.25f));
        }

        return bitmap;
    }


    public GPUImageFilter calculateFilter(ProcessingResult result) {
        return calculateFilter((float)result.getGenderProbability(),
                                (float)result.getPitch(),
                                (float)result.getMaxFrequency(),
                                (float)result.getEmotionData().getAngerProbability(),
                                (float)result.getEmotionData().getSadnessProbability(),
                                (float)result.getEmotionData().getHappinessProbability(),
                                (float)result.getEmotionData().getSpeechIntensity());
    }

    public Bitmap calculateOverlay(ProcessingResult result) {
        return calculateOverlay((float)result.getGenderProbability(),
                                (float)result.getEmotionData().getAngerProbability(),
                                (float)result.getEmotionData().getSadnessProbability(),
                                (float)result.getEmotionData().getHappinessProbability());

    }
}
