package io.mosip.pms.encryptutility.service;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.pms.encryptutility.entity.Partner;
import io.mosip.pms.encryptutility.entity.PartnerContact;
import io.mosip.pms.encryptutility.entity.PartnerH;
import io.mosip.pms.encryptutility.repository.PartnerContactRepository;
import io.mosip.pms.encryptutility.repository.PartnerHRepository;
import io.mosip.pms.encryptutility.repository.PartnerRepository;
import io.mosip.pms.encryptutility.util.KeyManagerUtil;
import io.mosip.pms.encryptutility.util.PMSLogger;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.mosip.pms.encryptutility.constants.AppConstants.SYSTEM;

@Service
public class DataEncryptionService {

    private static final Logger LOGGER = PMSLogger.getLogger(DataEncryptionService.class);

    @Autowired
    private PartnerRepository partnerServiceRepository;

    @Autowired
    PartnerHRepository partnerHRepository;

    @Autowired
    PartnerContactRepository partnerContactRepository;

    @Autowired
    private KeyManagerUtil keyManagerUtil;

    /**
     * Encrypts all sensitive data for partners, partner history, and partner contacts.
     */
    public void encryptData() {
        LOGGER.info("DataEncryptionService: encryptData - START");
        try {
            List<Partner> partners = encryptPartnerData();
            List<PartnerH> partnerHList = encryptPartnerHData();
            List<PartnerContact> partnerContacts = encryptPartnerContactData();
            LOGGER.info("DataEncryptionService: Encryption completed - Partner records: {}, PartnerH records: {}, PartnerContact records: {}",
                    partners.size(), partnerHList.size(), partnerContacts.size());
            LOGGER.info("DataEncryptionService: encryptData - END");
        } catch (Exception ex) {
            LOGGER.error("An error occurred while encrypting data. Error: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Encrypts existing Partner entities.
     */
    private List<Partner> encryptPartnerData() {
        LOGGER.info("Starting encryption process for partner data.");
        List<Partner> partnerList = partnerServiceRepository.findPartnersWithNullEmailHash();

        if (partnerList.isEmpty()) {
            LOGGER.info("No partner records found for encryption.");
            return partnerList;
        }

        List<Partner> encryptedPartners = new ArrayList<>();
        List<String> encryptedPartnerIds = new ArrayList<>();
        List<String> encryptionFailedPartnerIds = new ArrayList<>();
        List<String> skippedPartnerIds = new ArrayList<>();

        for (Partner partner : partnerList) {
            String partnerId = partner.getId();
            try {
                String emailId = partner.getEmailId();
                String address = partner.getAddress();
                String contactNo = partner.getContactNo();

                if (emailId == null || emailId.trim().isEmpty()) {
                    LOGGER.info("Skipping encryption for partner record with Partner ID: {} because email ID is missing.", partnerId);
                    skippedPartnerIds.add(partnerId);
                    continue;
                }

                if (address == null || address.trim().isEmpty()) {
                    LOGGER.info("Skipping encryption for partner record with Partner ID: {} due to missing address.", partnerId);
                    skippedPartnerIds.add(partnerId);
                    continue;
                }

                if (contactNo == null || contactNo.trim().isEmpty()) {
                    LOGGER.info("Skipping encryption for partner record with Partner ID: {} due to missing contact number.", partnerId);
                    skippedPartnerIds.add(partnerId);
                    continue;
                }

                partner.setEmailId(keyManagerUtil.encryptData(emailId));
                partner.setAddress(keyManagerUtil.encryptData(address));
                partner.setContactNo(keyManagerUtil.encryptData(contactNo));
                partner.setEmailIdHash(DigestUtils.sha256Hex(emailId.toLowerCase()));
                partner.setUpdBy(SYSTEM);
                partner.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now()));

                partnerServiceRepository.save(partner);
                encryptedPartners.add(partner);
                encryptedPartnerIds.add(partnerId);
                LOGGER.info("Encrypted and saved partner record successfully for Partner ID: {}", partnerId);
            } catch (Exception ex) {
                LOGGER.error("Failed to encrypt/save partner record with Partner ID: {}. Error: {}", partnerId, ex.getMessage(), ex);
                encryptionFailedPartnerIds.add(partnerId);
            }
        }

        LOGGER.info("Encrypted Partner IDs: {}", encryptedPartnerIds);
        LOGGER.info("Failed Partner IDs: {}", encryptionFailedPartnerIds);
        LOGGER.info("Skipped Partner IDs (missing required fields): {}", skippedPartnerIds);
        LOGGER.info("Total successfully encrypted partner records: {}", encryptedPartners.size());

        return encryptedPartners;
    }

    /**
     * Encrypts existing PartnerH entities.
     */
    private List<PartnerH> encryptPartnerHData() {
        LOGGER.info("Starting encryption process for partner history data.");
        List<PartnerH> partnerHList = partnerHRepository.findPartnerHWithNullEmailHash();

        if (partnerHList.isEmpty()) {
            LOGGER.info("No PartnerH records found for encryption.");
            return partnerHList;
        }

        List<PartnerH> encryptedPartnerHList = new ArrayList<>();
        List<String> encryptedPartnerHIds = new ArrayList<>();
        List<String> encryptionFailedPartnerHIds = new ArrayList<>();
        List<String> skippedPartnerHIds = new ArrayList<>();

        for (PartnerH partnerH : partnerHList) {
            String partnerId = partnerH.getId().getId();
            try {
                String emailId = partnerH.getEmailId();
                String address = partnerH.getAddress();
                String contactNo = partnerH.getContactNo();

                if (emailId == null || emailId.trim().isEmpty()) {
                    LOGGER.warn("Skipping encryption for partner history record with ID: {} due to missing email ID.", partnerId);
                    skippedPartnerHIds.add(partnerId);
                    continue;
                }

                if (address == null || address.trim().isEmpty()) {
                    LOGGER.warn("Skipping encryption for partner history record with ID: {} due to missing address.", partnerId);
                    skippedPartnerHIds.add(partnerId);
                    continue;
                }

                if (contactNo == null || contactNo.trim().isEmpty()) {
                    LOGGER.warn("Skipping encryption for partner history record with ID: {} due to missing contact number.", partnerId);
                    skippedPartnerHIds.add(partnerId);
                    continue;
                }

                partnerH.setEmailId(keyManagerUtil.encryptData(emailId));
                partnerH.setAddress(keyManagerUtil.encryptData(address));
                partnerH.setContactNo(keyManagerUtil.encryptData(contactNo));
                partnerH.setEmailIdHash(DigestUtils.sha256Hex(emailId.toLowerCase()));
                partnerH.setUpdBy(SYSTEM);
                partnerH.setUpdDtimes(Timestamp.valueOf(LocalDateTime.now()));

                partnerHRepository.save(partnerH);
                encryptedPartnerHList.add(partnerH);
                encryptedPartnerHIds.add(partnerId);
                LOGGER.info("Encrypted and saved partner history record successfully for ID: {}", partnerId);
            } catch (Exception ex) {
                LOGGER.error("Failed to encrypt/save partner history record with ID: {}. Error: {}", partnerId, ex.getMessage(), ex);
                encryptionFailedPartnerHIds.add(partnerId);
            }
        }

        LOGGER.info("Encrypted PartnerH IDs: {}", encryptedPartnerHIds);
        LOGGER.info("Failed PartnerH IDs: {}", encryptionFailedPartnerHIds);
        LOGGER.info("Skipped PartnerH IDs (missing required fields): {}", skippedPartnerHIds);
        LOGGER.info("Total successfully encrypted PartnerH records: {}", encryptedPartnerHList.size());

        return encryptedPartnerHList;
    }

    /**
     * Encrypts existing PartnerContact entities.
     */
    private List<PartnerContact> encryptPartnerContactData() {
        LOGGER.info("Starting encryption process for partner contact data.");
        List<PartnerContact> contactList = partnerContactRepository.findPartnerContactWithNullEmailHash();

        if (contactList.isEmpty()) {
            LOGGER.info("No PartnerContact records found for encryption.");
            return contactList;
        }

        List<PartnerContact> encryptedContacts = new ArrayList<>();
        List<String> encryptedContactIds = new ArrayList<>();
        List<String> encryptionFailedContactIds = new ArrayList<>();
        List<String> skippedContactIds = new ArrayList<>();

        for (PartnerContact contact : contactList) {
            String contactId = contact.getId();
            try {
                String emailId = contact.getEmailId();
                String address = contact.getAddress();
                String contactNo = contact.getContactNo();

                if (emailId == null || emailId.trim().isEmpty()) {
                    LOGGER.warn("Skipping encryption for partner contact record with ID: {} due to missing email ID.", contactId);
                    skippedContactIds.add(contactId);
                    continue;
                }

                if (address == null || address.trim().isEmpty()) {
                    LOGGER.warn("Skipping encryption for partner contact record with ID: {} due to missing address.", contactId);
                    skippedContactIds.add(contactId);
                    continue;
                }

                if (contactNo == null || contactNo.trim().isEmpty()) {
                    LOGGER.warn("Skipping encryption for partner contact record with ID: {} due to missing contact number.", contactId);
                    skippedContactIds.add(contactId);
                    continue;
                }

                contact.setEmailId(keyManagerUtil.encryptData(emailId));
                contact.setEmailIdHash(DigestUtils.sha256Hex(emailId.toLowerCase()));
                contact.setContactNo(keyManagerUtil.encryptData(contactNo));
                contact.setAddress(keyManagerUtil.encryptData(address));
                contact.setUpdBy(SYSTEM);
                contact.setUpdDtimes(LocalDateTime.now());

                partnerContactRepository.save(contact);
                encryptedContacts.add(contact);
                encryptedContactIds.add(contactId);
                LOGGER.info("Encrypted and saved partner contact record successfully for ID: {}", contactId);
            } catch (Exception ex) {
                LOGGER.error("Failed to encrypt/save partner contact with ID: {}. Error: {}", contactId, ex.getMessage(), ex);
                encryptionFailedContactIds.add(contactId);
            }
        }

        LOGGER.info("Encrypted PartnerContact IDs: {}", encryptedContactIds);
        LOGGER.info("Failed PartnerContact IDs: {}", encryptionFailedContactIds);
        LOGGER.info("Skipped PartnerContact IDs (missing required fields): {}", skippedContactIds);
        LOGGER.info("Total successfully encrypted PartnerContact records: {}", encryptedContacts.size());

        return encryptedContacts;
    }

}
