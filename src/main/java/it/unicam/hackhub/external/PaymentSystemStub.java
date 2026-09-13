package it.unicam.hackhub.external;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class PaymentSystemStub implements PaymentSystem {
    private String lastErrorMessage;
    private String lastReceiptId;
    private final java.util.Map<Long, String> receipts = new java.util.HashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public synchronized boolean payPrize(long hackathonId, BigDecimal amount, String toTeamName) {
        lastErrorMessage = null;
        lastReceiptId = null;

        if (receipts.containsKey(hackathonId)) {
            lastReceiptId = receipts.get(hackathonId);
            return true;
        }
        if (hackathonId <= 0 || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || toTeamName == null || toTeamName.trim().isBlank()) {
            lastErrorMessage = "Pagamento non valido";
            return false;
        }

        long n = seq.getAndIncrement();
        lastReceiptId = String.format("PAY-%06d", n);
        receipts.put(hackathonId, lastReceiptId);
        return true;
    }

    @Override
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    @Override
    public String getLastReceiptId() {
        return lastReceiptId;
    }
}
