package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.DoctorWork.WeeklyScheduleRequest;
import _2.LTW.dto.response.DoctorDailySlotResponse.AvailableSlotReponse;
import _2.LTW.dto.response.UserResponse;
import _2.LTW.dto.response.DoctorWork.SlotResponse;
import _2.LTW.dto.response.DoctorWork.WeeklyScheduleResponse;
import _2.LTW.entity.DoctorDailySlot.BookingType;
import _2.LTW.entity.DoctorWork.SlotStatus;
import _2.LTW.service.DoctorDailySlotService;
import _2.LTW.service.DoctorWorkService;
import _2.LTW.util.CustomPrincipal;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/doctor-works")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DoctorWorkController {

    DoctorWorkService doctorWorkService;
    DoctorDailySlotService doctorDailySlotService;

    @PostMapping("/me")
    ApiResponse<WeeklyScheduleResponse> createWeeklySchedule(@Valid @RequestBody WeeklyScheduleRequest request) {

        return ApiResponse.ok(doctorWorkService.createNextWeekSchedule(request));

    }

    @GetMapping("/available")
    ApiResponse<AvailableSlotReponse> getAvailableDoctor(
            @RequestParam LocalDate bookingDate,
            @RequestParam BookingType bookingType,
            @RequestParam List<Long> serviceIds
    ){

        return ApiResponse.ok(doctorDailySlotService.getAvailableSlots(bookingDate, bookingType, serviceIds));

    }

    @GetMapping("/full-slots")
    ApiResponse<List<SlotResponse>> getFullSlots(
            @AuthenticationPrincipal CustomPrincipal principal,
            @RequestParam LocalDate weekStart
    ){

        return ApiResponse.ok(doctorWorkService.getFullSlots(principal.getId(), weekStart).stream()
                .map(s -> new SlotResponse(s.dayOfWeek(), s.shiftType()))
                .toList());

    }

    @GetMapping
    ApiResponse<List<WeeklyScheduleResponse>> getWeeklySchedules(
            @RequestParam LocalDate weekStart,
            @RequestParam(required = false) SlotStatus status
    ) {

        return ApiResponse.ok(doctorWorkService.getWeeklySchedules(weekStart, status));

    }

    @GetMapping("/{id}")
    ApiResponse<WeeklyScheduleResponse> getWeeklyScheduleByDoctor(
            @PathVariable("id") Long doctorId,
            @RequestParam LocalDate weekStart,
            @RequestParam(required = false) SlotStatus status
    ) {

        return ApiResponse.ok(doctorWorkService.getWeeklyScheduleByDoctor(doctorId, weekStart, status));

    }

    @GetMapping("/me")
    ApiResponse<WeeklyScheduleResponse> getMyWeeklySchedule(
            @AuthenticationPrincipal CustomPrincipal principal,
            @RequestParam LocalDate weekStart,
            @RequestParam(required = false) SlotStatus status
    ) {

        return ApiResponse.ok(doctorWorkService.getWeeklyScheduleByDoctor(principal.getId(), weekStart, status));

    }

    @PutMapping("/me")
    ApiResponse<WeeklyScheduleResponse> updateMySchedule(
            @AuthenticationPrincipal CustomPrincipal principal,
            @Valid @RequestBody WeeklyScheduleRequest request
    ){

        return ApiResponse.ok(doctorWorkService.updateWeeklySchedule(principal.getId(), request));

    }

    @PutMapping("/{id}")
    ApiResponse<WeeklyScheduleResponse> updateScheduleByDoctorId(
            @PathVariable Long id,
            @Valid @RequestBody WeeklyScheduleRequest request
    ){

        return ApiResponse.ok(doctorWorkService.updateWeeklySchedule(id, request));

    }

    @PatchMapping("/{id}/{status}")
    ApiResponse<WeeklyScheduleResponse> setStatusSchedule(
            @PathVariable Long id,
            @PathVariable SlotStatus status,
            @RequestParam LocalDate weekStart
    ){

        return ApiResponse.ok(doctorWorkService.updateScheduleStatus(id, status, weekStart));

    }

}