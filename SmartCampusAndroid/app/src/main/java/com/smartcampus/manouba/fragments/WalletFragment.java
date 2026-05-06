package com.smartcampus.manouba.fragments;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.smartcampus.manouba.R;
import com.smartcampus.manouba.utils.SharedPrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WalletFragment extends Fragment {

    // Simple in-memory wallet state (persisted via SharedPrefs key)
    private double balance = 145.50;
    private final List<Transaction> transactions = new ArrayList<>();

    private TextView tvBalance;
    private ViewGroup txContainer;
    private MaterialButton btnTopUp, btnPay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wallet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvBalance    = view.findViewById(R.id.tv_wallet_balance);
        txContainer  = view.findViewById(R.id.tx_container);
        btnTopUp     = view.findViewById(R.id.btn_top_up);
        btnPay       = view.findViewById(R.id.btn_pay);

        // Restore balance from SharedPrefs
        balance = requireActivity().getSharedPreferences("wallet_prefs", 0)
                .getFloat("balance", 145.50f);

        // Seed some transactions if first time
        if (transactions.isEmpty()) {
            transactions.add(new Transaction("Café – Coffee & Snack", "Yesterday, 10:30 AM", -4.50, "expense"));
            transactions.add(new Transaction("Library – Printing",    "May 2, 14:15",        -1.20, "expense"));
            transactions.add(new Transaction("Top Up",                "May 1, 09:00",        +50.00, "income"));
        }

        updateBalanceUI();
        renderTransactions();

        btnTopUp.setOnClickListener(v -> showTopUpBottomSheet());
        btnPay.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v).navigate(R.id.scanQrFragment);
        });
    }

    private void showTopUpBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.dialog_add_funds, null);
        dialog.setContentView(sheetView);

        EditText etAmount = sheetView.findViewById(R.id.et_topup_amount);
        EditText etCard = sheetView.findViewById(R.id.et_card_number);
        EditText etExpiry = sheetView.findViewById(R.id.et_card_expiry);
        EditText etCvv = sheetView.findViewById(R.id.et_card_cvv);
        View btnConfirm = sheetView.findViewById(R.id.btn_confirm_topup);

        btnConfirm.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString();
            String cardStr = etCard.getText().toString();
            
            if (amountStr.isEmpty() || cardStr.length() < 16) {
                Toast.makeText(requireContext(), "Enter valid amount and 16-digit card number", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                balance += amount;
                String time = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date());
                transactions.add(0, new Transaction("Top Up (Card ****" + cardStr.substring(12) + ")", time, amount, "income"));
                
                saveBalance();
                updateBalanceUI();
                renderTransactions();
                
                Toast.makeText(requireContext(), "Successfully added " + amount + " TND!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error processing payment", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showCustomPayDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Amount in TND");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext(), R.style.Theme_SmartCampus_Dialog)
                .setTitle("Custom Payment")
                .setView(input)
                .setPositiveButton("Pay", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try { processPayment("Payment", Double.parseDouble(val)); }
                        catch (NumberFormatException e) { Toast.makeText(requireContext(), "Invalid amount.", Toast.LENGTH_SHORT).show(); }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processPayment(String label, double amount) {
        if (amount > balance) {
            Toast.makeText(requireContext(), "Insufficient balance!", Toast.LENGTH_SHORT).show();
            return;
        }
        balance -= amount;
        String time = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(new Date());
        transactions.add(0, new Transaction(label, time, -amount, "expense"));
        saveBalance();
        updateBalanceUI();
        renderTransactions();
        Toast.makeText(requireContext(), String.format(Locale.getDefault(), "Paid %.2f TND ✓", amount), Toast.LENGTH_SHORT).show();
    }

    private void updateBalanceUI() {
        if (tvBalance != null)
            tvBalance.setText(String.format(Locale.getDefault(), "%.2f TND", balance));
    }

    private void renderTransactions() {
        if (txContainer == null) return;
        txContainer.removeAllViews();
        for (Transaction tx : transactions) {
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_transaction, txContainer, false);

            TextView tvLabel   = row.findViewById(R.id.tv_tx_label);
            TextView tvDate    = row.findViewById(R.id.tv_tx_date);
            TextView tvAmount  = row.findViewById(R.id.tv_tx_amount);

            tvLabel.setText(tx.label);
            tvDate.setText(tx.date);
            if (tx.type.equals("income")) {
                tvAmount.setText(String.format(Locale.getDefault(), "+%.2f TND", tx.amount));
                tvAmount.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                tvAmount.setText(String.format(Locale.getDefault(), "%.2f TND", tx.amount));
                tvAmount.setTextColor(Color.parseColor("#C62828"));
            }
            txContainer.addView(row);
        }
    }

    private void saveBalance() {
        requireActivity().getSharedPreferences("wallet_prefs", 0)
                .edit().putFloat("balance", (float) balance).apply();
    }

    static class Transaction {
        String label, date, type;
        double amount;
        Transaction(String label, String date, double amount, String type) {
            this.label = label; this.date = date; this.amount = amount; this.type = type;
        }
    }
}
