package com.ensi.campuswallet.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.ensi.campuswallet.R;
import com.ensi.campuswallet.models.Transaction;
import com.ensi.campuswallet.utils.WalletManager;

import java.util.Locale;

/**
 * PaymentActivity handles all payment flows:
 *
 *  1. Card payment  – user fills card number, expiry, CVV then confirms.
 *  2. D17 payment   – single-button flow (no card fields needed).
 *  3. QR code scan  – simulated scan that auto-triggers payment.
 *
 * The activity receives the service type, category and amount from
 * MainActivity via Intent extras.
 */
public class PaymentActivity extends AppCompatActivity {

    // ── Intent extras (keys) ─────────────────────────────────────────────────
    public static final String EXTRA_TYPE     = "payment_type";
    public static final String EXTRA_CATEGORY = "payment_category";
    public static final String EXTRA_AMOUNT   = "payment_amount";

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView      tvServiceName;
    private TextView      tvServiceAmount;
    private TextView      tvCurrentBalance;
    private RadioGroup    rgPaymentMethod;
    private LinearLayout  layoutCardFields;
    private EditText      etCardNumber;
    private EditText      etExpiry;
    private EditText      etCvv;
    private Button        btnConfirm;
    private Button        btnScanQr;
    private Button        btnPayD17;
    private ProgressBar   progressBar;
    private CardView      cardD17;
    private LinearLayout  layoutCardMethod;

    // ── Data ──────────────────────────────────────────────────────────────────
    private String        serviceType;
    private String        serviceCategory;
    private double        serviceAmount;
    private WalletManager wallet;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        wallet = WalletManager.getInstance();

        // Read data passed from the previous screen
        serviceType     = getIntent().getStringExtra(EXTRA_TYPE);
        serviceCategory = getIntent().getStringExtra(EXTRA_CATEGORY);
        serviceAmount   = getIntent().getDoubleExtra(EXTRA_AMOUNT, 0.0);

        bindViews();
        populateServiceInfo();
        setupPaymentMethodToggle();
        setupConfirmButton();
        setupQrButton();
        setupD17Button();
        setupCardNumberFormatter();
        setupExpiryFormatter();

        // Back arrow
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private void bindViews() {
        tvServiceName      = findViewById(R.id.tv_service_name);
        tvServiceAmount    = findViewById(R.id.tv_service_amount);
        tvCurrentBalance   = findViewById(R.id.tv_current_balance);
        rgPaymentMethod    = findViewById(R.id.rg_payment_method);
        layoutCardFields   = findViewById(R.id.layout_card_fields);
        etCardNumber       = findViewById(R.id.et_card_number);
        etExpiry           = findViewById(R.id.et_expiry);
        etCvv              = findViewById(R.id.et_cvv);
        btnConfirm         = findViewById(R.id.btn_confirm);
        btnScanQr          = findViewById(R.id.btn_scan_qr);
        btnPayD17          = findViewById(R.id.btn_pay_d17);
        progressBar        = findViewById(R.id.progress_bar);
        cardD17            = findViewById(R.id.card_d17);
        layoutCardMethod   = findViewById(R.id.layout_card_method);
    }

    private void populateServiceInfo() {
        tvServiceName.setText(serviceType);
        tvServiceAmount.setText(String.format(Locale.ENGLISH, "%.2f TND", serviceAmount));
        tvCurrentBalance.setText(String.format(Locale.ENGLISH, "Balance: %.2f TND",
                wallet.getBalance()));
    }

    // ── Payment method toggle ─────────────────────────────────────────────────

