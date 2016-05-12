package hr.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import hr.picsona.R;

/**
 * Created by Ante on 6.5.2016..
 */
public class OverlayGenerator {
    int originalWidth, originalHeight;
    int countW, countH;
    int segmentW, segmentH;
    Context context;

    ArrayList<Bitmap> originalEmojis;

    final Random random = new Random();
    int[] drawableIDs = {
            R.drawable.a15,
            R.drawable.a198,
            R.drawable.a47,
            R.drawable.a703
    };

    public OverlayGenerator(Context context, int originalWidth, int originalHeight, int countPerWidth, int countPerHeight){
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.context = context;
        countW = countPerWidth;
        countH = countPerHeight;

        segmentW = originalWidth / countPerWidth;
        segmentH = originalHeight / countPerHeight;

        originalEmojis = loadEmojis(2);

    }

    private ArrayList<Bitmap> loadEmojis(int count){
        ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

        List<Integer> ints = new ArrayList<>(drawableIDs.length);
        for(int i = 0; i < drawableIDs.length; i++){
            ints.add(i);
        }
        Collections.shuffle(ints);

        for(int i = 0; i < count; i++){

            int rand = ints.get(i);
            int resourceId = drawableIDs[rand];
            Bitmap emojiImage = BitmapFactory.decodeResource(context.getResources(), resourceId);
            bitmaps.add(emojiImage);
        }
        return bitmaps;
    }

    public Bitmap createOverlay(){
        return placeBitmapsRandomlyInImage(null, originalEmojis, 4);
    }

    private Bitmap placeBitmapsRandomlyInImage(Bitmap image, List<Bitmap> bitmaps, int count){
        if(image == null){
            image = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(image, new Matrix(), null);

        for(int i = 0; i< count; i++){
            int randx = random.nextInt(countW);
            int randy = random.nextInt(countH);
            int randn = random.nextInt(bitmaps.size());

            Bitmap emoji = bitmaps.get(randn);

            emojiLocations.put(new Point(randx, randy),emoji);

            emoji = BitmapUtils.ScaleBitmap(emoji, getSmallerSegmentDimension(), getSmallerSegmentDimension());

            canvas.drawBitmap(emoji,randx * segmentW, randy * segmentH, null);
        }
        return image;
    }

    HashMap<Point, Bitmap> emojiLocations = new HashMap<Point, Bitmap>();

    public Bitmap reCreateOverlayForSize(int width, int height){
        Bitmap image = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(image, new Matrix(), null);

        int segmentW = width / countW;
        int segmentH = height / countH;

        for(Point k : emojiLocations.keySet()){

            Bitmap emoji = BitmapUtils.ScaleBitmap(emojiLocations.get(k), segmentH, segmentH);
            canvas.drawBitmap(emoji, k.x * segmentW, k.y * segmentH, null);
        }
        return image;
    }

    private int getSmallerSegmentDimension(){
        if(segmentH > segmentW){
            return segmentW;
        }
        else{
            return segmentH;
        }
    }

    private int getBiggerSegmentDimension(){
        if(segmentH < segmentW){
            return segmentW;
        }
        else{
            return segmentH;
        }
    }
}
