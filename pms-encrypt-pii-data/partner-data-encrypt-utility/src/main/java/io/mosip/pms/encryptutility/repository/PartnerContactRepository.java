package io.mosip.pms.encryptutility.repository;

import io.mosip.pms.encryptutility.entity.PartnerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PartnerContactRepository extends JpaRepository<PartnerContact, String> {
    @Query(value = "SELECT * FROM partner_contact pc WHERE pc.email_id_hash IS NULL", nativeQuery = true)
    List<PartnerContact> findPartnerContactWithNullEmailHash();
} 