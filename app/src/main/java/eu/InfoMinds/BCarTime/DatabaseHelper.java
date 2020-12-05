package eu.InfoMinds.BCarTime;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.util.Locale;
import java.util.UUID;

import androidx.annotation.Nullable;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by @Roberto Fiume 17-3-2020.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASENAME = "BCarTime.db";
    private static final String REGISTERED_DEVICES_TABLE = "REGISTERED_DEVICES";
    private static final String MONITORIZED_REGIONS_TABLE = "MONITORIZED_REGIONS";
    private static final String SETTINGS_TABLE = "SETTINGS";
    private static final String TIMEATTENDANCE_TABLE = "TIMEATTENDANCE";

    public DatabaseHelper(Context context) {
        super(context, DATABASENAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_REGISTERED_DEVICES_TABLE = "CREATE TABLE IF NOT EXISTS " + REGISTERED_DEVICES_TABLE +
                                                 " (" +
                                                 "  SERIALNUMBER VARCHAR(50) NOT NULL PRIMARY KEY, " +
                                                 "  DESCRIPTION VARCHAR(100), "+
                                                 "  PRIORITY SMALLINT " +
                                                 " )";
        db.execSQL(CREATE_REGISTERED_DEVICES_TABLE);

        String CREATE_MONITORIZED_REGIONS_TABLE = "CREATE TABLE IF NOT EXISTS " + MONITORIZED_REGIONS_TABLE +
                                                  " (" +
                                                  " UUID VARCHAR(50) NOT NULL PRIMARY KEY, " +
                                                  " MAJOR SMALLINT, " +
                                                  " MINOR SMALLINT, " +
                                                  " DESCRIPTION VARCHAR(100) " +
                                                  " )";
        db.execSQL(CREATE_MONITORIZED_REGIONS_TABLE);

        String CREATE_SETTINGS_TABLE = "CREATE TABLE IF NOT EXISTS " + SETTINGS_TABLE +
                                       " (" +
                                       " USERNAME VARCHAR(100) NOT NULL PRIMARY KEY, " +
                                       " PASSWORD VARCHAR(100), " +
                                       " AUTOLOGIN SMALLINT " +
                                       " )";
        db.execSQL(CREATE_SETTINGS_TABLE);

        String CREATE_TIMEATTENDANCE_TABLE = "CREATE TABLE IF NOT EXISTS " + TIMEATTENDANCE_TABLE +
                                             " (" +
                                             " ID VARCHAR(40) NOT NULL PRIMARY KEY, " +
                                             " FROMTIME DATETIME, " +
                                             " TOTIME DATETIME, " +
                                             " RECOVERYTIME DATETIME, " +
                                             " SYNCHRONIZED SMALLINT " +
                                             " )";
        db.execSQL(CREATE_TIMEATTENDANCE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
    }

    private String getDateTime(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(date);
    }

    public boolean insertTimeAttendance (Date date) {
        SQLiteDatabase db = this.getWritableDatabase();

        String uniqueID = UUID.randomUUID().toString();
        ContentValues contentValues = new ContentValues();

        contentValues.put("ID", uniqueID);
        contentValues.put("FROMTIME", getDateTime(date));
        contentValues.put("SYNCHRONIZED", 0);

        long result = db.insert(TIMEATTENDANCE_TABLE, null, contentValues);
        if (result == -1)
            return false;
        else
            return true;
    }

    public void updateTimeAttendance (Date date) {
        SQLiteDatabase db = this.getWritableDatabase();

        String uniqueID = UUID.randomUUID().toString();
        ContentValues contentValues = new ContentValues();
        contentValues.put("TOTIME", getDateTime(date));

        db.update(TIMEATTENDANCE_TABLE,contentValues,"TOTIME IS NULL",null);
    }

    public void updateRecoveryTimeAttendance (Date date) {
        SQLiteDatabase db = this.getWritableDatabase();

        String uniqueID = UUID.randomUUID().toString();
        ContentValues contentValues = new ContentValues();
        contentValues.put("RECOVERYTIME", getDateTime(date));

        db.update(TIMEATTENDANCE_TABLE,contentValues,"TOTIME IS NULL",null);
    }


    public Cursor getTimeAttendance () {
        SQLiteDatabase db = this.getWritableDatabase();
        String SQL = "SELECT ID AS _id,FROMTIME,TOTIME,SYNCHRONIZED, " +
                     "       IFNULL(strftime('%H:%M:%S',FROMTIME),'') AS FROM_TIME, IFNULL(strftime('%H:%M:%S',TOTIME),'') AS TO_TIME, " +
                     "       IFNULL(strftime('%H:%M:%S',cast((julianday(TOTIME)-julianday(FROMTIME)) as real),'12:00'),'') AS TOTAL_TIME " +
                     "  FROM " + TIMEATTENDANCE_TABLE +
                     "  ORDER BY FROMTIME DESC";

        Cursor cursor = db.rawQuery(SQL,null);
        if (cursor != null)
            cursor.moveToFirst();

        return cursor;
    }

    public void setTimeAttendance (Date date) {
        SQLiteDatabase db = this.getWritableDatabase();

        String SQL = "SELECT ID AS _id,FROMTIME,RECOVERYTIME " +
                     "  FROM " + TIMEATTENDANCE_TABLE +
                     "  WHERE FROMTIME IS NOT NULL AND TOTIME IS NULL " +
                     "  ORDER BY FROMTIME DESC";

        Cursor cursor = db.rawQuery(SQL,null);
        if (cursor != null) {
            if (cursor.moveToNext()) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                String fromTimeValue =  cursor.getString(cursor.getColumnIndex("FROMTIME"));
                String recoveryTimeValue = cursor.getString(cursor.getColumnIndex("RECOVERYTIME"));

                Date fromTime = new Date();
                Date recoveryTime = new Date();
                try {
                    fromTime = dateFormat.parse(fromTimeValue);
                    if ((recoveryTimeValue != null) && (!recoveryTimeValue.isEmpty()))
                        recoveryTime =  dateFormat.parse(recoveryTimeValue);
                    else
                        recoveryTime = date;

                    //Controllo se l'ultima data inserita rispetto alla data è maggiore di 2 minuti
                    long diffInMillisec = date.getTime() - fromTime.getTime();
                    long minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillisec);
                    if (minutes > 2) {
                        //Aggiorna il record corrente con i dati della recovery time
                        updateTimeAttendance(recoveryTime);

                        //Inserisce un nuovo record
                        insertTimeAttendance(date);
                    }
                    else
                    {
                        //aggiorna la data di recovery time
                        updateRecoveryTimeAttendance(recoveryTime);
                    }
                } catch (ParseException e) {

                }
            }
            else
            {
                insertTimeAttendance(date);
            }

        }
    }
}
