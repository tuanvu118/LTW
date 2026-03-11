package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.MedicalRecordRequest.*;
import _2.LTW.dto.response.MedicalRecordResponse.*;
import _2.LTW.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordController {
    MedicalRecordService medicalRecordService;

    @PostMapping("/medical-records")
    ApiResponse<MedicalRecordResponse> createMedicalRecord(
            @Valid @RequestBody CreateMedicalRecordRequest request
    ) {
        return ApiResponse.ok(
                medicalRecordService.createMedicalRecord(request),
                "Tạo bệnh án thành công"
        );
    }

    @PutMapping("/medical-records/{id}")
    ApiResponse<MedicalRecordResponse> updateMedicalRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMedicalRecordRequest request
    ) {
        return ApiResponse.ok(
                medicalRecordService.updateMedicalRecord(id, request),
                "Cập nhật bệnh án thành công"
        );
    }

    @GetMapping("/medical-records/{id}")
    ApiResponse<MedicalRecordResponse> getMedicalRecord(@PathVariable Long id) {
        return ApiResponse.ok(medicalRecordService.getMedicalRecord(id));
    }

    @GetMapping("/medical-records/booking/{bookingId}")
    ApiResponse<MedicalRecordResponse> getMedicalRecordByBooking(@PathVariable Integer bookingId) {
        return ApiResponse.ok(medicalRecordService.getMedicalRecordByBooking(bookingId));
    }

    @PostMapping("/medical-records/{id}/prescriptions")
    ApiResponse<PrescriptionResponse> addPrescription(
            @PathVariable Long id,
            @Valid @RequestBody CreatePrescriptionRequest request
    ) {
        return ApiResponse.ok(
                medicalRecordService.addPrescription(id, request),
                "Thêm đơn thuốc thành công"
        );
    }

    @PutMapping("/medical-records/{id}/prescriptions/{prescriptionId}")
    ApiResponse<PrescriptionResponse> updatePrescription(
            @PathVariable Long id,
            @PathVariable Long prescriptionId,
            @Valid @RequestBody UpdatePrescriptionRequest request
    ) {
        return ApiResponse.ok(
                medicalRecordService.updatePrescription(id, prescriptionId, request),
                "Cập nhật đơn thuốc thành công"
        );
    }

    @DeleteMapping("/medical-records/{id}/prescriptions/{prescriptionId}")
    ApiResponse<String> deletePrescription(
            @PathVariable Long id,
            @PathVariable Long prescriptionId
    ) {
        medicalRecordService.deletePrescription(id, prescriptionId);
        return ApiResponse.ok("Xóa đơn thuốc thành công");
    }

    @GetMapping("/medical-records/{id}/prescriptions")
    ApiResponse<List<PrescriptionResponse>> getPrescriptions(@PathVariable Long id) {
        return ApiResponse.ok(medicalRecordService.getPrescriptions(id));
    }

    @PostMapping("/medical-records/{id}/treatments")
    ApiResponse<TreatmentRecordResponse> addTreatment(
            @PathVariable Long id,
            @Valid @RequestBody CreateTreatmentRecordRequest request
    ) {
        return ApiResponse.ok(
                medicalRecordService.addTreatment(id, request),
                "Thêm treatment thành công"
        );
    }

    @PutMapping("/medical-records/{id}/treatments/{treatmentId}")
    ApiResponse<TreatmentRecordResponse> updateTreatment(
            @PathVariable Long id,
            @PathVariable Long treatmentId,
            @Valid @RequestBody UpdateTreatmentRecordRequest request
    ) {
        return ApiResponse.ok(
                medicalRecordService.updateTreatment(id, treatmentId, request),
                "Cập nhật treatment thành công"
        );
    }

    @DeleteMapping("/medical-records/{id}/treatments/{treatmentId}")
    ApiResponse<String> deleteTreatment(
            @PathVariable Long id,
            @PathVariable Long treatmentId
    ) {
        medicalRecordService.deleteTreatment(id, treatmentId);
        return ApiResponse.ok("Xóa treatment thành công");
    }

    @GetMapping("/medical-records/{id}/treatments")
    ApiResponse<List<TreatmentRecordResponse>> getTreatments(@PathVariable Long id) {
        return ApiResponse.ok(medicalRecordService.getTreatments(id));
    }

    @GetMapping("/pets/{petId}/medical-records")
    ApiResponse<List<MedicalRecordSummaryResponse>> getPetMedicalRecords(@PathVariable Integer petId) {
        return ApiResponse.ok(medicalRecordService.getPetMedicalRecords(petId));
    }

    @GetMapping("/medical-bookings/{bookingId}/visit-detail")
    ApiResponse<VisitDetailResponse> getVisitDetail(@PathVariable Integer bookingId) {
        return ApiResponse.ok(medicalRecordService.getVisitDetail(bookingId));
    }
}
