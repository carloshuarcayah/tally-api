package pe.com.carlosh.tallyapi.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.com.carlosh.tallyapi.user.dto.UserResponseDTO;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    //ADMIN
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> findMe(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(userService.findById(user.getId()));
    }

    @PatchMapping("/me/onboarding")
    public ResponseEntity<Void> completeOnboarding(@AuthenticationPrincipal User user) {
        userService.completeOnboarding(user.getId());
        return ResponseEntity.ok().build();
    }

}