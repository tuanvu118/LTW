package _2.LTW.controller;

import _2.LTW.dto.request.PetCreateRequest;
import _2.LTW.dto.request.PetUpdateRequest;
import _2.LTW.dto.response.PetResponse;
import _2.LTW.service.PetService.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {
    private final PetService petService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PetResponse create(@ModelAttribute @Valid PetCreateRequest request) {
        return petService.create(request);
    }

    @GetMapping("/{id}")
    public PetResponse getById(@PathVariable Integer id) {
        return petService.getById(id);
    }

    @GetMapping
    public Page<PetResponse> getAll(
            @RequestParam(required = false) Long ownerId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return petService.getAll(ownerId, pageable);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PetResponse update(
            @PathVariable Integer id,
            @ModelAttribute @Valid PetUpdateRequest request
    ) {
        return petService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        petService.delete(id);
    }

}
