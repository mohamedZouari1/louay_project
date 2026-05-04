package com.ensi.campuswallet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ensi.campuswallet.R;
import com.ensi.campuswallet.models.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying Transaction objects.
 * Used by both MainActivity (recent 3) and HistoryActivity (all).
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> data;

    public TransactionAdapter(List<Transaction> transactions) {
        this.data = transactions != null ? transactions : new ArrayList<>();
    }

    /** Replace the dataset and refresh the RecyclerView. */
    public void updateData(List<Transaction> transactions) {
        this.data = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ── RecyclerView overrides ────────────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction txn = data.get(position);
        holder.bind(txn);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivIcon;
        private final TextView  tvType;
        private final TextView  tvDate;
        private final TextView  tvAmount;
        private final TextView  tvMethod;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon   = itemView.findViewById(R.id.iv_txn_icon);
            tvType   = itemView.findViewById(R.id.tv_txn_type);
            tvDate   = itemView.findViewById(R.id.tv_txn_date);
            tvAmount = itemView.findViewById(R.id.tv_txn_amount);
            tvMethod = itemView.findViewById(R.id.tv_txn_method);
        }

        void bind(Transaction txn) {
            tvType.setText(txn.getType());
            tvDate.setText(txn.getDate());
            tvAmount.setText("- " + txn.getFormattedAmount());
            tvMethod.setText(txn.getMethod());

            // Choose icon based on category
            switch (txn.getCategory()) {
                case Transaction.CAT_TRANSPORT:
                    ivIcon.setImageResource(R.drawable.ic_transport);
                    ivIcon.setColorFilter(itemView.getContext()
                            .getResources().getColor(R.color.color_transport));
                    break;
                case Transaction.CAT_INSCRIPTION:
                    ivIcon.setImageResource(R.drawable.ic_inscription);
                    ivIcon.setColorFilter(itemView.getContext()
                            .getResources().getColor(R.color.color_inscription));
                    break;
                case Transaction.CAT_EVENT:
                    ivIcon.setImageResource(R.drawable.ic_event);
                    ivIcon.setColorFilter(itemView.getContext()
                            .getResources().getColor(R.color.color_event));
                    break;
                default:
                    ivIcon.setImageResource(R.drawable.ic_payment);
                    ivIcon.clearColorFilter();
                    break;
            }
        }
    }
}
