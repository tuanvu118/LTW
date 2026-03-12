package _2.LTW.repository;

import _2.LTW.entity.MedicalRecord.TreatmentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TreatmentRecordRepository extends JpaRepository<TreatmentRecord, Integer> {
    @Query("""
        select tr
        from TreatmentRecord tr
        join fetch tr.treatmentMethods tm
        where tr.medicalRecord.id = :medicalRecordId
        order by tr.id desc
    """)
    List<TreatmentRecord> findAllByMedicalRecordId(@Param("medicalRecordId") Long medicalRecordId);

    @Query("""
        select tr
        from TreatmentRecord tr
        join fetch tr.treatmentMethods tm
        where tr.id = :id
          and tr.medicalRecord.id = :medicalRecordId
    """)
    Optional<TreatmentRecord> findDetailByIdAndMedicalRecordId(
            @Param("id") Long id,
            @Param("medicalRecordId") Long medicalRecordId
    );
}
