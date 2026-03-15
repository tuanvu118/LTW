package _2.LTW.repository;

import _2.LTW.entity.CareService;
import _2.LTW.entity.Pets.PetSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CareServiceRepository extends JpaRepository<CareService, Long> {

    // Lấy danh sách dịch vụ đang hoạt động
    List<CareService> findByIsActiveTrue();

    // Lấy danh sách dịch vụ theo loại pet
    default List<CareService> findBySpeciesContainingAndIsActiveTrue(PetSpecies species) {
        if (species == null) {
            return findByIsActiveTrue();
        }
        return findActiveBySpeciesToken(species.name());
    }

    @Query(value = """
            SELECT *
            FROM care_services cs
            WHERE cs.is_active = true
              AND (
                    cs.species = :species
                 OR cs.species LIKE CONCAT(:species, ',%')
                 OR cs.species LIKE CONCAT('%,', :species, ',%')
                 OR cs.species LIKE CONCAT('%,', :species)
              )
            """, nativeQuery = true)
    List<CareService> findActiveBySpeciesToken(@Param("species") String species);

    // Kiểm tra tên dịch vụ đã tồn tại
    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
