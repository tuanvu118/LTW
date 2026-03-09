package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.MedicalBookingRequest.CreateMedicalBookingRequest;
import _2.LTW.dto.response.MedicalBookingResponse.DoctorAvailabilityResponse;
import _2.LTW.dto.response.MedicalBookingResponse.MedicalBookingResponse;
import _2.LTW.entity.MedicalBooking.Status;
import _2.LTW.service.MedicalBookingAppService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/medical-bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalBookingController {
    MedicalBookingAppService medicalBookingAppService;

    @PostMapping
    ApiResponse<MedicalBookingResponse> createBooking(
            @Valid @RequestBody CreateMedicalBookingRequest request
    ) {
        return ApiResponse.ok(
                medicalBookingAppService.createBooking(request),
                "Đặt lịch thành công"
        );
    }

    @GetMapping("/me")
    ApiResponse<List<MedicalBookingResponse>> getMyBookings() {
        return ApiResponse.ok(medicalBookingAppService.getMyBookings());
    }

    @GetMapping("/doctor/me")
    ApiResponse<List<MedicalBookingResponse>> getMyDoctorBookings() {
        return ApiResponse.ok(medicalBookingAppService.getMyDoctorBookings());
    }

    @GetMapping("/availability")
    ApiResponse<List<DoctorAvailabilityResponse>> getAvailability(
            @RequestParam LocalDate bookingDate,
            @RequestParam LocalTime startTime,
            @RequestParam List<Long> serviceIds
    ) {
        return ApiResponse.ok(
                medicalBookingAppService.getAvailableDoctors(bookingDate, startTime, serviceIds)
        );
    }

    @GetMapping("/{id}")
    ApiResponse<MedicalBookingResponse> getBookingDetail(@PathVariable Integer id) {
        return ApiResponse.ok(medicalBookingAppService.getBookingDetail(id));
    }

    @PatchMapping("/{id}/cancel")
    ApiResponse<MedicalBookingResponse> cancelBooking(@PathVariable Integer id) {
        return ApiResponse.ok(
                medicalBookingAppService.cancelBooking(id),
                "Hủy lịch thành công"
        );
    }

    @PatchMapping("/{id}/complete")
    ApiResponse<MedicalBookingResponse> completeBooking(@PathVariable Integer id) {
        return ApiResponse.ok(
                medicalBookingAppService.completeBooking(id),
                "Hoàn thành lịch khám thành công"
        );
    }

    @GetMapping
    ApiResponse<List<MedicalBookingResponse>> getAllBookings(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) LocalDate bookingDate
    ) {
        return ApiResponse.ok(
                medicalBookingAppService.getAllBookings(doctorId, status, bookingDate)
        );
    }
}
