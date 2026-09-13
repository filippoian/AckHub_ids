package it.unicam.hackhub.external;

import java.math.BigDecimal;

public interface PaymentSystem {
    // hackathonId identifica stabilmente il premio: un nuovo tentativo riusa la stessa chiave.
    boolean payPrize(long hackathonId, BigDecimal amount, String toTeamName);

    default String getLastErrorMessage() {
        return null;
    }

    default String getLastReceiptId() {
        return null;
    }
}
