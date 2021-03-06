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
    final int ARRAYLIST_SIZE = 15;
    ArrayList<Integer> drawableIDsAnger = new ArrayList<Integer>(ARRAYLIST_SIZE);
    ArrayList<Integer> drawableIDsSadness = new ArrayList<Integer>(ARRAYLIST_SIZE);
    ArrayList<Integer> drawableIDsHapiness = new ArrayList<Integer>(ARRAYLIST_SIZE);
    ArrayList<Integer> drawableIDsFemale = new ArrayList<Integer>(ARRAYLIST_SIZE);
    ArrayList<Integer> drawableIDsMale = new ArrayList<Integer>(ARRAYLIST_SIZE);

    public enum EmojiType {
        Angry,
        Happy,
        Sad,
        Female,
        Male
    }

    void getDrawableIds() {
        Field[] ID_Fields = R.drawable.class.getFields();
        for (int i = 0; i < ID_Fields.length; i++) {
            try {
                int resArrayId = ID_Fields[i].getInt(null);
                String name = context.getResources().getResourceEntryName(resArrayId);
                if (name.length() <= 4)
                    switch (name.charAt(0)) {
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

    public OverlayGenerator(Context context) {
        this.context = context;
        getDrawableIds();
    }

    public void setInitializationParams(int originalWidth, int originalHeight, int countPerWidth, int countPerHeight) {
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        countW = countPerWidth;
        countH = countPerHeight;

        segmentW = originalWidth / countPerWidth;
        segmentH = originalHeight / countPerHeight;
    }

    /**
     * Loads bitmaps of emojis according to resource IDs
     *
     * @param numOfEmojis number of distinct emojis loaded
     * @param type        emotion
     */
    public void prepareEmojis(int numOfEmojis, EmojiType type) {
        originalEmojis = loadEmojis(numOfEmojis, type);
    }

    private ArrayList<Bitmap> loadEmojis(int count, EmojiType type) {

        List<Integer> drawableIDs;
        switch (type) {
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
        for (int i = 0; i < drawableIDs.size(); i++) {
            ints.add(i);
        }
        Collections.shuffle(ints);

        for (int i = 0; i < count; i++) {

            int rand = ints.get(i);
            int resourceId = drawableIDs.get(rand);
            Bitmap emojiImage = BitmapFactory.decodeResource(context.getResources(), resourceId);
            bitmaps.add(emojiImage);
        }
        return bitmaps;
    }

    public Bitmap createOverlay(int numberOfEmojis) {
        emojiLocations.clear();
        if (originalEmojis == null) {
            originalEmojis = loadEmojis(2, EmojiType.Sad);
        }

        lastOverlay = placeBitmapsSymetrically(null, originalEmojis, numberOfEmojis);
        return lastOverlay;
    }


    Bitmap lastOverlay;

    public Bitmap reCreateOverlayWithMoreEmojis(int numberOfEmojis) {
        if (originalEmojis == null) {
            originalEmojis = loadEmojis(2, EmojiType.Sad);
        }

        lastOverlay = placeBitmapsSymetrically(lastOverlay, originalEmojis, numberOfEmojis);
        return lastOverlay;
    }


    private Bitmap placeBitmapsRandomlyInImage(Bitmap image, List<Bitmap> bitmaps, int count) {
        if (image == null) {
            image = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
            Log.d("created bitmap", originalWidth + "x" + originalHeight);
        }

        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(image, new Matrix(), null);

        for (int i = 0; i < count; i++) {
            int randx = random.nextInt(countW);
            int randy = random.nextInt(countH);

            if (random.nextInt(2) == 1) {//postaviti na granice - dalje od sredine slike
                randx = random.nextInt(2) == 1 ? countW - 1 : 0;
            } else {
                randy = random.nextInt(2) == 1 ? countH - 1 : 0;
            }

            int randn = random.nextInt(bitmaps.size());
            Bitmap emoji = bitmaps.get(randn);
            emojiLocations.put(new Point(randx, randy), emoji);
            int dimm = getSmallerSegmentDimension(segmentH, segmentW);
            emoji = BitmapUtils.ScaleBitmap(emoji, dimm, dimm);
            canvas.drawBitmap(emoji, randx * segmentW, randy * segmentH, null);
        }
        return image;
    }

    private Bitmap placeBitmapsDiagonally(Bitmap image, List<Bitmap> bitmaps, int count) {
        if (image == null) {
            image = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(image, new Matrix(), null);

        for (int i = 0; i < count; i++) {
            int randx = i;
            int randy = i;
            int randn = random.nextInt(bitmaps.size());
            Bitmap emoji = bitmaps.get(randn);
            int dimm = getSmallerSegmentDimension(segmentH, segmentW);
            emoji = BitmapUtils.ScaleBitmap(emoji, dimm, dimm);

            emojiLocations.put(new Point(randx, randy), emoji);
            canvas.drawBitmap(emoji, randx * segmentW, randy * segmentH, null);

        }
        return image;
    }

    private Bitmap placeBitmapsSymetrically(Bitmap image, List<Bitmap> bitmaps, int count) {
        if (image == null) {
            image = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(image, new Matrix(), null);

        for (int i = 0; i < count; i++) {

            Point first = new Point();
            Point second = new Point();
            int j = 10;//sprijecava infinite loop
            do {
                first.x = random.nextInt(countW);
                first.y = random.nextInt(countH);
                if (random.nextInt(2) == 1) {//postaviti na granice - dalje od sredine slike
                    first.x = (random.nextInt(2) == 1 ? countW - 2 : 0) + random.nextInt(2);
                } else {
                    first.y = (random.nextInt(2) == 1 ? countH - 2 : 0) + random.nextInt(2);
                }
                second.x = countW - first.x - 1;
                second.y = first.y;
                j--;
            }
            while ((emojiLocations.containsKey(first) || emojiLocations.containsKey(second)) && j > 0);

            int randn = random.nextInt(bitmaps.size());
            Bitmap emoji = bitmaps.get(randn);
            int dimm = getSmallerSegmentDimension(segmentH, segmentW);
            emoji = BitmapUtils.ScaleBitmap(emoji, dimm, dimm);

            emojiLocations.put(first, emoji);
            emojiLocations.put(second, emoji);


            canvas.drawBitmap(emoji, first.x * segmentW, first.y * segmentH, null);
            canvas.drawBitmap(emoji, second.x * segmentW, second.y * segmentH, null);
        }
        return image;
    }

    HashMap<Point, Bitmap> emojiLocations = new HashMap<Point, Bitmap>();

    public Bitmap reCreateOverlayForSize(int width, int height) {
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(image, new Matrix(), null);

        int segmentW = width / countW;
        int segmentH = height / countH;

        for (Point k : emojiLocations.keySet()) {

            Bitmap emoji = emojiLocations.get(k);

            int dimm = getSmallerSegmentDimension(segmentH, segmentW);

            emoji = BitmapUtils.ScaleBitmap(emoji, dimm, dimm);
            canvas.drawBitmap(emoji, k.x * segmentW, k.y * segmentH, null);
        }
        return image;
    }

    public Bitmap reCreateOverlayForSize(Bitmap image, int width, int height, boolean flipHorizontal) {

        image = image.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(image, new Matrix(), null);

        int segmentW = width / countW;
        int segmentH = height / countH;

        for (Point k : emojiLocations.keySet()) {

            Bitmap emoji = emojiLocations.get(k);

            int dimm = getSmallerSegmentDimension(segmentH, segmentW);

            if (flipHorizontal) {
                emoji = BitmapUtils.flipBitmap(emoji);
            }
            emoji = BitmapUtils.ScaleBitmap(emoji, dimm, dimm);


            canvas.drawBitmap(emoji, k.x * segmentW, k.y * segmentH, null);
        }
        return image;
    }


    public Bitmap getLastOverlay() {
        if (lastOverlay == null) {
            lastOverlay = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        }
        return lastOverlay;
    }

    public Bitmap clearLastOverlay() {
        emojiLocations.clear();
        lastOverlay = Bitmap.createBitmap(originalWidth, originalHeight, Bitmap.Config.ARGB_8888);
        return lastOverlay;
    }

    private int getSmallerSegmentDimension(int segmentH, int segmentW) {
        if (segmentH > segmentW) {
            return segmentW;
        } else {
            return segmentH;
        }
    }

    private int getBiggerSegmentDimension(int segmentH, int segmentW) {
        if (segmentH < segmentW) {
            return segmentW;
        } else {
            return segmentH;
        }
    }
}
