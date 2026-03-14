package _2.LTW.repository;

import _2.LTW.entity.CareRecord.CareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CareRecordRepository extends JpaRepository<CareRecord, Long> {

    boolean existsByCareBooking_Id(Long careBookingId);

    @Query("""
        select cr
        from CareRecord cr
        join fetch cr.careBooking cb
        join fetch cb.pet p
        join fetch p.user owner
        join fetch cb.doctor d
        where cr.id = :id
          and cb.deleteAt is null
    """)
    Optional<CareRecord> findDetailById(@Param("id") Long id);

    @Query("""
        select cr
        from CareRecord cr
        join fetch cr.careBooking cb
        join fetch cb.pet p
        join fetch p.user owner
        join fetch cb.doctor d
        where cb.deleteAt is null
        order by cb.bookingDate desc, cb.startTime desc
    """)
    List<CareRecord> findAllDetail();

    @Query("""
        select cr
        from CareRecord cr
        join fetch cr.careBooking cb
        join fetch cb.pet p
        join fetch p.user owner
        join fetch cb.doctor d
        where d.id = :doctorId
          and cb.deleteAt is null
        order by cb.bookingDate desc, cb.startTime desc
    """)
    List<CareRecord> findAllByDoctorId(@Param("doctorId") Long doctorId);

    @Query("""
        select cr
        from CareRecord cr
        join fetch cr.careBooking cb
        join fetch cb.pet p
        join fetch p.user owner
        join fetch cb.doctor d
        where owner.id = :ownerId
          and cb.deleteAt is null
        order by cb.bookingDate desc, cb.startTime desc
    """)
    List<CareRecord> findAllByOwnerId(@Param("ownerId") Long ownerId);
}

