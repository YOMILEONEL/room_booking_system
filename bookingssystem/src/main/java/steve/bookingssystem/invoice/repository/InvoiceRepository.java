package steve.bookingssystem.invoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.invoice.model.Invoice;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByPayment_Id(Long paymentId);
    long countByInvoiceNumberStartingWith(String prefix);
}
