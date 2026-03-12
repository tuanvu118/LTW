package _2.LTW.repository;

import _2.LTW.entity.MedicalBooking.MedicalBooking;
import _2.LTW.entity.MedicalBooking.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MedicalBookingRepository extends JpaRepository<MedicalBooking, Integer> {
    @Query("""
        select distinct mb
        from MedicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        left join fetch mb.medicalBookingsService mbs
        left join fetch mbs.medicalService ms
        where mb.id = :id
    """)
    Optional<MedicalBooking> findDetailById(@Param("id") Integer id);

    @Query("""
        select distinct mb
        from MedicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        left join fetch mb.medicalBookingsService mbs
        left join fetch mbs.medicalService ms
        where p.user.id = :ownerId
        order by mb.bookingDate desc, mb.startTime desc
    """)
    List<MedicalBooking> findAllByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
        select distinct mb
        from MedicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        left join fetch mb.medicalBookingsService mbs
        left join fetch mbs.medicalService ms
        where d.id = :doctorId
        order by mb.bookingDate desc, mb.startTime desc
    """)
    List<MedicalBooking> findAllByDoctorId(@Param("doctorId") Long doctorId);

    @Query("""
        select distinct mb
        from MedicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        left join fetch mb.medicalBookingsService mbs
        left join fetch mbs.medicalService ms
        where mb.doctor.id = :doctorId
          and mb.bookingDate = :bookingDate
          and mb.status in :statuses
    """)
    List<MedicalBooking> findDoctorBookingsInDate(
            @Param("doctorId") Long doctorId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("statuses") List<Status> statuses
    );

    @Query("""
        select distinct mb
        from MedicalBooking mb
        join fetch mb.pets p
        join fetch p.user owner
        join fetch mb.doctor d
        left join fetch mb.medicalBookingsService mbs
        left join fetch mbs.medicalService ms
        where (:doctorId is null or d.id = :doctorId)
          and (:status is null or mb.status = :status)
          and (:bookingDate is null or mb.bookingDate = :bookingDate)
        order by mb.bookingDate desc, mb.startTime desc
    """)
    List<MedicalBooking> search(
            @Param("doctorId") Long doctorId,
            @Param("status") Status status,
            @Param("bookingDate") LocalDate bookingDate
    );
}
