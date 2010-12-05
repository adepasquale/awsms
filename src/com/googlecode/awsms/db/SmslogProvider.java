package com.googlecode.awsms.db;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Object to manage database, useful e.g. to keep track of sent messages number
 * 
 * @author Andrea De Pasquale
 */
public class SmslogProvider extends ContentProvider {
	
    private static final String DB_NAME = "awsms.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "smslog";
    
	private static final String AUTHORITY = "com.googlecode.awsms";
    private static final int SMSLOG = 1;

    private static final class SmslogColumns implements BaseColumns {
    	private SmslogColumns() {} // cannot instantiate
    	public static final Uri CONTENT_URI = 
    		Uri.parse("content://" + AUTHORITY + "/smslog"); 
    	public static final String DATE = "date";
    	public static final String SENDER = "sender";
    	public static final String SIZE = "size";
    }
    
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
					SmslogColumns._ID + " INTEGER PRIMARY KEY," +
					SmslogColumns.DATE + " TEXT," +
					SmslogColumns.SENDER + " TEXT," +
					SmslogColumns.SIZE + " INTEGER" +
					");");
		}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + ";");
            onCreate(db);
        }
    }
    
	private DatabaseHelper databaseHelper;
    private UriMatcher uriMatcher;
    
    @Override
    public boolean onCreate() {
    	databaseHelper = new DatabaseHelper(getContext());
    	uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    	uriMatcher.addURI(AUTHORITY, "smslog", SMSLOG);
    	return true;
    }
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);

		if (uriMatcher.match(uri) != SMSLOG) {
			return null;
		}

		SQLiteDatabase database = databaseHelper.getReadableDatabase();
		Cursor c = queryBuilder.query(database, projection, selection,
				selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (uriMatcher.match(uri) != SMSLOG) {
			return null;
		}
		
		if (values == null) {
			values = new ContentValues();
		}
		
		if (!values.containsKey(SmslogColumns.DATE)) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			values.put(SmslogColumns.DATE, dateFormat.format(new Date()));
		}
		
		if (!values.containsKey(SmslogColumns.SENDER)) {
			values.put(SmslogColumns.SENDER, "WebSender");
		}
		
		if (!values.containsKey(SmslogColumns.SIZE)) {
			values.put(SmslogColumns.SIZE, 1);
		}
		
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		long rowID = database.insert(TABLE_NAME, SmslogColumns.SENDER, values); 
        if (rowID > 0) {
        	Uri smslogUri = 
        		ContentUris.withAppendedId(SmslogColumns.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(smslogUri, null);
            return smslogUri;
        }

		return null;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		if (uriMatcher.match(uri) != SMSLOG) {
			return 0;
		}
		
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		int count = database.update(TABLE_NAME, values, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (uriMatcher.match(uri) != SMSLOG) {
			return 0;
		}
		
		SQLiteDatabase database = databaseHelper.getWritableDatabase();
		int count = database.delete(TABLE_NAME, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case SMSLOG:
			return "vnd.android.cursor.dir/vnd.awsms.smslog";
		default:
			return null;
		}
	}
	
}