    private void setupPaymentMethodToggle() {
        // Default: card method visible, D17 hidden
        showCardMethod();

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_card) {
                showCardMethod();
            } else if (checkedId == R.id.rb_d17) {
                showD17Method();
            }
        });
    }

    private void showCardMethod() {
        layoutCardFields.setVisibility(View.VISIBLE);
        cardD17.setVisibility(View.GONE);
        btnConfirm.setVisibility(View.VISIBLE);
    }

    private void showD17Method() {
        layoutCardFields.setVisibility(View.GONE);
        cardD17.setVisibility(View.VISIBLE);
        btnConfirm.setVisibility(View.GONE);
    }

    // ── Confirm (card payment) ────────────────────────────────────────────────

    private void setupConfirmButton() {
        btnConfirm.setOnClickListener(v -> {
            if (!validateCardFields()) return;
            processPayment("Card");
        });
    }

    /** Basic empty-field validation – no real card logic needed for prototype */
    private boolean validateCardFields() {
        String cardNum = etCardNumber.getText().toString().replace(" ", "");
        String expiry  = etExpiry.getText().toString().trim();
        String cvv     = etCvv.getText().toString().trim();

        if (cardNum.length() < 16) {
            etCardNumber.setError("Enter a valid 16-digit card number");
            etCardNumber.requestFocus();
            return false;
        }
        if (expiry.length() < 5) {
            etExpiry.setError("Enter expiry as MM/YY");
            etExpiry.requestFocus();
            return false;
        }
        if (cvv.length() < 3) {
            etCvv.setError("Enter a 3-digit CVV");
            etCvv.requestFocus();
            return false;
        }
        return true;
    }

    // ── D17 payment ───────────────────────────────────────────────────────────

    private void setupD17Button() {
        btnPayD17 = findViewById(R.id.btn_pay_d17);
        btnPayD17.setOnClickListener(v -> processPayment("D17"));
    }

    // ── QR code simulation ────────────────────────────────────────────────────

    private void setupQrButton() {
        btnScanQr.setOnClickListener(v -> simulateQrScan());
    }

    /**
     * Simulates a QR code scan:
     *  1. Show a "scanning…" toast immediately.
     *  2. After 1.5 s, trigger the payment automatically.
     */
    private void simulateQrScan() {
        Toast.makeText(this, "📷  Scanning QR code…", Toast.LENGTH_SHORT).show();
        setInputEnabled(false);

        new Handler().postDelayed(() -> {
            Toast.makeText(this, "✅  QR Code recognised!", Toast.LENGTH_SHORT).show();
            processPayment("QR Code");
        }, 1500);
    }

    // ── Core payment logic ────────────────────────────────────────────────────

    /**
     * Main payment processing method.
     * Shows a progress indicator, waits 1.5 s (simulates network call),
     * then either shows success or an insufficient-balance error.
     */
    private void processPayment(final String method) {
        // Guard: not enough money
        if (serviceAmount > wallet.getBalance()) {
            showInsufficientBalanceDialog();
            setInputEnabled(true);
            return;
        }

        // Show processing state
        progressBar.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);
        btnPayD17.setEnabled(false);
        btnScanQr.setEnabled(false);

        new Handler().postDelayed(() -> {
            // Create and commit the transaction
            Transaction txn = new Transaction(serviceType, serviceCategory, serviceAmount, method);
            boolean ok = wallet.addTransaction(txn);

            progressBar.setVisibility(View.GONE);

            if (ok) {
                showSuccessDialog(txn);
            } else {
                // Edge-case: balance changed between check and commit
                showInsufficientBalanceDialog();
                setInputEnabled(true);
            }
        }, 1500); // 1.5 s simulated processing delay
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    private void showSuccessDialog(Transaction txn) {
        new AlertDialog.Builder(this)
                .setTitle("✅  Payment Successful")
                .setMessage(
                    "Service:  " + txn.getType() + "\n" +
                    "Amount:   " + txn.getFormattedAmount() + "\n" +
                    "Method:   " + txn.getMethod() + "\n" +
                    "Ref:      " + txn.getId() + "\n\n" +
                    "New balance: " + String.format(Locale.ENGLISH, "%.2f TND",
                            wallet.getBalance())
                )
                .setPositiveButton("Done", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void showInsufficientBalanceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⚠️  Insufficient Balance")
                .setMessage(String.format(Locale.ENGLISH,
                        "Your current balance (%.2f TND) is not enough\nto pay %.2f TND.",
                        wallet.getBalance(), serviceAmount))
                .setPositiveButton("OK", null)
                .show();
    }

    // ── Input formatters ──────────────────────────────────────────────────────

    /** Auto-inserts spaces every 4 digits: "1234 5678 9012 3456" */
    private void setupCardNumberFormatter() {
        etCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean editing = false;

            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (editing) return;
                editing = true;

                String digits = s.toString().replace(" ", "");
                if (digits.length() > 16) digits = digits.substring(0, 16);

                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) formatted.append(' ');
                    formatted.append(digits.charAt(i));
                }
                etCardNumber.setText(formatted.toString());
                etCardNumber.setSelection(formatted.length());
                editing = false;
            }
        });
    }

    /** Auto-inserts "/" after 2 digits for MM/YY format */
    private void setupExpiryFormatter() {
        etExpiry.addTextChangedListener(new TextWatcher() {
            private boolean editing = false;

            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (editing) return;
                editing = true;

                String raw = s.toString().replace("/", "");
                if (raw.length() > 4) raw = raw.substring(0, 4);

                String formatted = raw.length() > 2
                        ? raw.substring(0, 2) + "/" + raw.substring(2)
                        : raw;

                etExpiry.setText(formatted);
                etExpiry.setSelection(formatted.length());
                editing = false;
            }
        });
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void setInputEnabled(boolean enabled) {
        etCardNumber.setEnabled(enabled);
        etExpiry.setEnabled(enabled);
        etCvv.setEnabled(enabled);
        btnConfirm.setEnabled(enabled);
        btnPayD17.setEnabled(enabled);
        btnScanQr.setEnabled(enabled);
    }
}
