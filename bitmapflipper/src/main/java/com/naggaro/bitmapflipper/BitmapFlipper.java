package com.naggaro.bitmapflipper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.File;
import java.util.ArrayList;

public class BitmapFlipper extends AppCompatActivity {

    private static final int NO_VALUE_INT = -1;
    private static final int X_THRESHOLD = 10;
    private ImageView mImageView;
    private RelativeLayout mRootView;
    private static final String KEY_FILE_PATHS = "key_file_paths";
    private static final String KEY_CENTER_IMAGE_INDEX = "key_center_image_index";
    private DisplayImageOptions mDisplayImageOptions;
    private ImageLoader mImageLoader;
    private int mCurrentImageIndex;
    private ArrayList<String> mInputBitmapFilePaths;
    private int mInputCenterIndex;

    public static Intent createIntent(Context context
            , ArrayList<String> bitmapFilePaths, int indexOfCenterImage) {
        Intent intent = new Intent(context, BitmapFlipper.class);
        intent.putStringArrayListExtra(KEY_FILE_PATHS, bitmapFilePaths);
        intent.putExtra(KEY_CENTER_IMAGE_INDEX, indexOfCenterImage);
        return intent;
    }

    private static ArrayList<String> getBitmapFilePaths(Intent intent) {
        return intent != null && intent.getStringArrayListExtra(KEY_FILE_PATHS) != null
                ? intent.getStringArrayListExtra(KEY_FILE_PATHS) : null;
    }

    private static int getIndexOfCenterImage(Intent intent) {
        return intent != null
                ? intent.getIntExtra(KEY_CENTER_IMAGE_INDEX, NO_VALUE_INT) : NO_VALUE_INT;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap_flipper);
        validateInput();
        initImageLoader();
        initView();
        showCenterImage();
    }

    private void validateInput() {
        mInputBitmapFilePaths = getBitmapFilePaths(getIntent());
        mInputCenterIndex = getIndexOfCenterImage(getIntent());
        if (mInputBitmapFilePaths == null
                || mInputBitmapFilePaths.isEmpty()) {
            throw new IllegalArgumentException("Image File Paths can not be empty");
        }
        if (mInputCenterIndex < 0) {
            throw new IllegalArgumentException("Center Image Index can not be less than zero(0)");
        }
        if (mInputCenterIndex >= mInputBitmapFilePaths.size()) {
            throw new IllegalArgumentException("Center Image Index can not be more" +
                    " than size of bitmap list");
        }
    }

    private void showCenterImage() {
        showImageWithIndex(mInputCenterIndex);
    }

    private void showImageWithIndex(int imageIndex) {
        mImageLoader
                .displayImage(Uri.fromFile(new File(mInputBitmapFilePaths.get(imageIndex))).toString()
                        , mImageView, mDisplayImageOptions);
        mCurrentImageIndex = imageIndex;
    }

    private void initView() {
        mImageView = (ImageView) findViewById(R.id.face_image_views);
        mRootView = (RelativeLayout) findViewById(R.id.activity_bitmap_flipper_root);
        mRootView.setOnTouchListener(new RootViewTouchListener(this));
    }

    private void initImageLoader() {
        ImageLoaderConfiguration imageLoaderConfiguration
                = ImageLoaderConfiguration.createDefault(this);
        mImageLoader = ImageLoader
                .getInstance();
        mImageLoader.init(imageLoaderConfiguration);
        mDisplayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        mImageLoader.clearDiskCache();
        mImageLoader.clearMemoryCache();
    }

    private class RootViewTouchListener implements View.OnTouchListener {
        private final int mThresholdChange;//value of threshold X axis in pixels
        private float mInitialX;

        RootViewTouchListener(Context context) {
            if (context == null) {
                throw new NullPointerException("Context can not be null");
            }
            mThresholdChange = (int) (X_THRESHOLD * context.getResources().getDisplayMetrics().density);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mInitialX = event.getX();
                    break;
                case MotionEvent.ACTION_UP:
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(event.getX() - mInitialX) >= mThresholdChange) {
                        if (event.getX() - mInitialX > 0) {
                            rightImageRequested();
                        } else {
                            leftImageRequested();
                        }
                        mInitialX = event.getX();
                    }
            }
            return true;
        }
    }

    private void leftImageRequested() {
        toShowImage(mCurrentImageIndex - 1);
    }

    private void rightImageRequested() {
        toShowImage(mCurrentImageIndex + 1);
    }

    private void toShowImage(int newRequestedImageIndex) {
        if (newRequestedImageIndex < 0
                || newRequestedImageIndex >= mInputBitmapFilePaths.size()) {
            //do nothing
            return;
        }
        showImageWithIndex(newRequestedImageIndex);
    }
}
