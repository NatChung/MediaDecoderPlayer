package nat.chung.mediadecoderplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PlaybackFileSQLiteOpenHelper extends SQLiteOpenHelper
{
	public String PlayBackTableNames[];
	public String PlayBackFieldNames[][];
	public String PlayBackFieldTypes[][];
	public static String NO_CREATE_TABLES = "no tables";
	private String message = "";
	private final static String _TAG = "SQLiteOpenHelper";

	public PlaybackFileSQLiteOpenHelper(Context context, String dbname, CursorFactory factory, int version, String tableNames[], String fieldNames[][], String fieldTypes[][])
	{
		//super(context, dbname, factory, version);
		super(context, dbname, null, version);
		PlayBackTableNames = tableNames;
		PlayBackFieldNames = fieldNames;
		PlayBackFieldTypes = fieldTypes;
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		//db.execSQL(DICTIONARY_TABLE_CREATE);
		if (PlayBackTableNames != null)
		{
			message = NO_CREATE_TABLES;
			return;
		}
		/*
		 * for (int i = 0; i < PlayBackTableNames.length; i++) { String sql = "CREATE TABLE " + PlayBackTableNames[i] + " ("; for (int j = 0; j < PlayBackFieldNames[i].length; j++) { sql += PlayBackFieldNames[i][j] + " " + PlayBackFieldTypes[i][j] + ","; } sql = sql.substring(0, sql.length() - 1); sql += ")"; db.execSQL(sql); }
		 */
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		Log.d(_TAG, "onUpgrade called, oldVersion=" + oldVersion + ", newVersion=" + newVersion);
	}

	public void execSQL(String sql) throws java.sql.SQLException
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL(sql);
	}
	
	public Cursor select(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy)
	{
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
		return cursor;
	}

	public long insert(String table, String fields[], String values[])
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		for (int i = 0; i < fields.length; i++)
		{
			cv.put(fields[i], values[i]);
		}
		return db.insert(table, null, cv);
	}

	public long insertOrThrow(String table, String fields[], String values[])
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues cv = new ContentValues();
		for (int i = 0; i < fields.length; i++)
		{
			cv.put(fields[i], values[i]);
		}
		return db.insertOrThrow(table, null, cv);
	}

	public int delete(String table, String where, String[] whereValue)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		return db.delete(table, where, whereValue);
	}

	public int update(String table, String updateFields[], String updateValues[], String where, String[] whereValue)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues cv = new ContentValues();
		for (int i = 0; i < updateFields.length; i++)
		{
			cv.put(updateFields[i], updateValues[i]);
		}
		return db.update(table, cv, where, whereValue);
	}

	public String getMessage()
	{
		return message;
	}

	@Override
	public synchronized void close()
	{
		// TODO Auto-generated method stub
		super.close();
	}
}
