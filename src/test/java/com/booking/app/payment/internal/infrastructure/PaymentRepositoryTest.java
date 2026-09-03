package com.booking.app.payment.internal.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.booking.app.TestcontainersConfiguration;
import com.booking.app.config.JpaConfig;
import com.booking.app.payment.internal.domain.Payment;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Tag("integration")
class PaymentRepositoryTest {

    private static final BigDecimal DEFAULT_AMOUNT = BigDecimal.valueOf(100.00);
    private static final String DEFAULT_CURRENCY = "USD";

    @Autowired
    private PaymentRepository paymentRepository;

    private static Payment payment(UUID bookingId, UUID idempotencyKey) {
        return new Payment(bookingId, UUID.randomUUID(), DEFAULT_AMOUNT, DEFAULT_CURRENCY, idempotencyKey);
    }

    @Test
    @DisplayName("Should save and find payment by publicId")
    void shouldSaveAndFindByPublicId() {
        Payment payment = payment(UUID.randomUUID(), UUID.randomUUID());
        paymentRepository.saveAndFlush(payment);

        Optional<Payment> found = paymentRepository.findByPublicId(payment.getPublicId());

        assertThat(found).isPresent();
        assertThat(found.get().getBookingId()).isEqualTo(payment.getBookingId());
        assertThat(found.get().getAmount()).isEqualByComparingTo(DEFAULT_AMOUNT);
        assertThat(found.get().getCurrency()).isEqualTo(DEFAULT_CURRENCY);
    }

    @Test
    @DisplayName("Should find payment by idempotencyKey")
    void shouldFindByIdempotencyKey() {
        UUID idempotencyKey = UUID.randomUUID();
        Payment payment = payment(UUID.randomUUID(), idempotencyKey);
        paymentRepository.saveAndFlush(payment);

        Optional<Payment> found = paymentRepository.findByIdempotencyKey(idempotencyKey);

        assertThat(found).isPresent();
        assertThat(found.get().getIdempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(found.get().getPublicId()).isEqualTo(payment.getPublicId());
    }

    @Test
    @DisplayName("Should find payments by bookingId with pagination")
    void shouldFindByBookingId() {
        UUID bookingId = UUID.randomUUID();
        Payment payment1 = payment(bookingId, UUID.randomUUID());
        Payment payment2 = payment(bookingId, UUID.randomUUID());
        Payment otherBookingPayment = payment(UUID.randomUUID(), UUID.randomUUID());

        paymentRepository.saveAndFlush(payment1);
        paymentRepository.saveAndFlush(payment2);
        paymentRepository.saveAndFlush(otherBookingPayment);

        Page<Payment> page = paymentRepository.findByBookingId(bookingId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Payment::getBookingId).containsOnly(bookingId);
    }

    @Test
    @DisplayName("Should reject duplicate idempotency key via unique constraint")
    void shouldRejectDuplicateIdempotencyKey() {
        UUID idempotencyKey = UUID.randomUUID();
        Payment first = payment(UUID.randomUUID(), idempotencyKey);
        paymentRepository.saveAndFlush(first);

        Payment second = payment(UUID.randomUUID(), idempotencyKey);

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should persist payment with generated audit and version fields")
    void shouldPersistWithGeneratedFields() {
        Payment payment = payment(UUID.randomUUID(), UUID.randomUUID());
        Payment saved = paymentRepository.saveAndFlush(payment);

        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
