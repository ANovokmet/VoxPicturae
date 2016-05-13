package hr.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import jp.co.cyberagent.android.gpuimage.GPUImage;

/**
 * Created by Ante on 13.5.2016..
 */
public class SaverTasker {

    GPUImage mGPUImage;
    Context mContext;
    public SaverTasker(Context context, GPUImage gpuImage){
        mGPUImage = gpuImage;
        mContext = context;
    }

    public void saveToPicturesWithOverlay(final Bitmap bitmap, final OverlayGenerator overlayGenerator, final int orientation, final boolean flipHorizontally, final String folderName, final String fileName,
                                          final GPUImage.OnPictureSavedListener listener) {
        new SaveWithOverlayTask(bitmap, overlayGenerator, orientation, flipHorizontally, folderName, fileName, listener).execute();
    }

    private class SaveWithOverlayTask extends AsyncTask<Void, Void, Void> {

        private final Bitmap mBitmap;
        private final String mFolderName;
        private final String mFileName;
        private final GPUImage.OnPictureSavedListener mListener;
        private final Handler mHandler;

        private final OverlayGenerator mOverlayGenerator;
        private final int mOrientation;
        private final boolean flipHorizontally;



        public SaveWithOverlayTask(final Bitmap bitmap, final OverlayGenerator overlayGenerator, final int orientation, final boolean flipHorizontally, final String folderName, final String fileName,
                                   final GPUImage.OnPictureSavedListener listener) {
            mOverlayGenerator = overlayGenerator;
            mBitmap = bitmap;
            mFolderName = folderName;
            mFileName = fileName;
            mListener = listener;
            mHandler = new Handler();

            mOrientation = orientation;
            this.flipHorizontally = flipHorizontally;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            Bitmap result = mGPUImage.getBitmapWithFilterAppliedAndRotation(mBitmap, mOrientation, flipHorizontally);

            //TODO FLIP WHEN flipHoriz == true;
            result = mOverlayGenerator.reCreateOverlayForSize(result,result.getWidth(),result.getHeight(), flipHorizontally);

            saveImage(mFolderName, mFileName, result);
            return null;
        }

        private void saveImage(final String folderName, final String fileName, final Bitmap image) {
            File path = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(path, folderName + "/" + fileName);
            try {
                file.getParentFile().mkdirs();
                image.compress(Bitmap.CompressFormat.JPEG, 80, new FileOutputStream(file));
                MediaScannerConnection.scanFile(mContext,
                        new String[]{
                                file.toString()
                        }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(final String path, final Uri uri) {
                                if (mListener != null) {
                                    mHandler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            mListener.onPictureSaved(uri);
                                        }
                                    });
                                }
                            }
                        });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }




}
