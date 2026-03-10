package _2.LTW.repository;

import _2.LTW.entity.CareService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareServiceRepository extends JpaRepository<CareService, Long> {

    // Lấy danh sách dịch vụ đang hoạt động
    Page<CareService> findByIsActiveTrue(Pageable pageable);

    // Lấy danh sách dịch vụ theo loại pet
    Page<CareService> findByPetTypeAndIsActiveTrue(CareService.PetType petType, Pageable pageable);

    // Lấy tất cả dịch vụ (admin)
    Page<CareService> findAll(Pageable pageable);

    // Kiểm tra tên dịch vụ đã tồn tại
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}

