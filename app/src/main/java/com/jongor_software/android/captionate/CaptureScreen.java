package com.jongor_software.android.captionate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;


public class CaptureScreen extends Activity {

    private CameraManager mCameraManager;
    private TextureView mCameraView;
    private String[] mCameraIds;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mCaptureBuilder;
    private CameraCaptureSession mCaptureSession;
    private Size mPreviewSize;

    private Button mCaptureButton;

    private TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
            try {
                /* Open first camera for now, response handled in callback */
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraIds[0]);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
                mCameraManager.openCamera(mCameraIds[0], mCameraStateCallback, null);
            }
            catch (CameraAccessException e) {
                cameraError("Exception: " + e.toString());
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

        }
    };

    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            /* We are given a camera device to work with once opened */
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {

        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {

        }
    };

    private CameraCaptureSession.StateCallback mPreviewStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;
            mCaptureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            Handler backgroundHandler = new Handler(thread.getLooper());

            try {
                mCaptureSession.setRepeatingRequest(mCaptureBuilder.build(), null, backgroundHandler);
            }
            catch (CameraAccessException e) {
                cameraError("Exception: " + e.toString());
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture_screen);

        /* Firstly, obtain a CameraManager instance */
        mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        if (mCameraManager == null) {
            cameraError("No manager found");
            return;
        }

        /* Now attempt to grab all camera IDs */
        try {
            mCameraIds = mCameraManager.getCameraIdList();
        }
        catch (CameraAccessException e) {
            cameraError("Exception: " + e.toString());
        }

        /* Ensure we actually have cameras... */
        if (mCameraIds.length == 0) {
            cameraError("No cameras found");
            return;
        }

        /* Now grab the view needed to show a camera */
        mCameraView = (TextureView) findViewById(R.id.camera_view);
        if (mCameraView == null) {
            cameraError("No view found");
            return;
        }

        /* Listen for access to the texture view before continuing */
        mCameraView.setSurfaceTextureListener(mSurfaceTextureListener);

        /* Find capture button */
        mCaptureButton = (Button) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }

    private void startPreview() {
        /* Start preview and set it to our texture view */

        /* Grab the texture and turn into into a workable surface */
        SurfaceTexture texture = mCameraView.getSurfaceTexture();
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        /* Create a capture request */
        try {
            mCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }
        catch (CameraAccessException e) {
            cameraError("Exception: " + e.toString());
        }
        mCaptureBuilder.addTarget(surface);

        /* Create capture session */
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface), mPreviewStateCallback, null);
        }
        catch (CameraAccessException e) {
            cameraError("Exception: " + e.toString());
        }
    }

    private void captureImage() {

    }

    private void cameraError(String error_message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error!");
        builder.setMessage(error_message);

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
