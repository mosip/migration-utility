package io.mosip.pms.encryptutility.repository;

import io.mosip.pms.encryptutility.entity.PartnerH;
import io.mosip.pms.encryptutility.entity.PartnerHPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartnerHRepository extends JpaRepository<PartnerH, PartnerHPK> {
    @Query(value = "SELECT * FROM partner_h p WHERE p.email_id_hash IS NULL", nativeQuery = true)
    List<PartnerH> findPartnerHWithNullEmailHash();
} 