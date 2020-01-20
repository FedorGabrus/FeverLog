package au.id.fedorgabrus.feverlog.DAO;

import android.provider.BaseColumns;

/**
 * Defines structure of the treatments.db.
 */
final class TreatmentsContract {
    // Private constructor to prevent initialization.
    private TreatmentsContract(){}

    /**
     * Structure of the history table.
     */
    static class TreatmentsHistory implements BaseColumns {
        static final String TABLE_NAME = "history";
        static final String COLUMN_NAME_USAGE_DATE = "usage_date";
        static final String COLUMN_NAME_TREATMENT_NAME = "treatment_name";
        static final String INDEX_NAME_USAGE_DATE = "idx_usage_date";

        static final String SQL_CREATE_HISTORY_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TreatmentsHistory.TABLE_NAME + " ("
                + TreatmentsHistory._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TreatmentsHistory.COLUMN_NAME_USAGE_DATE + " DATETIME NOT NULL, "
                + TreatmentsHistory.COLUMN_NAME_TREATMENT_NAME + " TEXT NOT NULL)";

        static final String SQL_DROP_HISTORY_TABLE =
                "DROP TABLE IF EXISTS " + TreatmentsHistory.TABLE_NAME;

        static final String SQL_CREATE_INDEX_ON_USAGE_DATE =
                "CREATE INDEX IF NOT EXISTS " + INDEX_NAME_USAGE_DATE
                        + " ON " + TABLE_NAME + " (" + COLUMN_NAME_USAGE_DATE + " DESC)";
    }
}
