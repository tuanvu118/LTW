package _2.LTW.service;

import _2.LTW.dto.request.MedicalBookingRequest.CreateMedicalBookingRequest;
import _2.LTW.dto.request.MedicalBookingRequest.CreateMedicalBookingServiceItemRequest;
import _2.LTW.dto.response.MedicalBookingResponse.DoctorAvailabilityResponse;
import _2.LTW.dto.response.MedicalBookingResponse.MedicalBookingResponse;
import _2.LTW.entity.*;
import _2.LTW.entity.MedicalBooking.MedicalBooking;
import _2.LTW.entity.MedicalBooking.MedicalBookingService;
import _2.LTW.entity.MedicalBooking.Status;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.enums.RoleEnum;
import _2.LTW.enums.ShiftType;
import _2.LTW.enums.SlotStatus;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.MedicalBookingMapper;
import _2.LTW.repository.*;
import _2.LTW.util.SecurityUtil;
import _2.LTW.validate.BookingDateTimeValidator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalBookingAppService {
    MedicalBookingRepository medicalBookingRepository;
    MedicalServiceRepository medicalServiceRepository;
    DoctorWorkRepository doctorWorkRepository;
    PetRepository petRepository;
    UserRepository userRepository;
    UserRoleRepository userRoleRepository;
    SecurityUtil securityUtil;
    BookingDateTimeValidator bookingDateTimeValidator;
    MedicalBookingMapper medicalBookingMapper;

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Transactional
    public MedicalBookingResponse createBooking(CreateMedicalBookingRequest request) {
        bookingDateTimeValidator.validate(request.getBookingDate(), request.getStartTime());

        Pets pet = getPetAndValidateOwner(request.getPetId());
        User doctor = getDoctorAndValidateRole(request.getDoctorId());

        Map<Long, MedicalService> serviceMap = getMedicalServiceMap(request.getServices());
        int totalDuration = calculateTotalDuration(request.getServices(), serviceMap);
        LocalTime estimatedEndTime = request.getStartTime().plusMinutes(totalDuration);

        validateWithinSingleShift(request.getStartTime(), estimatedEndTime);
        ensureDoctorHasApprovedShift(doctor.getId(), request.getBookingDate(), request.getStartTime());
        ensureNoBookingOverlap(doctor.getId(), request.getBookingDate(), request.getStartTime(), estimatedEndTime);

        MedicalBooking booking = medicalBookingMapper.toMedicalBooking(request);
        booking.setPets(pet);
        booking.setDoctor(doctor);
        booking.setStatus(Status.BOOKED);

        booking.setDeleteAt(null);

        List<MedicalBookingService> details = buildBookingDetails(request, booking, serviceMap);
        booking.setMedicalBookingsService(details);

        MedicalBooking saved = medicalBookingRepository.save(booking);
        return medicalBookingMapper.toMedicalBookingResponse(saved);
    }

    @PreAuthorize("hasRole('USER')")
    public List<MedicalBookingResponse> getMyBookings() {
        return medicalBookingMapper.toMedicalBookingResponses(
                medicalBookingRepository.findAllByOwnerId(securityUtil.getCurrentUserId())
        );
    }

    @PreAuthorize("isAuthenticated()")
    public MedicalBookingResponse getBookingDetail(Integer bookingId) {
        MedicalBooking booking = medicalBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy booking"));

        boolean isOwner = booking.getPets().getUser().getId().equals(securityUtil.getCurrentUserId());
        boolean isAssignedDoctor = booking.getDoctor().getId().equals(securityUtil.getCurrentUserId());

        if (!securityUtil.isAdmin() && !isOwner && !isAssignedDoctor) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền xem booking này");
        }

        return medicalBookingMapper.toMedicalBookingResponse(booking);
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Transactional
    public MedicalBookingResponse cancelBooking(Integer bookingId) {
        MedicalBooking booking = medicalBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy booking"));

        boolean isOwner = booking.getPets().getUser().getId().equals(securityUtil.getCurrentUserId());

        if (!securityUtil.isAdmin() && !isOwner) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền hủy booking này");
        }

        if (booking.getStatus() != Status.BOOKED) {
            throw ErrorCode.BAD_REQUEST.toException("Chỉ được hủy booking đang ở trạng thái BOOKED");
        }

        booking.setStatus(Status.CANCELLED);
        return medicalBookingMapper.toMedicalBookingResponse(medicalBookingRepository.save(booking));
    }

    @PreAuthorize("hasRole('DOCTOR')")
    public List<MedicalBookingResponse> getMyDoctorBookings() {
        return medicalBookingMapper.toMedicalBookingResponses(
                medicalBookingRepository.findAllByDoctorId(securityUtil.getCurrentUserId())
        );
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public MedicalBookingResponse completeBooking(Integer bookingId) {
        MedicalBooking booking = medicalBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy booking"));

        boolean isAssignedDoctor = booking.getDoctor().getId().equals(securityUtil.getCurrentUserId());

        if (!securityUtil.isAdmin() && !isAssignedDoctor) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền hoàn thành booking này");
        }

        if (booking.getStatus() != Status.BOOKED) {
            throw ErrorCode.BAD_REQUEST.toException("Chỉ được hoàn thành booking đang ở trạng thái BOOKED");
        }

        booking.setStatus(Status.COMPLETED);
        return medicalBookingMapper.toMedicalBookingResponse(medicalBookingRepository.save(booking));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<MedicalBookingResponse> getAllBookings(Long doctorId, Status status, LocalDate bookingDate) {
        return medicalBookingMapper.toMedicalBookingResponses(
                medicalBookingRepository.search(doctorId, status, bookingDate)
        );
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<DoctorAvailabilityResponse> getAvailableDoctors(
            LocalDate bookingDate,
            LocalTime startTime,
            List<Long> serviceIds
    ) {
        bookingDateTimeValidator.validate(bookingDate, startTime);

        List<MedicalService> services = medicalServiceRepository.findAllById(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw ErrorCode.NOT_FOUND.toException("Có dịch vụ không tồn tại");
        }

        int totalDuration = services.stream()
                .map(MedicalService::getTimeDuration)
                .reduce(0, Integer::sum);

        LocalTime estimatedEndTime = startTime.plusMinutes(totalDuration);
        validateWithinSingleShift(startTime, estimatedEndTime);

        int dayOfWeek = bookingDate.getDayOfWeek().getValue();
        ShiftType shiftType = calculateShiftType(startTime);

        List<DoctorWork> works = doctorWorkRepository.findAvailableDoctors(
                bookingDate,
                dayOfWeek,
                shiftType,
                SlotStatus.APPROVED
        );

        return works.stream()
                .map(DoctorWork::getDoctor)
                .distinct()
                .filter(doctor -> isDoctorFree(doctor.getId(), bookingDate, startTime, estimatedEndTime))
                .map(doctor -> DoctorAvailabilityResponse.builder()
                        .doctorId(doctor.getId())
                        .doctorName(doctor.getUsername())
                        .bookingDate(bookingDate)
                        .startTime(startTime)
                        .estimatedEndTime(estimatedEndTime)
                        .totalDuration(totalDuration)
                        .build())
                .toList();
    }

    private Pets getPetAndValidateOwner(Integer petId) {
        Pets pet = petRepository.findActiveById(petId)
                .orElseThrow(() -> ErrorCode.PET_NOT_FOUND.toException("Không tìm thấy pet"));

        if (!securityUtil.isAdmin() && !pet.getUser().getId().equals(securityUtil.getCurrentUserId())) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền đặt lịch cho pet này");
        }

        return pet;
    }

    private User getDoctorAndValidateRole(Long doctorId) {
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException("Không tìm thấy bác sĩ"));

        boolean isDoctor = userRoleRepository.findByUser_Id(doctor.getId()).stream()
                .map(UserRole::getRole)
                .map(Role::getRoleEnum)
                .anyMatch(role -> role == RoleEnum.DOCTOR);

        if (!isDoctor) {
            throw ErrorCode.BAD_REQUEST.toException("Người được chọn không phải bác sĩ");
        }

        return doctor;
    }

    private Map<Long, MedicalService> getMedicalServiceMap(List<CreateMedicalBookingServiceItemRequest> items) {
        List<Long> ids = items.stream()
                .map(CreateMedicalBookingServiceItemRequest::getMedicalServiceId)
                .distinct()
                .toList();

        List<MedicalService> services = medicalServiceRepository.findAllById(ids);

        if (services.size() != ids.size()) {
            throw ErrorCode.NOT_FOUND.toException("Có dịch vụ không tồn tại");
        }

        return services.stream()
                .collect(Collectors.toMap(MedicalService::getId, Function.identity()));
    }

    private int calculateTotalDuration(
            List<CreateMedicalBookingServiceItemRequest> items,
            Map<Long, MedicalService> serviceMap
    ) {
        return items.stream()
                .mapToInt(item -> serviceMap.get(item.getMedicalServiceId()).getTimeDuration())
                .sum();
    }

    private List<MedicalBookingService> buildBookingDetails(
            CreateMedicalBookingRequest request,
            MedicalBooking booking,
            Map<Long, MedicalService> serviceMap
    ) {
        return request.getServices().stream()
                .map(medicalBookingMapper::toMedicalBookingService)
                .peek(detail -> {
                    Long medicalServiceId = request.getServices()
                            .get(request.getServices().indexOf(
                                    request.getServices().stream()
                                            .filter(item -> item.getNotes() == null
                                                    ? detail.getNotes() == null
                                                    : item.getNotes().equals(detail.getNotes()))
                                            .findFirst()
                                            .orElseThrow()
                            ))
                            .getMedicalServiceId();

                    MedicalService master = serviceMap.get(medicalServiceId);
                    detail.setMedicalBooking(booking);
                    detail.setMedicalService(master);
                    detail.setTimeDuration(master.getTimeDuration());
                })
                .toList();
    }

    private void validateWithinSingleShift(LocalTime startTime, LocalTime estimatedEndTime) {
        ShiftType startShift = calculateShiftType(startTime);
        ShiftType endShift = calculateShiftType(estimatedEndTime.minusMinutes(1));

        if (startShift != endShift) {
            throw ErrorCode.BAD_REQUEST.toException("Tổng thời lượng dịch vụ vượt quá phạm vi 1 ca khám");
        }
    }

    private void ensureDoctorHasApprovedShift(Long doctorId, LocalDate bookingDate, LocalTime startTime) {
        int dayOfWeek = bookingDate.getDayOfWeek().getValue();
        ShiftType shiftType = calculateShiftType(startTime);

        boolean hasApprovedShift = doctorWorkRepository.findAvailableDoctors(
                bookingDate,
                dayOfWeek,
                shiftType,
                SlotStatus.APPROVED
        ).stream().anyMatch(work -> work.getDoctor().getId().equals(doctorId));

        if (!hasApprovedShift) {
            throw ErrorCode.BAD_REQUEST.toException("Bác sĩ không có lịch làm việc hợp lệ ở thời điểm này");
        }
    }

    private void ensureNoBookingOverlap(
            Long doctorId,
            LocalDate bookingDate,
            LocalTime newStart,
            LocalTime newEnd
    ) {
        List<MedicalBooking> bookings = medicalBookingRepository.findDoctorBookingsInDate(
                doctorId,
                bookingDate,
                List.of(Status.BOOKED)
        );

        boolean overlapped = bookings.stream().anyMatch(existing ->
                existing.getStartTime().isBefore(newEnd)
                        && medicalBookingMapper.toEstimatedEndTime(existing).isAfter(newStart)
        );

        if (overlapped) {
            throw ErrorCode.CONFLICT.toException("Bác sĩ đã bận trong khoảng thời gian này");
        }
    }

    private boolean isDoctorFree(Long doctorId, LocalDate bookingDate, LocalTime newStart, LocalTime newEnd) {
        return medicalBookingRepository.findDoctorBookingsInDate(
                doctorId,
                bookingDate,
                List.of(Status.BOOKED)
        ).stream().noneMatch(existing ->
                existing.getStartTime().isBefore(newEnd)
                        && medicalBookingMapper.toEstimatedEndTime(existing).isAfter(newStart)
        );
    }

    private ShiftType calculateShiftType(LocalTime startTime) {
        if (startTime.isBefore(LocalTime.of(12, 30))) {
            return ShiftType.MORNING;
        }
        return ShiftType.AFTERNOON;
    }
}
