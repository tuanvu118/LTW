package _2.LTW.service;

import _2.LTW.repository.TreatmentMethodsRepository;
import _2.LTW.dto.request.TreatmentMethodsRequest;
import _2.LTW.dto.response.TreatmentMethodsResponse;
import _2.LTW.dto.response.MessageResponse;
import _2.LTW.entity.TreatmentMethods;
import _2.LTW.mapper.TreatmentMethodsMapper;
import _2.LTW.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TreatmentMethodsService {

    TreatmentMethodsRepository treatmentMethodsRepository;
    TreatmentMethodsMapper treatmentMethodsMapper;

    public TreatmentMethodsResponse createTreatmentMethods(TreatmentMethodsRequest request){
        try {
            TreatmentMethods treatmentMethods = treatmentMethodsRepository.save(treatmentMethodsMapper.toEntity(request));
            return treatmentMethodsMapper.toResponse(treatmentMethods);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.CONFLICT.toException("Phương pháp điều trị đã tồn tại");
        }
    }

    public List<TreatmentMethodsResponse> getAllTreatmentMethods(){
        return treatmentMethodsMapper.toResponses(treatmentMethodsRepository.findAll());
    }

    public MessageResponse deleteTreatmentMethods(Long id){
        TreatmentMethods treatmentMethods = treatmentMethodsRepository.findById(id)
            .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Không tìm thấy phương pháp điều trị này"));
        treatmentMethodsRepository.deleteById(id);
        return new MessageResponse("Phương pháp điều trị đã được xóa");
    }

    public TreatmentMethodsResponse updateTreatmentMethods(Long id, TreatmentMethodsRequest request){
        try {
            TreatmentMethods treatmentMethods = treatmentMethodsRepository.findById(id).orElseThrow(() 
            -> ErrorCode.NOT_FOUND.toException("Không tìm thấy phương pháp điều trị này"));
            treatmentMethods.setName(request.getName());
            treatmentMethods.setPrice(request.getPrice());
            treatmentMethodsRepository.save(treatmentMethods);
            return treatmentMethodsMapper.toResponse(treatmentMethods);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.CONFLICT.toException("Tên phương pháp điều trị đã tồn tại");
        }
    }
}
