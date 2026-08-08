package steve.bookingssystem.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import steve.bookingssystem.security.AuthorizationService;

@RestController
@RequestMapping("/admin")
public class AdminDashboardController {

    @Autowired
    private AdminDashboardService adminDashboardService;
    @Autowired
    private AuthorizationService authorizationService;

    @GetMapping("/dashboard")
    public AdminDashboardDto getDashboard() {
        authorizationService.requireAdmin();
        return adminDashboardService.getDashboard();
    }
}
