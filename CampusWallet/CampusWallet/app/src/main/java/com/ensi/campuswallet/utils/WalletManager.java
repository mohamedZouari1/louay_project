package com.ensi.campuswallet.utils;

import com.ensi.campuswallet.models.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * WalletManager is a simple Singleton that holds the student's balance
 * and the list of transactions for the entire app session.
 *
 * In a real app this would use a database (Room) and a backend API,
 * but for this prototype an in-memory store is sufficient.
 */
public class WalletManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static WalletManager instance;

    private WalletManager() {
        // Pre-populate with a few demo transactions so the history page
        // is not empty when the app first launches.
        transactions.add(new Transaction(
                Transaction.TYPE_TRANSPORT, Transaction.CAT_TRANSPORT, 30.0, "D17"));
        transactions.add(new Transaction(
                Transaction.TYPE_CONCERT, Transaction.CAT_EVENT, 15.0, "Card"));
    }

    public static synchronized WalletManager getInstance() {
        if (instance == null) {
            instance = new WalletManager();
        }
        return instance;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    /** Starting balance in TND. Reduced by every confirmed payment. */
    private double balance = 120.0;

    /** All committed transactions (newest first after addTransaction). */
    private final List<Transaction> transactions = new ArrayList<>();

    // ── Balance helpers ───────────────────────────────────────────────────────

    public double getBalance() {
        return balance;
    }

    /**
     * Attempts to deduct {@code amount} from the balance.
     * @return true if the deduction succeeded, false if balance was insufficient.
     */
    public boolean deduct(double amount) {
        if (amount > balance) return false;
        balance -= amount;
        return true;
    }

    // ── Transaction helpers ───────────────────────────────────────────────────

    /**
     * Records a new transaction and deducts its amount from the balance.
     * Returns false if the balance is too low.
     */
    public boolean addTransaction(Transaction t) {
        if (!deduct(t.getAmount())) return false;
        transactions.add(0, t); // newest first
        return true;
    }

    /** Returns a copy of the transaction list (newest first). */
    public List<Transaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    /** Returns the most recent transaction, or null if none. */
    public Transaction getLastTransaction() {
        if (transactions.isEmpty()) return null;
        return transactions.get(0);
    }

    // ── Smart suggestion logic ────────────────────────────────────────────────

    /**
     * Produces a context-aware suggestion string based on the last payment.
     * This is intentionally simple rule-based logic – no AI required.
     */
    public String getSuggestion() {
        Transaction last = getLastTransaction();
        if (last == null) return null;

        switch (last.getCategory()) {
            case Transaction.CAT_TRANSPORT:
                return "🚌  Your transport subscription is active. Remember to renew it next month!";
            case Transaction.CAT_INSCRIPTION:
                return "🎓  Tuition recorded. Don't forget the remaining tranche before the deadline.";
            case Transaction.CAT_EVENT:
                return "🎉  You may be interested in upcoming campus events. Check the noticeboard!";
            default:
                return null;
        }
    }
}
