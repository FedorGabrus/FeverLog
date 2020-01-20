package au.id.fedorgabrus.feverlog.models;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.List;

import au.id.fedorgabrus.feverlog.R;

/**
 * Adapter for treatment history data.
 */
public class HistoryRecyclerViewAdapter
        extends RecyclerView.Adapter<HistoryRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "HistoryRecyclerViewAdap";
    private List<FeverTreatment> data;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy hh:mm a");
    // When this flag is true -> adapter shows all data from the data model,
    // When false -> only for the past 24 hours.
    private boolean showAllDataFromModel;

    /**
     * Constructor for the adapter.
     *
     * @param data List with data.
     * @param showAllDataFromModel if true shows all values from the provided data model,
     *                             if false shows treatments for the last 24 hours.
     */
    public HistoryRecyclerViewAdapter(List<FeverTreatment> data, boolean showAllDataFromModel) {
        this.data = data;
        this.showAllDataFromModel = showAllDataFromModel;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_treatment_rv_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemNumberRVItemTextView.setText(String.valueOf(position + 1));
        FeverTreatment treatment = data.get(position);
        holder.treatmentTimeRVItemTextView.setText(treatment.getTreatmentTime()
                .format(dateTimeFormatter).toUpperCase());
        holder.treatmentNameRVItemTextView.setText(treatment.getTreatmentName());
    }

    @Override
    public int getItemCount() {
        if (showAllDataFromModel) {
            return TreatmentData.getInstance().getDataSize();
        }
        return TreatmentData.getInstance().getTreatmentsNumber24h();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemNumberRVItemTextView;
        TextView treatmentTimeRVItemTextView;
        TextView treatmentNameRVItemTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemNumberRVItemTextView = itemView.findViewById(R.id.itemNumberRVItemTextView);
            treatmentTimeRVItemTextView = itemView.findViewById(R.id.treatmentTimeRVItemTextView);
            treatmentNameRVItemTextView = itemView.findViewById(R.id.treatmentNameRVItemTextView);
        }
    }
}
