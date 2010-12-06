package com.googlecode.awsms.db;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

/**
 * Object to manage database, useful e.g. to keep track of sent messages number
 * 
 * @author Andrea De Pasquale
 */
public class SmslogDatabase {
// FIXME counter is always zero, read problem or write problem?
	
    private static final String DB_NAME = "awsms.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "smslog";
    
	public static final String _ID = "_id";
	public static final String DATE = "date";
	public static final String SENDER = "sender";
	public static final String SIZE = "size";
    
    private static class SmslogOpenHelper extends SQLiteOpenHelper {

        SmslogOpenHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
					_ID + " INTEGER PRIMARY KEY," +
					DATE + " TEXT," +
					SENDER + " TEXT," +
					SIZE + " INTEGER" +
					");");
		}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME + ";");
            onCreate(db);
        }
    }
    
	private SmslogOpenHelper dbOpenHelper;
	private SimpleDateFormat dateFormat;
    
    public SmslogDatabase(Context context) {
    	dbOpenHelper = new SmslogOpenHelper(context);
    	dateFormat = new SimpleDateFormat("yyyyMMdd");
    }
	
	public int query(String sender) {
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);

        SQLiteDatabase database = dbOpenHelper.getReadableDatabase();
		Cursor cursor = queryBuilder.query(database, 
				new String[] { SmslogDatabase._ID, SmslogDatabase.SIZE },
				"?=? AND ?=?", new String[] { 
				SmslogDatabase.DATE, dateFormat.format(new Date()), 
				SmslogDatabase.SENDER, sender,
				}, null, null, null);

		int size = 0;
	    cursor.moveToFirst();
	    while (cursor.isAfterLast() == false) {
	    	size += cursor.getInt(cursor.getColumnIndex(SIZE));
	   	    cursor.moveToNext();
	    }
		
		cursor.close();
        return size;
	}
	
	public void insert(String sender, int size) {
		ContentValues values = new ContentValues();
		values.put(DATE, dateFormat.format(new Date()));
		values.put(SENDER, sender);
		values.put(SIZE, size);

		SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
		database.insert(TABLE_NAME, SENDER, values);
	}

	public void delete() {
		SQLiteDatabase database = dbOpenHelper.getWritableDatabase();
		database.delete(TABLE_NAME, "?<>?", new String[] { 
				SmslogDatabase.DATE, dateFormat.format(new Date()) });
	}
	
}
