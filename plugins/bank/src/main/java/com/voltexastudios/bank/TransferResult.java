package com.voltexastudios.bank;

/**
 * Outcome of a deposit or withdraw. {@link #outcome()} is {@code OK} when the
 * transfer applied (and {@link #balance()} is the resulting balance); otherwise
 * it is one of the rejection reasons and {@link #balance()} is {@code -1}.
 */
public record TransferResult(Outcome outcome, long balance) {

    /** The possible outcomes of a transfer. */
    public enum Outcome {
        /** The transfer applied; the balance is the resulting balance. */
        OK,
        /** The account does not exist (withdraw only). */
        NO_ACCOUNT,
        /** The account's balance is below the requested amount. */
        INSUFFICIENT_BALANCE,
        /** The deposit would push the balance past the configured cap. */
        EXCEEDS_MAX_BALANCE,
        /** The amount is below the configured minimum transaction. */
        AMOUNT_BELOW_MIN
    }

    /** A successful transfer with the resulting balance. */
    public static TransferResult ok(long balance) {
        return new TransferResult(Outcome.OK, balance);
    }

    /** A rejected transfer with no resulting balance. */
    public static TransferResult fail(Outcome outcome) {
        return new TransferResult(outcome, -1L);
    }
}
