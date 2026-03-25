package pe.com.carlosh.tallyapi.user;

import pe.com.carlosh.tallyapi.user.dto.UserRequestDTO;
import pe.com.carlosh.tallyapi.user.dto.UserResponseDTO;

public class UserMapper {


    public static User toEntity(UserRequestDTO req, String encodedPassword) {

        return new User(
                req.email(),
                req.phone(),
                req.username(),
                encodedPassword,
                req.firstName(),
                req.lastName()
        );
    }

    public static UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}