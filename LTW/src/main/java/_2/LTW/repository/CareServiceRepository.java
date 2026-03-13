package _2.LTW.repository;

import _2.LTW.entity.CareService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareServiceRepository extends JpaRepository<CareService, Long> {

    // Lấy danh sách dịch vụ đang hoạt động
    List<CareService> findByIsActiveTrue();

    // Lấy danh sách dịch vụ theo loại pet
    List<CareService> findByPetTypesContainingAndIsActiveTrue(CareService.PetType petType);

    // Kiểm tra tên dịch vụ đã tồn tại
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
