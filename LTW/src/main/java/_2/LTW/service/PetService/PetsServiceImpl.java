package _2.LTW.service.PetService;

import _2.LTW.dto.request.PetCreateRequest;
import _2.LTW.dto.request.PetUpdateRequest;
import _2.LTW.dto.response.PetResponse;
import _2.LTW.entity.Pets.Pets;
import _2.LTW.entity.User;
import _2.LTW.exception.ErrorCode;
import _2.LTW.mapper.PetMapper;
import _2.LTW.repository.PetRepository;
import _2.LTW.repository.UserRepository;
import _2.LTW.service.CloudinaryService;
import _2.LTW.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PetsServiceImpl implements PetService {
    private final PetRepository petsRepository;
    private final UserRepository userRepository;
    private final PetMapper petMapper;
    private final CloudinaryService cloudinaryService;
    private final SecurityUtil securityUtil;

    private boolean isAdminOrDoctor() {
        return securityUtil.isAdmin() || securityUtil.isDoctor();
    }

    private void assertOwnerOrAdminDoctor(Long ownerId) {
        if (isAdminOrDoctor()) {
            return;
        }

        if (!securityUtil.isOwner(ownerId)) {
            throw ErrorCode.UNAUTHORIZED.toException("Ban khong co quyen thao tac thu cung nay");
        }
    }

    @Override
    @Transactional
    public PetResponse create(PetCreateRequest request) {
        Long ownerId = resolveOwnerIdForCreate(request);

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException(
                        "Khong tim thay user id=" + ownerId
                ));

        Pets pet = petMapper.toEntity(request, owner);
        pet.setUser(owner);

        if (request.getImg_url() != null && !request.getImg_url().isEmpty()) {
            Map data = cloudinaryService.upload(request.getImg_url());
            String url = (String) data.getOrDefault("secure_url", data.get("url"));
            pet.setImg_url(url);
        }

        Pets saved = petsRepository.save(pet);
        return petMapper.toResponse(saved);
    }

    private Long resolveOwnerIdForCreate(PetCreateRequest request) {
        Long currentUserId = securityUtil.getCurrentUserId();
        Long requestOwnerId = request.getOwner_id();

        if (isAdminOrDoctor()) {
            return requestOwnerId != null ? requestOwnerId : currentUserId;
        }

        if (requestOwnerId != null && !requestOwnerId.equals(currentUserId)) {
            throw ErrorCode.UNAUTHORIZED.toException("Ban khong co quyen tao pet cho nguoi dung khac");
        }

        return currentUserId;
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponse getById(Integer id) {
        Pets pet = petsRepository.findActiveById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Khong tim thay pet id=" + id));
        assertOwnerOrAdminDoctor(pet.getUser().getId());
        return petMapper.toResponse(pet);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PetResponse> getAll(Long ownerId, Pageable pageable) {
        if (isAdminOrDoctor()) {
            Page<Pets> page = (ownerId == null)
                    ? petsRepository.findAllActive(pageable)
                    : petsRepository.findActiveByOwnerId(ownerId, pageable);

            return page.map(petMapper::toResponse);
        }

        Long me = securityUtil.getCurrentUserId();

        if (ownerId != null && !ownerId.equals(me)) {
            throw ErrorCode.UNAUTHORIZED.toException("Ban khong co quyen xem thu cung cua nguoi khac");
        }

        return petsRepository.findActiveByOwnerId(me, pageable)
                .map(petMapper::toResponse);
    }

    @Override
    @Transactional
    public PetResponse update(Integer id, PetUpdateRequest request) {
        Pets pet = petsRepository.findActiveById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Khong tim thay pet id=" + id));
        assertOwnerOrAdminDoctor(pet.getUser().getId());

        petMapper.updateEntity(pet, request);

        if (request.getImg_url() != null && !request.getImg_url().isEmpty()) {
            Map data = cloudinaryService.upload(request.getImg_url());
            String url = (String) data.getOrDefault("secure_url", data.get("url"));
            pet.setImg_url(url);
        }

        Pets saved = petsRepository.save(pet);
        return petMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Pets pet = petsRepository.findActiveById(id)
                .orElseThrow(() -> ErrorCode.NOT_FOUND.toException("Khong tim thay pet id=" + id));
        assertOwnerOrAdminDoctor(pet.getUser().getId());
        pet.setDelete_at(LocalDateTime.now());
        petsRepository.save(pet);
    }
}
