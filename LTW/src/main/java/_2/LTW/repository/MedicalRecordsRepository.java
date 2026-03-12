package _2.LTW.repository;

import _2.LTW.entity.MedicalRecord.MedicalRecords;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicalRecordsRepository extends JpaRepository<MedicalRecords, Long> {
    boolean existsByMedicalBooking_Id(Integer medicalBookingId);

    @Query("""
        select mr
        from MedicalRecords mr
        join fetch mr.medicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        where mr.id = :id
    """)
    Optional<MedicalRecords> findDetailById(@Param("id") Long id);

    @Query("""
        select mr
        from MedicalRecords mr
        join fetch mr.medicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        where mb.id = :bookingId
    """)
    Optional<MedicalRecords> findDetailByBookingId(@Param("bookingId") Integer bookingId);

    @Query("""
        select mr
        from MedicalRecords mr
        join fetch mr.medicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        where p.id = :petId
        order by mb.bookingDate desc, mb.startTime desc
    """)
    List<MedicalRecords> findAllByPetId(@Param("petId") Integer petId);
}
