package steve.bookingssystem.discount.Controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.discount.model.DiscountCode;
import steve.bookingssystem.discount.model.DiscountCodeResponseDTO;
import steve.bookingssystem.discount.service.DiscountCodeService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/discount-code")
public class DiscountCodeController {

    @Autowired
    private DiscountCodeService discountCodeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountCodeResponseDTO create(@Valid @RequestBody DiscountCode discountCode) {
        return DiscountCodeResponseDTO.from(discountCodeService.create(discountCode));
    }

    @GetMapping
    public List<DiscountCodeResponseDTO> getAll() {
        return discountCodeService.findAll().stream().map(DiscountCodeResponseDTO::from).toList();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        discountCodeService.delete(id);
    }

}
