package com.naggaro.earlobedetection;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EarLobeActivity extends AppCompatActivity {
    private static final String CASCADE_LEFT_EAR_FILE = "left_ear_cascade.xml";
    private static final String CASCADE_RIGHT_EAR_FILE = "right_ear_cascade.xml";
    private static final String CASCADE_FRONTAL_FACE_ALT_FILE = "haarcascade_frontalface_alt.xml";
    private static final String CASCADE_FRONTAL_FACE_ALT2_FILE = "haarcascade_frontalface_alt2.xml";
    private static final String CASCADE_FRONTAL_FACE_ALT_TREE_FILE = "haarcascade_frontalface_alt_tree.xml";
    private static final String CASCADE_FRONTAL_FACE_DEFAULT_FILE = "haarcascade_frontalface_default.xml";
    private static final String CASCADE_PROFILE_FACE_FILE = "haarcascade_profileface.xml";
    private static final String CASCADE_AKSH_FILE = "haarcascade_aksh.xml";
    private static final String FACE_IMAGE_INPUT_FILE = "face_image_input_file.jpg";
    private static final String FACE_IMAGE_OUTPUT_FILE = "face_image_output_file.jpg";
    private static final String TAG = "ELA";

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "init debug failed");
        } else {
            Log.e(TAG, "init debug success");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ear_lobe);
        earlobeDetection();
    }

    private void earlobeDetection() {
        if (!copyAssetCascadeFilesInDir()) {
            Log.e(TAG, "open cv setup failed: files not copied");
            return;
        }
        Mat inputImageMat = Imgcodecs.imread(getAbsolutePath(FACE_IMAGE_INPUT_FILE));
        detect(new CascadeClassifier(getAbsolutePath(CASCADE_LEFT_EAR_FILE))
                , inputImageMat, getScalar(Color.BLUE));
        detect(new CascadeClassifier(getAbsolutePath(CASCADE_RIGHT_EAR_FILE))
                , inputImageMat, getScalar(Color.GREEN));
        detect(new CascadeClassifier(getAbsolutePath(CASCADE_PROFILE_FACE_FILE))
                , inputImageMat, getScalar(Color.WHITE
                ));
        detect(new CascadeClassifier(getAbsolutePath(CASCADE_AKSH_FILE))
                , inputImageMat, getScalar(Color.RED
                ));
        Imgcodecs.imwrite(getAbsolutePath(FACE_IMAGE_OUTPUT_FILE, true), inputImageMat);
    }

    @NonNull
    private Scalar getScalar(int color) {
        return new Scalar(Color.blue(color), Color.green(color), Color.red(color));
    }

    private void detect(CascadeClassifier cascadeClassifier, Mat inputImage, Scalar color) {

        if (cascadeClassifier == null || cascadeClassifier.empty()) {
            Log.e(TAG, "open cv setup failed: files are empty");
            return;
        }
        MatOfRect matOfRect = new MatOfRect();
        cascadeClassifier.detectMultiScale(inputImage, matOfRect);
        if (matOfRect.empty()) {
            Log.e(TAG, "mat of rect is empty- no detections were made");
            return;
        }
        Rect[] recArray = matOfRect.toArray();
        for (Rect rect : recArray) {
            int centerX = rect.x + rect.width / 2;
            int centerY = rect.y + rect.height;
            Imgproc.rectangle(inputImage, new Point(rect.x, rect.y)
                    , new Point(rect.x + rect.width, rect.y + rect.height)
                    , color);
            Imgproc.circle(inputImage, new Point(centerX, centerY)
                    , 4, color);
        }
    }

    @NonNull
    private String getAbsolutePath(String filename) {
        return new File(this.getFilesDir()
                , filename).getAbsolutePath();
    }

    @NonNull
    private String getAbsolutePath(String filename, boolean isExternal) {
        return new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                , filename).getAbsolutePath();
    }


    private boolean copyAssetCascadeFilesInDir() {
        return
                copyAssetFileTo("cascades/haarcascade_mcs_leftear.xml"
                        , getAbsolutePath(CASCADE_LEFT_EAR_FILE))
                        && copyAssetFileTo("cascades/haarcascade_mcs_rightear.xml"
                        , getAbsolutePath(CASCADE_RIGHT_EAR_FILE))
                        && copyAssetFileTo("cascades/haarcascade_frontalface_alt.xml"
                        , getAbsolutePath(CASCADE_FRONTAL_FACE_ALT_FILE))
                        && copyAssetFileTo("cascades/haarcascade_frontalface_alt2.xml"
                        , getAbsolutePath(CASCADE_FRONTAL_FACE_ALT2_FILE))
                        && copyAssetFileTo("cascades/haarcascade_frontalface_alt_tree.xml"
                        , getAbsolutePath(CASCADE_FRONTAL_FACE_ALT_TREE_FILE))
                        && copyAssetFileTo("cascades/haarcascade_frontalface_default.xml"
                        , getAbsolutePath(CASCADE_FRONTAL_FACE_DEFAULT_FILE))
                        && copyAssetFileTo("cascades/haarcascade_profileface.xml"
                        , getAbsolutePath(CASCADE_PROFILE_FACE_FILE))
                        && copyAssetFileTo("cascades/aksh_cascade.xml"
                        , getAbsolutePath(CASCADE_AKSH_FILE))
                        && copyAssetFileTo("vto/resultFrame7.jpg"
                        , getAbsolutePath(FACE_IMAGE_INPUT_FILE));
    }

    private boolean copyAssetFileTo(String assetFileName, String outputFileName) {
        boolean isFileCopied = true;

        InputStream assetInputStream = null;
        FileOutputStream outputStream = null;
        try {
            assetInputStream = this.getAssets().open(assetFileName);
            outputStream = new FileOutputStream(outputFileName, false);
            int b = assetInputStream.read();
            while (b != -1) {
                outputStream.write(b);
                b = assetInputStream.read();
            }
        } catch (IOException e) {
            isFileCopied = false;
        } finally {
            if (assetInputStream != null) {
                try {
                    assetInputStream.close();
                } catch (IOException e) {
                    // no action required
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    //no action required
                }
            }
        }
        return isFileCopied;
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
}
