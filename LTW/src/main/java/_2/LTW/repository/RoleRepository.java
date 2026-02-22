package _2.LTW.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import _2.LTW.entity.Role;
import _2.LTW.enums.RoleEnum;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleEnum(RoleEnum roleEnum);
}
