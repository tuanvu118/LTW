package _2.LTW.service;

import _2.LTW.dto.request.care_service.CareServiceCreateRequest;
import _2.LTW.dto.request.care_service.CareServiceUpdateRequest;
import _2.LTW.dto.response.CareServiceResponse;
import _2.LTW.entity.CareService;
import _2.LTW.exception.AppException;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.CareServiceMapper;
import _2.LTW.repository.CareServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CareServiceService {

    private final CareServiceRepository careServiceRepository;
    private final CareServiceMapper careServiceMapper;

    /**
     * POST /care-services - Tạo dịch vụ chăm sóc mới (admin)
     */
    public CareServiceResponse createService(CareServiceCreateRequest request) {
        // Kiểm tra tên dịch vụ đã tồn tại
        if (careServiceRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.CONFLICT);
        }

        CareService careService = careServiceMapper.toEntity(request);
        CareService saved = careServiceRepository.save(careService);

        return careServiceMapper.toResponse(saved);
    }

    /**
     * GET /care-services - Lấy danh sách dịch vụ (authen)
     */
    @Transactional(readOnly = true)
    public Page<CareServiceResponse> getAllServices(Pageable pageable) {
        Page<CareService> services = careServiceRepository.findByIsActiveTrue(pageable);
        return services.map(careServiceMapper::toResponse);
    }

    /**
     * GET /care-services/{id} - Xem chi tiết dịch vụ (authen)
     */
    @Transactional(readOnly = true)
    public CareServiceResponse getServiceDetail(Long id) {
        CareService careService = careServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        return careServiceMapper.toResponse(careService);
    }

    /**
     * PUT /care-services/{id} - Cập nhật thông tin dịch vụ (admin)
     */
    public CareServiceResponse updateService(Long id, CareServiceUpdateRequest request) {
        CareService careService = careServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        // Kiểm tra tên dịch vụ đã tồn tại (trừ chính nó)
        if (request.getName() != null && careServiceRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new AppException(ErrorCode.CONFLICT);
        }

        careServiceMapper.updateEntity(careService, request);
        CareService updated = careServiceRepository.save(careService);

        return careServiceMapper.toResponse(updated);
    }

    /**
     * PATCH /care-services/{id}/status - Ẩn/hiện/tạm ngưng dịch vụ (admin)
     */
    public CareServiceResponse toggleServiceStatus(Long id) {
        CareService careService = careServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        careService.setIsActive(!careService.getIsActive());
        CareService updated = careServiceRepository.save(careService);

        return careServiceMapper.toResponse(updated);
    }

    /**
     * DELETE /care-services/{id} - Xóa dịch vụ (admin)
     */
    public void deleteService(Long id) {
        CareService careService = careServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        careServiceRepository.delete(careService);
    }
}

