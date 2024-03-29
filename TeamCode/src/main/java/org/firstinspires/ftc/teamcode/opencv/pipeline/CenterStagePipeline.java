package org.firstinspires.ftc.teamcode.opencv.pipeline;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class CenterStagePipeline implements VisionProcessor {

    public enum PropPosition {
        LEFT,
        RIGHT,
        CENTER
    }

    private Paint linePaint = new Paint();

    private final Point LEFT_RECT_P = new Point(20, 330);
    private final Point CENTER_RECT_P = new Point(280, 320);
    private final Point RIGHT_RECT_P = new Point(580, 330);

    private final Rect LEFT_RECT = new Rect(
        LEFT_RECT_P,
        new Size(40, 40)
    );
    private final Rect CENTER_RECT = new Rect(
        CENTER_RECT_P,
        new Size(40, 40)
    );
    private final Rect RIGHT_RECT = new Rect(
        RIGHT_RECT_P,
        new Size(40, 40)
    );

    private Mat frame = new Mat();
    private Mat channel = new Mat();

    private Mat left, right, center;

    private volatile PropPosition propPos = PropPosition.LEFT;

    private volatile int coi = 1;

    private void toYCrCb(Mat rgbFrame){
        Imgproc.cvtColor(rgbFrame, frame, Imgproc.COLOR_RGB2YCrCb);
        Core.extractChannel(frame, channel, coi);
    }

    @Override
    public void init(int width, int height, CameraCalibration calibration) {
        linePaint.setColor(Color.BLUE);
        linePaint.setAlpha(100);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3);
    }

    private boolean initialized = false;

    @Override
    public Object processFrame(Mat input, long captureTimeNanos) {
        toYCrCb(input);
        if(!initialized) {
            left = channel.submat(LEFT_RECT);
            center = channel.submat(CENTER_RECT);
            right = channel.submat(RIGHT_RECT);
            initialized = true;
        }

        double leftAvg = Core.mean(left).val[0];
        double centerAvg = Core.mean(center).val[0];
        double rightAvg = Core.mean(right).val[0];

        if(leftAvg > centerAvg){
            if(leftAvg > rightAvg) propPos = PropPosition.LEFT;
            else propPos = PropPosition.RIGHT;
        } else if(rightAvg > centerAvg) propPos = PropPosition.RIGHT;
        else propPos = PropPosition.CENTER;

/*
        Imgproc.cvtColor(input, input, Imgproc.COLOR_RGB2YCrCb);
        Core.extractChannel(input, input, 2);
 */
        return input;
    }

    public void extractChannel(int coi){
        this.coi = coi;
    }

    @Override
    public void onDrawFrame(Canvas canvas, int onscreenWidth, int onscreenHeight, float scaleBmpPxToCanvasPx, float scaleCanvasDensity, Object userContext) {
        canvas.drawRect(toDrawableRect(LEFT_RECT, scaleBmpPxToCanvasPx), linePaint);
        canvas.drawRect(toDrawableRect(CENTER_RECT, scaleBmpPxToCanvasPx), linePaint);
        canvas.drawRect(toDrawableRect(RIGHT_RECT, scaleBmpPxToCanvasPx), linePaint);

        linePaint.setStyle(Paint.Style.FILL);

        switch(propPos){
            case LEFT:
                canvas.drawRect(toDrawableRect(LEFT_RECT, scaleBmpPxToCanvasPx), linePaint);
                break;
            case CENTER:
                canvas.drawRect(toDrawableRect(CENTER_RECT, scaleBmpPxToCanvasPx), linePaint);
                break;
            case RIGHT:
                canvas.drawRect(toDrawableRect(RIGHT_RECT, scaleBmpPxToCanvasPx), linePaint);
                break;
        }

        linePaint.setStyle(Paint.Style.STROKE);
    }

    private android.graphics.Rect toDrawableRect(Rect rect, float scale){
        return new android.graphics.Rect((int)(rect.x * scale), (int)(rect.y * scale), (int)((rect.x + rect.width) * scale), (int)((rect.y + rect.height) * scale));
    }

    public PropPosition getPosition(){
        return propPos;
    }
}
