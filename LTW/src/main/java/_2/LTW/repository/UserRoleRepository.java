package _2.LTW.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import _2.LTW.entity.UserRole;
import _2.LTW.entity.User;
import _2.LTW.entity.Role;
    @Repository
    public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
        List<UserRole> findByUser_Id(Long userId);
        List<UserRole> findByRole_Id(Long roleId);
        List<UserRole> findByUserAndRole(User user, Role role);
    }
