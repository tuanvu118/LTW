package _2.LTW.repository;

import _2.LTW.entity.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {

    @Query("""
        SELECT COALESCE(SUM(ms.timeDuration), 0)
        FROM MedicalService ms
        WHERE ms.id IN :serviceIds
    """)
    int sumDuration(@Param("serviceIds") List<Long> serviceIds);

}
