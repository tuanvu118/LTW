package _2.LTW.util;
/**
 * Utility class để check kiểu isOwner, isAdmin, isDoctor 
 * phục vụ cho tầng service
 */

import _2.LTW.entity.User;
import _2.LTW.exception.ErrorCode;
import _2.LTW.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public CustomPrincipal getCurrentPrincipal(){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication == null || !authentication.isAuthenticated()){
            throw ErrorCode.UNAUTHORIZED.toException();
        }

        Object principal = authentication.getPrincipal();

        if(!(principal instanceof CustomPrincipal customPrincipal)){
            throw ErrorCode.UNAUTHORIZED.toException();
        }

        return customPrincipal;

    }

    public User getCurrentUser() {
        String username = getCurrentPrincipal().getUsername();
        return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Long getCurrentUserId(){
        return getCurrentPrincipal().getId();
    }

    public String getCurrentUsername(){
        return getCurrentPrincipal().getUsername();
    }

    public String getCurrentRole(){
        return getCurrentPrincipal().getRole();
    }

    public boolean isOwner(Long userId) {
        return getCurrentUserId().equals(userId);
    }

    public boolean isUser() {
        return getCurrentRole().equals("user");
    }

    public boolean isAdmin() {
        return getCurrentRole().equals("admin");
    }

    public boolean isDoctor() {
        return getCurrentRole().equals("doctor");
    }    
}
