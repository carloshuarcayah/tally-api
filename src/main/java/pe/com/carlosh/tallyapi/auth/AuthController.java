package pe.com.carlosh.tallyapi.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.com.carlosh.tallyapi.user.UserService;
import pe.com.carlosh.tallyapi.user.dto.LoginResponseDTO;
import pe.com.carlosh.tallyapi.user.dto.LoginRequestDTO;
import pe.com.carlosh.tallyapi.user.dto.UserRequestDTO;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRequestDTO req) {
        // El servicio ya no devuelve un JWT, solo ejecuta el guardado y envía el email
        userService.register(req);

        // Devolvemos un JSON estándar de mensaje
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Registro exitoso. Por favor revisa tu bandeja de entrada para verificar tu cuenta."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO req) {
        return ResponseEntity.ok(userService.login(req));
    }

    @GetMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        // Le pasamos el token al servicio para que haga la magia
        userService.verifyEmail(token);

        return ResponseEntity.ok(
                Map.of("message", "Correo verificado exitosamente. Ya puedes volver a la aplicación e iniciar sesión.")
        );
    }
}