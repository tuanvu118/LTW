package _2.LTW.controller;

import _2.LTW.dto.request.TreatmentMethodsRequest;
import _2.LTW.dto.response.TreatmentMethodsResponse;
import _2.LTW.dto.response.MessageResponse;
import _2.LTW.service.TreatmentMethodsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@RestController
@RequestMapping("/treatment-methods")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class TreatmentMethodsController {

    TreatmentMethodsService treatmentMethodsService;

    @PostMapping
    public TreatmentMethodsResponse createTreatmentMethods(@Valid @RequestBody TreatmentMethodsRequest request){
        return treatmentMethodsService.createTreatmentMethods(request);
    }

    @GetMapping
    public List<TreatmentMethodsResponse> getAllTreatmentMethods(){
        return treatmentMethodsService.getAllTreatmentMethods();
    }

    @DeleteMapping("/{id}")
    public MessageResponse deleteTreatmentMethods(@PathVariable Long id){
        return treatmentMethodsService.deleteTreatmentMethods(id);
    }

    @PutMapping("/{id}")
    public TreatmentMethodsResponse updateTreatmentMethods(@PathVariable Long id, @Valid @RequestBody TreatmentMethodsRequest request){
        return treatmentMethodsService.updateTreatmentMethods(id, request);
    }
}
