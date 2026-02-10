package _2.LTW.util;
/**
 * Utility class để check kiểu isOwner, isAdmin, isDoctor 
 * phục vụ cho tầng service
 */

import _2.LTW.entity.User;
import _2.LTW.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
        return user;
    }

    public boolean isOwner(Long userId) {
        User currentUser = getCurrentUser();
        return currentUser.getId().equals(userId);
    }

    public boolean isAdmin() {
        User currentUser = getCurrentUser();
        return currentUser.getRole().getName().equals("admin");
    }

    public boolean isDoctor() {
        User currentUser = getCurrentUser();
        return currentUser.getRole().getName().equals("doctor");
    }    
}
