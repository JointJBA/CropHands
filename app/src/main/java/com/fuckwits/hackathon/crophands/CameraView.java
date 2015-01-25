package com.fuckwits.hackathon.crophands;

/**
 * Created by Belal on 1/25/2015.
 */
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView implements SurfaceHolder.Callback
{
    private SurfaceHolder surfaceHolder = null;
    public Camera camera = null;

    @SuppressWarnings("deprecation")
    public CameraView(Context context)
    {
        super(context);

        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /*
     * (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceCreated(android.view.SurfaceHolder)
     */

    public static Camera cam(){
        final String TAG = "cam";
        int numCameras = Camera.getNumberOfCameras();
        if(numCameras == 0){
            Log.w(TAG, "No Cameras");
            return null;
        }

        int index = 0;
        while(index < numCameras){
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index,cameraInfo);
            if(cameraInfo.facing==Camera.CameraInfo.CAMERA_FACING_BACK){
                break;
            }
            index++;
        }
        Camera camera = null;
        long timeout = System.currentTimeMillis() + 1000;
        int attempt = 0;
        while(camera == null && System.currentTimeMillis() < timeout) {
            attempt++;
            Log.v(TAG, "Sleeping 100ms - attempt " + attempt);
            try {
                Thread.sleep(100);
            }
            catch(Exception e) {

            }

            try {
                if (index < numCameras) {
                    Log.i(TAG, "Opening camera #" + index);
                    camera = Camera.open(index);
                } else {
                    Log.i(TAG, "No camera facing back; returning camera #0");
                    camera = Camera.open(0);
                }
            } catch (RuntimeException e) {
                Log.w(TAG, "RuntimeException: " + e.getMessage());
            }
        }

        return camera;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        if (camera != null) {
            releaseCamera();
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d("A","Camera has been opened");
        camera = Camera.open();

        // Set the Hotfix for Google Glass
        this.setCameraParameters(camera);

        // Show the Camera display
        try
        {
            camera.setPreviewDisplay(holder);
        }
        catch (Exception e)
        {
            this.releaseCamera();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceChanged(android.view.SurfaceHolder, int, int, int)
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // Start the preview for surfaceChanged
        if (camera != null)
        {
            camera.startPreview();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.view.SurfaceHolder.Callback#surfaceDestroyed(android.view.SurfaceHolder)
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        // Do not hold the camera during surfaceDestroyed - view should be gone
        this.releaseCamera();
    }

    /**
     * Important HotFix for Google Glass (post-XE11) update
     * @param camera Object
     */
    public void setCameraParameters(Camera camera)
    {
        if (camera != null)
        {
            Parameters parameters = camera.getParameters();
            parameters.setPreviewFpsRange(30000, 30000);
            camera.setParameters(parameters);
        }
    }

    /**
     * Release the camera from use
     */
    public void releaseCamera()
    {
        if (camera != null)
        {
            camera.release();
            camera = null;
        }
    }

    public String takePicture() {
        return "";
    }
}