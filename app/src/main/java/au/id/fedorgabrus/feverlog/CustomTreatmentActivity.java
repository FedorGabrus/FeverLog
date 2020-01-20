package au.id.fedorgabrus.feverlog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import au.id.fedorgabrus.feverlog.models.FeverTreatment;
import au.id.fedorgabrus.feverlog.models.TreatmentData;

public class CustomTreatmentActivity extends AppCompatActivity{
    /**
     * Used in intents to pass purpose of the new activity (Create new/Edit existed treatment).
     */
    public static final String PURPOSE_INTENT_EXTRA_HEADER = "PURPOSE_INTENT_EXTRA_HEADER";
    /**
     * Used to flag new activity as for creating new treatment.
     */
    public static final String CREATE_NEW = "CREATE_NEW";
    /**
     * Used to flag new activity as for editing existing treatment.
     */
    public static final String EDIT = "EDIT";
    /**
     * Used to store/extract treatment from intent.
     */
    public static final String TREATMENT_INTENT_EXTRA_HEADER = "TREATMENT_INTENT_EXTRA_HEADER";
    /**
     * Used to put/extract treatment index into/from intent.
     */
    public static final String TREATMENT_INDEX_EXTRA_HEADER = "TREATMENT_INDEX_EXTRA_HEADER";

    // Pattern for date in the date EditText view.
    private static final String DATE_FORMATTER_PATTERN = "dd/MM/yyy";
    // Pattern for time in the time EditText view.
    private static final String TIME_FORMATTER_PATTERN = "hh:mm a";
    private static final String TAG = "CustomTreatmentActivity";

    private static String activityPurpose = null;

    private EditText nameCustomTreatmentEditText;
    private EditText dateCustomTreatmentEditText;
    private EditText timeCustomTreatmentEditText;

