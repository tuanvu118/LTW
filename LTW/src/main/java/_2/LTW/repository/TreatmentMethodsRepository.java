package _2.LTW.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import _2.LTW.entity.TreatmentMethods;

public interface TreatmentMethodsRepository extends JpaRepository<TreatmentMethods, Long> {
        boolean existsByName(String name);
}

