package hr.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.util.Log;

import java.lang.reflect.Field;
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
    ArrayList<Integer> drawableIDsAnger = new ArrayList<Integer>(15);
    ArrayList<Integer> drawableIDsSadness = new ArrayList<Integer>(15);
    ArrayList<Integer> drawableIDsHapiness = new ArrayList<Integer>(15);
    ArrayList<Integer> drawableIDsFemale = new ArrayList<Integer>(15);
    ArrayList<Integer> drawableIDsMale = new ArrayList<Integer>(15);

    void getDrawableIds(){
        Field[] ID_Fields = R.drawable.class.getFields();
        for(int i = 0; i < ID_Fields.length; i++) {
            try {
                int resArrayId = ID_Fields[i].getInt(null);
                String name = context.getResources().getResourceEntryName(resArrayId);
                if(name.length()<=4)
                switch (name.charAt(0)){
                    case 'a':
                        drawableIDsAnger.add(resArrayId);
                        break;
                    case 's':
                        drawableIDsSadness.add(resArrayId);
                        break;
                    case 'h':
                        drawableIDsHapiness.add(resArrayId);
                        break;
                    case 'f':
                        drawableIDsFemale.add(resArrayId);
                        break;
                    case 'm':
                        drawableIDsMale.add(resArrayId);
                        break;
                }


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public enum EmojiType{
        Angry,
        Happy,
        Sad,
        Female,
        Male
    }


    public OverlayGenerator(Context context, int originalWidth, int originalHeight, int countPerWidth, int countPerHeight){
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.context = context;
        countW = countPerWidth;
        countH = countPerHeight;

        segmentW = originalWidth / countPerWidth;
        segmentH = originalHeight / countPerHeight;

        getDrawableIds();

        originalEmojis = loadEmojis(2, EmojiType.Angry);

    }

    private ArrayList<Bitmap> loadEmojis(int count, EmojiType type){

        List<Integer> drawableIDs;
        switch (type){
            case Angry:
                drawableIDs = drawableIDsAnger;
                break;
            case Sad:
                drawableIDs = drawableIDsSadness;
                break;
            case Happy:
                drawableIDs = drawableIDsHapiness;
                break;
            case Female:
                drawableIDs = drawableIDsFemale;
                break;
            case Male:
                drawableIDs = drawableIDsMale;
                break;
            default:
                drawableIDs = new ArrayList<Integer>();
        }


        ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();

        List<Integer> ints = new ArrayList<>(drawableIDs.size());
        for(int i = 0; i < drawableIDs.size(); i++){
            ints.add(i);
        }
        Collections.shuffle(ints);

        for(int i = 0; i < count; i++){

            int rand = ints.get(i);
            int resourceId = drawableIDs.get(rand);
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
