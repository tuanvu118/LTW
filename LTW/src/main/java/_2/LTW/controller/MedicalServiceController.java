package _2.LTW.controller;

import _2.LTW.dto.request.ApiResponse;
import _2.LTW.dto.request.MedicalServiceRequest;
import _2.LTW.dto.response.MedicalServiceResponse;
import _2.LTW.entity.MedicalService;
import _2.LTW.service.MedicalServiceService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/medical-services")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalServiceController {

    MedicalServiceService medicalServiceService;

    @PostMapping
    ApiResponse<MedicalServiceResponse> createMedicalService(@Valid @RequestBody MedicalServiceRequest request){

        return ApiResponse.ok(medicalServiceService.createMedicalService(request));

    }

    @GetMapping
    ApiResponse<List<MedicalServiceResponse>> getMedicalServices(){

        return ApiResponse.ok(medicalServiceService.getMedicalServices());

    }

    @GetMapping("/{id}")
    ApiResponse<MedicalServiceResponse> getMedicalService(@PathVariable Long id){

        return ApiResponse.ok(medicalServiceService.getMedicalService(id));

    }

    @PutMapping("/{id}")
    ApiResponse<MedicalServiceResponse> updateMedicalService(@PathVariable Long id,
                                                             @Valid @RequestBody MedicalServiceRequest request){

        return ApiResponse.ok(medicalServiceService.updateMedicalService(id, request));

    }

}
