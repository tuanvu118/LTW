package _2.LTW.repository;

import _2.LTW.entity.MedicalRecord.Prescriptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionsRepository extends JpaRepository<Prescriptions, Long> {
    @Query("""
        select p
        from Prescriptions p
        join fetch p.medicine m
        where p.medicalRecord.id = :medicalRecordId
        order by p.id desc
    """)
    List<Prescriptions> findAllByMedicalRecordId(@Param("medicalRecordId") Long medicalRecordId);

    @Query("""
        select p
        from Prescriptions p
        join fetch p.medicine m
        where p.id = :id
          and p.medicalRecord.id = :medicalRecordId
    """)
    Optional<Prescriptions> findDetailByIdAndMedicalRecordId(
            @Param("id") Long id,
            @Param("medicalRecordId") Long medicalRecordId
    );
}
