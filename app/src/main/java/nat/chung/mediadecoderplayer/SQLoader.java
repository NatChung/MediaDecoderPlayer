package nat.chung.mediadecoderplayer;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by starvedia on 2017/2/9.
 */

public class SQLoader implements ISQLiteLoader {

    private String DB_PATH;
    private final Context mContext;
    public PlaybackFileSQLiteOpenHelper pbFileHelper = null;
    private final String PBtables[] = { "Recording", "ESFrame" };
    String f[] = { "*" };
    private final int version = 1;
    private final String PlayBackfieldNames[][] =
            {
                    { "ID", "IsVideo", "PtsTimeMs", "Data", "isKeyFrame", "DataLen", "FrameNo" }
            };
    private final String PlayBackfieldTypes[][] =
            {
                    { "INTEGER PRIMARY KEY", "INTEGER", "INTEGER", "Blob", "INTEGER", "INTEGER", "INTEGER" }
            };

    public SQLoader(String sqlPath, Context context) throws IOException {
        this.DB_PATH = sqlPath;
        this.mContext = context;

        if (!checkDbExists())
            throw new IOException("SQL file don't exit.");

        pbFileHelper = new PlaybackFileSQLiteOpenHelper(mContext, DB_PATH, null, version, PBtables, PlayBackfieldNames, PlayBackfieldTypes);
    }

    private Boolean checkDbExists(){
        return new File(this.DB_PATH).exists();
    }

    public interface OnDataLoadListener{
        void onVideoRawData(byte[] videoData, int pts, int isKeyFrame);
        void onAudioRawData(byte[] audioData, int pts);
        void onFileFinish();
    }

    OnDataLoadListener onDataLoadListener;

    public void setOnDataLoadListener(OnDataLoadListener listener){
        this.onDataLoadListener = listener;
    }

    @Override
    public void start() {

        if (null == pbFileHelper){ return; }

        Cursor c = pbFileHelper.select(PBtables[1], f, null, null, null, null, null);
        while (c.moveToNext()) {
            int isVideo = c.getInt(1);
            int isKeyFrame = c.getInt(2);
            long pts = c.getLong(3);
            byte[] rawData = c.getBlob(5);
            if (isVideo == 1){  // video
                onDataLoadListener.onVideoRawData(rawData, (int)pts, isKeyFrame);
            }
            else{   //audio
                onDataLoadListener.onAudioRawData(rawData, (int)pts);
            }
        }
        onDataLoadListener.onFileFinish();
    }
}
