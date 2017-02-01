package nat.chung.mediadecoderplayer.decorator.Zoom;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;

import nat.chung.mediadecoderplayer.IPlayer;
import nat.chung.mediadecoderplayer.decorator.PlayerDecorator;
import nat.chung.mediadecoderplayer.decorator.Zoom.gestures.MoveGestureDetector;

/**
 * Created by Nat on 2017/1/30.
 */

public class ZoomDecorator extends PlayerDecorator implements View.OnTouchListener{

    private static final String TAG = "ZoomDecorator";
    private Matrix mMatrix;
    private ScaleGestureDetector scaleDetector;
    private MoveGestureDetector moveDetector;
    private GestureDetector gestureDetector;
    private float mScaleFactor = 1.f;
    private float mFocusX = 0.f;
    private float mFocusY = 0.f;
    private TextureView textureView;
    private SimpleOnGestureListener simpleOnGestureListener = null;

    public interface SimpleOnGestureListener{
        boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY);
        void onViewTap(View view, float x, float y);
    }


    public ZoomDecorator(Context context, IPlayer iPlayer) {
        super(iPlayer);

        textureView = iPlayer.getTextureView();
        textureView.setOnTouchListener(this);
        init(context);
        setDisplayMetrics(context);
    }

    private void init(Context context) {
        mMatrix = new Matrix();
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        moveDetector = new MoveGestureDetector(context, new MoveListener());
        gestureDetector = new GestureDetector(context, new SingleTapListener());
    }

    private void setDisplayMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        mFocusX = displayMetrics.widthPixels / 2;
        mFocusY = displayMetrics.widthPixels / 2;
    }

    public void setSimpleOnGestureListener(SimpleOnGestureListener simpleOnGestureListener){
        this.simpleOnGestureListener = simpleOnGestureListener;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        scaleDetector.onTouchEvent(motionEvent);
        moveDetector.onTouchEvent(motionEvent);
        gestureDetector.onTouchEvent(motionEvent);

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

    private class SingleTapListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (simpleOnGestureListener != null && mScaleFactor <= 1.0 )  {
                return simpleOnGestureListener.onFling(e1, e2, velocityX, velocityY);
            }

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            if (simpleOnGestureListener != null ){
                simpleOnGestureListener.onViewTap(textureView, e.getX(), e.getY());
                return true;
            }

            return super.onSingleTapConfirmed(e);
        }
    }
}
