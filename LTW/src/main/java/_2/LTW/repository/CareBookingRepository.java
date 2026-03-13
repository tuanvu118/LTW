package _2.LTW.repository;

import _2.LTW.entity.CareBooking.CareBooking;
import _2.LTW.entity.CareBooking.CareBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CareBookingRepository extends JpaRepository<CareBooking, Long> {

    @Query("""
        select distinct cb
        from CareBooking cb
        join fetch cb.pet p
        join fetch p.user owner
        join fetch cb.doctor d
        left join fetch cb.careBookingServices cbs
        left join fetch cbs.careService cs
        where cb.id = :id
          and cb.deleteAt is null
    """)
    Optional<CareBooking> findDetailById(@Param("id") Long id);

    @Query("""
        select cb
        from CareBooking cb
        where cb.createdBy.id = :creatorId
          and cb.deleteAt is null
        order by cb.bookingDate desc, cb.startTime desc
    """)
    List<CareBooking> findAllByCreatorId(@Param("creatorId") Long creatorId);

    @Query("""
        select cb
        from CareBooking cb
        where cb.doctor.id = :doctorId
          and (:date is null or cb.bookingDate = :date)
          and (:status is null or cb.status = :status)
          and cb.deleteAt is null
        order by cb.bookingDate desc, cb.startTime desc
    """)
    List<CareBooking> findAllByDoctorId(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("status") CareBookingStatus status
    );

    @Query("""
        select cb
        from CareBooking cb
        where cb.deleteAt is null
        order by cb.bookingDate desc, cb.startTime desc
    """)
    List<CareBooking> findAllActive();

    @Query("""
        select distinct cb
        from CareBooking cb
        left join fetch cb.careBookingServices cbs
        left join fetch cbs.careService cs
        where cb.doctor.id = :doctorId
          and cb.bookingDate = :bookingDate
          and cb.status in :statuses
          and cb.deleteAt is null
    """)
    List<CareBooking> findDoctorBookingsInDate(
            @Param("doctorId") Long doctorId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("statuses") List<CareBookingStatus> statuses
    );
}
