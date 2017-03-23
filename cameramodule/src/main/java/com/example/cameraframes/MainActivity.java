package com.example.cameraframes;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.naggaro.earlobedetection.MultipleEarLobeActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.ImageFormat.NV21;
import static android.os.Environment.getExternalStorageDirectory;

public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static final long FRAME_TIME_WINDOW = 10 * 1000; // in mili seconds
    private static final int CAPTURE_FRAME_INTERVAL = 10; // process every Nth image
    private static final int REQ_DETECT = 123;
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    boolean previewing = false;
    boolean startCapture = false;
    List<byte[]> rawBitmapList;
    long firstFrameTime;
    int fps;
    int frameDivideFactor;
    //    private List<Bitmap> bitmapist = null;
    private ImageView imgShutter = null;
    private Button btnStopCapture = null;
    private Handler mProgressHandler = null;
    private Runnable checkProgress = new Runnable() {
        @Override
        public void run() {
            if (startCapture) {
                btnStopCapture.callOnClick();
            }
        }
    };
    private int count;
    private View.OnClickListener mProcessingButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            btnStopCapture.setVisibility(View.GONE);
            if (startCapture) {
                startCapture = false;
                Toast.makeText(MainActivity.this, "Stop Capture", Toast.LENGTH_SHORT).show();
            }
            mProgressHandler.removeCallbacks(checkProgress);
            if (rawBitmapList != null && rawBitmapList.size() > 0) {
                Log.i("BITMAPS ", rawBitmapList.size() + "");
            }
            //TODO:
            ArrayList<String> imageFiles = new ArrayList<>();
            File imageDir = new File(Environment.getExternalStorageDirectory()
                    + File.separator
                    + getResources().getString(R.string.app_name));
            if (imageDir.isDirectory()) {
                for (File child : imageDir.listFiles()) {
                    imageFiles.add(child.getAbsolutePath());
                }
                startActivity(MultipleEarLobeActivity.createIntent(MainActivity.this, imageFiles));
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = (SurfaceView) findViewById(R.id.camerapreview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        rawBitmapList = new ArrayList<>();
        imgShutter = (ImageView) findViewById(R.id.img_shutter);
        imgShutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mProgressHandler.postDelayed(checkProgress, FRAME_TIME_WINDOW);

                view.setVisibility(View.GONE);
                btnStopCapture.setVisibility(View.VISIBLE);

                startCapture = true;

                Toast.makeText(MainActivity.this, "Start Capture", Toast.LENGTH_SHORT).show();
            }
        });
        btnStopCapture = (Button) findViewById(R.id.btn_stop_capture);

        btnStopCapture.setOnClickListener(mProcessingButtonClickListener);
        mProgressHandler = new Handler();
        deleteDirectory();
    }

    private void deleteDirectory() {
        String path = getExternalStorageDirectory() + File.separator
                + getResources().getString(R.string.app_name);

        File outputDir = new File(path);

        if (outputDir.exists() && outputDir.isDirectory()) {
            deleteRecursive(outputDir);
        }
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            if (fileOrDirectory.listFiles() != null && fileOrDirectory.listFiles().length > 0) {
                for (File child : fileOrDirectory.listFiles()) {
                    deleteRecursive(child);
                }
            }
            fileOrDirectory.delete();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (camera != null) {
            camera.startPreview();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera != null) {
            camera.release();
        }
        if (mProgressHandler != null) {
            mProgressHandler.removeCallbacks(checkProgress);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (previewing) {
            camera.stopPreview();
            previewing = false;
        }

        if (camera != null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        camera.setDisplayOrientation(90);
        camera.setPreviewCallback(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();
        camera = null;
        previewing = false;
    }

    @Override
    public void onPreviewFrame(byte[] bitmapdata, Camera camera) {
        if (startCapture) {
            //rawBitmapList.add(bitmapdata);

            if (count % CAPTURE_FRAME_INTERVAL == 0) {
                processImage(bitmapdata, count);
            }
            Log.i("count", "" + count++);
            btnStopCapture.setText(getResources().getString(R.string.stop_capture) + count);
        }
    }

    private void processImage(final byte[] bitmapdata, final int index) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                int imageFormat = parameters.getPreviewFormat();
                if (imageFormat == NV21) {
                    Rect rect = new Rect(0, 0, size.width, size.height);
                    YuvImage img = new YuvImage(bitmapdata, NV21, size.width, size.height,
                            null);
                    Bitmap imgBitmap = rotateBitmap(img, -90, rect);
                    saveImage(index, imgBitmap);
                }
            }

            private void saveImage(int index, Bitmap imgBitmap) {
                String path = getExternalStorageDirectory() + File.separator
                        + getResources().getString(R.string.app_name);

                File outputDir = new File(path);

                if (!(outputDir.exists() && outputDir.isDirectory())) {
                    outputDir.mkdirs();
                }
                File newFile = new File(path + File.separator + index + ".jpg");
                try {
                    FileOutputStream out = new FileOutputStream(newFile);
                    imgBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
                    out.flush();
                    out.close();
                    Log.i("Image created -- ", index + "");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imgBitmap.recycle();
            }

            private Bitmap rotateBitmap(YuvImage yuvImage, int orientation, Rect rectangle) {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(rectangle, 100, os);

                Matrix matrix = new Matrix();
                matrix.preScale(0.5f, 0.5f);
                matrix.postRotate(orientation);

                byte[] bytes = os.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,
                        true);
            }
        });
        thread.start();
    }
}