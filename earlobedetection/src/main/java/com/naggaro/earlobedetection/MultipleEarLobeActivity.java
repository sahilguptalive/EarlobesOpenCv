package com.naggaro.earlobedetection;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.naggaro.bitmapflipper.BitmapFlipper;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
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
import java.util.ArrayList;

public class MultipleEarLobeActivity extends AppCompatActivity {
    private static final String CASCADE_LEFT_EAR_FILE = "left_ear_cascade.xml";
    private static final String CASCADE_RIGHT_EAR_FILE = "right_ear_cascade.xml";
    private static final String TAG = "ELA";
    private static final String KEY_FILE_PATHS = "key_file_paths";
    private static final java.lang.String EARING_FILE = "earing_file.jpg";


    private ProgressDialog progressDialog;

    public static Intent createIntent(Context context
            , ArrayList<String> bitmapFilePaths) {
        Intent intent = new Intent(context, MultipleEarLobeActivity.class);
        intent.putStringArrayListExtra(KEY_FILE_PATHS, bitmapFilePaths);
        return intent;
    }

    private static ArrayList<String> getBitmapFilePaths(Intent intent) {
        return intent != null && intent.getStringArrayListExtra(KEY_FILE_PATHS) != null
                ? intent.getStringArrayListExtra(KEY_FILE_PATHS) : null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ear_lobe);
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Please wait.....");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();



        new Thread(new Runnable() {
            @Override
            public void run() {
                //show progress dialog

                boolean isOpenCvLoaded = OpenCVLoader.initDebug();
                if (isOpenCvLoaded) {
                    //hide progress dialog
                    earlobeDetection(getBitmapFilePaths(getIntent()));
                }
            }
        }).start();
}

    private void earlobeDetection(final ArrayList<String> bitmapFilePaths) {
        if (!copyAssetCascadeFilesInDir()
                || bitmapFilePaths == null
                || bitmapFilePaths.isEmpty()) {

            //do nothing
            //setResult(RESULT_OK, null);
            //finish();
            return;
        }
        CascadeClassifier leftCascade = new CascadeClassifier(getAbsolutePath(CASCADE_LEFT_EAR_FILE));
        CascadeClassifier rightCascade = new CascadeClassifier(getAbsolutePath(CASCADE_RIGHT_EAR_FILE));
        EarLobeDetectedPosition[] detectedPositions
                = new EarLobeDetectedPosition[bitmapFilePaths.size()];
        recursiveApproach(detectedPositions, bitmapFilePaths, 0, leftCascade, rightCascade);
    }

    void recursiveApproach(final EarLobeDetectedPosition[] detectedPositions
            , final ArrayList<String> bitmapFilePaths, final int index, final CascadeClassifier leftCascade, final CascadeClassifier rightCascade) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (index < bitmapFilePaths.size()) {
                    detectedPositions[index]
                            = earlobeDetection(bitmapFilePaths.get(index), leftCascade, rightCascade);
                    recursiveApproach(detectedPositions, bitmapFilePaths, index + 1, leftCascade, rightCascade);
                } else {
                    MultipleEarLobeActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                            startActivity(BitmapFlipper.createIntent(MultipleEarLobeActivity.this
                                    , bitmapFilePaths, bitmapFilePaths.size() / 2));
                            finish();
                        }
                    });
                }
            }
        }).start();
    }

    private EarLobeDetectedPosition earlobeDetection(String bitmapFilePath,
                                                     CascadeClassifier leftCascade,
                                                     CascadeClassifier rightCascade) {
        Mat inputImageMat = Imgcodecs.imread(bitmapFilePath);
        Mat grayedImageMat = new Mat();
        Imgproc.cvtColor(inputImageMat, grayedImageMat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayedImageMat, grayedImageMat);
        EarLobeDetectedPosition earLobeDetectedPosition = new EarLobeDetectedPosition();
        Rect leftRect = detect(leftCascade
                , grayedImageMat, bitmapFilePath);
        if(leftRect==null) {
            Rect rightRect = detect(rightCascade
                    , grayedImageMat, bitmapFilePath);
            if (rightRect != null) {
                rightRect.height *= .8;
                rightRect.x *= .95;
                Point centerFromRectForRight = getCenterFromRectForRight(rightRect);
                earLobeDetectedPosition.setRightEar(centerFromRectForRight);
                earLobeDetectedPosition.setRightRect(rightRect);
            }
        }else{
            leftRect.height *= .8;
            leftRect.x *= 1.05;
            Point centerFromRectForLeft = getCenterFromRectForLeft(leftRect);
            earLobeDetectedPosition.setLeftEar(centerFromRectForLeft);
            earLobeDetectedPosition.setLeftRect(leftRect);
        }
        drawOnInputImage(inputImageMat, earLobeDetectedPosition);
        saveInputImage(inputImageMat, bitmapFilePath);
        inputImageMat.release();
        grayedImageMat.release();
        return earLobeDetectedPosition;
    }

    private void saveInputImage(Mat inputImageMat, String filePath) {
        Imgcodecs.imwrite(filePath, inputImageMat);
    }

    private void drawOnInputImage(Mat inputImageMat, EarLobeDetectedPosition earLobeDetectedPosition) {
        if (earLobeDetectedPosition == null) {
            return;
        }

        if (earLobeDetectedPosition.getLeftEar() != null) {
            drawEarringOnEarLobes(inputImageMat, earLobeDetectedPosition.getLeftEar()
                    , earLobeDetectedPosition.getLeftRect(), true);
        } else if (earLobeDetectedPosition.getRightEar() != null) {
            drawEarringOnEarLobes(inputImageMat, earLobeDetectedPosition.getRightEar()
                    , earLobeDetectedPosition.getRightRect(), false);
        }
    }

    private void drawEarringOnEarLobes(Mat inputImageMat
            , Point ear, Rect rect, boolean isLeftEar) {
        Mat earringImage = Imgcodecs.imread(getAbsolutePath(EARING_FILE));
        Mat subMat;
        subMat = inputImageMat.submat(
                new Rect((int) (ear.x +
                        ((double) (rect.width + (earringImage.cols() * (isLeftEar ? 1 : -1)))
                                * (isLeftEar ? -0.5 : +0.5)))
                        , (int) ear.y
                        , earringImage.cols()
                        , earringImage.rows()));
        Core.addWeighted(earringImage, 1, subMat, 1, .0, subMat);
    }

    @NonNull
    private Scalar getScalar(int color) {
        return new Scalar(Color.blue(color), Color.green(color), Color.red(color));
    }

    private Point getCenterFromRectForRight(Rect rect) {
        return new Point(rect.x, (rect.y + rect.height));
    }

    private Point getCenterFromRectForLeft(Rect rect) {
        return new Point(rect.x + rect.width, (rect.y + rect.height));
    }

    private Rect detect(CascadeClassifier cascadeClassifier, Mat inputImage, String logRef) {

        if (cascadeClassifier == null || cascadeClassifier.empty()) {
            Log.e(TAG, logRef + " : open cv setup failed: files are empty");
            return null;
        }
        MatOfRect matOfRect = new MatOfRect();
        cascadeClassifier.detectMultiScale(inputImage, matOfRect);
        if (matOfRect.empty()) {
            Log.e(TAG, logRef + " : mat of rect is empty- no detections were made");
            return null;
        }
        Rect[] recArray = matOfRect.toArray();
        return recArray != null && recArray.length > 0 ? recArray[0] : null;
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
                        && copyAssetFileTo("earing/earing_20.png",
                        getAbsolutePath(EARING_FILE));

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
