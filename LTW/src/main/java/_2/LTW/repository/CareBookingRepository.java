package _2.LTW.repository;

import _2.LTW.entity.CareBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CareBookingRepository extends JpaRepository<CareBooking, Long> {

    // Tìm booking theo id và chưa bị xóa
    @Query("SELECT cb FROM CareBooking cb WHERE cb.id = :id AND cb.deleteAt IS NULL")
    Optional<CareBooking> findByIdAndNotDeleted(@Param("id") Long id);

    // Lấy danh sách booking của user (qua pet owner)
    @Query("SELECT cb FROM CareBooking cb WHERE cb.pet.user.id = :ownerId AND cb.deleteAt IS NULL ORDER BY cb.bookingDate DESC, cb.startTime DESC")
    Page<CareBooking> findByPetOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    // Lấy danh sách booking của bác sĩ
    @Query("SELECT cb FROM CareBooking cb WHERE cb.doctor.id = :doctorId AND cb.deleteAt IS NULL ORDER BY cb.bookingDate DESC, cb.startTime DESC")
    Page<CareBooking> findByDoctorId(@Param("doctorId") Long doctorId, Pageable pageable);

    // Lấy danh sách booking của bác sĩ theo ngày
    @Query("SELECT cb FROM CareBooking cb WHERE cb.doctor.id = :doctorId AND cb.bookingDate = :date AND cb.deleteAt IS NULL ORDER BY cb.startTime")
    List<CareBooking> findByDoctorIdAndBookingDate(@Param("doctorId") Long doctorId, @Param("date") LocalDate date);

    // Lấy danh sách booking của bác sĩ theo trạng thái
    @Query("SELECT cb FROM CareBooking cb WHERE cb.doctor.id = :doctorId AND cb.status = :status AND cb.deleteAt IS NULL ORDER BY cb.bookingDate DESC")
    Page<CareBooking> findByDoctorIdAndStatus(@Param("doctorId") Long doctorId, @Param("status") CareBooking.CareBookingStatus status, Pageable pageable);

    // Lấy danh sách booking của bác sĩ theo ngày và trạng thái
    @Query("SELECT cb FROM CareBooking cb WHERE cb.doctor.id = :doctorId " +
            "AND (:date IS NULL OR cb.bookingDate = :date) " +
            "AND (:status IS NULL OR cb.status = :status) " +
            "AND cb.deleteAt IS NULL ORDER BY cb.bookingDate DESC, cb.startTime DESC")
    Page<CareBooking> findByDoctorIdWithFilters(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("status") CareBooking.CareBookingStatus status,
            Pageable pageable);

    // Lấy tất cả booking (admin)
    @Query("SELECT cb FROM CareBooking cb WHERE cb.deleteAt IS NULL ORDER BY cb.bookingDate DESC, cb.startTime DESC")
    Page<CareBooking> findAllNotDeleted(Pageable pageable);
}

