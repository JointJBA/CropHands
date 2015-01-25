package com.fuckwits.hackathon.crophands;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;
import com.google.android.glass.app.VoiceTriggers;
import com.google.android.glass.view.WindowUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import java.util.ArrayList;

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

    /**
     * {@link CardScrollView} to use as the main content view.
     */

    private final String TAG = "MainActivity";

    //private static final int TAKE_PICTURE_REQUEST = 1;
    private CameraView camera;
    //private String voiceDetector = null;
    //private ArrayList<String> voiceResults;
    /**
     * "Hello World!" {@link View}.
     */
    private View mView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        camera = new CameraView(this);
        /*ector.equals(VoiceTriggers.Command.TAKE_A_PICTURE)){
            Log.d(TAG, "Taking a picture.");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if(intent != null){
                Log.d(TAG, "Launching activity.");
                startActivityForResult(intent, TAKE_PICTURE_REQUEST);
            } else {
                Log.d(TAG, "Not launching activity.");
            }
        } else {
            Log.d(TAG, "Nothing's happening. Shit.");
        }
*/
        setContentView(camera);
    }

/*    @Override
    public int onStartCommand(Intent intent, int flags, int startid){
        voiceResults = intent.getExtras().getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
        System.out.println(voiceResults);
    }*/

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "OnResume");
        if (camera != null) {
            Log.d(TAG, "Releasing camera.");
            camera.releaseCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.releaseCamera();
        }
    }


}