    private int treatmentIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_treatment);

        Intent intent = getIntent();
        activityPurpose = intent.getStringExtra(PURPOSE_INTENT_EXTRA_HEADER);
        /*
         This activity should be started with either CREATE_NEW or EDIT goal.
         Closes activity, if no purpose is provided in the intent.
         */
        if (activityPurpose == null) {
            Log.e(TAG, "Activity started without specified purpose.");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        // Gets UI.
        final TextView customTreatmentHeaderTextView = findViewById(R.id.customTreatmentHeaderTextView);
        nameCustomTreatmentEditText = findViewById(R.id.nameCustomTreatmentEditText);
        dateCustomTreatmentEditText = findViewById(R.id.dateCustomTreatmentEditText);
        dateCustomTreatmentEditText.setInputType(EditorInfo.TYPE_NULL);
        timeCustomTreatmentEditText = findViewById(R.id.timeCustomTreatmentEditText);
        timeCustomTreatmentEditText.setInputType(EditorInfo.TYPE_NULL);
        final Button okCustomTreatmentButton = findViewById(R.id.okCustomTreatmentButton);
        final Button cancelCustomTreatmentButton = findViewById(R.id.cancelCustomTreatmentButton);

        // Adjusts UI and behaviour depending on current purpose of the activity.
        if (activityPurpose.equals(CREATE_NEW)) {
            customTreatmentHeaderTextView.setText(R.string.new_header_custom_treatment);
            okCustomTreatmentButton.setText(R.string.create_custom_treatment);
        }
        else if (activityPurpose.equals(EDIT)) {
            customTreatmentHeaderTextView.setText(R.string.edit_header_custom_treatment);
            okCustomTreatmentButton.setText(R.string.update_custom_treatment);
            // Gets treatment index from intent.
            treatmentIndex = intent.getIntExtra(TREATMENT_INDEX_EXTRA_HEADER, -1);
            if (treatmentIndex == -1) {
                Log.e(TAG, "No treatment index was received with intent.");
                finish();
                return;
            }
            // Gets treatment to edit.
            FeverTreatment treatmentToEdit = TreatmentData.getInstance().getTreatment(treatmentIndex);
            if (treatmentToEdit == null) {
                Log.e(TAG, "No treatment with provided by intent index in the data model.");
                finish();
                return;
            }
            populateInterfaceTreatment(treatmentToEdit);
        }
        
        // Handles ok button click.
        okCustomTreatmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndSaveData();
            }
        });

        // Handles Cancel button click. Ends activity with canceled result.
        cancelCustomTreatmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        /*
        Handles selection of time input field. Uses two event listeners as when text view is empty,
        first click is received by a TextInputLayout.
         */
        timeCustomTreatmentEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    startTimePickerDialog();
                }
            }
        });
        timeCustomTreatmentEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimePickerDialog();
            }
        });

        // Handles date selection.
        dateCustomTreatmentEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    startDatePickerDialog();
                }
            }
        });
        dateCustomTreatmentEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDatePickerDialog();
            }
        });
    }

    /**
     * Populates UI with data from the provided treatment.
     *
     * @param treatment treatment object to edit.
     */
    private void populateInterfaceTreatment(FeverTreatment treatment) {
        if (treatment == null) {
            Log.d(TAG, "populateInterfaceTreatment(FeverTreatment treatment): null argument");
            return;
        }
        nameCustomTreatmentEditText.setText(treatment.getTreatmentName());
        dateCustomTreatmentEditText.setText(treatment.getTreatmentTime().toLocalDate()
                .format(DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN)));
        timeCustomTreatmentEditText.setText(treatment.getTreatmentTime().toLocalTime()
                .format(DateTimeFormatter.ofPattern(TIME_FORMATTER_PATTERN)));
    }

    /**
     * Starts time picker dialog.
     */
    private void startTimePickerDialog() {
        DialogFragment fragment = new TimePickerFragment();
        fragment.show(getSupportFragmentManager(), "timePicker");
    }

    /**
     * Starts date picker dialog.
     */
    private void startDatePickerDialog() {
        DialogFragment fragment = new DatePickerFragment();
        fragment.show(getSupportFragmentManager(), "datePicker");
    }

    /**
     * Checks if input fields are not empty and ends activity with passing new data as a OK result.
     */
    private void validateAndSaveData() {
        String name = nameCustomTreatmentEditText.getText().toString().trim(); 
        if (name.isEmpty()) {
            nameCustomTreatmentEditText.requestFocus();
            nameCustomTreatmentEditText.selectAll();
            Toast.makeText(this, R.string.empty_name_field, Toast.LENGTH_SHORT).show();
            return;
        }
        String dateString = dateCustomTreatmentEditText.getText().toString().trim();
        if (dateString.isEmpty()) {
            Toast.makeText(this, R.string.empty_date_field, Toast.LENGTH_SHORT).show();
            // Triggers OnFocusChangeListener.
            dateCustomTreatmentEditText.clearFocus();
            dateCustomTreatmentEditText.requestFocus();
            return;
        }
        String timeString = timeCustomTreatmentEditText.getText().toString().trim();
        if (timeString.isEmpty()) {
            Toast.makeText(this, R.string.empty_time_field, Toast.LENGTH_SHORT).show();
            // Triggers OnFocusChangeListener.
            timeCustomTreatmentEditText.clearFocus();
            timeCustomTreatmentEditText.requestFocus();
            return;
        }
        final LocalDateTime dateTime = LocalDateTime.parse(dateString + timeString,
                DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN + TIME_FORMATTER_PATTERN));
        Intent intent = new Intent();
        // Creates new treatment.
        FeverTreatment treatment;
        if (activityPurpose.equals(CREATE_NEW)) {
            treatment = new FeverTreatment(dateTime, name);
            intent.putExtra(TREATMENT_INTENT_EXTRA_HEADER, treatment);
        }
        // Updates existed treatment.
        else if (activityPurpose.equals(EDIT)) {
            treatment = TreatmentData.getInstance().getTreatment(treatmentIndex);
            if (treatment == null) {
                Log.e(TAG, "validateAndSaveData(): invalid treatmentIndex value");
                finish();
                return;
            }
            // Finishes activity as cancelled if no value has changed.
            if (treatment.getTreatmentName().equals(name) && treatment.getTreatmentTime().isEqual(dateTime)) {
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
            intent.putExtra(TREATMENT_INDEX_EXTRA_HEADER, treatmentIndex);
            treatment.setTreatmentName(name);
            treatment.setTreatmentTime(dateTime);
        }
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Time picker dialog.
     */
    public static class TimePickerFragment
            extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        private EditText timeEditText;

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            timeEditText = Objects.requireNonNull(getActivity())
                    .findViewById(R.id.timeCustomTreatmentEditText);
            // Uses treatment time if purpose is edit as a start ones.
            LocalTime time;
            if (activityPurpose.equals(EDIT)) {
                time = LocalTime.parse(timeEditText.getText().toString(),
                        DateTimeFormatter.ofPattern(TIME_FORMATTER_PATTERN));
            }
            else {
                time = LocalTime.now();
            }
            int hour = time.getHour();
            int minute = time.getMinute();

            return new TimePickerDialog(getActivity(), this, hour, minute, false);
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // Puts result into the ExtEdit view.
            LocalTime time = LocalTime.of(hourOfDay, minute);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMATTER_PATTERN);
            timeEditText.setText(time.format(formatter));
        }

        @Override
        public void onCancel(@NonNull DialogInterface dialog) {
            // Removes focus if no time was selected.
            timeEditText.clearFocus();
            super.onCancel(dialog);
        }
    }

    /**
     * Date picker dialog.
     */
    public static class DatePickerFragment
            extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        private EditText dateCustomTreatmentEditText;

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            dateCustomTreatmentEditText = Objects.requireNonNull(getActivity())
                    .findViewById(R.id.dateCustomTreatmentEditText);
            // Uses treatment time as a start time if purpose is edit.
            LocalDate date;
            if (activityPurpose.equals(EDIT)) {
                date = LocalDate.parse(dateCustomTreatmentEditText.getText().toString(),
                        DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN));
            }
            else {
                date = LocalDate.now();
            }
            int year = date.getYear();
            int month = date.getMonthValue() - 1; // DatePickerDialog constructor accepts values 0-11.
            int dayOfMonth = date.getDayOfMonth();

            DatePickerDialog dialog = new DatePickerDialog(getActivity(), this,
                    year, month, dayOfMonth);
            // Sets current date as a latest date available in the picker.
            dialog.getDatePicker().setMaxDate(new Date().getTime());
            return dialog;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            // Sets formatted result into the date textEdit view.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMATTER_PATTERN);
            LocalDate date = LocalDate.of(year, (month + 1), dayOfMonth); // month is in range 0-11.
            dateCustomTreatmentEditText.setText(date.format(formatter));
        }

        @Override
        public void onCancel(@NonNull DialogInterface dialog) {
            // Removes focus from the edit text view.
            dateCustomTreatmentEditText.clearFocus();
            super.onCancel(dialog);
        }
    }
}
