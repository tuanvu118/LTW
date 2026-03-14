package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.CareServiceRequest.CareServiceCreateRequest;
import _2.LTW.dto.request.CareServiceRequest.CareServiceUpdateRequest;
import _2.LTW.dto.response.CareServiceResponse;
import _2.LTW.service.CareServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/care-services")
@RequiredArgsConstructor
public class CareServiceController {

    private final CareServiceService careServiceService;

    /**
     * POST /care-services - Tạo dịch vụ chăm sóc mới (admin)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CareServiceResponse>> createService(
            @Valid @RequestBody CareServiceCreateRequest request) {
        CareServiceResponse response = careServiceService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Tạo dịch vụ thành công"));
    }

    /**
     * GET /care-services - Lấy danh sách dịch vụ (authen)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CareServiceResponse>>> getAllServices() {
        List<CareServiceResponse> response = careServiceService.getAllServices();
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy danh sách dịch vụ thành công"));
    }

    /**
     * GET /care-services/{id} - Xem chi tiết dịch vụ (authen)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CareServiceResponse>> getServiceDetail(@PathVariable Long id) {
        CareServiceResponse response = careServiceService.getServiceDetail(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Lấy chi tiết dịch vụ thành công"));
    }

    /**
     * PUT /care-services/{id} - Cập nhật thông tin dịch vụ (admin)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CareServiceResponse>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody CareServiceUpdateRequest request) {
        CareServiceResponse response = careServiceService.updateService(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật dịch vụ thành công"));
    }

    /**
     * PATCH /care-services/{id}/status - Ẩn/hiện/tạm ngưng dịch vụ (admin)
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CareServiceResponse>> toggleServiceStatus(@PathVariable Long id) {
        CareServiceResponse response = careServiceService.toggleServiceStatus(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Cập nhật trạng thái dịch vụ thành công"));
    }

    /**
     * DELETE /care-services/{id} - Xóa dịch vụ (admin)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        careServiceService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Xóa dịch vụ thành công"));
    }
}
