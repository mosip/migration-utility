package io.mosip.pms.pii.encryptutility.service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.pms.common.entity.Partner;
import io.mosip.pms.common.entity.PartnerContact;
import io.mosip.pms.common.entity.PartnerH;
import io.mosip.pms.common.repository.PartnerContactRepository;
import io.mosip.pms.common.repository.PartnerHRepository;
import io.mosip.pms.common.repository.PartnerServiceRepository;
import io.mosip.pms.pii.encryptutility.util.KeyManagerUtil;
import io.mosip.pms.pii.encryptutility.util.PMSLogger;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PiiDataEncryptionService {

    private static final Logger LOGGER = PMSLogger.getLogger(PiiDataEncryptionService.class);

    @Autowired
    private PartnerServiceRepository partnerServiceRepository;

    @Autowired
    private PartnerHRepository partnerHRepository;

    @Autowired
    private PartnerContactRepository partnerContactRepository;

    @Autowired
    private KeyManagerUtil keyManagerUtil;

    /**
     * Encrypts all sensitive data for Partner, PartnerH, and PartnerContact records.
     */
    public void encryptPiiData() {
        LOGGER.info("PiiDataEncryptionService: encryptPiiData - START");
        try {
            List<String> partners = encryptPartnerPiiData();
            List<String> partnerHList = encryptPartnerHPiiData();
            List<String> partnerContacts = encryptPartnerContactPiiData();

            LOGGER.info("PiiDataEncryptionService: Encryption completed - Partner records: {}, PartnerH records: {}, PartnerContact records: {}",
                    partners.size(), partnerHList.size(), partnerContacts.size());
        } catch (Exception ex) {
            LOGGER.error("An error occurred while encrypting PII data. Error: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            LOGGER.info("PiiDataEncryptionService: encryptPiiData - END");
        }
    }

    /**
     * Encrypts PII data for Partner entities.
     *
     * @return List of Partner IDs encrypted successfully.
     */
    private List<String> encryptPartnerPiiData() {
        LOGGER.info("Starting PII encryption for partner records.");

        List<Partner> partnersToEncrypt = partnerServiceRepository.findPartnersWithNullEmailHash();
        if (partnersToEncrypt.isEmpty()) {
            LOGGER.info("No partner records found requiring PII encryption.");
            return Collections.emptyList();
        }

        List<String> encryptedPartnerIds = new ArrayList<>();
        List<String> encryptionFailedPartnerIds = new ArrayList<>();
        List<String> skippedPartnerIds = new ArrayList<>();

        for (Partner partner : partnersToEncrypt) {
            String partnerId = partner.getId();
            try {
                String email = partner.getEmailId();
                String address = partner.getAddress();
                String contact = partner.getContactNo();

                if (isMissingPiiFields(email, address, contact)) {
                    LOGGER.warn("Skipping PII data encryption for Partner ID [{}] due to missing field(s): {}", partnerId, getMissingFields(email, address, contact));
                    skippedPartnerIds.add(partnerId);
                    continue;
                }

                partner.setEmailId(keyManagerUtil.encryptData(email));
                partner.setAddress(keyManagerUtil.encryptData(address));
                partner.setContactNo(keyManagerUtil.encryptData(contact));
                partner.setEmailIdHash(DigestUtils.sha256Hex(email.toLowerCase()));
                partner.setUpdBy(this.getClass().getSimpleName());
                partner.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now()));

                partnerServiceRepository.save(partner);
                encryptedPartnerIds.add(partnerId);

                LOGGER.info("Successfully encrypted and saved PII data for Partner ID: {}", partnerId);
            } catch (Exception ex) {
                LOGGER.error("PII data encryption failed for Partner ID [{}]: {}", partnerId, ex.getMessage(), ex);
                encryptionFailedPartnerIds.add(partnerId);
            }
        }

        LOGGER.info("Successfully encrypted PII data for Partner IDs: {}", encryptedPartnerIds);
        LOGGER.info("Skipped encryption due to missing PII fields for Partner IDs: {}", skippedPartnerIds);
        LOGGER.info("Failed to encrypt PII data for Partner IDs: {}", encryptionFailedPartnerIds);
        LOGGER.info("Total partners with successfully encrypted PII data: {}", encryptedPartnerIds.size());

        return encryptedPartnerIds;
    }

    /**
     * Encrypts PII data for PartnerH (history) entities.
     *
     * @return List of PartnerH IDs encrypted successfully.
     */
    private List<String> encryptPartnerHPiiData() {
        LOGGER.info("Starting PII encryption for partner history records.");

        List<PartnerH> partnerHList = partnerHRepository.findPartnerHWithNullEmailHash();
        if (partnerHList.isEmpty()) {
            LOGGER.info("No PartnerH records found requiring PII encryption.");
            return Collections.emptyList();
        }

        List<String> encryptedPartnerHIds = new ArrayList<>();
        List<String> encryptionFailedPartnerHIds = new ArrayList<>();
        List<String> skippedPartnerHIds = new ArrayList<>();

        for (PartnerH partnerH : partnerHList) {
            String partnerId = partnerH.getId().getId();
            try {
                String email = partnerH.getEmailId();
                String address = partnerH.getAddress();
                String contact = partnerH.getContactNo();

                if (isMissingPiiFields(email, address, contact)) {
                    LOGGER.warn("Skipping PII data encryption for PartnerH ID [{}] due to missing field(s): {}", partnerId, getMissingFields(email, address, contact));
                    skippedPartnerHIds.add(partnerId);
                    continue;
                }

                partnerH.setEmailId(keyManagerUtil.encryptData(email));
                partnerH.setAddress(keyManagerUtil.encryptData(address));
                partnerH.setContactNo(keyManagerUtil.encryptData(contact));
                partnerH.setEmailIdHash(DigestUtils.sha256Hex(email.toLowerCase()));
                partnerH.setUpdBy(this.getClass().getSimpleName());
                partnerH.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now()));

                partnerHRepository.save(partnerH);
                encryptedPartnerHIds.add(partnerId);

                LOGGER.info("Successfully encrypted and saved PII data for PartnerH ID: {}", partnerId);
            } catch (Exception ex) {
                LOGGER.error("PII data encryption failed for PartnerH ID [{}]: {}", partnerId, ex.getMessage(), ex);
                encryptionFailedPartnerHIds.add(partnerId);
            }
        }

        LOGGER.info("Successfully encrypted PII data for PartnerH IDs: {}", encryptedPartnerHIds);
        LOGGER.info("Skipped encryption due to missing PII fields for PartnerH IDs: {}", skippedPartnerHIds);
        LOGGER.info("Failed to encrypt PII data for PartnerH IDs: {}", encryptionFailedPartnerHIds);
        LOGGER.info("Total PartnerH records with successfully encrypted PII data: {}", encryptedPartnerHIds.size());

        return encryptedPartnerHIds;
    }

    /**
     * Encrypts PII data for PartnerContact entities.
     *
     * @return List of PartnerContact IDs encrypted successfully.
     */
    private List<String> encryptPartnerContactPiiData() {
        LOGGER.info("Starting PII encryption for partner contact records.");

        List<PartnerContact> contactList = partnerContactRepository.findPartnerContactWithNullEmailHash();
        if (contactList.isEmpty()) {
            LOGGER.info("No PartnerContact records found requiring PII encryption.");
            return Collections.emptyList();
        }

        List<String> encryptedContactIds = new ArrayList<>();
        List<String> encryptionFailedContactIds = new ArrayList<>();
        List<String> skippedContactIds = new ArrayList<>();

        for (PartnerContact contact : contactList) {
            String contactId = contact.getId();
            try {
                String email = contact.getEmailId();
                String address = contact.getAddress();
                String phone = contact.getContactNo();

                if (isMissingPiiFields(email, address, phone)) {
                    LOGGER.warn("Skipping PII data encryption for PartnerContact ID [{}] due to missing field(s): {}", contactId, getMissingFields(email, address, phone));
                    skippedContactIds.add(contactId);
                    continue;
                }

                contact.setEmailId(keyManagerUtil.encryptData(email));
                contact.setAddress(keyManagerUtil.encryptData(address));
                contact.setContactNo(keyManagerUtil.encryptData(phone));
                contact.setEmailIdHash(DigestUtils.sha256Hex(email.toLowerCase()));
                contact.setUpdBy(this.getClass().getSimpleName());
                contact.setUpdDtimes(LocalDateTime.now());

                partnerContactRepository.save(contact);
                encryptedContactIds.add(contactId);

                LOGGER.info("Successfully encrypted and saved PII data for PartnerContact ID: {}", contactId);
            } catch (Exception ex) {
                LOGGER.error("PII data encryption failed for PartnerContact ID [{}]: {}", contactId, ex.getMessage(), ex);
                encryptionFailedContactIds.add(contactId);
            }
        }

        LOGGER.info("Successfully encrypted PII data for PartnerContact IDs: {}", encryptedContactIds);
        LOGGER.info("Skipped encryption due to missing PII fields for PartnerContact IDs: {}", skippedContactIds);
        LOGGER.info("Failed to encrypt PII data for PartnerContact IDs: {}", encryptionFailedContactIds);
        LOGGER.info("Total PartnerContact records with successfully encrypted PII data: {}", encryptedContactIds.size());

        return encryptedContactIds;
    }

    /**
     * Checks whether any of the PII fields is null or blank.
     */
    private boolean isMissingPiiFields(String email, String address, String contact) {
        return isBlank(email) || isBlank(address) || isBlank(contact);
    }

    /**
     * Utility method to check if a string is null or blank.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Returns a comma-separated list of missing PII field names.
     */
    private String getMissingFields(String email, String address, String contact) {
        List<String> missingFields = new ArrayList<>();
        if (isBlank(email)) missingFields.add("email");
        if (isBlank(address)) missingFields.add("address");
        if (isBlank(contact)) missingFields.add("contact");
        return String.join(", ", missingFields);
    }
}