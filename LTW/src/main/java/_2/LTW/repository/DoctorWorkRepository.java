package _2.LTW.repository;

import _2.LTW.entity.DoctorWork;
import _2.LTW.enums.ShiftType;
import _2.LTW.enums.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DoctorWorkRepository extends JpaRepository<DoctorWork, Long> {

    List<DoctorWork> findByDoctor_IdAndApplyFromWeek(
            Long doctorId,
            LocalDate applyFromWeek
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        JOIN FETCH dw.doctor d
        WHERE dw.applyFromWeek = (
            SELECT MAX(dw2.applyFromWeek)
            FROM DoctorWork dw2
            WHERE dw.doctor = dw2.doctor
            AND dw2.applyFromWeek <= :weekStart
        )
    """)
    List<DoctorWork> findLastestSchedulesByWeek(
            @Param("weekStart") LocalDate weekStart
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        JOIN FETCH dw.doctor d
        WHERE dw.applyFromWeek = (
            SELECT MAX(dw2.applyFromWeek)
            FROM DoctorWork dw2
            WHERE dw.doctor = dw2.doctor
            AND dw2.applyFromWeek <= :weekStart
            AND dw2.slotStatus = :status
        )
        AND dw.slotStatus = :status
    """)
    List<DoctorWork> findLastestSchedulesByWeekAndStatus(
            @Param("weekStart") LocalDate weekStart,
            @Param("status") SlotStatus status
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        WHERE dw.applyFromWeek = (
            SELECT MAX(dw2.applyFromWeek)
            FROM DoctorWork dw2
            WHERE dw2.applyFromWeek <= :weekStart
            AND dw2.doctor.id = :doctorId
        )
        AND dw.doctor.id = :doctorId
    """)
    List<DoctorWork> findLastestScheduleByWeek(
            @Param("doctorId") Long doctorId,
            @Param("weekStart") LocalDate weekStart
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        WHERE dw.applyFromWeek = (
            SELECT MAX(dw2.applyFromWeek)
            FROM DoctorWork dw2
            WHERE dw2.applyFromWeek <= :weekStart
            AND dw2.doctor.id = :doctorId
            AND dw2.slotStatus = :status
        )
        AND dw.doctor.id = :doctorId
        AND dw.slotStatus = :status
    """)
    List<DoctorWork> findLastestScheduleByWeekAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("weekStart") LocalDate weekStart,
            @Param("status") SlotStatus status
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        JOIN FETCH dw.doctor d
        WHERE dw.applyFromWeek = (
            SELECT MAX(dw2.applyFromWeek)
            FROM DoctorWork dw2
            WHERE dw2.doctor = dw.doctor
            AND :bookingDate >= dw2.applyFromWeek
        )
        AND dw.dayOfWeek = :dayOfWeek
        AND dw.shiftType = :shiftType
        AND dw.slotStatus = :status
    """)
    List<DoctorWork> findAvailableDoctors(
            @Param("bookingDate") LocalDate bookingDate,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("shiftType") ShiftType shiftType,
            @Param("status") SlotStatus status
    );

    void deleteByDoctor_IdAndApplyFromWeek(Long doctorId, LocalDate applyFromWeek);

}
