package _2.LTW.service;

import _2.LTW.dto.request.MedicalServiceRequest;
import _2.LTW.dto.response.MedicalServiceResponse;
import _2.LTW.entity.MedicalService;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.MedicalServiceMapper;
import _2.LTW.repository.MedicalServiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalServiceService {

    MedicalServiceRepository medicalServiceRepository;
    MedicalServiceMapper medicalServiceMapper;

    @PreAuthorize("hasRole('ADMIN')")
    public MedicalServiceResponse createMedicalService(MedicalServiceRequest request){

        MedicalService medicalService = medicalServiceMapper.toMedicalService(request);

        try {
            medicalService = medicalServiceRepository.save(medicalService);
        } catch(DataIntegrityViolationException e){
            throw ErrorCode.CONFLICT.toException("Dịch vụ đã tồn tại");
        }

        return medicalServiceMapper.toMedicalServiceResponse(medicalService);
    }

    public List<MedicalServiceResponse> getMedicalServices(){

        return medicalServiceMapper.toMedicalServiceResponses(medicalServiceRepository.findAll());

    }

    public MedicalServiceResponse getMedicalService(Long id){

        return medicalServiceMapper.toMedicalServiceResponse(medicalServiceRepository.findById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy dịch vụ này")));

    }

    @PreAuthorize("hasRole('ADMIN')")
    public MedicalServiceResponse updateMedicalService(Long id, MedicalServiceRequest request){

        MedicalService medicalService = medicalServiceRepository.findById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy dịch vụ này"));

        medicalServiceMapper.updateMedicalService(medicalService, request);

        return medicalServiceMapper.toMedicalServiceResponse(medicalServiceRepository.save(medicalService));

    }

}
