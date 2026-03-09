package _2.LTW.service;

import _2.LTW.dto.request.MedicineRequest;
import _2.LTW.dto.response.MedicineResponse;
import _2.LTW.entity.Medicine;
import _2.LTW.mapper.MedicineMapper;
import _2.LTW.repository.MedicineRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineMapper medicineMapper;

    public List<MedicineResponse> getAllMedicines() {
        return medicineMapper.toMedicineResponses(medicineRepository.findAll());
    }

    public MedicineResponse getMedicineById(Integer id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tồn tại thuốc này!"));
        return medicineMapper.toMedicineResponse(medicine);
    }

    public MedicineResponse createMedicine(Medicine medicine) {
        medicineRepository.save(medicine);
        return medicineMapper.toMedicineResponse(medicine);
    }


    public MedicineResponse updateMedicine(Integer id, MedicineRequest request) {
        Medicine existing = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tồn tại thuốc này!"));

        medicineMapper.updateMedicineFromRequest(request, existing);

        Medicine saved = medicineRepository.save(existing);
        return medicineMapper.toMedicineResponse(saved);
    }

    public MedicineResponse deleteMedicine(Integer id) {
        Medicine existing = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tồn tại thuốc này!"));

        medicineRepository.delete(existing);
        return medicineMapper.toMedicineResponse(existing);
    }
}
