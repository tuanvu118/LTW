package _2.LTW.service;

import _2.LTW.dto.response.MedicineResponse;
import _2.LTW.entity.Medicine;
import _2.LTW.mapper.MedicineMapper;
import _2.LTW.repository.MedicineRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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

    @PreAuthorize("hasRole('ADMIN')")
    public MedicineResponse createMedicine(Medicine medicine) {
        medicineRepository.save(medicine);
        return medicineMapper.toMedicineResponse(medicine);
    }
}
