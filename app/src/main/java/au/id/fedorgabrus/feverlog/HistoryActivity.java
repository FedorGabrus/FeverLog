package au.id.fedorgabrus.feverlog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import au.id.fedorgabrus.feverlog.DAO.TreatmentsDBHelper;
import au.id.fedorgabrus.feverlog.models.FeverTreatment;
import au.id.fedorgabrus.feverlog.models.HistoryRecyclerViewAdapter;
import au.id.fedorgabrus.feverlog.models.TreatmentData;

public class HistoryActivity extends AppCompatActivity {
    private static final String TAG = "HistoryActivity";
    // Used to distinguish result from create new treatment activity.
    private static final int ADD_CUSTOM_TREATMENT_REQUEST = 1;
    // Request code to edit treatment.
    private static final int EDIT_TREATMENT_REQUEST = 2;

    private TreatmentsDBHelper dbHelper = null;
    private static SQLiteDatabase db = null;
    private TextView noHistoryTextView;
    private RecyclerView allHistoryRecyclerView;
    private MenuItem clearHistoryMenuItem;
    private FloatingActionButton clearFloatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Connects to the DB.
        dbHelper = new TreatmentsDBHelper(this);
        new ConnectToDB().execute(dbHelper);

        noHistoryTextView = findViewById(R.id.noHistoryTextView);
        allHistoryRecyclerView = findViewById(R.id.allHistoryRecyclerView);
        clearFloatingActionButton = findViewById(R.id.clearFloatingActionButton);
        updateUI();

        // Sets up recycler view.
        allHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        allHistoryRecyclerView.setAdapter(new HistoryRecyclerViewAdapter(
                TreatmentData.getInstance().getTreatmentHistory(),
                true
        ));
        allHistoryRecyclerView.addItemDecoration(new DividerItemDecoration(
                allHistoryRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL
        ));

