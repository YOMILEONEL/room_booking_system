package steve.bookingssystem.discount.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.discount.model.DiscountCode;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, UUID> {
    Optional<DiscountCode> findByCode(String code);
}
