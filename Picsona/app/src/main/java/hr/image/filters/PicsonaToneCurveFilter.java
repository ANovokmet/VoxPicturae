package hr.image.filters;

import android.graphics.Point;
import android.graphics.PointF;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import jp.co.cyberagent.android.gpuimage.GPUImageToneCurveFilter;

/**
 * Created by Ante on 2.4.2016..
 * Filter that creates actual curves. Data must be initialized and calculated before running.
 * Input is an array of points with values [0,255]. The array can be unsorted (will be sorted
 * by the x value before creating the curve). Superclass is missing classes to load RGB
 * values from an array.
 */
public class PicsonaToneCurveFilter extends GPUImageToneCurveFilter {

    /**
     * Input red points to make a curve from. Every point has 2 values, x being the input and
     * y the output value. Both values in the range of [0,255].
     *
     * @param points array of points
     */
    public void setRedCurveFromArray(Point[] points) {
        PointF[] outPoints = convertRGBPointArrayToPointFArray(points);
        setRedControlPoints(outPoints);
    }

    /**
     * Input green points to make a curve from. Every point has 2 values, x being the input and
     * y the output value. Both values in the range of [0,255].
     *
     * @param points array of points
     */
    public void setGreenCurveFromArray(Point[] points) {
        PointF[] outPoints = convertRGBPointArrayToPointFArray(points);
        setGreenControlPoints(outPoints);
    }

    /**
     * Input blue points to make a curve from. Every point has 2 values, x being the input and
     * y the output value. Both values in the range of [0,255].
     *
     * @param points array of points
     */
    public void setBlueCurveFromArray(Point[] points) {
        PointF[] outPoints = convertRGBPointArrayToPointFArray(points);
        setBlueControlPoints(outPoints);
    }

    /**
     * Input composite points to make a curve from. Every point has 2 values, x being the input and
     * y the output value. Both values in the range of [0,255]. Assigning this value will have an
     * effect on overall brightness of image.
     *
     * @param points array of points
     */
    public void setCompositeCurveFromArray(Point[] points) {
        PointF[] outPoints = convertRGBPointArrayToPointFArray(points);
        setRgbCompositeControlPoints(outPoints);
    }

    /**
     * Overrided method from superclass. When there are spline points
     * missing at the end or beginning (no point with x=0 or x=255)
     * it adds spline points with the same output value as the last
     * one or first one, not 0 or 255.
     *
     * @param points
     * @return
     */
    @Override
    protected ArrayList<Float> createSplineCurve(PointF[] points) {
        if (points == null || points.length <= 0) {
            return null;
        }

        // Sort the array
        PointF[] pointsSorted = points.clone();
        Arrays.sort(pointsSorted, new Comparator<PointF>() {
            @Override
            public int compare(PointF point1, PointF point2) {
                if (point1.x < point2.x) {
                    return -1;
                } else if (point1.x > point2.x) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        // Convert from (0, 1) to (0, 255).
        Point[] convertedPoints = new Point[pointsSorted.length];
        for (int i = 0; i < points.length; i++) {
            PointF point = pointsSorted[i];
            convertedPoints[i] = new Point((int) (point.x * 255), (int) (point.y * 255));
        }

        ArrayList<Point> splinePoints = createSplineCurve2(convertedPoints);

        // If we have a first point like (0.3, 0) we'll be missing some points at the beginning
        // that should be 0.
        Point firstSplinePoint = splinePoints.get(0);
        if (firstSplinePoint.x > 0) {
            for (int i = firstSplinePoint.x; i >= 0; i--) {
                splinePoints.add(new Point(i, firstSplinePoint.y));
            }
        }

        // Insert points similarly at the end, if necessary.
        Point lastSplinePoint = splinePoints.get(splinePoints.size() - 1);
        if (lastSplinePoint.x < 255) {
            for (int i = lastSplinePoint.x + 1; i <= 255; i++) {
                splinePoints.add(new Point(i, lastSplinePoint.y));
            }
        }

        // Prepare the spline points.
        ArrayList<Float> preparedSplinePoints = new ArrayList<Float>(splinePoints.size());
        for (Point newPoint : splinePoints) {
            Point origPoint = new Point(newPoint.x, newPoint.x);

            float distance = (float) Math.sqrt(Math.pow((origPoint.x - newPoint.x), 2.0) + Math.pow((origPoint.y - newPoint.y), 2.0));

            if (origPoint.y > newPoint.y) {
                distance = -distance;
            }

            preparedSplinePoints.add(distance);
        }

        return preparedSplinePoints;
    }

    /**
     * Converts array of points with int values [0,255] to an array of points with floats [0,1]
     *
     * @param points
     * @return result array
     */
    private PointF[] convertRGBPointArrayToPointFArray(Point[] points) {
        float pointRate = 1.0f / 255;

        int pointCount = points.length;

        PointF[] outPoints = new PointF[pointCount];

        for (int i = 0; i < pointCount; i++) {
            float x = points[i].x * pointRate;
            float y = points[i].y * pointRate;
            outPoints[i] = new PointF(x, y);
        }
        return outPoints;
    }
}

