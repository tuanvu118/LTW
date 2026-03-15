package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.CareRecordRequest.CreateCareRecordRequest;
import _2.LTW.dto.request.CareRecordRequest.UpdateCareRecordRequest;
import _2.LTW.dto.response.CareRecordResponse.CareRecordResponse;
import _2.LTW.dto.response.CareRecordResponse.CareRecordSummaryResponse;
import _2.LTW.service.CareRecordService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CareRecordController {

    CareRecordService careRecordService;

    @GetMapping("/care-records")
    ApiResponse<List<CareRecordSummaryResponse>> getAllCareRecords() {
        return ApiResponse.ok(careRecordService.getAllCareRecords());
    }

    @GetMapping("/care-records/me")
    ApiResponse<List<CareRecordSummaryResponse>> getMyCareRecords() {
        return ApiResponse.ok(careRecordService.getMyCareRecords());
    }

    @GetMapping("/care-records/{id}")
    ApiResponse<CareRecordResponse> getCareRecord(@PathVariable Long id) {
        return ApiResponse.ok(careRecordService.getCareRecord(id));
    }

    @PostMapping("/care-records")
    ApiResponse<CareRecordResponse> createCareRecord(
            @Valid @RequestBody CreateCareRecordRequest request
    ) {
        return ApiResponse.ok(
                careRecordService.createCareRecord(request),
                "Tạo hồ sơ chăm sóc thành công"
        );
    }

    @PutMapping("/care-records/{id}")
    ApiResponse<CareRecordResponse> updateCareRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCareRecordRequest request
    ) {
        return ApiResponse.ok(
                careRecordService.updateCareRecord(id, request),
                "Cập nhật hồ sơ chăm sóc thành công"
        );
    }

    @DeleteMapping("/care-records/{id}")
    ApiResponse<String> deleteCareRecord(@PathVariable Long id) {
        careRecordService.deleteCareRecord(id);
        return ApiResponse.ok("Xóa hồ sơ chăm sóc thành công");
    }
}

