package _2.LTW.service;

import _2.LTW.dto.request.CareRecordRequest.CreateCareRecordRequest;
import _2.LTW.dto.request.CareRecordRequest.UpdateCareRecordRequest;
import _2.LTW.dto.response.CareRecordResponse.CareRecordResponse;
import _2.LTW.dto.response.CareRecordResponse.CareRecordSummaryResponse;
import _2.LTW.entity.CareBooking.CareBooking;
import _2.LTW.entity.CareBooking.CareBookingServiceItem;
import _2.LTW.entity.CareBooking.CareBookingStatus;
import _2.LTW.entity.CareRecord.CareRecord;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.CareRecordMapper;
import _2.LTW.repository.CareBookingRepository;
import _2.LTW.repository.CareRecordRepository;
import _2.LTW.util.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CareRecordService {

    CareRecordRepository careRecordRepository;
    CareBookingRepository careBookingRepository;
    CareRecordMapper careRecordMapper;
    SecurityUtil securityUtil;

    @PreAuthorize("hasRole('ADMIN')")
    public List<CareRecordSummaryResponse> getAllCareRecords() {
        return careRecordMapper.toCareRecordSummaryResponses(careRecordRepository.findAllDetail());
    }

    @PreAuthorize("isAuthenticated()")
    public CareRecordResponse getCareRecord(Long id) {
        CareRecord careRecord = careRecordRepository.findDetailById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy hồ sơ chăm sóc"));

        validateViewPermission(careRecord);
        return careRecordMapper.toCareRecordResponse(careRecord);
    }

    @PreAuthorize("isAuthenticated()")
    public List<CareRecordSummaryResponse> getMyCareRecords() {
        List<CareRecord> careRecords;

        if (securityUtil.isAdmin()) {
            careRecords = careRecordRepository.findAllDetail();
        } else if (securityUtil.isDoctor()) {
            careRecords = careRecordRepository.findAllByDoctorId(securityUtil.getCurrentUserId());
        } else {
            careRecords = careRecordRepository.findAllByOwnerId(securityUtil.getCurrentUserId());
        }

        return careRecordMapper.toCareRecordSummaryResponses(careRecords);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public CareRecordResponse createCareRecord(CreateCareRecordRequest request) {
        CareBooking booking = careBookingRepository.findDetailById(request.getCareBookingId())
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy lịch chăm sóc"));

        validateEditPermission(booking);

        if (booking.getStatus() != CareBookingStatus.COMPLETED) {
            throw ErrorCode.BAD_REQUEST.toException("Chỉ được lập hồ sơ cho lịch chăm sóc đã hoàn thành");
        }

        if (careRecordRepository.existsByCareBooking_Id(request.getCareBookingId())) {
            throw ErrorCode.CONFLICT.toException("Lịch chăm sóc này đã có hồ sơ");
        }

        CareRecord careRecord = careRecordMapper.toCareRecord(request);
        careRecord.setCareBooking(booking);
        careRecord.setTotalCost(calculateBookingTotalCost(booking));

        CareRecord saved = careRecordRepository.save(careRecord);
        return careRecordMapper.toCareRecordResponse(saved);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public CareRecordResponse updateCareRecord(Long id, UpdateCareRecordRequest request) {
        CareRecord careRecord = getCareRecordForEdit(id);

        if (careRecord.getCareBooking().getStatus() != CareBookingStatus.COMPLETED) {
            throw ErrorCode.BAD_REQUEST.toException("Chỉ được sửa hồ sơ cho lịch chăm sóc đã hoàn thành");
        }

        careRecordMapper.updateCareRecord(request, careRecord);
        careRecord.setTotalCost(calculateBookingTotalCost(careRecord.getCareBooking()));

        CareRecord saved = careRecordRepository.save(careRecord);
        return careRecordMapper.toCareRecordResponse(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteCareRecord(Long id) {
        CareRecord careRecord = careRecordRepository.findById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy hồ sơ chăm sóc"));

        careRecordRepository.delete(careRecord);
    }

    private CareRecord getCareRecordForEdit(Long id) {
        CareRecord careRecord = careRecordRepository.findDetailById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy hồ sơ chăm sóc"));

        validateEditPermission(careRecord.getCareBooking());
        return careRecord;
    }

    private void validateEditPermission(CareBooking booking) {
        boolean isAssignedDoctor = booking.getDoctor() != null
                && booking.getDoctor().getId().equals(securityUtil.getCurrentUserId());

        if (!securityUtil.isAdmin() && !isAssignedDoctor) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền tạo hoặc chỉnh sửa hồ sơ này");
        }
    }

    private void validateViewPermission(CareRecord careRecord) {
        CareBooking booking = careRecord.getCareBooking();

        boolean isAssignedDoctor = booking.getDoctor() != null
                && booking.getDoctor().getId().equals(securityUtil.getCurrentUserId());
        boolean isOwner = booking.getPet().getUser().getId().equals(securityUtil.getCurrentUserId());

        if (!securityUtil.isAdmin() && !isAssignedDoctor && !isOwner) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền xem hồ sơ chăm sóc này");
        }
    }

    private BigDecimal calculateBookingTotalCost(CareBooking booking) {
        if (booking.getCareBookingServices() == null || booking.getCareBookingServices().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return booking.getCareBookingServices().stream()
                .map(CareBookingServiceItem::getPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
