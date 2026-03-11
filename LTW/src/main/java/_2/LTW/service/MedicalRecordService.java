package _2.LTW.service;

import _2.LTW.dto.request.MedicalRecordRequest.*;
import _2.LTW.dto.response.MedicalBookingResponse.MedicalBookingResponse;
import _2.LTW.dto.response.MedicalRecordResponse.*;
import _2.LTW.entity.MedicalBooking.MedicalBooking;
import _2.LTW.entity.MedicalBooking.Status;
import _2.LTW.entity.MedicalRecord.MedicalRecords;
import _2.LTW.entity.MedicalRecord.Prescriptions;
import _2.LTW.entity.MedicalRecord.TreatmentRecord;
import _2.LTW.entity.Medicine;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.entity.TreatmentMethods;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.MedicalBookingMapper;
import _2.LTW.mapper.MedicalRecordMapper;
import _2.LTW.repository.*;
import _2.LTW.util.SecurityUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordService {
    MedicalRecordsRepository medicalRecordsRepository;
    PrescriptionsRepository prescriptionsRepository;
    TreatmentRecordRepository treatmentRecordRepository;
    MedicalBookingRepository medicalBookingRepository;
    MedicineRepository medicineRepository;
    TreatmentMethodsRepository treatmentMethodsRepository;
    PetRepository petRepository;
    MedicalRecordMapper medicalRecordMapper;
    MedicalBookingMapper medicalBookingMapper;
    SecurityUtil securityUtil;

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest request) {
        MedicalBooking booking = medicalBookingRepository.findDetailById(request.getMedicalBookingId())
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy lịch khám"));

        validateEditPermission(booking);

        if (booking.getStatus() != Status.BOOKED && booking.getStatus() != Status.COMPLETED) {
            throw ErrorCode.BAD_REQUEST.toException("Chỉ được lập bệnh án cho booking đang BOOKED hoặc COMPLETED");
        }

        if (medicalRecordsRepository.existsByMedicalBooking_Id(request.getMedicalBookingId())) {
            throw ErrorCode.CONFLICT.toException("Booking này đã có bệnh án");
        }

        MedicalRecords medicalRecord = medicalRecordMapper.toMedicalRecord(request);
        medicalRecord.setMedicalBooking(booking);
        medicalRecord.setTotalCost(BigDecimal.ZERO);

        MedicalRecords saved = medicalRecordsRepository.save(medicalRecord);
        return toMedicalRecordDetail(saved);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public MedicalRecordResponse updateMedicalRecord(Long id, UpdateMedicalRecordRequest request) {
        MedicalRecords medicalRecord = getMedicalRecordForEdit(id);

        medicalRecordMapper.updateMedicalRecord(request, medicalRecord);

        MedicalRecords saved = medicalRecordsRepository.save(medicalRecord);
        return toMedicalRecordDetail(saved);
    }

    @PreAuthorize("isAuthenticated()")
    public MedicalRecordResponse getMedicalRecord(Long id) {
        MedicalRecords medicalRecord = medicalRecordsRepository.findDetailById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy bệnh án"));

        validateViewPermission(medicalRecord.getMedicalBooking());
        return toMedicalRecordDetail(medicalRecord);
    }

    @PreAuthorize("isAuthenticated()")
    public MedicalRecordResponse getMedicalRecordByBooking(Integer bookingId) {
        MedicalRecords medicalRecord = medicalRecordsRepository.findDetailByBookingId(bookingId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Booking này chưa có bệnh án"));

        validateViewPermission(medicalRecord.getMedicalBooking());
        return toMedicalRecordDetail(medicalRecord);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public PrescriptionResponse addPrescription(Long medicalRecordId, CreatePrescriptionRequest request) {
        MedicalRecords medicalRecord = getMedicalRecordForEdit(medicalRecordId);

        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy thuốc"));

        Prescriptions prescription = medicalRecordMapper.toPrescription(request);
        prescription.setMedicalRecord(medicalRecord);
        prescription.setMedicine(medicine);
        prescription.setPrice(medicine.getPrice());

        Prescriptions saved = prescriptionsRepository.save(prescription);
        recalculateTotalCost(medicalRecord.getId());

        return medicalRecordMapper.toPrescriptionResponse(saved);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public PrescriptionResponse updatePrescription(
            Long medicalRecordId,
            Long prescriptionId,
            UpdatePrescriptionRequest request
    ) {
        MedicalRecords medicalRecord = getMedicalRecordForEdit(medicalRecordId);

        Prescriptions prescription = prescriptionsRepository.findDetailByIdAndMedicalRecordId(prescriptionId, medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy đơn thuốc"));

        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy thuốc"));

        medicalRecordMapper.updatePrescription(request, prescription);
        prescription.setMedicalRecord(medicalRecord);
        prescription.setMedicine(medicine);
        prescription.setPrice(medicine.getPrice());

        Prescriptions saved = prescriptionsRepository.save(prescription);
        recalculateTotalCost(medicalRecord.getId());

        return medicalRecordMapper.toPrescriptionResponse(saved);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public void deletePrescription(Long medicalRecordId, Long prescriptionId) {
        getMedicalRecordForEdit(medicalRecordId);

        Prescriptions prescription = prescriptionsRepository.findDetailByIdAndMedicalRecordId(prescriptionId, medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy đơn thuốc"));

        prescriptionsRepository.delete(prescription);
        recalculateTotalCost(medicalRecordId);
    }

    @PreAuthorize("isAuthenticated()")
    public List<PrescriptionResponse> getPrescriptions(Long medicalRecordId) {
        MedicalRecords medicalRecord = medicalRecordsRepository.findDetailById(medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy bệnh án"));

        validateViewPermission(medicalRecord.getMedicalBooking());

        return medicalRecordMapper.toPrescriptionResponses(
                prescriptionsRepository.findAllByMedicalRecordId(medicalRecordId)
        );
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public TreatmentRecordResponse addTreatment(Long medicalRecordId, CreateTreatmentRecordRequest request) {
        MedicalRecords medicalRecord = getMedicalRecordForEdit(medicalRecordId);

        TreatmentMethods treatmentMethods = treatmentMethodsRepository.findById(request.getTreatmentMethodId())
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy phương pháp điều trị"));

        TreatmentRecord treatmentRecord = medicalRecordMapper.toTreatmentRecord(request);
        treatmentRecord.setMedicalRecord(medicalRecord);
        treatmentRecord.setTreatmentMethods(treatmentMethods);
        treatmentRecord.setPrice(treatmentMethods.getPrice());

        TreatmentRecord saved = treatmentRecordRepository.save(treatmentRecord);
        recalculateTotalCost(medicalRecord.getId());

        return medicalRecordMapper.toTreatmentRecordResponse(saved);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public TreatmentRecordResponse updateTreatment(
            Long medicalRecordId,
            Long treatmentRecordId,
            UpdateTreatmentRecordRequest request
    ) {
        MedicalRecords medicalRecord = getMedicalRecordForEdit(medicalRecordId);

        TreatmentRecord treatmentRecord = treatmentRecordRepository.findDetailByIdAndMedicalRecordId(
                        treatmentRecordId, medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy treatment"));

        TreatmentMethods treatmentMethods = treatmentMethodsRepository.findById(request.getTreatmentMethodId())
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy phương pháp điều trị"));

        medicalRecordMapper.updateTreatmentRecord(request, treatmentRecord);
        treatmentRecord.setMedicalRecord(medicalRecord);
        treatmentRecord.setTreatmentMethods(treatmentMethods);
        treatmentRecord.setPrice(treatmentMethods.getPrice());

        TreatmentRecord saved = treatmentRecordRepository.save(treatmentRecord);
        recalculateTotalCost(medicalRecord.getId());

        return medicalRecordMapper.toTreatmentRecordResponse(saved);
    }

    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    @Transactional
    public void deleteTreatment(Long medicalRecordId, Long treatmentRecordId) {
        getMedicalRecordForEdit(medicalRecordId);

        TreatmentRecord treatmentRecord = treatmentRecordRepository.findDetailByIdAndMedicalRecordId(
                        treatmentRecordId, medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy treatment"));

        treatmentRecordRepository.delete(treatmentRecord);
        recalculateTotalCost(medicalRecordId);
    }

    @PreAuthorize("isAuthenticated()")
    public List<TreatmentRecordResponse> getTreatments(Long medicalRecordId) {
        MedicalRecords medicalRecord = medicalRecordsRepository.findDetailById(medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy bệnh án"));

        validateViewPermission(medicalRecord.getMedicalBooking());

        return medicalRecordMapper.toTreatmentRecordResponses(
                treatmentRecordRepository.findAllByMedicalRecordId(medicalRecordId)
        );
    }

    @PreAuthorize("isAuthenticated()")
    public List<MedicalRecordSummaryResponse> getPetMedicalRecords(Integer petId) {
        Pets pet = petRepository.findActiveById(petId)
                .orElseThrow(() -> ErrorCode.PET_NOT_FOUND.toException("Không tìm thấy pet"));

        if (!securityUtil.isAdmin() && !securityUtil.isDoctor()
                && !pet.getUser().getId().equals(securityUtil.getCurrentUserId())) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền xem lịch sử khám của pet này");
        }

        return medicalRecordMapper.toMedicalRecordSummaryResponses(
                medicalRecordsRepository.findAllByPetId(petId)
        );
    }

    @PreAuthorize("isAuthenticated()")
    public VisitDetailResponse getVisitDetail(Integer bookingId) {
        MedicalBooking booking = medicalBookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy booking"));

        validateViewPermission(booking);

        MedicalRecords medicalRecord = medicalRecordsRepository.findDetailByBookingId(bookingId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Booking này chưa có bệnh án"));

        MedicalBookingResponse bookingResponse = medicalBookingMapper.toMedicalBookingResponse(booking);

        return VisitDetailResponse.builder()
                .booking(bookingResponse)
                .medicalRecord(toMedicalRecordDetail(medicalRecord))
                .build();
    }

    private MedicalRecords getMedicalRecordForEdit(Long medicalRecordId) {
        MedicalRecords medicalRecord = medicalRecordsRepository.findDetailById(medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy bệnh án"));

        validateEditPermission(medicalRecord.getMedicalBooking());
        return medicalRecord;
    }

    private void validateEditPermission(MedicalBooking booking) {
        boolean isAssignedDoctor = booking.getDoctor() != null
                && booking.getDoctor().getId().equals(securityUtil.getCurrentUserId());

        if (!securityUtil.isAdmin() && !isAssignedDoctor) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền tạo hoặc chỉnh sửa bệnh án này");
        }
    }

    private void validateViewPermission(MedicalBooking booking) {
        boolean isAssignedDoctor = booking.getDoctor() != null
                && booking.getDoctor().getId().equals(securityUtil.getCurrentUserId());
        boolean isOwner = booking.getPets().getUser().getId().equals(securityUtil.getCurrentUserId());

        if (!securityUtil.isAdmin() && !isAssignedDoctor && !isOwner) {
            throw ErrorCode.UNAUTHORIZED.toException("Bạn không có quyền xem bệnh án này");
        }
    }

    private void recalculateTotalCost(Long medicalRecordId) {
        MedicalRecords medicalRecord = medicalRecordsRepository.findById(medicalRecordId)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy bệnh án"));

        BigDecimal prescriptionTotal = prescriptionsRepository.findAllByMedicalRecordId(medicalRecordId).stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal treatmentTotal = treatmentRecordRepository.findAllByMedicalRecordId(medicalRecordId).stream()
                .map(TreatmentRecord::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        medicalRecord.setTotalCost(prescriptionTotal.add(treatmentTotal));
        medicalRecordsRepository.save(medicalRecord);
    }

    private MedicalRecordResponse toMedicalRecordDetail(MedicalRecords medicalRecord) {
        MedicalRecordResponse response = medicalRecordMapper.toMedicalRecordResponse(medicalRecord);

        response.setPrescriptions(
                medicalRecordMapper.toPrescriptionResponses(
                        prescriptionsRepository.findAllByMedicalRecordId(medicalRecord.getId())
                )
        );

        response.setTreatments(
                medicalRecordMapper.toTreatmentRecordResponses(
                        treatmentRecordRepository.findAllByMedicalRecordId(medicalRecord.getId())
                )
        );

        return response;
    }
}
