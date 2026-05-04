package com.ensi.campuswallet.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Transaction model representing a single payment record.
 * Implements Serializable so it can be passed between Activities via Intent.
 */
public class Transaction implements Serializable {

    // Payment type constants
    public static final String TYPE_TRANSPORT      = "Transport Subscription";
    public static final String TYPE_INSCRIPTION_T1 = "Inscription – Tranche 1";
    public static final String TYPE_INSCRIPTION_T2 = "Inscription – Tranche 2";
    public static final String TYPE_CONCERT        = "ENSI Music Club Concert";
    public static final String TYPE_MARATHON       = "Green Fortnight Marathon";

    // Category constants used by suggestions logic
    public static final String CAT_TRANSPORT    = "TRANSPORT";
    public static final String CAT_INSCRIPTION  = "INSCRIPTION";
    public static final String CAT_EVENT        = "EVENT";

    private final String id;          // Unique transaction ID
    private final String type;        // Human-readable payment type
    private final String category;    // Category for smart suggestions
    private final double amount;      // Amount paid in TND
    private final String date;        // Formatted date string
    private final String method;      // Payment method (D17 / Card / QR)

    public Transaction(String type, String category, double amount, String method) {
        // Generate a simple unique ID from timestamp
        this.id       = "TXN-" + System.currentTimeMillis();
        this.type     = type;
        this.category = category;
        this.amount   = amount;
        this.method   = method;
        // Format current date/time
        this.date     = new SimpleDateFormat("dd MMM yyyy  HH:mm", Locale.ENGLISH)
                            .format(new Date());
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getId()       { return id; }
    public String getType()     { return type; }
    public String getCategory() { return category; }
    public double getAmount()   { return amount; }
    public String getDate()     { return date; }
    public String getMethod()   { return method; }

    /** Returns formatted amount string, e.g. "30.00 TND" */
    public String getFormattedAmount() {
        return String.format(Locale.ENGLISH, "%.2f TND", amount);
    }

    /** Returns the appropriate icon resource name for the type */
    public String getIconName() {
        switch (category) {
            case CAT_TRANSPORT:   return "ic_transport";
            case CAT_INSCRIPTION: return "ic_inscription";
            case CAT_EVENT:       return "ic_event";
            default:              return "ic_payment";
        }
    }
}