        // Handles floating clear button click.
        clearFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearHistory();
            }
        });

        // Creates helper to handle swipe right to delete and swipe left to edit.
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new OnItemSwipeHandler());
        itemTouchHelper.attachToRecyclerView(allHistoryRecyclerView);
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
    protected void onPause() {
        // Saves deletion if transaction isn't finalized.
        if (db.inTransaction()) {
            db.setTransactionSuccessful();
            db.endTransaction();
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.history_menu, menu);
        clearHistoryMenuItem = menu.findItem(R.id.clearHistoryMenuItem);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            // Case clear history.
            case R.id.clearHistoryMenuItem:
                clearHistory();
                break;
            // Case Add new treatment.
            case R.id.addTreatmentHistoryMenuItem:
                addCustomTreatment();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        // Disables clear history menu item if no data in the model.
        clearHistoryMenuItem.setEnabled(TreatmentData.getInstance().getDataSize() > 0);
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
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

                        Objects.requireNonNull(allHistoryRecyclerView.getAdapter())
                                .notifyDataSetChanged();
                        updateUI();
                        Toast.makeText(this, R.string.new_treatment_created, Toast.LENGTH_SHORT)
                                .show();
                    }
                    catch (IllegalArgumentException e) {
                        Log.e(TAG, "Newly received custom treatment is null.");
                    }
                }
                break;

            // Case result from editing activity.
            case EDIT_TREATMENT_REQUEST:
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        Log.e(TAG, "onActivityResult EDIT_TREATMENT_REQUEST has no attached data");
                        return;
                    }
                    int treatmentIndex = data.getIntExtra(CustomTreatmentActivity.TREATMENT_INDEX_EXTRA_HEADER, -1);
                    if (treatmentIndex < 0 || treatmentIndex >= TreatmentData.getInstance().getDataSize()) {
                        Log.e(TAG, "onActivityResult EDIT_TREATMENT_REQUEST: Returned index is out of boundaries");
                        return;
                    }
                    // Updates DB.
                    FeverTreatment treatment = TreatmentData.getInstance().getTreatment(treatmentIndex);
                    if (treatment.getId() == FeverTreatment.DEFAULT_ID) {
                        Log.e(TAG, "onActivityResult EDIT_TREATMENT_REQUEST: Treatment object has no ID. Can't update DB");
                        return;
                    }
                    if (dbHelper.updateTreatment(db, treatment) != 1) {
                        Log.e(TAG, "onActivityResult EDIT_TREATMENT_REQUEST: Update DB - error");
                    }
                    // Updates UI.
                    TreatmentData.getInstance().sortHistoryByDate();
                    Objects.requireNonNull(allHistoryRecyclerView.getAdapter()).notifyDataSetChanged();
                }
        }
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
     * Asks user for confirmation to clear all data and proceed if permitted.
     */
    private void clearHistory() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.delete_history_dialog_title)
                .setMessage(R.string.delete_history_dialog_message)
                .setPositiveButton(R.string.delete_history_dialog_ok_button,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.clearHistoryDB(db);
                        Objects.requireNonNull(allHistoryRecyclerView.getAdapter()).notifyDataSetChanged();
                        updateUI();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        // Creates and shows the dialog.
        builder.create().show();
    }

    /**
     * Updates UI by hiding elements if no data in the data model and vise versa.
     */
    @SuppressLint("RestrictedApi")
    private void updateUI() {
        if (TreatmentData.getInstance().getDataSize() > 0) {
            noHistoryTextView.setVisibility(View.GONE);
            allHistoryRecyclerView.setVisibility(View.VISIBLE);
            clearFloatingActionButton.setEnabled(true);
            clearFloatingActionButton.setVisibility(View.VISIBLE);
        }
        else {
            noHistoryTextView.setVisibility(View.VISIBLE);
            allHistoryRecyclerView.setVisibility(View.GONE);
            clearFloatingActionButton.setEnabled(false);
            clearFloatingActionButton.setVisibility(View.GONE);
        }
    }

    /**
     * Connects to the database.
     */
    private static class ConnectToDB extends AsyncTask<SQLiteOpenHelper, Void, SQLiteDatabase> {

        @Override
        protected SQLiteDatabase doInBackground(SQLiteOpenHelper... sqLiteOpenHelpers) {
            if ((sqLiteOpenHelpers.length == 0) || (sqLiteOpenHelpers[0] == null)) {
                return null;
            }
            return sqLiteOpenHelpers[0].getWritableDatabase();
        }

        @Override
        protected void onPostExecute(SQLiteDatabase sqLiteDatabase) {
            db = sqLiteDatabase;
            super.onPostExecute(sqLiteDatabase);
        }
    }

    /**
     * Starts a new activity to edit selected treatment.
     *
     * @param treatmentIndex index of the treatment in the data model.
     * @throws IllegalArgumentException if index value is less than 0.
     */
    private void editTreatment(int treatmentIndex) throws IllegalArgumentException {
        if (treatmentIndex < 0) {
            throw new IllegalArgumentException();
        }
        Intent intent = new Intent(this, CustomTreatmentActivity.class);
        // Flags new activity for editing purpose.
        intent.putExtra(CustomTreatmentActivity.PURPOSE_INTENT_EXTRA_HEADER, CustomTreatmentActivity.EDIT);
        intent.putExtra(CustomTreatmentActivity.TREATMENT_INDEX_EXTRA_HEADER, treatmentIndex);
        startActivityForResult(intent, EDIT_TREATMENT_REQUEST);
    }

    /**
     * Deletes specified treatment from the data model and DB.
     * <p>Shows snackbar that allows to undo deletion</p>
     *
     * @param treatmentIndex index of the treatment in the data model to delete.
     * @throws IllegalArgumentException if index value is out of boundaries.
     */
    private void deleteTreatment(int treatmentIndex) throws IllegalArgumentException {
        if (treatmentIndex < 0 || treatmentIndex >= TreatmentData.getInstance().getDataSize()) {
            throw new IllegalArgumentException();
        }
        final FeverTreatment treatment = TreatmentData.getInstance().getTreatment(treatmentIndex);
        if (treatment.getId() == FeverTreatment.DEFAULT_ID) {
            Log.e(TAG, "deleteTreatment(): Treatment has no ID, can't delete from the DB");
        }
        // Deletes data from the data model.
        TreatmentData.getInstance().deleteTreatment(treatment);
        // Deletes data from the DB.
        db.beginTransaction();
        if (dbHelper.deleteTreatment(db, treatment) != 1) {
            Log.e(TAG, "deleteTreatment(): Deletion failed. DB error");
        }
        allHistoryRecyclerView.getAdapter().notifyDataSetChanged();

        // Transaction finishes when snackbar disappears.
        // Snackbar.
        final Snackbar snackbar = Snackbar.make(allHistoryRecyclerView, R.string.deleted, Snackbar.LENGTH_LONG);
        // Undo deletion.
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (db.inTransaction()) {
                    db.endTransaction();
                }
                else {
                    Log.e(TAG, "deleteTreatment() Snackbar onClick: Current thread should" +
                            " have pending transaction, but it have not.");
                }
                TreatmentData.getInstance().addCustomTreatment(treatment);
                allHistoryRecyclerView.getAdapter().notifyDataSetChanged();
            }
        });
        snackbar.addCallback(new Snackbar.Callback() {

            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                super.onDismissed(transientBottomBar, event);
                // Commits changes if they weren't cancelled.
                if (db.inTransaction()) {
                    db.setTransactionSuccessful();
                    db.endTransaction();
                }
            }
        });
        snackbar.show();
    }

    /**
     * Handles on swipe left and right actions for the Recycler view items.
     */
    private class OnItemSwipeHandler extends ItemTouchHelper.SimpleCallback {

        // Constructor to handle swipes to left and right.
        OnItemSwipeHandler() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        // Paint object to decorate swipe actions.
        private final Paint paint = new Paint();

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c,
                                @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            // Draws background, icon and text under the swiped items.
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                final View v = viewHolder.itemView;
                final int textSize = 42;
                final int xPadding = 32;
                final int xMargin = 16;

                // On swipe to right (EDIT).
                if (dX > 0) {
                    // Sets background.
                    paint.setColor(Color.argb(250, 255, 140, 0));
                    c.drawRect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom(), paint);

                    // Sets Icon.
                    Drawable drawable = getDrawable(R.drawable.ic_edit_white_24dp);
                    assert drawable != null;
                    int drawableLeft = v.getLeft() + xPadding;
                    int drawableRight = drawableLeft + drawable.getIntrinsicWidth();
                    int drawableBottom = v.getBottom() - v.getHeight() / 2 + drawable.getIntrinsicHeight() / 2;
                    int drawableTop = drawableBottom - drawable.getIntrinsicHeight();
                    drawable.setBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
                    drawable.draw(c);

                    // Sets text.
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(textSize);
                    c.drawText(
                            getString(R.string.edit),
                            xPadding + drawable.getIntrinsicWidth() + xMargin,
                            v.getBottom() - v.getHeight() / 2f + textSize / 2f,
                            paint
                    );
                }
                // On swipe to left (DELETE).
                else {
                    // Sets background.
                    paint.setColor(Color.argb(250, 235, 64, 52));
                    c.drawRect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom(), paint);

                    // Sets Icon.
                    Drawable drawable = getDrawable(R.drawable.ic_delete_white_24dp);
                    assert drawable != null;
                    int drawableRight = v.getRight() - xPadding;
                    int drawableLeft = drawableRight - drawable.getIntrinsicWidth();
                    int drawableBottom = v.getBottom() - v.getHeight() / 2 + drawable.getIntrinsicHeight() / 2;
                    int drawableTop = drawableBottom - drawable.getIntrinsicHeight();
                    drawable.setBounds(drawableLeft, drawableTop, drawableRight, drawableBottom);
                    drawable.draw(c);

                    // Sets text.
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.WHITE);
                    paint.setTextSize(textSize);

                    c.drawText(
                            getString(R.string.delete),
                            (v.getRight() - xPadding - drawable.getIntrinsicWidth() - xMargin
                                    - paint.measureText(getString(R.string.delete))),
                            v.getBottom() - v.getHeight() / 2f + textSize / 2f,
                            paint
                    );
                }
            }
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                             int direction) {
            int rowIndex = viewHolder.getAdapterPosition();
            switch (direction) {
                // Edition.
                case ItemTouchHelper.RIGHT:
                    editTreatment(rowIndex);
                    break;

                // Deletion.
                case ItemTouchHelper.LEFT:
                    deleteTreatment(rowIndex);
                    break;
            }
            // Removes swipe decorations.
            Objects.requireNonNull(allHistoryRecyclerView.getAdapter()).notifyItemChanged(rowIndex);
        }
    }
}
