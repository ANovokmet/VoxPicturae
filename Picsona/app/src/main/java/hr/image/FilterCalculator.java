package hr.image;

import android.graphics.Point;
import android.graphics.PointF;

import java.util.ArrayList;

import hr.image.filters.PicsonaToneCurveFilter;
import hr.sound.ProcessingResult;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.GPUImageSharpenFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageVignetteFilter;

/**
 * Created by Ante on 3.4.2016..
 */
public class FilterCalculator {

    final float genderMidPoint = 0.5f;

    final float pitchLowPoint = 100f;
    final float pitchMidPoint = 110f;
    final float pitchHighPoint = 160f;

    final float frequencyLowPoint = 400f;
    final float frequencyHighPoint = 1000f;

    //thresholds are [-128,128]
    final float redThreshold = -96f;
    final float greenThreshold = 32f;
    final float blueThreshold = 128f;
    final float compositeThreshold = 32f;

    final float sharpenThreshold = 0.6f;

    public GPUImageFilter calculateFilter(ProcessingResult processingResult){
        ArrayList<GPUImageFilter> filters = new ArrayList<GPUImageFilter>();

        PicsonaToneCurveFilter ITCFfilter = new PicsonaToneCurveFilter();


        float maxFrequency = (float)processingResult.getMaxFrequency();
        if(maxFrequency > frequencyLowPoint){
            if(maxFrequency > frequencyHighPoint){
                maxFrequency = frequencyHighPoint;
            }
            float amount = ((maxFrequency - frequencyLowPoint) / (frequencyHighPoint - frequencyLowPoint)) * sharpenThreshold;
            filters.add(new GPUImageSharpenFilter(amount));
        }


        //gmd is going to be positive for male, negative for female
        float genderMidPointDifference = (float)processingResult.getGenderProbability() - genderMidPoint;
        Point[] redCurve = new Point[]{new Point(0, 0), new Point(128, 128 + (int)(redThreshold * genderMidPointDifference)), new Point(255, 255)};
        Point[] greenCurve = new Point[]{new Point(0, 0), new Point(128, 128 + (int)(greenThreshold * genderMidPointDifference)), new Point(255, 255)};

        Point firstBlue = new Point(0,0);
        if(genderMidPointDifference > 0){
            firstBlue.y = (int)(128f *  genderMidPointDifference);
        }
        Point[] blueCurve = new Point[]{firstBlue, new Point(128, 128 + (int)(blueThreshold * genderMidPointDifference)), new Point(255, 255)};

        float pitch = (float)processingResult.getPitch();
        if(pitch < pitchLowPoint){
            pitch = pitchLowPoint;
        }else if(pitch > pitchHighPoint){
            pitch = pitchHighPoint;
        }
        float pitchMidPointDifference = (pitch - pitchMidPoint) / (pitchHighPoint - pitchLowPoint);

        Point[] compositeCurve = new Point[]{new Point(0, 0), new Point(64, 96 + (int)(compositeThreshold * pitchMidPointDifference)), new Point(255, 255)};

        ITCFfilter.setCompositeCurveFromArray(compositeCurve);
        ITCFfilter.setRedCurveFromArray(redCurve);
        //necemo zelenu za sad
        //ITCFfilter.setGreenCurveFromArray(greenCurve);
        ITCFfilter.setBlueCurveFromArray(blueCurve);

        filters.add(ITCFfilter);


        /*PointF centerPoint = new PointF();
        centerPoint.x = 0.5f;
        centerPoint.y = 0.5f;
        GPUImageVignetteFilter vignetteFilter = new GPUImageVignetteFilter(centerPoint, new float[] {0.0f, 0.0f, 0.0f}, 0.3f, 0.75f);
        filters.add(vignetteFilter);*/

        return new GPUImageFilterGroup(filters);
    }

}
