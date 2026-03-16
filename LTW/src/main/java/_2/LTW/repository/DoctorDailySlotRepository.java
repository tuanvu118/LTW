package _2.LTW.repository;

import _2.LTW.entity.DoctorDailySlot.DoctorDailySlot;
import _2.LTW.entity.DoctorDailySlot.DoctorDailySlotId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DoctorDailySlotRepository extends JpaRepository<DoctorDailySlot, DoctorDailySlotId> {

    @Query("""
        SELECT s.slotTime, s.doctor.id, s.doctor.username, s.doctor.imageUrl
        FROM DoctorDailySlot s
        WHERE s.slotDate = :date
        AND s.status = 'AVAILABLE'
        ORDER BY s.slotTime
    """)
    List<Object[]> findAvailableSlots(@Param("date") LocalDate date);

    @Query("""
        SELECT s
        FROM DoctorDailySlot s
        WHERE s.doctor.id = :doctorId
        AND s.slotDate = :date
        AND s.slotTime >= :startTime
        AND s.slotTime < :endTime
        ORDER BY s.slotTime
    """)
    List<DoctorDailySlot> findSlotsByDoctorIdAndDateTime(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM DoctorDailySlot s
        WHERE s.doctor.id = :doctorId
        AND s.slotDate = :date
        AND s.slotTime >= :startTime
        AND s.slotTime < :endTime
        ORDER BY s.slotTime
    """)
    List<DoctorDailySlot> lockSlots(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    boolean existsDoctorDailySlotBySlotDate(LocalDate date);
}
