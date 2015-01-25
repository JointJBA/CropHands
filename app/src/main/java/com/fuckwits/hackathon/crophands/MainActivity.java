package com.fuckwits.hackathon.crophands;

import com.google.android.glass.content.Intents;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.FileObserver;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity {


    private View mView;
    private CameraView cView;
    static final int TAKE_PICTURE_REQUEST = 1;
    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("BS", "OpenCV loaded successfully");
                    // Create and set View

                } break;
                case LoaderCallbackInterface.INIT_FAILED:
                {
                    Log.d("WTF", "BS");
                }
                default:
                {
                    Log.d("WTF", Integer.toString(status));
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mOpenCVCallBack);
        if (!OpenCVLoader.initDebug()){
            Log.d("WTF", "DEAR LORD WHY");
        }
        cView = new CameraView(this) {
            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {
                // Start the preview for surfaceChanged
                super.surfaceChanged(holder, format, width, height);
                new CountDownTimer(3000, 2000) {

                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        takepicture();
                    }
                }.start();
            }
        };
        mView = cView;
        setContentView(mView);
        //takepicture();
        /*Camera.Parameters parameters = cView.camera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        cView.camera.setParameters(parameters);
        SurfaceView mview = new SurfaceView(getBaseContext());
        try {
            cView.camera.setPreviewDisplay(mview.getHolder()) ;
            cView.camera.startPreview();
            cView.camera.takePicture(null,null,photoCallback);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
    }



    Camera.PictureCallback photoCallback=new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {

            Uri uriTarget = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
            OutputStream imageFileOS;

            try {

                imageFileOS = getContentResolver().openOutputStream(uriTarget);
                Bitmap originalBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap bmp = Bitmap.createBitmap(originalBmp, 70, 150, originalBmp.getWidth() - 150, originalBmp.getHeight() - 70);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imageFileOS.write(stream.toByteArray());
                imageFileOS.flush();
                imageFileOS.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();

            }
            finish();

        }
    };


    void takepicture() {
        cView.releaseCamera();
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String path = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
            Log.d("MainActivity", path);
            processImage(path);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void processImage(final String picturePath) {
        final File pictureFile = new File(picturePath);

        if (pictureFile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(picturePath);
            Mat mat = new Mat();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] img = stream.toByteArray();
            mat.put(0, 0, img);
            ColorBlobDetector cbd = new ColorBlobDetector();
            cbd.setHsvColor(new Scalar(10, 90, 60, 60));
            cbd.setColorRadius(new Scalar(10, 60, 60));
            cbd.process(mat);
            Log.d("BS", Integer.toString(cbd.getContours().size()));
            finish();
            Log.d("Su", "HA");
        } else {
            // The file does not exist yet. Before starting the file observer, you
            // can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail
            // image and a progress indicator).

            final File parentDirectory = pictureFile.getParentFile();
            FileObserver observer = new FileObserver(parentDirectory.getPath(),
                    FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO) {
                // Protect against additional pending events after CLOSE_WRITE
                // or MOVED_TO is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        File affectedFile = new File(parentDirectory, path);
                        isFileWritten = affectedFile.equals(pictureFile);

                        if (isFileWritten) {
                            stopWatching();

                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processImage(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (cView != null) {
            cView.releaseCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cView != null) {
            cView.releaseCamera();
        }
    }


}
