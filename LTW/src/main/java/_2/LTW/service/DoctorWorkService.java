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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorWorkService {

    UserRepository userRepository;
    DoctorWorkRepository doctorWorkRepository;
    DoctorWorkMapper doctorWorkMapper;
    UserMapper userMapper;
    SecurityUtil securityUtil;
    WeeklyScheduleValidator weeklyScheduleValidator;
    BookingDateTimeValidator bookingDateTimeValidator;
    Clock clock;

    @PreAuthorize("hasRole('DOCTOR')")
    public WeeklyScheduleResponse createNextWeekSchedule(WeeklyScheduleRequest request){

        Long doctorId = securityUtil.getCurrentUserId();

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException("Bác sĩ này không tồn tại"));

        LocalDate weekStart = getNextWeekMonday();

        weeklyScheduleValidator.validateSlots(request.getSlots());

        var exists = doctorWorkRepository.findByDoctor_IdAndApplyFromWeek(doctorId, weekStart);

        if(!exists.isEmpty()){
            throw ErrorCode.CONFLICT.toException("Đã đăng kí lịch tuần sau");
        }

        List<DoctorWork> slots = buildPendingSlots(doctor, weekStart, request);

        doctorWorkRepository.saveAll(slots);

        return buildWeeklyResponse(doctor, weekStart, slots);

    }

    private LocalDate getNextWeekMonday(){

        LocalDate today = LocalDate.now(clock);
        LocalDate nextWeek = today.plusWeeks(1);
        return nextWeek.with(DayOfWeek.MONDAY);

    }

    private List<DoctorWork> buildPendingSlots(
            User doctor,
            LocalDate weekStart,
            WeeklyScheduleRequest request
    ){

        return request.getSlots()
                .stream()
                .map(slotRequest -> {
                    DoctorWork slot = doctorWorkMapper.toDoctorWork(slotRequest);
                    slot.setDoctor(doctor);
                    slot.setApplyFromWeek(weekStart);
                    slot.setSlotStatus(SlotStatus.PENDING);

                    return slot;
                })
                .toList();

    }

    private List<DoctorWork> buildApprovedSlots(
            User doctor,
            LocalDate weekStart,
            WeeklyScheduleRequest request
    ){

        return request.getSlots()
                .stream()
                .map(slotRequest -> {
                    DoctorWork slot = doctorWorkMapper.toDoctorWork(slotRequest);
                    slot.setDoctor(doctor);
                    slot.setApplyFromWeek(weekStart);
                    slot.setSlotStatus(SlotStatus.APPROVED);

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

    public List<UserResponse> getAvailableDoctors(LocalDate bookingDate, LocalTime startTime){

        bookingDateTimeValidator.validate(bookingDate, startTime);

        int dayOfWeek = bookingDate.getDayOfWeek().getValue();
        ShiftType shiftType = calculateShiftType(startTime);

        List<DoctorWork> works = doctorWorkRepository.findAvailableDoctors(
                bookingDate,
                dayOfWeek,
                shiftType,
                SlotStatus.PENDING
        );

        return works.stream()
                .map(DoctorWork::getDoctor)
                .distinct()
                .map(userMapper::toUserResponse)
                .toList();

    }

    public List<WeeklyScheduleResponse> getWeeklySchedules(LocalDate weekStart, SlotStatus status){

        log.info(String.valueOf(LocalDateTime.now(clock)));

        if(securityUtil.isUser()){
            weeklyScheduleValidator.validateViewWeekStart(weekStart);
        }

        if(weekStart.getDayOfWeek() != DayOfWeek.MONDAY){
            throw ErrorCode.BAD_REQUEST.toException("Ngày bắt đầu tuần phải là thứ Hai");
        }

        List<DoctorWork> works = status != null
                ? doctorWorkRepository.findLastestSchedulesByWeekAndStatus(weekStart, status)
                :doctorWorkRepository.findLastestSchedulesByWeek(weekStart);

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

        if(securityUtil.isUser()){
            weeklyScheduleValidator.validateViewWeekStart(weekStart);
        }

        if(weekStart.getDayOfWeek() != DayOfWeek.MONDAY){
            throw ErrorCode.BAD_REQUEST.toException("Ngày bắt đầu tuần phải là thứ Hai");
        }

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Bác sĩ này không tồn tại"));

        List<DoctorWork> slots = status != null
                ? doctorWorkRepository.findLastestScheduleByWeekAndStatus(doctorId, weekStart, status)
                : doctorWorkRepository.findLastestScheduleByWeek(doctorId, weekStart);

        return buildWeeklyResponse(doctor, slots.getFirst().getApplyFromWeek(), slots);

    }

    @PreAuthorize("hasRole('ADMIN')or#doctorId==authentication.principal.id")
    @Transactional
    public WeeklyScheduleResponse updateWeeklySchedule(Long doctorId, WeeklyScheduleRequest request){

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException("Bác sĩ này không tồn tại"));

        LocalDate weekStart = getNextWeekMonday();

        List<DoctorWork> slots = doctorWorkRepository.findByDoctor_IdAndApplyFromWeek(doctorId, weekStart);

        if(slots.isEmpty()){
            throw ErrorCode.NOT_FOUND.toException("Bác sĩ chưa đăng kí lịch tuần này");
        }

        doctorWorkRepository.deleteByDoctor_IdAndApplyFromWeek(doctorId, weekStart);
        doctorWorkRepository.flush();

        weeklyScheduleValidator.validateSlots(request.getSlots());

        slots = securityUtil.isAdmin()
                ? buildApprovedSlots(doctor, weekStart, request)
                : buildPendingSlots(doctor, weekStart, request);

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

}
