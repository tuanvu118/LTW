package _2.LTW.repository;

import _2.LTW.entity.Pets.Pets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pets, Integer> {
    @Query("select p from Pets p where p.delete_at is null")
    Page<Pets> findAllActive(Pageable pageable);

    @Query("select p from Pets p where p.id = :id and p.delete_at is null")
    Optional<Pets> findActiveById(@Param("id") Integer id);

    @Query("select p from Pets p where p.user.id = :owner_id and p.delete_at is null")
    Page<Pets> findActiveByOwnerId(@Param("owner_id") Long ownerId, Pageable pageable);
}
