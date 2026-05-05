package com.ensi.campuswallet.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ensi.campuswallet.R;
import com.ensi.campuswallet.adapters.TransactionAdapter;
import com.ensi.campuswallet.utils.WalletManager;

/**
 * HistoryActivity shows the complete list of all transactions
 * using a RecyclerView with the shared TransactionAdapter.
 */
public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Back arrow
        findViewById(R.id.btn_back_history).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rv_all_transactions);
        rv.setLayoutManager(new LinearLayoutManager(this));

        // Load all transactions from the shared WalletManager
        TransactionAdapter adapter = new TransactionAdapter(
                WalletManager.getInstance().getTransactions()
        );
        rv.setAdapter(adapter);

        // Show empty-state message if there are no transactions yet
        TextView tvEmpty = findViewById(R.id.tv_empty_history);
        if (WalletManager.getInstance().getTransactions().isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
        }
    }
}
