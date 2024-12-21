package steve.bookingssystem.user.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.service.UserService;

import java.util.List;


@RestController
@RequestMapping("/user")
public class Controller {

    @Autowired
    private UserService userService;


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }

    @GetMapping("/get/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/getAll")
    public List<User> getUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userService.updateUser(id, user);
    }



}
