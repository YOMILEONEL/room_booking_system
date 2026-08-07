package steve.bookingssystem.user.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserDTO;
import steve.bookingssystem.user.service.UserService;

import java.util.List;


@CrossOrigin
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthorizationService authorizationService;


    @DeleteMapping("/delete/{id}")
    public void deleteUser(@PathVariable Long id) {
        authorizationService.requireAdmin();
        userService.deleteUser(id);
    }

    @GetMapping("/get/{id}")
    public UserDTO getUser(@PathVariable Long id) {
        authorizationService.requireOwnerOrAdmin(id);
        return User.getUserDTO(userService.getUserById(id));
    }

    @GetMapping("/getAll")
    public List<UserDTO> getUsers() {
        authorizationService.requireAdmin();
        return userService.getAllUsers();
    }

    @PutMapping("update/{id}")
    public void updateUser(@PathVariable Long id, @RequestBody User user) {
        authorizationService.requireOwnerOrAdmin(id);
        userService.updateUser(id, user);
    }



}
