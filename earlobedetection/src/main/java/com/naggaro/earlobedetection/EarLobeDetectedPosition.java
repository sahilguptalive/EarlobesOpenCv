package com.naggaro.earlobedetection;


import android.os.Parcel;

import org.opencv.core.Point;
import org.opencv.core.Rect;

/**
 * Created by sahilgupta on 02-03-2017.
 */

public class EarLobeDetectedPosition{

    private static final double INVALID_POINT = -1;
    private Point leftEar;
    private Point rightEar;
    private Rect leftRect;
    private Rect rightRect;
    private float mEuelerAngle;


    public EarLobeDetectedPosition() {
    }

    protected EarLobeDetectedPosition(Parcel in) {
        leftEar=convertToPoint(in);
        rightEar=convertToPoint(in);
        mEuelerAngle = in.readFloat();
    }

    private Point convertToPoint(Parcel in) {
        double x=in.readDouble();
        double y=in.readDouble();
        if(y==INVALID_POINT||x==INVALID_POINT){
            return null;
        }else{
            return new Point(x,y);
        }
    }

//    public static final Creator<EarLobeDetectedPosition> CREATOR = new Creator<EarLobeDetectedPosition>() {
//        @Override
//        public EarLobeDetectedPosition createFromParcel(Parcel in) {
//            return new EarLobeDetectedPosition(in);
//        }
//
//        @Override
//        public EarLobeDetectedPosition[] newArray(int size) {
//            return new EarLobeDetectedPosition[size];
//        }
//    };

    public Point getLeftEar() {
        return leftEar;
    }

    public void setLeftEar(Point leftEar) {
        this.leftEar = leftEar;
    }

    public Point getRightEar() {
        return rightEar;
    }

    public void setRightEar(Point rightEar) {
        this.rightEar = rightEar;
    }

    public float getmEuelerAngle() {
        return mEuelerAngle;
    }

    public void setmEuelerAngle(float mEuelerAngle) {
        this.mEuelerAngle = mEuelerAngle;
    }

    public Rect getLeftRect() {
        return leftRect;
    }

    public void setLeftRect(Rect leftRect) {
        this.leftRect = leftRect;
    }

    public Rect getRightRect() {
        return rightRect;
    }

    public void setRightRect(Rect rightRect) {
        this.rightRect = rightRect;
    }


    //    @Override
//    public int describeContents() {
//        return 0;
//    }
//
//    @Override
//    public void writeToParcel(Parcel dest, int flags) {
//        dest.writeDouble(leftEar==null?INVALID_POINT:leftEar.x);
//        dest.writeDouble(leftEar==null?INVALID_POINT:leftEar.y);
//        dest.writeDouble(rightEar==null?INVALID_POINT:rightEar.x);
//        dest.writeDouble(rightEar==null?INVALID_POINT:rightEar.y);
//        dest.writeFloat(mEuelerAngle);
//    }
}
