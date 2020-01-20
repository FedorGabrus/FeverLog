package au.id.fedorgabrus.feverlog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

import au.id.fedorgabrus.feverlog.DAO.TreatmentsDBHelper;
import au.id.fedorgabrus.feverlog.models.AppSettings;
import au.id.fedorgabrus.feverlog.models.FeverTreatment;
import au.id.fedorgabrus.feverlog.models.HistoryRecyclerViewAdapter;
import au.id.fedorgabrus.feverlog.models.TreatmentData;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // Request code to start settings activity.
    private static final int SET_SETTINGS_REQUEST = 1;
    // Request code for add custom treatment activity.
    private static final int ADD_CUSTOM_TREATMENT_REQUEST = 2;

    private TreatmentsDBHelper dbHelper = null;
    private SQLiteDatabase db = null;
    private NextTreatmentTimer timer = null;
    private TextView thisDayLimitTextView;
    private ProgressBar progressBar;
    private TextView progressTextView;
    private RecyclerView thisDayTreatmentsRecyclerView;
    private Button viewHistoryButton;
    private TextView noDataTextView;
    private MenuItem historyMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configures DB connection.
        dbHelper = new TreatmentsDBHelper(this);
        db = dbHelper.getWritableDatabase();
        TreatmentData.getInstance().loadFromHistory(dbHelper.loadHistoryFromDB(db));

        // Gets settings from the shared preferences.
        SharedPreferences treatmentSettings = this.getSharedPreferences(
                AppSettings.SETTINGS_FILE_KEY, Context.MODE_PRIVATE
        );
        // Opens settings activity if no settings in the treatmentSettings.
        if (!AppSettings.loadSettings(treatmentSettings)) {
            openSettingsActivity();
        }

        // Gets UI.
        progressBar = findViewById(R.id.progressBar);
        progressTextView = findViewById(R.id.progressTextView);
        thisDayLimitTextView = findViewById(R.id.thisDayLimitTextView);
        noDataTextView = findViewById(R.id.noDataTextView);
        thisDayTreatmentsRecyclerView = findViewById(R.id.thisDayTreatmentsRecyclerView);
        viewHistoryButton = findViewById(R.id.viewHistoryButton);
        final Button addTreatmentButton = findViewById(R.id.addTreatmentButton);
        final FloatingActionButton addNewFloatingActionButton =
                findViewById(R.id.addNewfloatingActionButton);
        setUpProgressBar();
        // Sets up recycler view.
        thisDayTreatmentsRecyclerView.setAdapter(new HistoryRecyclerViewAdapter(
                TreatmentData.getInstance().getTreatmentHistory(), false));
        thisDayTreatmentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                thisDayTreatmentsRecyclerView.getContext(), DividerItemDecoration.VERTICAL
        );
        thisDayTreatmentsRecyclerView.addItemDecoration(dividerItemDecoration);
        // Handler for view history button click.
        viewHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openHistoryActivity();
            }
        });
        // Handles add treatment button click.
        addTreatmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTreatmentIfCan();
            }
        });
        // Handles floating button click.
        addNewFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTreatmentIfCan();
            }
        });
    }

    @Override
    protected void onResume() {
        timer = startTimerIfNeeded();
        super.onResume();
    }

    @Override
    protected void onPause() {
        // Stops timer from running in background.
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // Closes DB.
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        // Saves reference to the history menu item.
        historyMenuItem = menu.findItem(R.id.historyMenuItem);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            // Case Add new.
            case R.id.addTreatmentHistoryMenuItem:
                addCustomTreatment();
                break;
            // Case Settings.
            case R.id.settingsMenuItem:
                openSettingsActivity();
                break;
            // Case History.
            case R.id.historyMenuItem:
                openHistoryActivity();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        // Disables history menu item if data model is empty.
        historyMenuItem.setEnabled(TreatmentData.getInstance().getDataSize() > 0);
        return super.onMenuOpened(featureId, menu);
    }

    /**
     * Handles SettingsActivity menu item click.
     * <p>Opens SettingsActivity activity.</p>
     */
    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, SET_SETTINGS_REQUEST);
    }

    /**
     * Starts new history activity.
     */
    private void openHistoryActivity() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            // Case result from Settings activity.
            case SET_SETTINGS_REQUEST:
                // If result OK, saves new settings to the shared preferences.
                if (resultCode == RESULT_OK && data != null) {
                    // Updates and saves new settings.
                    try {
                        AppSettings.updateSettings(
                                data.getStringExtra(AppSettings.SETTINGS_TREATMENT_NAME_KEY),
                                data.getIntExtra(AppSettings.SETTINGS_MIN_TREATMENT_INTERVAL_KEY, 0),
                                data.getIntExtra(AppSettings.SETTINGS_MAX_DAILY_USAGE_KEY, 0));
                    }
                    catch (IllegalArgumentException e) {
                        Log.e(
                                TAG,
                                "Tried to update application settings with illegal settings",
                                e
                        );
                        return;
                    }
                    setUpProgressBar();
                    SharedPreferences treatmentSettings = this.getSharedPreferences(
                            AppSettings.SETTINGS_FILE_KEY, Context.MODE_PRIVATE
                    );
                    AppSettings.saveSettings(treatmentSettings);
                    Toast toast = Toast.makeText(getBaseContext(), R.string.toast_settings_updated, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.BOTTOM, 0, 40);
                    toast.show();
                }
                break;

            // Case result from the create new activity.
            case ADD_CUSTOM_TREATMENT_REQUEST:
                if (resultCode == RESULT_OK && data != null) {
                    // Gets new treatment.
                    FeverTreatment newTreatment = (FeverTreatment) data
                            .getSerializableExtra(CustomTreatmentActivity.TREATMENT_INTENT_EXTRA_HEADER);
                    // Saves new treatment into the model and DB.
                    try {
                        int treatmentID = dbHelper.saveTreatmentIntoDB(
                                TreatmentData.getInstance().addCustomTreatment(newTreatment),
                                db
                        );
                        // Updates treatment's ID.
                        if (treatmentID == -1) {
                            Log.e(TAG, "New treatment wasn't saved to the DB.");
                        }
                        newTreatment.setId(treatmentID);

                        Objects.requireNonNull(thisDayTreatmentsRecyclerView.getAdapter())
                                .notifyDataSetChanged();
                        Toast.makeText(this, R.string.new_treatment_created, Toast.LENGTH_SHORT).show();
                    }
                    catch (IllegalArgumentException e) {
                        Log.e(TAG, "Newly received custom treatment is null");
                    }
                }
                break;
        }
    }

    /**
     * Checks if timer is running or if daily maximum will be exceeded. Asks for user confirmation
     * if needed before adding new treatment.
     */
    private void addTreatmentIfCan() {
        // Adds treatment if no need to ask for user confirmation.
        if ((timer == null)
                && (TreatmentData.getInstance().getTreatmentsNumber24h() < AppSettings.getMaxDailyUsage())) {
            addDefaultTreatment();
            return;
        }
        // Creates an alert dialog to get user confirmation if the method called when timer is still
        // running or new treatment will exceed the maximum daily limit from settings.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.alert_dialog_text)
                .setTitle(R.string.alert_dialog_title);
        builder.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Adds treatment if user pressed ok.
                addDefaultTreatment();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        // Builds and shows the confirmation dialog.
        builder.create().show();
    }

    /**
     * Adds default treatment to the data model and saves it into the DB.
     */
    private void addDefaultTreatment() {
        if (timer != null) {
            timer.cancel();
        }
        // Adds new treatment to the data model and saves it into the DB.
        FeverTreatment treatment = TreatmentData.getInstance().addNewDefaultTreatment();
        treatment.setId(dbHelper.saveTreatmentIntoDB(treatment, db));
        if (treatment.getId() == -1) {
            Log.e(TAG, "addDefaultTreatment(): Error saving to DB.");
        }
        timer = startTimerIfNeeded();
        Objects.requireNonNull(thisDayTreatmentsRecyclerView.getAdapter())
                .notifyDataSetChanged();
        Toast.makeText(this, R.string.new_treatment_created, Toast.LENGTH_SHORT).show();
    }

    /**
     * Starts activity to create custom treatment.
     */
    private void addCustomTreatment() {
        Intent intent = new Intent(this, CustomTreatmentActivity.class);
        // Flags new activity as for new treatment.
        intent.putExtra(CustomTreatmentActivity.PURPOSE_INTENT_EXTRA_HEADER,
                CustomTreatmentActivity.CREATE_NEW);
        startActivityForResult(intent, ADD_CUSTOM_TREATMENT_REQUEST);
    }

    /**
     * Sets the upper range of progress bar to be equal to minTreatmentInterval in seconds.
     * <p>Assumes that minTreatmentInterval is in hours.</p>
     */
    private void setUpProgressBar() {
        progressBar.setMax(AppSettings.getMinTreatmentInterval() * 60 * 60);
        progressBar.setProgress(0);
    }

    /**
     * Hides history button if data model is empty, hides recycler view if no treatments in the
     * past 24 hours.
     */
    private void updateHistoryCardUI() {
        viewHistoryButton.setVisibility(
                (TreatmentData.getInstance().getDataSize() > 0) ? View.VISIBLE : View.GONE
        );
        if (TreatmentData.getInstance().getTreatmentsNumber24h() > 0) {
            thisDayTreatmentsRecyclerView.setVisibility(View.VISIBLE);
            noDataTextView.setVisibility(View.GONE);
        }
        else {
            thisDayTreatmentsRecyclerView.setVisibility(View.GONE);
            noDataTextView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates text view that shows treatments number in last 24 hours.
     */
    @SuppressLint("DefaultLocale")
    private void updateDailyUsageTitle() {
        int currentUsage = TreatmentData.getInstance().getTreatmentsNumber24h();
        thisDayLimitTextView.setText(String.format("%d of %d treatments",
                currentUsage, AppSettings.getMaxDailyUsage()));
    }

    /**
     * Checks time till next treatment and if it is more than 1 second, starts countdown.
     *
     * @return timer if countdown started, null otherwise.
     */
    public NextTreatmentTimer startTimerIfNeeded() {
        updateDailyUsageTitle();
        updateHistoryCardUI();
        long millisInFuture = TreatmentData.getInstance().calculateTimeTillNextTreatmentAvailable();
        if ((millisInFuture / 1000) > 0) {
            NextTreatmentTimer timer = new NextTreatmentTimer(millisInFuture);
            timer.start();
            return timer;
        }
        progressBar.setProgress(0);
        progressTextView.setText(getString(R.string.progress_text_view));
        return null;
    }

    /**
     * Represents count down timer till the next treatment.
     */
    private class NextTreatmentTimer extends CountDownTimer {
        // Default countdown interval = 1 second.
        private static final long DEFAULT_COUNTDOWN_INTERVAL = 1000;

        private NextTreatmentTimer(long timerDuration) {
            super(timerDuration, DEFAULT_COUNTDOWN_INTERVAL);
        }

        /**
         * Updates progress bar and timer text on each count down.
         *
         * @param millisUntilFinished time till the end of the current timer.
         */
        @SuppressLint("DefaultLocale")
        @Override
        public void onTick(long millisUntilFinished) {
            // Progress for progress bar is an overall amount of seconds in millisUntilFinished.
            int overallSeconds = (int) (millisUntilFinished / 1000);
            progressBar.setProgress(overallSeconds, true);
            int hours = overallSeconds / 60 / 60;
            int minutes = overallSeconds / 60 - hours * 60;
            int seconds = overallSeconds - minutes * 60 - hours * 60 * 60;
            progressTextView.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
        }

        @Override
        public void onFinish() {
            // Update UI.
            progressBar.setProgress(0);
            progressTextView.setText(getString(R.string.progress_text_view));
            updateDailyUsageTitle();
            timer = null;
        }
    }
}
