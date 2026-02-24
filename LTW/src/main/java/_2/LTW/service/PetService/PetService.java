package _2.LTW.service.PetService;

import _2.LTW.dto.request.PetCreateRequest;
import _2.LTW.dto.request.PetUpdateRequest;
import _2.LTW.dto.response.PetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PetService {
    PetResponse create(PetCreateRequest request);
    PetResponse getById(Integer id);
    Page<PetResponse> getAll(Long ownerId, Pageable pageable);
    PetResponse update(Integer id, PetUpdateRequest request);
    void delete(Integer id);
}
