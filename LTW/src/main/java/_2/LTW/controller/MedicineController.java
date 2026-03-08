package _2.LTW.controller;


import _2.LTW.dto.response.MedicineResponse;
import _2.LTW.entity.Medicine;
import _2.LTW.service.MedicineService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medicines")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping("/all")
    public List<MedicineResponse> getAllMedicines() {
        return medicineService.getAllMedicines();
    }

    @GetMapping("/{id}")
    public MedicineResponse getMedicineById(@PathVariable Integer id) {
        return medicineService.getMedicineById(id);
    }

    @PostMapping("")
    public MedicineResponse createMedicine(@RequestBody Medicine medicine) {
        return medicineService.createMedicine(medicine);
    }
}

