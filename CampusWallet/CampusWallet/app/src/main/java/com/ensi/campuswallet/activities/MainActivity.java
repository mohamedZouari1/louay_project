package com.ensi.campuswallet.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ensi.campuswallet.R;
import com.ensi.campuswallet.adapters.TransactionAdapter;
import com.ensi.campuswallet.models.Transaction;
import com.ensi.campuswallet.utils.WalletManager;

import java.util.List;
import java.util.Locale;

/**
 * MainActivity – the Campus Wallet dashboard.
 *
 * Displays:
 *  • Current balance card at the top
 *  • 4 payment option cards (Transport, Inscription, Concert, Marathon)
 *  • Recent transactions RecyclerView (last 3)
 *  • Notification strip with simulated reminders
 *  • Smart suggestion banner based on last transaction
 */
public class MainActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextView tvBalance;
    private TextView tvSuggestion;
    private TextView tvNotification;
    private RecyclerView rvRecentTransactions;

    private TransactionAdapter adapter;
    private WalletManager wallet;

    // Notification messages cycled with a Handler
    private final String[] notifications = {
        "📅  Tuition deadline is approaching – pay your tranche now.",
        "🚌  Your transport subscription expires soon – renew it.",
        "🏃  Green Fortnight Marathon 2027 registration is open!"
    };
    private int notifIndex = 0;
    private final Handler notifHandler = new Handler();

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wallet = WalletManager.getInstance();

        bindViews();
        setupPaymentCards();
        setupNavigation();
        startNotificationCycle();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh balance and transactions every time we return to this screen
        refreshUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notifHandler.removeCallbacksAndMessages(null);
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private void bindViews() {
        tvBalance            = findViewById(R.id.tv_balance);
        tvSuggestion         = findViewById(R.id.tv_suggestion);
        tvNotification       = findViewById(R.id.tv_notification);
        rvRecentTransactions = findViewById(R.id.rv_recent_transactions);

        rvRecentTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(null); // data set later in refreshUI
        rvRecentTransactions.setAdapter(adapter);
    }

    private void refreshUI() {
        // Update balance display
        tvBalance.setText(String.format(Locale.ENGLISH, "%.2f TND", wallet.getBalance()));

        // Show smart suggestion from last transaction
        String suggestion = wallet.getSuggestion();
        if (suggestion != null) {
            tvSuggestion.setVisibility(View.VISIBLE);
            tvSuggestion.setText(suggestion);
        } else {
            tvSuggestion.setVisibility(View.GONE);
        }

        // Show only the 3 most recent transactions in the home preview
        List<Transaction> recent = wallet.getTransactions();
        if (recent.size() > 3) recent = recent.subList(0, 3);
        adapter.updateData(recent);
    }

    // ── Payment cards ─────────────────────────────────────────────────────────

    private void setupPaymentCards() {

        // 1. Transport Subscription
        CardView cardTransport = findViewById(R.id.card_transport);
        cardTransport.setOnClickListener(v -> openPayment(
                Transaction.TYPE_TRANSPORT,
                Transaction.CAT_TRANSPORT,
                30.0
        ));

        // 2a. Inscription – Tranche 1
        CardView cardT1 = findViewById(R.id.card_inscription_t1);
        cardT1.setOnClickListener(v -> openPayment(
                Transaction.TYPE_INSCRIPTION_T1,
                Transaction.CAT_INSCRIPTION,
                250.0
        ));

        // 2b. Inscription – Tranche 2
        CardView cardT2 = findViewById(R.id.card_inscription_t2);
        cardT2.setOnClickListener(v -> openPayment(
                Transaction.TYPE_INSCRIPTION_T2,
                Transaction.CAT_INSCRIPTION,
                250.0
        ));

        // 3. ENSI Music Club Concert
        CardView cardConcert = findViewById(R.id.card_concert);
        cardConcert.setOnClickListener(v -> openPayment(
                Transaction.TYPE_CONCERT,
                Transaction.CAT_EVENT,
                15.0
        ));

        // 4. Green Fortnight Marathon
        CardView cardMarathon = findViewById(R.id.card_marathon);
        cardMarathon.setOnClickListener(v -> openPayment(
                Transaction.TYPE_MARATHON,
                Transaction.CAT_EVENT,
                20.0
        ));
    }

    /** Opens PaymentActivity pre-loaded with the chosen service details. */
    private void openPayment(String type, String category, double amount) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PaymentActivity.EXTRA_TYPE,     type);
        intent.putExtra(PaymentActivity.EXTRA_CATEGORY, category);
        intent.putExtra(PaymentActivity.EXTRA_AMOUNT,   amount);
        startActivity(intent);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private void setupNavigation() {
        // "View all" button opens the full transaction history
        findViewById(R.id.btn_view_all).setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));
    }

    // ── Simulated notifications ────────────────────────────────────────────────

    /**
     * Cycles through notification messages every 4 seconds to simulate
     * a live notification feed.
     */
    private void startNotificationCycle() {
        tvNotification.setText(notifications[notifIndex]);

        notifHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                notifIndex = (notifIndex + 1) % notifications.length;
                tvNotification.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            tvNotification.setText(notifications[notifIndex]);
                            tvNotification.animate().alpha(1f).setDuration(300).start();
                        }).start();
                notifHandler.postDelayed(this, 4000);
            }
        }, 4000);
    }
}
