package _2.LTW.mapper;

import _2.LTW.dto.request.MedicalRecordRequest.*;
import _2.LTW.dto.response.MedicalRecordResponse.MedicalRecordResponse;
import _2.LTW.dto.response.MedicalRecordResponse.MedicalRecordSummaryResponse;
import _2.LTW.dto.response.MedicalRecordResponse.PrescriptionResponse;
import _2.LTW.dto.response.MedicalRecordResponse.TreatmentRecordResponse;
import _2.LTW.entity.MedicalRecord.MedicalRecords;
import _2.LTW.entity.MedicalRecord.Prescriptions;
import _2.LTW.entity.MedicalRecord.TreatmentRecord;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MedicalRecordMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "medicalBooking", ignore = true)
    @Mapping(target = "totalCost", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "prescriptions", ignore = true)
    @Mapping(target = "treatmentRecords", ignore = true)
    MedicalRecords toMedicalRecord(CreateMedicalRecordRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "medicalBooking", ignore = true)
    @Mapping(target = "totalCost", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "prescriptions", ignore = true)
    @Mapping(target = "treatmentRecords", ignore = true)
    void updateMedicalRecord(UpdateMedicalRecordRequest request, @MappingTarget MedicalRecords medicalRecord);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "medicine", ignore = true)
    @Mapping(target = "price", ignore = true)
    Prescriptions toPrescription(CreatePrescriptionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "medicine", ignore = true)
    @Mapping(target = "price", ignore = true)
    void updatePrescription(UpdatePrescriptionRequest request, @MappingTarget Prescriptions prescription);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "treatmentMethods", ignore = true)
    @Mapping(target = "price", ignore = true)
    TreatmentRecord toTreatmentRecord(CreateTreatmentRecordRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "treatmentMethods", ignore = true)
    @Mapping(target = "price", ignore = true)
    void updateTreatmentRecord(UpdateTreatmentRecordRequest request, @MappingTarget TreatmentRecord treatmentRecord);

    @Mapping(target = "medicalBookingId", source = "medicalBooking.id")
    @Mapping(target = "petId", source = "medicalBooking.pets.id")
    @Mapping(target = "petName", source = "medicalBooking.pets.name")
    @Mapping(target = "ownerId", source = "medicalBooking.pets.user.id")
    @Mapping(target = "ownerName", source = "medicalBooking.pets.user.username")
    @Mapping(target = "doctorId", source = "medicalBooking.doctor.id")
    @Mapping(target = "doctorName", source = "medicalBooking.doctor.username")
    @Mapping(target = "bookingDate", source = "medicalBooking.bookingDate")
    @Mapping(target = "prescriptions", ignore = true)
    @Mapping(target = "treatments", ignore = true)
    MedicalRecordResponse toMedicalRecordResponse(MedicalRecords medicalRecord);

    @Mapping(target = "medicalBookingId", source = "medicalBooking.id")
    @Mapping(target = "petId", source = "medicalBooking.pets.id")
    @Mapping(target = "petName", source = "medicalBooking.pets.name")
    @Mapping(target = "doctorId", source = "medicalBooking.doctor.id")
    @Mapping(target = "doctorName", source = "medicalBooking.doctor.username")
    @Mapping(target = "bookingDate", source = "medicalBooking.bookingDate")
    MedicalRecordSummaryResponse toMedicalRecordSummaryResponse(MedicalRecords medicalRecord);

    List<MedicalRecordSummaryResponse> toMedicalRecordSummaryResponses(List<MedicalRecords> medicalRecords);

    @Mapping(target = "medicineId", source = "medicine.id")
    @Mapping(target = "medicineName", source = "medicine.name")
    PrescriptionResponse toPrescriptionResponse(Prescriptions prescription);

    List<PrescriptionResponse> toPrescriptionResponses(List<Prescriptions> prescriptions);

    @Mapping(target = "treatmentMethodId", source = "treatmentMethods.id")
    @Mapping(target = "treatmentMethodName", source = "treatmentMethods.name")
    TreatmentRecordResponse toTreatmentRecordResponse(TreatmentRecord treatmentRecord);

    List<TreatmentRecordResponse> toTreatmentRecordResponses(List<TreatmentRecord> treatmentRecords);
}
