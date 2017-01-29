package nat.chung.mediadecoderplayer.decorator;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import nat.chung.mediadecoderplayer.IPlayer;
import uzb.uz.PanZoomPlayer.pan.zoom.gestures.MoveGestureDetector;

/**
 * Created by Nat on 2017/1/30.
 */

public class ZoomDecorator extends PlayerDecorator implements View.OnTouchListener{

    private static final String TAG = "ZoomDecorator";
    private Matrix mMatrix;
    private ScaleGestureDetector mScaleDetector;
    private MoveGestureDetector mMoveDetector;
    private float mScaleFactor = 1.f;
    private float mFocusX = 0.f;
    private float mFocusY = 0.f;
    private TextureView textureView;

    public ZoomDecorator(Context context, IPlayer iPlayer) {
        super(iPlayer);

        textureView = iPlayer.getTextureView();
        textureView.setOnTouchListener(this);
        init(context);
        setDisplayMetrics(context);
    }

    private void init(Context context) {
        mMatrix = new Matrix();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mMoveDetector = new MoveGestureDetector(context, new MoveListener());
    }

    public void setDisplayMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mFocusX = displayMetrics.widthPixels / 2;
        mFocusY = displayMetrics.widthPixels / 2;

        Log.i(TAG,"X:"+mFocusX+", Y:"+mFocusY);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        mScaleDetector.onTouchEvent(motionEvent);
        mMoveDetector.onTouchEvent(motionEvent);

        float scaledImageCenterX = (textureView.getWidth() * mScaleFactor) / 2;
        float scaledImageCenterY = (textureView.getHeight() * mScaleFactor) / 2;

        mMatrix.reset();
        mMatrix.postScale(mScaleFactor, mScaleFactor);

        float dx = mFocusX - scaledImageCenterX;
        float dy = mFocusY - scaledImageCenterY;

        if (dx < ((1 - mScaleFactor) * textureView.getWidth())) {
            dx = (1 - mScaleFactor) * textureView.getWidth();
            mFocusX = dx + scaledImageCenterX;
        }

        if (dy < ((1 - mScaleFactor) * textureView.getHeight())) {
            dy = (1 - mScaleFactor) * textureView.getHeight();
            mFocusY = dy + scaledImageCenterY;
        }

        if (dx > 0) {
            dx = 0;
            mFocusX = dx + scaledImageCenterX;
        }

        if (dy > 0) {
            dy = 0;
            mFocusY = dy + scaledImageCenterY;
        }
//        Log.i(TAG, mMatrix.toString());
//        Log.i(TAG, "dx:"+dx+", dy:"+dy);
        mMatrix.postTranslate(dx, dy);
        textureView.setTransform(mMatrix);
        textureView.setAlpha(1);
        return true; // indicate event was handled

    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            mScaleFactor *= detector.getScaleFactor(); // scale change since previous event
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(1.f, Math.min(mScaleFactor, 4.0f));
            return true;
        }
    }

    private class MoveListener extends MoveGestureDetector.SimpleOnMoveGestureListener {
        @Override
        public boolean onMove(MoveGestureDetector detector) {

            PointF d = detector.getFocusDelta();
            mFocusX += d.x;
            mFocusY += d.y;
            return true;
        }
    }
}
