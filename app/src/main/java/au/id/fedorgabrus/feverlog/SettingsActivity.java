package au.id.fedorgabrus.feverlog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import au.id.fedorgabrus.feverlog.R;
import au.id.fedorgabrus.feverlog.models.AppSettings;

public class SettingsActivity extends AppCompatActivity {
    private EditText nameSettingsEditText;
    private EditText intervalSettingsEditText;
    private EditText maxDailySettingsEditText;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Gets user's interface.
        nameSettingsEditText = findViewById(R.id.nameSettingsEditText);
        intervalSettingsEditText = findViewById(R.id.intervalSettingsEditText);

        maxDailySettingsEditText = findViewById(R.id.maxDailySettingsEditText);
        final Button saveSettingsButton = findViewById(R.id.saveSettingsButton);
        final Button cancelSettingsButton = findViewById(R.id.cancelSettingsButton);

        // Disables cancel button if no settings were loaded from the shared preferences. Prevents
        // from going to the main activity without proper settings. Checks for default values
        // assigned in loadSettings() method in FeverTreatment class.
        if ((AppSettings.getDefaultName() == null)
                || (AppSettings.getMinTreatmentInterval() == 0)
                || (AppSettings.getMaxDailyUsage() == 0)) {
            cancelSettingsButton.setEnabled(false);
        }
        // Else populates user interface with the current settings.
        else {
            nameSettingsEditText.setText(AppSettings.getDefaultName());
            intervalSettingsEditText.setText(Integer.toString(AppSettings.getMinTreatmentInterval()));
            maxDailySettingsEditText.setText(Integer.toString(AppSettings.getMaxDailyUsage()));
        }

        // Handles cancel button click: closes activity with canceled result.
        cancelSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        // Handles save button click.
        saveSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateUpdateSettings();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Validates and saves new settings if save menu item clicked.
        if (item.getItemId() == R.id.saveSettingsMenuItem) {
            validateUpdateSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Validates user interface, if new data is valid and has new values, closes activity with OK
     * code and sends new settings to the parent activity. If new settings are the same as
     * current, closes activity as canceled.
     * <p>Treatment name should not be blank.</p>
     * <p>Min break and max daily usage should be greater then 0.</p>
     */
    private void validateUpdateSettings() {
        // New name should not be null or empty string.
        String treatmentName = nameSettingsEditText.getText().toString().trim();
        if (treatmentName.isEmpty()) {
            Toast.makeText(this, "Blank treatment name", Toast.LENGTH_SHORT).show();
            nameSettingsEditText.requestFocus();
            nameSettingsEditText.setText("");
            return;
        }
        // Usage interval should be greater then 0.
        String intervalString = intervalSettingsEditText.getText().toString().trim();
        if (intervalString.isEmpty()) {
            Toast.makeText(this, "Blank Min Break", Toast.LENGTH_SHORT).show();
            intervalSettingsEditText.requestFocus();
            return;
        }
        int interval = Integer.parseInt(intervalString);
        if (interval <= 0) {
            Toast.makeText(this, "Min Break should be greater then 0",
                    Toast.LENGTH_SHORT).show();
            intervalSettingsEditText.requestFocus();
            intervalSettingsEditText.selectAll();
            return;
        }
        // Max daily usage should be greater then 0.
        String dailyUsageString = maxDailySettingsEditText.getText().toString().trim();
        if (dailyUsageString.isEmpty()) {
            Toast.makeText(this, "Blank Max Daily Usage", Toast.LENGTH_SHORT).show();
            maxDailySettingsEditText.requestFocus();
            return;
        }
        int dailyUsage = Integer.parseInt(dailyUsageString);
        if (dailyUsage <= 0) {
            Toast.makeText(this, "Max Daily Usage should be greater then 0",
                    Toast.LENGTH_SHORT).show();
            maxDailySettingsEditText.requestFocus();
            maxDailySettingsEditText.selectAll();
            return;
        }
        // Sets result OK and sends new settings to the parent activity only if new settings
        // are differ from the current ones.
        if (!treatmentName.equals(AppSettings.getDefaultName())
                || interval != AppSettings.getMinTreatmentInterval()
                || dailyUsage != AppSettings.getMaxDailyUsage()) {
            Intent intent = new Intent();
            intent.putExtra(AppSettings.SETTINGS_TREATMENT_NAME_KEY, treatmentName);
            intent.putExtra(AppSettings.SETTINGS_MIN_TREATMENT_INTERVAL_KEY, interval);
            intent.putExtra(AppSettings.SETTINGS_MAX_DAILY_USAGE_KEY, dailyUsage);
            setResult(RESULT_OK, intent);
            finish();
        }
        // Returns cancel if no new settings received.
        else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
