package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.CareBookingRequest.CareBookingCreateRequest;
import _2.LTW.dto.request.CareBookingRequest.CareBookingStatusUpdateRequest;
import _2.LTW.dto.request.CareBookingRequest.CareBookingUpdateRequest;
import _2.LTW.dto.response.CareBookingResponse;
import _2.LTW.service.CareBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class CareBookingController {

    private final CareBookingService careBookingService;

    @PostMapping("/care-bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareBookingResponse>> createBooking(
            @Valid @RequestBody CareBookingCreateRequest request) {
        CareBookingResponse response = careBookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Đặt lịch dịch vụ thành công"));
    }

    @GetMapping("/care-bookings/my-care-services")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CareBookingResponse>>> getMyBookings() {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.getMyBookings(),
                "Lấy danh sách booking của tôi thành công"
        ));
    }

    @GetMapping("/care-bookings/my-care-services/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareBookingResponse>> getMyBookingDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.getMyBookingDetail(id),
                "Lấy chi tiết booking thành công"
        ));
    }

    @PatchMapping("/care-bookings/my-care-services/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CareBookingResponse>> cancelMyBooking(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.cancelMyBooking(id),
                "Hủy booking thành công"
        ));
    }

    @GetMapping("/care-bookings-doctors")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<CareBookingResponse>>> getDoctorBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.getDoctorBookings(date, status),
                "Lấy danh sách booking thuộc về bác sĩ thành công"
        ));
    }

    @GetMapping("/care-bookings-doctors/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<CareBookingResponse>> getDoctorBookingDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.getDoctorBookingDetail(id),
                "Lấy chi tiết booking thuộc về bác sĩ thành công"
        ));
    }

    @PatchMapping("/care-bookings-doctors/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<CareBookingResponse>> updateDoctorBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody CareBookingStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.updateDoctorBookingStatus(id, request.getStatus()),
                "Cập nhật trạng thái booking thành công"
        ));
    }

    @GetMapping("/care-bookings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CareBookingResponse>>> getAllBookings() {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.getAllBookings(),
                "Lấy toàn bộ booking của mọi bác sĩ thành công"
        ));
    }

    @PatchMapping("/care-bookings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CareBookingResponse>> adminUpdateBooking(
            @PathVariable Long id,
            @RequestBody CareBookingUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                careBookingService.adminUpdateBooking(id, request),
                "Cập nhật thông tin booking thành công"
        ));
    }

    @DeleteMapping("/care-bookings/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBooking(@PathVariable Long id) {
        careBookingService.deleteBooking(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa booking thành công"));
    }
}
