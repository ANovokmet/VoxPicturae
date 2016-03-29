package jp.co.cyberagent.android.gpuimage;

import android.opengl.GLES20;

/**
 * Created by Ante on 27.3.2016..
 */
public class ProbaVoiceFilter  extends GPUImageFilter {
    public static final String RGB_FRAGMENT_SHADER = "" +
            "  varying highp vec2 textureCoordinate;\n" +
            "  \n" +
            "  uniform sampler2D inputImageTexture;\n" +
            "  uniform highp float redmin;\n" +
            "  uniform highp float redmax;\n" +
            "  uniform highp float greenmin;\n" +
            "  uniform highp float greenmax;\n" +
            "  uniform highp float bluemin;\n" +
            "  uniform highp float bluemax;\n" +
            "\n" +
            " highp float normalize(highp float value, highp float destmin, highp float destmax) {\n" +
            "     highp float destrange = destmax - destmin;\n" +
            "     return destmin + value * destrange;\n" +
            " }\n" +
            " \n" +
            "  void main()\n" +
            "  {\n" +
            "      highp vec4 textureColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "      \n" +
            "      gl_FragColor = vec4(normalize(textureColor.r, redmin, redmax), normalize(textureColor.g, greenmin, greenmax), normalize(textureColor.b, bluemin, bluemax), 1.0);\n" +
            "  }\n";

    private int mRedMinLocation;
    private float mRedMin;
    private int mGreenMinLocation;
    private float mGreenMin;
    private int mBlueMinLocation;
    private float mBlueMin;
    private int mRedMaxLocation;
    private float mRedMax;
    private int mGreenMaxLocation;
    private float mGreenMax;
    private int mBlueMaxLocation;
    private float mBlueMax;
    private boolean mIsInitialized = false;

    public ProbaVoiceFilter() {
        this(0.15234f, 0.96875f, 0.375f, 0.83203f, 0.30078f, 0.63281f);
    }

    public ProbaVoiceFilter(final float redMin, final float redMax, final float greenMin, final float greenMax, final float blueMin, final float blueMax) {
        super(NO_FILTER_VERTEX_SHADER, RGB_FRAGMENT_SHADER);
        mRedMin = redMin;
        mRedMax = redMax;
        mGreenMin = greenMin;
        mGreenMax = greenMax;
        mBlueMin = blueMin;
        mBlueMax = blueMax;
    }

    @Override
    public void onInit() {
        super.onInit();
        mRedMinLocation = GLES20.glGetUniformLocation(getProgram(), "redmin");
        mRedMaxLocation = GLES20.glGetUniformLocation(getProgram(), "redmax");
        mGreenMinLocation = GLES20.glGetUniformLocation(getProgram(), "greenmin");
        mGreenMaxLocation = GLES20.glGetUniformLocation(getProgram(), "greenmax");
        mBlueMinLocation = GLES20.glGetUniformLocation(getProgram(), "bluemin");
        mBlueMaxLocation = GLES20.glGetUniformLocation(getProgram(), "bluemax");
        mIsInitialized = true;
        setRed(mRedMin, mRedMax);
        setGreen(mGreenMin, mGreenMax);
        setBlue(mBlueMin, mGreenMax);
    }

    public void setRed(final float min, final float max) {
        mRedMin = min;
        mRedMax = max;
        if (mIsInitialized) {
            setFloat(mRedMinLocation, mRedMin);
            setFloat(mRedMaxLocation, mRedMax);
        }
    }

    public void setGreen(final float min, final float max) {
        mGreenMin = min;
        mGreenMax = max;
        if (mIsInitialized) {
            setFloat(mGreenMinLocation, mGreenMin);
            setFloat(mGreenMaxLocation, mGreenMax);
        }
    }

    public void setBlue(final float min, final float max) {
        mBlueMin = min;
        mBlueMax = max;
        if (mIsInitialized) {
            setFloat(mBlueMinLocation, mBlueMin);
            setFloat(mBlueMaxLocation, mBlueMax);
        }
    }
}

