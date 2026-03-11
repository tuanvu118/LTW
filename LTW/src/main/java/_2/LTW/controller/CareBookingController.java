package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.care_booking.CareBookingCreateRequest;
import _2.LTW.dto.request.care_booking.CareBookingStatusUpdateRequest;
import _2.LTW.dto.request.care_booking.CareBookingUpdateRequest;
import _2.LTW.dto.response.CareBookingResponse;
import _2.LTW.service.CareBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class CareBookingController {

    private final CareBookingService careBookingService;

    /**
     * POST /care-bookings - Đặt lịch dịch vụ (authen)
     */
    @PostMapping("/care-bookings")
    public ResponseEntity<ApiResponse<CareBookingResponse>> createBooking(
            @Valid @RequestBody CareBookingCreateRequest request) {
        CareBookingResponse response = careBookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Đặt lịch thành công"));
    }

    /**
     * GET /care-bookings/my - Lấy danh sách booking của tôi (authen)
     */
    @GetMapping("/care-bookings/my")
    public ResponseEntity<ApiResponse<Page<CareBookingResponse>>> getMyBookings(Pageable pageable) {
        Page<CareBookingResponse> response = careBookingService.getMyBookings(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy danh sách booking thành công"));
    }

    /**
     * GET /care-bookings/my/{id} - Xem chi tiết booking (authen)
     */
    @GetMapping("/care-bookings/my/{id}")
    public ResponseEntity<ApiResponse<CareBookingResponse>> getBookingDetail(@PathVariable Long id) {
        CareBookingResponse response = careBookingService.getBookingDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy chi tiết booking thành công"));
    }

    /**
     * PATCH /care-bookings/{id} - Cập nhật booking (chỉ được hủy nếu chưa xử lý) (authen)
     */
    @PatchMapping("/care-bookings/{id}")
    public ResponseEntity<ApiResponse<CareBookingResponse>> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody CareBookingUpdateRequest request) {
        CareBookingResponse response = careBookingService.updateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật booking thành công"));
    }

    /**
     * GET /care-bookings-doctors/ - Lấy danh sách booking thuộc về bác sĩ (lọc theo ngày, trạng thái) (Bác sĩ)
     */
    @GetMapping("/care-bookings-doctors")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<CareBookingResponse>>> getDoctorBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<CareBookingResponse> response = careBookingService.getDoctorBookings(date, status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy danh sách booking của bác sĩ thành công"));
    }

    /**
     * GET /care-bookings-doctors/{id} - Xem chi tiết booking thuộc về bác sĩ (Bác sĩ)
     */
    @GetMapping("/care-bookings-doctors/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CareBookingResponse>> getDoctorBookingDetail(@PathVariable Long id) {
        CareBookingResponse response = careBookingService.getDoctorBookingDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy chi tiết booking thành công"));
    }

    /**
     * PATCH /care-bookings-doctors/{id} - Cập nhật trạng thái booking (confirmed, in_progress, completed) (Bác sĩ)
     */
    @PatchMapping("/care-bookings-doctors/{id}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CareBookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody CareBookingStatusUpdateRequest request) {
        CareBookingResponse response = careBookingService.updateBookingStatus(id, request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật trạng thái booking thành công"));
    }

    /**
     * GET /care-bookings - Lấy toàn bộ booking của mọi bác sĩ (admin)
     */
    @GetMapping("/care-bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<CareBookingResponse>>> getAllBookings(Pageable pageable) {
        Page<CareBookingResponse> response = careBookingService.getAllBookings(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy tất cả booking thành công"));
    }

    /**
     * PATCH /care-bookings/{id} (admin) - Cập nhật thông tin booking (admin)
     */
    @PatchMapping("/care-bookings/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CareBookingResponse>> adminUpdateBooking(
            @PathVariable Long id,
            @Valid @RequestBody CareBookingUpdateRequest request) {
        CareBookingResponse response = careBookingService.adminUpdateBooking(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật booking thành công"));
    }

    /**
     * DELETE /care-bookings/{id} - Xóa booking (admin)
     */
    @DeleteMapping("/care-bookings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBooking(@PathVariable Long id) {
        careBookingService.deleteBooking(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa booking thành công"));
    }
}

