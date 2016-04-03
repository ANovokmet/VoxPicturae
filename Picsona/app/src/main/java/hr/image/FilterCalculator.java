package hr.image;

import android.graphics.Point;

import java.util.ArrayList;

import hr.image.filters.PicsonaToneCurveFilter;
import hr.sound.ProcessingResult;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilterGroup;

/**
 * Created by Ante on 3.4.2016..
 */
public class FilterCalculator {


    public GPUImageFilter calculateFilter(ProcessingResult processingResult){
        ArrayList<GPUImageFilter> filters = new ArrayList<GPUImageFilter>();

        PicsonaToneCurveFilter ITCFfilter = new PicsonaToneCurveFilter();

        ITCFfilter.setCompositeCurveFromArray(new Point[]{new Point(0, 0), new Point(89, 131), new Point(255, 255)});
        ITCFfilter.setRedCurveFromArray(new Point[]{new Point(0, 0), new Point(128, (int) (128 * processingResult.getGenderProbability())), new Point(255, 255)});
        ITCFfilter.setGreenCurveFromArray(new Point[]{new Point(0, 0), new Point(255, 255)});
        ITCFfilter.setBlueCurveFromArray(new Point[]{new Point(0, 0), new Point(255, 255)});

        filters.add(ITCFfilter);

        return new GPUImageFilterGroup(filters);
    }

}
