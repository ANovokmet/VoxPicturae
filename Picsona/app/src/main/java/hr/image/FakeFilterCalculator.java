package hr.image;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;

import hr.image.filters.PicsonaToneCurveFilter;
import hr.sound.ProcessingResult;
import jp.co.cyberagent.android.gpuimage.GPUImageContrastFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;
import jp.co.cyberagent.android.gpuimage.GPUImageLevelsFilter;

/**
 * Created by Ante on 6.5.2016..
 */
public class FakeFilterCalculator {

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

        float[] min = new float[] {0.0f, 0.0f ,0.0f};
        final float[] mid = new float[] {1.5f,1.5f,1.5f};
        final float[] max = new float[] {1.0f,1.0f,1.0f};
        final float[] minOut = new float[] {0.0f, 0.0f ,0.0f};
        final float[] maxOut = new float[] {1.0f,1.0f,1.0f};


        GPUImageLevelsFilter levelsFilter = new GPUImageLevelsFilter();
        levelsFilter.setMin(min[0],mid[0],max[0],minOut[0],maxOut[0]);
        filters.add(levelsFilter);
        return new GPUImageFilterGroup(filters);
    }



}