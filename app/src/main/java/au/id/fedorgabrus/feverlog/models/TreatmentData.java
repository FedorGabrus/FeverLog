package au.id.fedorgabrus.feverlog.models;

import android.util.Log;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


/**
 * Data model for fever treatments.
 *
 * <p>Realized as a singleton.</p>
 */
public class TreatmentData {
    private static final String TAG = "TreatmentData";

    private static final TreatmentData instance = new TreatmentData();

    private final LinkedList<FeverTreatment> treatmentsHistory;

    // Private constructor.
    private TreatmentData() {
        treatmentsHistory = new LinkedList<>();
    }

    /**
     * Getter for the data model instance.
     *
     * @return treatment data model.
     */
    public static TreatmentData getInstance() {
        return instance;
    }

    /**
     * Adds new treatment to the beginning of the list.
     * <p>Uses default name from settings and current date & time to create a new treatment</p>
     *
     * @return treatment that was added to the data model.
     */
    public FeverTreatment addNewDefaultTreatment() {
        FeverTreatment treatment = new FeverTreatment(LocalDateTime.now(), AppSettings.getDefaultName());
        treatmentsHistory.addFirst(treatment);
        return treatment;
    }

    /**
     * Adds provided custom treatment to the data model. Doesn't persist data.
     * <p>Sorts data model by treatment date & time in descending order</p>
     *
     * @param treatment treatment to add to the model.
     * @return treatment that was added to the model.
     * @throws IllegalArgumentException if provided treatment equals to null.
     */
    public FeverTreatment addCustomTreatment(FeverTreatment treatment) throws IllegalArgumentException {
        if (treatment == null) {
            throw new IllegalArgumentException("Adding null to the data model.");
        }
        treatmentsHistory.addFirst(treatment);
        treatmentsHistory.sort(new DescendingTimeTreatmentsComparator());
        return treatment;
    }

    /**
     * Replaces history data with the newly provided.
     *
     * @param history Set with a treatment history to use in the data model.
     */
    public void loadFromHistory(List<FeverTreatment> history) {
        if ((history != null) && (history.size() > 0)) {
            treatmentsHistory.clear();
            treatmentsHistory.addAll(history);
        }
    }

    /**
     * Calculates time in milliseconds till the next treatment becomes available.
     *
     * @return number of milliseconds till the next treatment becomes available, 0 if no need to wait.
     */
    public long calculateTimeTillNextTreatmentAvailable() {
        // Returns 0 if history is empty.
        if (treatmentsHistory.size() == 0) {
            return 0;
        }
        int thisDayTreatmentsNumber = getTreatmentsNumber24h();
        // Finds lastTreatment + minTreatmentInterval.
        LocalDateTime nextTreatmentAvailableAt =
                treatmentsHistory.getFirst().getTreatmentTime().plusHours(AppSettings.getMinTreatmentInterval());
        // If max daily usage is not exceeded returns duration between nextTreatmentAvailableAt and now.
        LocalDateTime now = LocalDateTime.now();
        if (thisDayTreatmentsNumber < AppSettings.getMaxDailyUsage()) {
            // No duration if now is greater then nextTreatmentAvailableAt
            if (now.compareTo(nextTreatmentAvailableAt) >= 0) {
                return 0;
            }
            return Duration.between(now, nextTreatmentAvailableAt).getSeconds() * 1000;
        }
        // If max daily usage exceeded.
        // Gets duration between limit for this day + 24h.
        LocalDateTime firstDayTreatmentTimePlus24 =
                treatmentsHistory.get(AppSettings.getMaxDailyUsage() - 1).getTreatmentTime().plusHours(24);
        // Picks the latest date.
        if (nextTreatmentAvailableAt.compareTo(firstDayTreatmentTimePlus24) < 0) {
            nextTreatmentAvailableAt = firstDayTreatmentTimePlus24;
        }
        if (now.compareTo(nextTreatmentAvailableAt) >= 0) {
            return 0;
        }
        return Duration.between(now, nextTreatmentAvailableAt).getSeconds() * 1000;
    }

    /**
     * Counts number of treatments used in the last 24 hours.
     *
     * @return number of treatments in the last 24 hours.
     */
    public int getTreatmentsNumber24h() {
        // Returns 0 if no treatments in the history.
        if (treatmentsHistory.size() == 0) {
            return 0;
        }
        // Gets date and time 24 hours ago.
        LocalDateTime time24hAgo = LocalDateTime.now().minusHours(24);
        int result = 0;
        // Counts all treatments that have been applied in the past 24 hours.
        for (FeverTreatment treatment : treatmentsHistory) {
            if (treatment.getTreatmentTime().compareTo(time24hAgo) >= 0) {
                result++;
            }
            // Treatments stored in descending time order.
            else {
                break;
            }
        }
        return result;
    }

    /**
     * Returns list of treatments.
     *
     * @return unmodifiable list of treatments.
     */
    public List<FeverTreatment> getTreatmentHistory() {
        return Collections.unmodifiableList(treatmentsHistory);
    }

    /**
     * Returns size of treatment history.
     *
     * @return size of treatment history.
     */
    public int getDataSize() {
        return treatmentsHistory.size();
    }

    /**
     * Clears data model.
     */
    public void clearHistory() {
        treatmentsHistory.clear();
    }

    /**
     * Gets treatment by its index from the history.
     *
     * @param index index of the treatment
     * @return treatment with the specified index or null if index is out of boundaries.
     */
    public FeverTreatment getTreatment(int index) {
        // Handles unacceptable index values.
        if (index < 0 || index >= treatmentsHistory.size()) {
            Log.e(TAG, "getTreatment(): index out of boundaries.");
            return null;
        }
        return treatmentsHistory.get(index);
    }

    /**
     * Sorts data model by date and time (descending).
     */
    public void sortHistoryByDate() {
        treatmentsHistory.sort(new DescendingTimeTreatmentsComparator());
    }

    /**
     * Deletes treatment from the data model. Doesn't persist changes.
     * @param treatment treatment to delete
     */
    public void deleteTreatment(FeverTreatment treatment) {
        if (treatment == null) {
            Log.e(TAG, "deleteTreatment(FeverTreatment treatment): null argument");
            return;
        }
        treatmentsHistory.remove(treatment);
    }

    /**
     * Comparator to sort fever treatments by usage time in descending order.
     */
    private class DescendingTimeTreatmentsComparator implements Comparator<FeverTreatment> {
        @Override
        public int compare(FeverTreatment o1, FeverTreatment o2) {
            // Both objects shouldn't be null.
            if (o1 == null || o2 == null) {
                throw new IllegalArgumentException();
            }
            // Case same object.
            if (o1 == o2) {
                return 0;
            }
            // Compare by date in descending order.
            int result = o1.getTreatmentTime().compareTo(o2.getTreatmentTime());
            if (result != 0) {
                // Returns the opposite number.
                return -result;
            }
            // Compares names in case if dates are equal.
            result = o1.getTreatmentName().compareTo(o2.getTreatmentName());
            if (result != 0) {
                return result;
            }
            return o1.getId() - o2.getId();
        }
    }
}
