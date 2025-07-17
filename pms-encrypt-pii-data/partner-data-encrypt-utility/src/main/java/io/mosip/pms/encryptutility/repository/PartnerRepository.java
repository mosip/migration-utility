package io.mosip.pms.encryptutility.repository;

import io.mosip.pms.encryptutility.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, String> {
    @Query(value = "SELECT * FROM partner p WHERE p.email_id_hash IS NULL", nativeQuery = true)
    List<Partner> findPartnersWithNullEmailHash();
} 