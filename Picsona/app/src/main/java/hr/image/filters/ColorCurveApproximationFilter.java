package hr.image.filters;

import android.opengl.GLES20;
import android.util.Log;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;

/**
 * Created by Ante on 2.4.2016..
 *
 * Filter thats applying a color curve to each of the channels.
 * The input is 4 float[17] arrays that are used to define the
 * curves as linear intervals between points. Could be useful for
 * dynamically changing the curve point values, otherwise use the other
 * classes.
 */
public class ColorCurveApproximationFilter extends GPUImageFilter {
    public static final String COLOR_CURVE_FRAGMENT_SHADER = "" +
            "  varying highp vec2 textureCoordinate;\n" +
            "  \n" +
            "  uniform sampler2D inputImageTexture;\n" +
            "  uniform highp float rgbCurve[17];\n" +
            "  uniform highp float rCurve[17];\n" +
            "  uniform highp float gCurve[17];\n" +
            "  uniform highp float bCurve[17];\n" +
            "\n" +
            "  highp float approximate(highp float value, highp float curve[17]) {\n" +
            "      highp float nigga = value * 16.0;\n" +
            "      int segment = int(nigga);\n" +
            "\n" +
            "      highp float seg0 = rgbCurve[segment];\n" +
            "      highp float seg1 = rgbCurve[segment+1];\n" +
            "\n" +
            "      return seg0 + (seg1-seg0) * (value - float(segment) / 16.0) / ((float(segment) + 1.) / 16. - float(segment) / 16.0);\n" +
            "  }\n" +
            "  void main()\n" +
            "  {\n" +
            "      highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "\n" +
            "\n" +
            "\n" +
            "      gl_FragColor = vec4(approximate(textureColor.r, rCurve), approximate(textureColor.g, gCurve), approximate(textureColor.b, bCurve), 1.0);\n" +
            "  }\n";

    private boolean mIsInitialized = false;
    private float[] mRgbCurve;
    private int mRgbCurveLocation;
    private float[] mRCurve;
    private int mRCurveLocation;
    private float[] mGCurve;
    private int mGCurveLocation;
    private float[] mBCurve;
    private int mBCurveLocation;

    public ColorCurveApproximationFilter() {
        this(new int[]{0,5,10,15,20,25,30,50,90,110,155,220,225,235,245,250,255},
                new int[]{41, 48, 57, 65, 72, 79, 86, 91, 96, 104, 109, 115, 122, 132, 143, 154, 165},
                new int[]{4, 10, 20, 34, 52, 73, 94, 117, 139, 157, 177, 192, 204, 214, 223, 229, 235 },
                new int[]{23, 41, 63, 87, 115, 139, 163, 185, 204, 219, 230, 238, 244, 247, 250, 252, 255});
    }

    public ColorCurveApproximationFilter(final int[] rgbCurve,final int[] rCurve,final int[] gCurve,final int[] bCurve) {
        super(NO_FILTER_VERTEX_SHADER, COLOR_CURVE_FRAGMENT_SHADER);

        mRgbCurve = intToFloatRgbCurve(rgbCurve);
        mRCurve = intToFloatRgbCurve(rCurve);
        mGCurve = intToFloatRgbCurve(gCurve);
        mBCurve = intToFloatRgbCurve(bCurve);

    }

    @Override
    public void onInit() {
        super.onInit();
        mRgbCurveLocation = GLES20.glGetUniformLocation(getProgram(), "rgbCurve");
        mRCurveLocation = GLES20.glGetUniformLocation(getProgram(), "rCurve");
        mGCurveLocation = GLES20.glGetUniformLocation(getProgram(), "gCurve");
        mBCurveLocation = GLES20.glGetUniformLocation(getProgram(), "bCurve");
        mIsInitialized = true;
        setRgbCurve(mRgbCurve);
        setRCurve(mRgbCurve);
        setGCurve(mRgbCurve);
        setBCurve(mRgbCurve);
    }

    public void setRgbCurve(float[] curve){
        if(curve.length != 17){
            Log.e("Color curve filter", "Set opengl array must have 17 elements");
        }
        mRgbCurve = curve;
        if (mIsInitialized) {
            setFloatArray(mRgbCurveLocation, mRgbCurve);
        }
    }

    public void setRCurve(float[] curve){
        if(curve.length != 17){
            Log.e("Color curve filter","Set opengl array must have 17 elements");
        }
        mRCurve = curve;
        if (mIsInitialized) {
            setFloatArray(mRCurveLocation, mRCurve);
        }
    }

    public void setGCurve(float[] curve){
        if(curve.length != 17){
            Log.e("Color curve filter","Set opengl array must have 17 elements");
        }
        mGCurve = curve;
        if (mIsInitialized) {
            setFloatArray(mGCurveLocation, mGCurve);
        }
    }

    public void setBCurve(float[] curve){
        if(curve.length != 17){
            Log.e("Color curve filter","Set opengl array must have 17 elements");
        }
        mBCurve = curve;
        if (mIsInitialized) {
            setFloatArray(mBCurveLocation, mBCurve);
        }
    }

    /**
     * Approximates an array representing the points of a function
     * @param inArray array of 17 integers representing values of function on 16 linear segments
     * @param outArray array which will hold the result, length must be at least 256
     * @deprecated OpenGL can't handle 256 uniform float values.
     */
    public void approximateRgbCurve(final int[] inArray, float[] outArray){
        if(outArray.length < 256 || inArray.length < 17){
            Log.e("Color curve filter","Out array must have at least 256 elements, in array 17");
        }

        for(int i = 0; i < 16; i++){
            float floorValue = (float)inArray[i] / 255;
            float ceilingValue = (float)inArray[i+1] / 255;
            float difference = ceilingValue - floorValue;
            for(int j=0; j<16; j++){
                outArray[i*16+j] = floorValue + difference * (float)j / 16;
            }
        }
    }

    public float[] intToFloatRgbCurve(final int[] inArray) {
        float[] outArray = new float[inArray.length];
        for(int i=0; i < inArray.length; i++){
            outArray[i] = (float)inArray[i]/255.0f;
        }
        return outArray;
    }
}

