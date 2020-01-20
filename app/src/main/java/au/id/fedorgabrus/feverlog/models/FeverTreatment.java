package au.id.fedorgabrus.feverlog.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents intake of a fever treatment.
 */
public final class FeverTreatment implements Serializable {
    /**
     * Used when instance wasn't retrieved from the DB.
     */
    public static final int DEFAULT_ID = -1;

    private static long serialVersionUID = 15L;
    // Id from DB.
    private int id;
    // Date and time of the treatment usage.
    private LocalDateTime treatmentTime;
    // Current treatment name.
    private String treatmentName;

    /**
     * All args constructor.
     *
     * @param id treatment id from the DB.
     * @param treatmentTime date and time of a usage.
     * @param treatmentName name for a treatment.
     */
    public FeverTreatment(int id, LocalDateTime treatmentTime, String treatmentName) {
        this.id = id;
        this.treatmentTime = treatmentTime;
        this.treatmentName = treatmentName;
    }

    /**
     * Constructor for treatment that wasn't fetched from db. Uses default id.
     *
     * @param treatmentTime time of the treatment.
     * @param treatmentName name of the treatment.
     */
    public FeverTreatment(LocalDateTime treatmentTime, String treatmentName) {
        this(DEFAULT_ID, treatmentTime, treatmentName);
    }

    /**
     * Getter fot the treatment id.
     *
     * @return if of the treatment.
     */
    public int getId() {
        return id;
    }

    /**
     * Updates ID.
     *
     * @param id new ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Getter for treatment date and time.
     *
     * @return date and time of the treatment.
     */
    public LocalDateTime getTreatmentTime() {
        return treatmentTime;
    }

    /**
     * Setter for treatment date and time.
     *
     * @param treatmentTime new treatment date and time.
     */
    public void setTreatmentTime(LocalDateTime treatmentTime) {
        this.treatmentTime = treatmentTime;
    }

    /**
     * Getter for the treatment date and time.
     *
     * @return date and time of the treatment.
     */
    public String getTreatmentName() {
        return treatmentName;
    }

    /**
     * Setter for the treatment name.
     *
     * @param treatmentName new name of the treatment.
     */
    public void setTreatmentName(String treatmentName) {
        this.treatmentName = treatmentName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeverTreatment treatment = (FeverTreatment) o;
        return id == treatment.id &&
                treatmentTime.equals(treatment.treatmentTime) &&
                treatmentName.equals(treatment.treatmentName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, treatmentTime, treatmentName);
    }
}
