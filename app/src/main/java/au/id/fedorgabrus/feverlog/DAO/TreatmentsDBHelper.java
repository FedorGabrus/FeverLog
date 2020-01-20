package au.id.fedorgabrus.feverlog.DAO;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;

import au.id.fedorgabrus.feverlog.models.FeverTreatment;
import au.id.fedorgabrus.feverlog.models.TreatmentData;

public class TreatmentsDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "TreatmentsDBHelper";
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "treatments.db";

    public TreatmentsDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TreatmentsContract.TreatmentsHistory.SQL_CREATE_HISTORY_TABLE);
        db.execSQL(TreatmentsContract.TreatmentsHistory.SQL_CREATE_INDEX_ON_USAGE_DATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drops all old data.
        db.execSQL(TreatmentsContract.TreatmentsHistory.SQL_DROP_HISTORY_TABLE);
        onCreate(db);
    }

    /**
     * Loads treatment history from DB.
     *
     * @param db database to query.
     * @return List of treatments. If no treatments in db, returns an empty list.
     */
    public List<FeverTreatment> loadHistoryFromDB(SQLiteDatabase db) {
        // Queries DB to get treatment history in descending order by usage date.
        String[] columns = new String[] {
                TreatmentsContract.TreatmentsHistory._ID,
                TreatmentsContract.TreatmentsHistory.COLUMN_NAME_USAGE_DATE,
                TreatmentsContract.TreatmentsHistory.COLUMN_NAME_TREATMENT_NAME
        };
        String orderBy = TreatmentsContract.TreatmentsHistory.COLUMN_NAME_USAGE_DATE + " DESC";
        Cursor cursor = db.query(
                TreatmentsContract.TreatmentsHistory.TABLE_NAME,
                columns,
                null,
                null,
                null,
                null,
                orderBy
        );
        // Retrieves data from the cursor.
        List<FeverTreatment> queryResult = new LinkedList<>();

        if (cursor.moveToFirst()) {
            int idColumnIndex = cursor.getColumnIndexOrThrow(
                    TreatmentsContract.TreatmentsHistory._ID);
            int usageDateColumnIndex = cursor.getColumnIndexOrThrow(
                    TreatmentsContract.TreatmentsHistory.COLUMN_NAME_USAGE_DATE);
            int nameColumnIndex = cursor.getColumnIndexOrThrow(
                    TreatmentsContract.TreatmentsHistory.COLUMN_NAME_TREATMENT_NAME);

            while(!cursor.isAfterLast()) {
                queryResult.add(new FeverTreatment(
                        cursor.getInt(idColumnIndex),
                        LocalDateTime.parse(cursor.getString(usageDateColumnIndex)),
                        cursor.getString(nameColumnIndex)
                ));
                cursor.moveToNext();
            }
        }
        // Closes cursor.
        cursor.close();

        return queryResult;
    }

    /**
     * Inserts treatment into a data base.
     *
     * @param treatment treatment to save.
     * @param db database to save into.
     * @return the row ID of the newly inserted row, or -1 if an error occurred.
     */
    public int saveTreatmentIntoDB(FeverTreatment treatment, SQLiteDatabase db) {
        if (treatment == null || db == null) {
            return -1;
        }
        ContentValues values = new ContentValues();
        values.put(TreatmentsContract.TreatmentsHistory.COLUMN_NAME_USAGE_DATE,
                treatment.getTreatmentTime().toString());
        values.put(TreatmentsContract.TreatmentsHistory.COLUMN_NAME_TREATMENT_NAME,
                treatment.getTreatmentName());
        return (int) db.insert(TreatmentsContract.TreatmentsHistory.TABLE_NAME, null, values);
    }

    /**
     * Deletes data from history table in provided DB, clears data in the data model.
     *
     * @param db db to query.
     */
    public void clearHistoryDB(SQLiteDatabase db) {
        if (db == null) {
            Log.w(TAG, "clearHistoryDB was called with a null db");
            return;
        }
        // Deletes all rows from the history table.
        db.delete(TreatmentsContract.TreatmentsHistory.TABLE_NAME, null, null);
        // Clears data model.
        TreatmentData.getInstance().clearHistory();
    }

    /**
     * Updates treatment record.
     *
     * @param db db to update.
     * @param treatment new data.
     * @return the number of rows affected (1 if successful).
     */
    public int updateTreatment(SQLiteDatabase db, FeverTreatment treatment) {
        if (db == null || treatment == null) {
            Log.w(TAG, "updateTreatment(...) null argument/s");
            return 0;
        }

        ContentValues values = new ContentValues();
        values.put(TreatmentsContract.TreatmentsHistory.COLUMN_NAME_TREATMENT_NAME,
                treatment.getTreatmentName());
        values.put(TreatmentsContract.TreatmentsHistory.COLUMN_NAME_USAGE_DATE,
                treatment.getTreatmentTime().toString());
        String whereClause = TreatmentsContract.TreatmentsHistory._ID + " = ?";
        String[] whereArgs = new String[] {String.valueOf(treatment.getId())};

        return db.update(
                TreatmentsContract.TreatmentsHistory.TABLE_NAME,
                values,
                whereClause,
                whereArgs
        );
    }

    /**
     * Deletes treatment record.
     *
     * @param db db to update.
     * @param treatment treatment to delete.
     * @return the number of rows affected (1 if successful).
     */
    public int deleteTreatment(SQLiteDatabase db, FeverTreatment treatment) {
        if (db == null || treatment == null) {
            Log.w(TAG, "updateTreatment(...) null argument/s");
            return 0;
        }

        String whereClause = TreatmentsContract.TreatmentsHistory._ID + " = ?";
        String[] whereArgs = new String[] {String.valueOf(treatment.getId())};

        return db.delete(
                TreatmentsContract.TreatmentsHistory.TABLE_NAME,
                whereClause,
                whereArgs
        );
    }
}
