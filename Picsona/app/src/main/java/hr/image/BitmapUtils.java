package hr.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import java.util.List;

/**
 * Created by Ante on 6.5.2016..
 */
public class BitmapUtils {
    public static Bitmap OverlayBitmapLessMemory(Bitmap bmp1, Bitmap bmp2) {
        bmp1 = bmp1.copy(Bitmap.Config.ARGB_8888,true);
        Canvas canvas = new Canvas(bmp1);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmp1;
    }

    public static Bitmap OverlayBitmap(Bitmap bmp1, Bitmap bmp2) {
        Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(), bmp1.getConfig());
        Canvas canvas = new Canvas(bmOverlay);
        canvas.drawBitmap(bmp1, new Matrix(), null);
        canvas.drawBitmap(bmp2, 0, 0, null);
        return bmOverlay;
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap ScaleBitmap(Bitmap source, int width, int height){
        return Bitmap.createScaledBitmap(source, width, height, false);
    }

    public static Bitmap flipBitmap(Bitmap src) {
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }
}
