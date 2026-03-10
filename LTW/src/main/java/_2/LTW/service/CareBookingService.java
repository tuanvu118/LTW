package _2.LTW.service;

import _2.LTW.dto.request.care_booking.CareBookingCreateRequest;
import _2.LTW.dto.request.care_booking.CareBookingUpdateRequest;
import _2.LTW.dto.response.CareBookingResponse;
import _2.LTW.entity.CareBooking;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.entity.User;
import _2.LTW.exception.AppException;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.CareBookingMapper;
import _2.LTW.repository.CareBookingRepository;
import _2.LTW.repository.PetRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CareBookingService {

    private final CareBookingRepository careBookingRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final CareBookingMapper careBookingMapper;
    private final SecurityUtil securityUtil;

    /**
     * POST /care-bookings - Đặt lịch dịch vụ (authen)
     */
    public CareBookingResponse createBooking(CareBookingCreateRequest request) {
        Long ownerId = securityUtil.getCurrentUserId();

        // Tìm pet
        Pets pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new AppException(ErrorCode.PET_NOT_FOUND));

        // Kiểm tra pet thuộc về user hiện tại
        if (!pet.getUser().getId().equals(ownerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Tìm doctor
        User doctor = userRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tạo booking
        CareBooking booking = careBookingMapper.toEntity(request, pet, doctor);
        CareBooking saved = careBookingRepository.save(booking);

        return careBookingMapper.toResponse(saved);
    }

    /**
     * GET /care-bookings/my - Lấy danh sách booking của tôi (authen)
     */
    @Transactional(readOnly = true)
    public Page<CareBookingResponse> getMyBookings(Pageable pageable) {
        Long ownerId = securityUtil.getCurrentUserId();
        Page<CareBooking> bookings = careBookingRepository.findByPetOwnerId(ownerId, pageable);
        return bookings.map(careBookingMapper::toResponse);
    }

    /**
     * GET /care-bookings/my/{id} - Xem chi tiết booking (authen)
     */
    @Transactional(readOnly = true)
    public CareBookingResponse getBookingDetail(Long bookingId) {
        Long ownerId = securityUtil.getCurrentUserId();
        CareBooking booking = careBookingRepository.findByIdAndNotDeleted(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CARE_BOOKING_NOT_FOUND));

        // Kiểm tra quyền: chỉ owner của pet mới xem được
        if (!booking.getPet().getUser().getId().equals(ownerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return careBookingMapper.toResponse(booking);
    }

    /**
     * PATCH /care-bookings/{id} - Cập nhật booking (chỉ được hủy nếu chưa xử lý) (authen)
     */
    public CareBookingResponse updateBooking(Long bookingId, CareBookingUpdateRequest request) {
        Long ownerId = securityUtil.getCurrentUserId();
        CareBooking booking = careBookingRepository.findByIdAndNotDeleted(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CARE_BOOKING_NOT_FOUND));

        // Kiểm tra quyền
        if (!booking.getPet().getUser().getId().equals(ownerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Chỉ được cập nhật khi status là PENDING
        if (booking.getStatus() != CareBooking.CareBookingStatus.PENDING) {
            throw new AppException(ErrorCode.CARE_BOOKING_CANNOT_UPDATE);
        }

        careBookingMapper.updateEntity(booking, request);
        CareBooking updated = careBookingRepository.save(booking);

        return careBookingMapper.toResponse(updated);
    }

    /**
     * GET /care-bookings-doctors/ - Lấy danh sách booking thuộc về bác sĩ (lọc theo ngày, trạng thái) (Bác sĩ)
     */
    @Transactional(readOnly = true)
    public Page<CareBookingResponse> getDoctorBookings(LocalDate date, String status, Pageable pageable) {
        Long doctorId = securityUtil.getCurrentUserId();

        CareBooking.CareBookingStatus bookingStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                bookingStatus = CareBooking.CareBookingStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.CARE_BOOKING_INVALID_STATUS);
            }
        }

        Page<CareBooking> bookings = careBookingRepository.findByDoctorIdWithFilters(doctorId, date, bookingStatus, pageable);
        return bookings.map(careBookingMapper::toResponse);
    }

    /**
     * GET /care-bookings-doctors/{id} - Xem chi tiết booking thuộc về bác sĩ (Bác sĩ)
     */
    @Transactional(readOnly = true)
    public CareBookingResponse getDoctorBookingDetail(Long bookingId) {
        Long doctorId = securityUtil.getCurrentUserId();
        CareBooking booking = careBookingRepository.findByIdAndNotDeleted(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CARE_BOOKING_NOT_FOUND));

        // Kiểm tra quyền: chỉ doctor của booking mới xem được
        if (!booking.getDoctor().getId().equals(doctorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return careBookingMapper.toResponse(booking);
    }

    /**
     * PATCH /care-bookings-doctors/{id} - Cập nhật trạng thái booking (confirmed, in_progress, completed) (Bác sĩ)
     */
    public CareBookingResponse updateBookingStatus(Long bookingId, String status) {
        Long doctorId = securityUtil.getCurrentUserId();
        CareBooking booking = careBookingRepository.findByIdAndNotDeleted(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CARE_BOOKING_NOT_FOUND));

        // Kiểm tra quyền
        if (!booking.getDoctor().getId().equals(doctorId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        try {
            CareBooking.CareBookingStatus newStatus = CareBooking.CareBookingStatus.valueOf(status.toUpperCase());
            booking.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.CARE_BOOKING_INVALID_STATUS);
        }

        CareBooking updated = careBookingRepository.save(booking);
        return careBookingMapper.toResponse(updated);
    }

    /**
     * GET /care-bookings - Lấy toàn bộ booking của mọi bác sĩ (admin)
     */
    @Transactional(readOnly = true)
    public Page<CareBookingResponse> getAllBookings(Pageable pageable) {
        Page<CareBooking> bookings = careBookingRepository.findAllNotDeleted(pageable);
        return bookings.map(careBookingMapper::toResponse);
    }

    /**
     * PATCH /care-bookings/{id} - Cập nhật thông tin booking (admin)
     */
    public CareBookingResponse adminUpdateBooking(Long bookingId, CareBookingUpdateRequest request) {
        CareBooking booking = careBookingRepository.findByIdAndNotDeleted(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CARE_BOOKING_NOT_FOUND));

        careBookingMapper.updateEntity(booking, request);
        CareBooking updated = careBookingRepository.save(booking);

        return careBookingMapper.toResponse(updated);
    }

    /**
     * DELETE /care-bookings/{id} - Xóa booking (soft delete) (admin)
     */
    public void deleteBooking(Long bookingId) {
        CareBooking booking = careBookingRepository.findByIdAndNotDeleted(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.CARE_BOOKING_NOT_FOUND));

        booking.setDeleteAt(LocalDateTime.now());
        careBookingRepository.save(booking);
    }
}

