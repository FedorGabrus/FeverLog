package au.id.fedorgabrus.feverlog.models;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * Contains application settings.
 * <p>All fields and methods are static, can't be instantiated.</p>
 */
public final class AppSettings {
    // Private constructor to prevent instantiation.
    private AppSettings() {}

    /**
     * Key for settings from the shared preferences.
     */
    public static final String SETTINGS_FILE_KEY =
            "au.id.fedorgabrus.feverjournal.models.SETTINGS_FILE_KEY";
    /**
     * Key for a treatment name in settings.
     */
    public static final String SETTINGS_TREATMENT_NAME_KEY = "TREATMENT_NAME";
    /**
     * Key for the maximum daily usage in settings.
     */
    public static final String SETTINGS_MAX_DAILY_USAGE_KEY = "MAX_DAILY_USAGE";
    /**
     * Key for the minimum interval between treatments in settings.
     */
    public static final String SETTINGS_MIN_TREATMENT_INTERVAL_KEY = "MIN_TREATMENT_INTERVAL";

    // Name for a new treatments from settings. Should not be null.
    private static String defaultName = null;
    // Maximum number of treatments allowed per 24 hours. Should be greater then 0.
    private static int maxDailyUsage = 0;
    // Minimum interval between treatments in hours. Should be greater then 0.
    private static int minTreatmentInterval = 0;

    /**
     * Getter for the treatment name from the settings.
     *
     * @return treatment name from settings.
     */
    public static String getDefaultName() {
        return defaultName;
    }

    /**
     * Getter for maximum number of treatments per 24 hour.
     *
     * @return maximum number of treatments per 24 hour.
     */
    public static int getMaxDailyUsage() {
        return maxDailyUsage;
    }

    /**
     * Getter for the minimum interval between treatments in hours.
     *
     * @return minimum interval between treatments in hours.
     */
    public static int getMinTreatmentInterval() {
        return minTreatmentInterval;
    }

    /**
     * Updates settings stored in the AppSettings class. Doesn't persist any data.
     *
     * @param defaultName new treatment name.
     * @param minTreatmentInterval  new min Treatment Interval.
     * @param maxDailyUsage new max daily usage.
     * @throws IllegalArgumentException if defaultName equals null, or either minTreatmentInterval
     *      or maxDailyUsage less or equal to zero.
     */
    public static void updateSettings(String defaultName, int minTreatmentInterval,
                                      int maxDailyUsage) throws IllegalArgumentException {
        if (defaultName == null || minTreatmentInterval <= 0 || maxDailyUsage <= 0) {
            throw new IllegalArgumentException();
        }
        AppSettings.defaultName = defaultName;
        AppSettings.minTreatmentInterval = minTreatmentInterval;
        AppSettings.maxDailyUsage = maxDailyUsage;
    }

    /**
     * Loads settings from a shared preferences.
     * <p>Name shouldn't be null, interval and daily usage should be greater than 0.</p>
     *
     * @param sharedPreferences data to load.
     *
     * @return true if data is present, false otherwise.
     */
    public static boolean loadSettings(SharedPreferences sharedPreferences) {
        // Returns false if any of settings is not in the preferences.
        if (sharedPreferences == null || !sharedPreferences.contains(SETTINGS_TREATMENT_NAME_KEY)
                || !sharedPreferences.contains(SETTINGS_MAX_DAILY_USAGE_KEY)
                || !sharedPreferences.contains(SETTINGS_MIN_TREATMENT_INTERVAL_KEY)) {
            Log.i("loadSettings()", "Shared preferences have no settings data.");
            return false;
        }
        // Retrieves data from shared preferences.
        defaultName = sharedPreferences.getString(SETTINGS_TREATMENT_NAME_KEY, null);
        minTreatmentInterval = sharedPreferences.getInt(SETTINGS_MIN_TREATMENT_INTERVAL_KEY,
                0);
        maxDailyUsage = sharedPreferences.getInt(SETTINGS_MAX_DAILY_USAGE_KEY, 0);
        // Validates retrieved data. Name shouldn't be null, interval and daily usage should
        // be greater than 0.
        return (defaultName != null) && (minTreatmentInterval > 0) && (maxDailyUsage > 0);
    }

    /**
     * Saves treatment settings into the shared preferences.
     *
     * @param sharedPreferences preferences that will hold the data.
     */
    public static void saveSettings(SharedPreferences sharedPreferences) {
        if (sharedPreferences == null) {
            Log.e("saveSettings()", "Provided sharedPreferences are null");
            return;
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SETTINGS_TREATMENT_NAME_KEY, defaultName);
        editor.putInt(SETTINGS_MIN_TREATMENT_INTERVAL_KEY, minTreatmentInterval);
        editor.putInt(SETTINGS_MAX_DAILY_USAGE_KEY, maxDailyUsage);
        editor.apply();
    }
}
