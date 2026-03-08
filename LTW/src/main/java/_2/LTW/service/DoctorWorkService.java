package _2.LTW.service;

import _2.LTW.dto.request.doctor_work.WeeklyScheduleRequest;
import _2.LTW.dto.response.UserResponse;
import _2.LTW.dto.response.doctor_work.WeeklyScheduleResponse;
import _2.LTW.entity.DoctorWork;
import _2.LTW.entity.User;
import _2.LTW.enums.ShiftType;
import _2.LTW.enums.SlotStatus;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.DoctorWorkMapper;
import _2.LTW.mapper.UserMapper;
import _2.LTW.repository.DoctorWorkRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.util.SecurityUtil;
import _2.LTW.validate.BookingDateTimeValidator;
import _2.LTW.validate.WeeklyScheduleValidator;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorWorkService {

    static int MAX_DOCTOR_PER_SHIFT = 2;

    private record SlotKey(Integer dayOfWeek, ShiftType shiftType){

        @Override
        public String toString() {
            return DayOfWeek.of(dayOfWeek).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                    + " " +
                    shiftType.name().toLowerCase();
        }
    }

    UserRepository userRepository;
    DoctorWorkRepository doctorWorkRepository;
    DoctorWorkMapper doctorWorkMapper;
    UserMapper userMapper;
    SecurityUtil securityUtil;
    WeeklyScheduleValidator weeklyScheduleValidator;
    BookingDateTimeValidator bookingDateTimeValidator;
    Clock clock;

    @PreAuthorize("hasRole('DOCTOR')")
    @Transactional
    public WeeklyScheduleResponse createNextWeekSchedule(WeeklyScheduleRequest request){

        LocalDate weekStart = getNextWeekMonday();

        doctorWorkRepository.lockWeekSlots(weekStart);

        Long doctorId = securityUtil.getCurrentUserId();

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException("Bác sĩ này không tồn tại"));
        weeklyScheduleValidator.validateSlots(request.getSlots());

        var exists = doctorWorkRepository.findByDoctor_IdAndApplyFromWeek(doctorId, weekStart);

        if(!exists.isEmpty()){
            throw ErrorCode.CONFLICT.toException("Đã đăng kí lịch tuần sau");
        }

        List<DoctorWork> slots = buildSlots(doctor, weekStart, request, SlotStatus.PENDING);

        doctorWorkRepository.saveAll(slots);

        return buildWeeklyResponse(doctor, weekStart, slots);

    }

    public List<UserResponse> getAvailableDoctors(LocalDate bookingDate, LocalTime startTime){

        bookingDateTimeValidator.validate(bookingDate, startTime);

        int dayOfWeek = bookingDate.getDayOfWeek().getValue();
        ShiftType shiftType = calculateShiftType(startTime);
        LocalDate weekStart = bookingDate.with(DayOfWeek.MONDAY);

        List<User> doctors = doctorWorkRepository.findAvailableDoctors(
                weekStart,
                dayOfWeek,
                shiftType,
                SlotStatus.APPROVED
        );

        return doctors.stream()
                .map(userMapper::toUserResponse)
                .toList();

    }

    public List<WeeklyScheduleResponse> getWeeklySchedules(LocalDate weekStart, SlotStatus status){

        log.info(String.valueOf(LocalDateTime.now(clock)));

        if(!securityUtil.isDoctor() && !securityUtil.isAdmin()){
            weeklyScheduleValidator.validateViewWeekStartForUser(weekStart, status);
        }

        if(weekStart.getDayOfWeek() != DayOfWeek.MONDAY){
            throw ErrorCode.BAD_REQUEST.toException("Ngày bắt đầu tuần phải là thứ Hai");
        }

        List<DoctorWork> works = status != null
                ? doctorWorkRepository.findSchedulesByWeekAndStatus(weekStart, status)
                :doctorWorkRepository.findSchedulesByWeek(weekStart);

        if(works.isEmpty()){
            return List.of();
        }

        return works.stream()
                .collect(Collectors.groupingBy(DoctorWork::getDoctor))
                .entrySet()
                .stream()
                .map(entry -> buildWeeklyResponse(
                        entry.getKey(),
                        entry.getValue().getFirst().getApplyFromWeek(),
                        entry.getValue()
                ))
                .toList();

    }

    public WeeklyScheduleResponse getWeeklyScheduleByDoctor(Long doctorId, LocalDate weekStart, SlotStatus status){

        log.info(String.valueOf(LocalDateTime.now(clock)));

        if(!securityUtil.isDoctor() && !securityUtil.isAdmin()){
            weeklyScheduleValidator.validateViewWeekStartForUser(weekStart, status);
        }

        if(weekStart.getDayOfWeek() != DayOfWeek.MONDAY){
            throw ErrorCode.BAD_REQUEST.toException("Ngày bắt đầu tuần phải là thứ Hai");
        }

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Bác sĩ này không tồn tại"));

        List<DoctorWork> slots = status != null
                ? doctorWorkRepository.findScheduleByWeekAndStatus(doctorId, weekStart, status)
                : doctorWorkRepository.findScheduleByWeek(doctorId, weekStart);

        if(slots.isEmpty()){
            throw ErrorCode.NOT_FOUND.toException("Bác sĩ chưa có lịch tuần này với trạng thái đăng tìm kiếm");
        }

        return buildWeeklyResponse(doctor, slots.getFirst().getApplyFromWeek(), slots);

    }

    @PreAuthorize("hasRole('ADMIN')or#doctorId==authentication.principal.id")
    @Transactional
    public WeeklyScheduleResponse updateWeeklySchedule(Long doctorId, WeeklyScheduleRequest request){

        LocalDate weekStart = getNextWeekMonday();

        doctorWorkRepository.lockWeekSlots(weekStart);

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException("Bác sĩ này không tồn tại"));


        List<DoctorWork> slots = doctorWorkRepository.findByDoctor_IdAndApplyFromWeek(doctorId, weekStart);

        if(slots.isEmpty()){
            throw ErrorCode.NOT_FOUND.toException("Bác sĩ chưa đăng kí lịch tuần này");
        }

        weeklyScheduleValidator.validateSlots(request.getSlots());

        doctorWorkRepository.deleteByDoctor_IdAndApplyFromWeek(doctorId, weekStart);
        doctorWorkRepository.flush();

        SlotStatus status = securityUtil.isAdmin()
                ? SlotStatus.APPROVED
                : SlotStatus.PENDING;

        slots = buildSlots(doctor, weekStart, request, status);

        doctorWorkRepository.saveAll(slots);

        return buildWeeklyResponse(doctor, weekStart, slots);

    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public WeeklyScheduleResponse updateScheduleStatus(
            Long doctorId,
            SlotStatus status,
            LocalDate weekStart
    ){

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException("Bác sĩ này không tồn tại"));

        List<DoctorWork> slots = doctorWorkRepository.findByDoctor_IdAndApplyFromWeek(doctorId, weekStart);

        if(slots.isEmpty()){
            throw ErrorCode.NOT_FOUND.toException("Bác sĩ chưa đăng kí lịch tuần này");
        }

        slots.forEach(slot -> slot.setSlotStatus(status));

        return buildWeeklyResponse(doctor, weekStart, slots);

    }

    private LocalDate getNextWeekMonday(){

        LocalDate today = LocalDate.now(clock);
        LocalDate nextWeek = today.plusWeeks(1);
        return nextWeek.with(DayOfWeek.MONDAY);

    }

    private List<DoctorWork> buildSlots(
            User doctor,
            LocalDate weekStart,
            WeeklyScheduleRequest request,
            SlotStatus status
    ){

        return request.getSlots()
                .stream()
                .map(slotRequest -> {
                    DoctorWork slot = doctorWorkMapper.toDoctorWork(slotRequest);
                    slot.setDoctor(doctor);
                    slot.setApplyFromWeek(weekStart);
                    slot.setSlotStatus(status);

                    return slot;
                })
                .toList();

    }

    private WeeklyScheduleResponse buildWeeklyResponse(
            User doctor,
            LocalDate weekStart,
            List<DoctorWork> slots
    ){

        SlotStatus status = calculateWeekStatus(slots);

        return WeeklyScheduleResponse.builder()
                .doctorId(doctor.getId())
                .doctorName(doctor.getUsername())
                .applyFromWeek(weekStart)
                .status(status)
                .slots(doctorWorkMapper.toSlotResponses(slots))
                .build();

    }

    private SlotStatus calculateWeekStatus(List<DoctorWork> slots){

        if(slots.isEmpty()){
            return null;
        }

        boolean allApproved = slots.stream().allMatch(s -> s.getSlotStatus() == SlotStatus.APPROVED);

        if (allApproved){
            return SlotStatus.APPROVED;
        }

        boolean anyRejected = slots.stream().anyMatch(s -> s.getSlotStatus() == SlotStatus.REJECTED);

        if(anyRejected){
            return SlotStatus.REJECTED;
        }

        return SlotStatus.PENDING;

    }

    private ShiftType calculateShiftType(LocalTime startTime){

        if(startTime.isBefore(LocalTime.of(12, 30))){
            return ShiftType.MORNING;
        }
        else return ShiftType.AFTERNOON;

    }

}
