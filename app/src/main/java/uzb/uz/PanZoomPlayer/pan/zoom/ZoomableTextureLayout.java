package uzb.uz.PanZoomPlayer.pan.zoom;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;


/**
 * Created by Nat on 2017/1/29.
 */

public class ZoomableTextureLayout extends FrameLayout {

    private static final String TAG ="ZoomableTextureLayout" ;
    public ZoomableTextureView zoomableTextureView;

    public ZoomableTextureLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //this.zoomableTextureView = new ZoomableTextureView(context, attrs, defStyleAttr);
        init(context);
    }

    public ZoomableTextureLayout(Context context) {
        super(context);
        //this.zoomableTextureView = new ZoomableTextureView(context);
        init(context);
    }

    public ZoomableTextureLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        //this.zoomableTextureView = new ZoomableTextureView(context, attrs);
        init(context);
    }

    private void init(Context context) {

        Log.d(TAG, "init in ZooableTextureLayout");
        this.zoomableTextureView = new ZoomableTextureView(context);
        zoomableTextureView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        this.addView(zoomableTextureView);
    }

}
