package _2.LTW.repository;

import _2.LTW.entity.DoctorWork;
import _2.LTW.entity.User;
import _2.LTW.enums.ShiftType;
import _2.LTW.enums.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
        WHERE dw.applyFromWeek = :weekStart
    """)
    List<DoctorWork> findSchedulesByWeek(
            @Param("weekStart") LocalDate weekStart
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        JOIN FETCH dw.doctor d
        WHERE dw.applyFromWeek = :weekStart
        AND dw.slotStatus = :status
    """)
    List<DoctorWork> findSchedulesByWeekAndStatus(
            @Param("weekStart") LocalDate weekStart,
            @Param("status") SlotStatus status
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        WHERE dw.applyFromWeek = :weekStart
        AND dw.doctor.id = :doctorId
    """)
    List<DoctorWork> findScheduleByWeek(
            @Param("doctorId") Long doctorId,
            @Param("weekStart") LocalDate weekStart
    );

    @Query("""
        SELECT dw
        FROM DoctorWork dw
        WHERE dw.applyFromWeek = :weekStart
        AND dw.doctor.id = :doctorId
        AND dw.slotStatus = :status
    """)
    List<DoctorWork> findScheduleByWeekAndStatus(
            @Param("doctorId") Long doctorId,
            @Param("weekStart") LocalDate weekStart,
            @Param("status") SlotStatus status
    );

    @Query("""
        SELECT dw.dayOfWeek, dw.shiftType
        FROM DoctorWork dw
        WHERE dw.applyFromWeek = :weekStart
        AND dw.doctor.id <> :doctorId
        AND dw.slotStatus IN :statuses
        GROUP BY dw.dayOfWeek, dw.shiftType
        HAVING COUNT(*) >= :capacity
    """)
    List<Object[]> findFullSlots(
            @Param("doctorId") Long doctorId,
            @Param("weekStart") LocalDate weekStart,
            @Param("statuses") List<SlotStatus> statuses,
            @Param("capacity") Integer capacity
    );

    @Query("""
        SELECT DISTINCT dw.doctor
        FROM DoctorWork dw
        JOIN dw.doctor d
        WHERE dw.applyFromWeek = :weekStart
        AND dw.dayOfWeek = :dayOfWeek
        AND dw.shiftType = :shiftType
        AND dw.slotStatus = :status
    """)
    List<User> findAvailableDoctors(
            @Param("weekStart") LocalDate weekStart,
            @Param("dayOfWeek") int dayOfWeek,
            @Param("shiftType") ShiftType shiftType,
            @Param("status") SlotStatus status
    );

    void deleteByDoctor_IdAndApplyFromWeek(Long doctorId, LocalDate applyFromWeek);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT dw
        FROM DoctorWork dw
        WHERE dw.applyFromWeek = :weekStart
    """)
    List<DoctorWork> lockWeekSlots(@Param("weekStart") LocalDate weekStart);

}
