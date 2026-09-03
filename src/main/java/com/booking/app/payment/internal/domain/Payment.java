package com.booking.app.payment.internal.domain;

import com.booking.app.common.AuditInfo;
import com.booking.app.common.Identifiable;
import com.booking.app.common.Require;
import com.booking.app.payment.InvalidStatusTransitionException;
import com.booking.app.payment.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment implements Identifiable {

    private static final int CURRENCY_CODE_LENGTH = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "payments_seq")
    @SequenceGenerator(name = "payments_seq", sequenceName = "payments_seq", allocationSize = 50)
    private Long id;

    @Column(name = "public_id", unique = true, updatable = false, nullable = false)
    private UUID publicId;

    @Version
    private Long version;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", updatable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = CURRENCY_CODE_LENGTH)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "idempotency_key", unique = true, nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "gateway_reference")
    private String gatewayReference;

    @Embedded
    private AuditInfo auditInfo;

    protected Payment() {}

    public Payment(UUID bookingId, UUID userId, BigDecimal amount, String currency, UUID idempotencyKey) {
        Require.notNull(bookingId, "bookingId cannot be null");
        Require.notNull(amount, "amount cannot be null");
        Require.argument(amount.compareTo(BigDecimal.ZERO) > 0, "amount must be greater than zero");
        Require.notNull(currency, "currency cannot be null");
        Require.argument(currency.strip().length() == CURRENCY_CODE_LENGTH, "currency must be 3 characters long");
        Require.notNull(idempotencyKey, "idempotencyKey cannot be null");

        this.publicId = UUID.randomUUID();
        this.auditInfo = new AuditInfo();
        this.bookingId = bookingId;
        this.userId = userId;
        this.amount = amount;
        this.currency = currency.strip().toUpperCase(Locale.ROOT);
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
    }

    @Override
    public UUID getPublicId() {
        return publicId;
    }

    public Long getVersion() {
        return version;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getGatewayReference() {
        return gatewayReference;
    }

    public AuditInfo getAuditInfo() {
        return auditInfo;
    }

    public void markAsSucceeded(String gatewayReference) {
        if (this.status == PaymentStatus.SUCCEEDED) {
            return;
        }
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidStatusTransitionException("Cannot succeed a payment with status " + this.status);
        }
        Require.notNull(gatewayReference, "gatewayReference cannot be null");
        this.gatewayReference = gatewayReference;
        this.status = PaymentStatus.SUCCEEDED;
    }

    public void markAsFailed(String gatewayReference) {
        if (this.status == PaymentStatus.FAILED) {
            return;
        }
        if (this.status != PaymentStatus.PENDING) {
            throw new InvalidStatusTransitionException("Cannot fail a payment with status " + this.status);
        }
        this.gatewayReference = gatewayReference;
        this.status = PaymentStatus.FAILED;
    }

    public void refund() {
        if (this.status == PaymentStatus.REFUNDED) {
            return;
        }
        if (this.status != PaymentStatus.SUCCEEDED) {
            throw new InvalidStatusTransitionException("Only SUCCEEDED payments can be refunded");
        }
        this.status = PaymentStatus.REFUNDED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment that)) return false;
        return Objects.equals(publicId, that.publicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicId);
    }
}
