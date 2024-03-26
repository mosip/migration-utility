package io.mosip.pms.ida.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.authmanager.authadapter.model.AuthUserDetails;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.pms.ida.constant.EventType;
import io.mosip.pms.ida.dao.AuthPolicy;
import io.mosip.pms.ida.dao.AuthPolicyRepository;
import io.mosip.pms.ida.dao.LastSync;
import io.mosip.pms.ida.dao.LastSyncRepository;
import io.mosip.pms.ida.dao.MISPLicenseEntity;
import io.mosip.pms.ida.dao.MispLicenseRepository;
import io.mosip.pms.ida.dao.Partner;
import io.mosip.pms.ida.dao.PartnerPolicy;
import io.mosip.pms.ida.dao.PartnerPolicyRepository;
import io.mosip.pms.ida.dao.PartnerRepository;
import io.mosip.pms.ida.dto.APIKeyDataPublishDto;
import io.mosip.pms.ida.dto.MISPDataPublishDto;
import io.mosip.pms.ida.dto.PartnerCertDownloadResponeDto;
import io.mosip.pms.ida.dto.PartnerDataPublishDto;
import io.mosip.pms.ida.dto.PolicyPublishDto;
import io.mosip.pms.ida.dto.Type;
import io.mosip.pms.ida.util.CertUtil;
import io.mosip.pms.ida.util.MapperUtils;
import io.mosip.pms.ida.util.RestUtil;
import io.mosip.pms.ida.util.UtilityLogger;
import io.mosip.pms.ida.websub.WebSubPublisher;

@Service
public class PMSDataMigrationService {
	
    public static final String CERT_CHAIN_DATA_SHARE_URL = "certChainDatashareUrl";
	
    public static final String PARTNER_DOMAIN = "partnerDomain";
    
    public static final String RUN_MODE_UPGRADE = "upgrade";

	private static final Logger LOGGER = UtilityLogger.getLogger(PMSDataMigrationService.class);

	@Autowired
	PartnerPolicyRepository partnerPolicyRepository;

	@Autowired
	PartnerRepository partnerRepository;

	@Autowired
	AuthPolicyRepository authPolicyRepository;

	@Autowired
	MispLicenseRepository mispLicenseRepository;
	
	@Autowired
	LastSyncRepository lastSyncRepository;

	@Autowired
	private Environment environment;
	
	@Value("${mosip.pms.utility.run.mode:upgrade}")
	private String runMode;
	
	@Value("#{T(java.util.Arrays).asList('${mosip.pms.allowed.partner.types}')}")
	private List<String> allowedPartnerTypes;

	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private CertUtil certUtil;
	
	@Autowired
	RestUtil restUtil;

	@Autowired
	private WebSubPublisher webSubPublisher;

	public void initialize() {
 
		LocalDateTime lastSync = null;
    	LocalDateTime latestSync = LocalDateTime.now();
		if(!runMode.equals(RUN_MODE_UPGRADE)) {
			 lastSync = getLastSyncTimeStamp();
		}
		LOGGER.info("------------------------Started publishing data from PMS to IDA------------------------");
		try {
				publishPartnerUpdated(lastSync,latestSync);
				publishAPIKeyData(lastSync,latestSync);
				publishMISPLicenseData(lastSync,latestSync);
				if(lastSync!=null) {
					publishUpdateApiKey(lastSync,latestSync);
				}
				if(!runMode.equals(RUN_MODE_UPGRADE)) {
					saveLatestSyncTimeStamp(latestSync);
				}
		} catch (Exception e) {
			LOGGER.error("Error occurred while publishing the data - " ,e);
		}
		LOGGER.info("------------------------Published data from PMS to IDA------------------------");
	}

	
	public void publishPartnerUpdated(LocalDateTime lastSync, LocalDateTime onGoingSync) throws Exception {
		List<Partner> partners = (lastSync == null) ? partnerRepository.findAll() : 
			partnerRepository.findByCreatedDtimeOrUpdDtimeGreaterThanAndIsDeletedFalseOrIsDeletedIsNull(lastSync,onGoingSync);
		Map<String,String> partnerDomainMap = certUtil.getPartnerDomainMap();
		for(Partner partner : partners) {
			if(allowedPartnerTypes.contains(partner.getPartnerTypeCode()) && partner.getCertificateAlias()!=null) {
				String signedPartnerCert = certUtil.getCertificate("PARTNER",partner.getId());
				String partnerDomain = partnerDomainMap.containsKey(partner.getPartnerTypeCode())?
						partnerDomainMap.get(partner.getPartnerTypeCode()):partnerDomainMap.get("Auth_Partner");
				LOGGER.info("Publishing the data for Partner :: " + partner.getId());
				notify(MapperUtils.mapDataToPublishDto(partner, signedPartnerCert), EventType.PARTNER_UPDATED);
				notify(certUtil.getDataShareurl(signedPartnerCert), partnerDomain);
				LOGGER.info("Published the data for label :: " + partner.getId() );
			} else
				LOGGER.info("Skipped publishing since not in allowedPartnerTypes :: " + partner.getId() + " " + partner.getPartnerTypeCode());
		}
		partnerDomainMap.clear();
	}
	
	public void publishUpdateApiKey(LocalDateTime lastSync, LocalDateTime onGoingSync) {
		List<PartnerPolicy> allApprovedPolicies = partnerPolicyRepository.findByUpdDtimeGreaterThanAndIsDeletedFalseOrIsDeletedIsNull(lastSync, onGoingSync);
		for (PartnerPolicy partnerPolicy : allApprovedPolicies) {
			LOGGER.info("Publishing the data for partner :: "
					+ partnerPolicy.getPartner().getId() + "  policy :: " + partnerPolicy.getPolicyId());
			notify(null, null, MapperUtils.mapKeyDataToPublishDto(partnerPolicy), EventType.APIKEY_UPDATED);
			LOGGER.info("Published the data for partner :: "
					+ partnerPolicy.getPartner().getId() + "  policy :: " + partnerPolicy.getPolicyId());
		}
	}
	
	
	public void publishAPIKeyData(LocalDateTime lastSync, LocalDateTime onGoingSync) throws Exception {
		List<PartnerPolicy> allApprovedPolicies = (lastSync == null)?partnerPolicyRepository.findAll():
		             partnerPolicyRepository.findByCreatedDtimeGreaterThanAndIsDeletedFalseOrIsDeletedIsNull(lastSync, onGoingSync);
		
		for (PartnerPolicy partnerPolicy : allApprovedPolicies) {
			LOGGER.info("Publishing the data for partner :: "
					+ partnerPolicy.getPartner().getId() + "  policy :: " + partnerPolicy.getPolicyId());
			Optional<Partner> partnerFromDb = partnerRepository.findById(partnerPolicy.getPartner().getId());
			if(!partnerFromDb.isPresent()) {
				LOGGER.info("Skipped publishing since partner entry not present db :: " + partnerPolicy.getPartner().getId() );
				continue;
			}
			Optional<AuthPolicy> validPolicy = authPolicyRepository.findById(partnerPolicy.getPolicyId());
			if(allowedPartnerTypes.contains(partnerFromDb.get().getPartnerTypeCode()) && partnerFromDb.get().getCertificateAlias()!=null) {
				notify(MapperUtils.mapDataToPublishDto(partnerFromDb.get(),
						getPartnerCertificate(partnerFromDb.get().getCertificateAlias())),
						MapperUtils.mapPolicyToPublishDto(validPolicy.get(),
								getPolicyObject(validPolicy.get().getPolicyFileId())),
						MapperUtils.mapKeyDataToPublishDto(partnerPolicy), EventType.APIKEY_APPROVED);
				LOGGER.info("Published the data for partner :: "
						+ partnerPolicy.getPartner().getId() + "  policy :: " + partnerPolicy.getPolicyId());
			} else
				LOGGER.info("Skipped publishing since not in allowedPartnerTypes :: " + partnerFromDb.get().getId() + " " + partnerFromDb.get().getPartnerTypeCode());

		}
	}

	public void publishMISPLicenseData(LocalDateTime lastSync, LocalDateTime onGoingSync) {
		List<MISPLicenseEntity> mispLicenseFromDb = (lastSync==null) ? mispLicenseRepository.findAll() : 
			mispLicenseRepository.findByCreatedDtimeOrUpdDtimeGreaterThanAndIsDeletedFalseOrIsDeletedIsNull(lastSync, onGoingSync);
		for (MISPLicenseEntity mispLicenseEntity : mispLicenseFromDb) {
			LOGGER.info("Publishing the data for MISPID :: " + mispLicenseEntity.getMispId());
			notify(MapperUtils.mapDataToPublishDto(mispLicenseEntity), EventType.MISP_LICENSE_GENERATED);
			LOGGER.info("Published the data for MISPID :: " + mispLicenseEntity.getMispId());
		}
	}

	private void notify(MISPDataPublishDto dataToPublish, EventType eventType) {
		Type type = new Type();
		type.setName("InfraProviderServiceImpl");
		type.setNamespace("io.mosip.pmp.partner.service.impl.InfraProviderServiceImpl");
		Map<String, Object> data = new HashMap<>();
		data.put("mispLicenseData", dataToPublish);
		webSubPublisher.notify(eventType, data, type);
	}
	
	private String getPartnerCertificate(String certificateAlias) throws Exception {
		Map<String, String> pathsegments = new HashMap<>();
		pathsegments.put("partnerCertId", certificateAlias);
		Map<String, Object> getApiResponse = restUtil
				.getApi(environment.getProperty("pmp.partner.certificate.get.rest.uri"), pathsegments, Map.class);
		PartnerCertDownloadResponeDto responseObject = null;
		try {
			responseObject = mapper.readValue(mapper.writeValueAsString(getApiResponse.get("response")),
					PartnerCertDownloadResponeDto.class);
		} catch (IOException e) {
			LOGGER.error("Error occured while parsing the response ", e);
		}
		if (responseObject == null && getApiResponse.containsKey("errors")) {

		}
		if (responseObject == null) {
			LOGGER.error("Response is null ");
		}

		return responseObject.getCertificateData();
	}

	private void notify(PartnerDataPublishDto partnerDataToPublish, PolicyPublishDto policyDataToPublish,
			APIKeyDataPublishDto apiKeyDataToPublish, EventType eventType) {
		Map<String, Object> data = new HashMap<>();
		if (partnerDataToPublish != null) {
			data.put("partnerData", partnerDataToPublish);
		}
		if (policyDataToPublish != null) {
			data.put("policyData", policyDataToPublish);
		}
		if (apiKeyDataToPublish != null) {
			data.put("apiKeyData", apiKeyDataToPublish);
		}
		notify(data, eventType);
	}

	private void notify(Map<String, Object> data, EventType eventType) {
		Type type = new Type();
		type.setName("PMSDataMigrationService");
		type.setNamespace("PMSDataMigrationService");
		webSubPublisher.notify(eventType, data, type);
	}
	
	private void notify(PartnerDataPublishDto mapDataToPublishDto, EventType partnerUpdated) {
		Type type = new Type();
		type.setName("PartnerServiceImpl");
		type.setNamespace("io.mosip.pmp.partner.service.impl.PartnerServiceImpl");
		Map<String, Object> data = new HashMap<>();
		data.put("partnerData", mapDataToPublishDto);
		webSubPublisher.notify(partnerUpdated, data, type);		
	}
	
	/**
	 * 
	 * @param certData
	 * @param partnerDomain
	 */
	private void notify(String certData, String partnerDomain) {
		Type type = new Type();
		type.setName("PartnerServiceImpl");
		type.setNamespace("io.mosip.pmp.partner.service.impl.PartnerServiceImpl");
		Map<String, Object> data = new HashMap<>();
		data.put(CERT_CHAIN_DATA_SHARE_URL, certData);
		data.put(PARTNER_DOMAIN, partnerDomain);
		webSubPublisher.notify(EventType.CA_CERTIFICATE_UPLOADED, data, type);
	}

	private JSONObject getPolicyObject(String policy) {
		JSONParser parser = new JSONParser();
		try {
			return ((JSONObject) parser.parse(policy));
		} catch (ParseException e) {
			LOGGER.error("Error occurred while parsing the policy file", e.getMessage());
		}
		return null;
	}
	
	public String getUser() {
		if (Objects.nonNull(SecurityContextHolder.getContext())
				&& Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())
				&& Objects.nonNull(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				&& SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof AuthUserDetails) {
			return ((AuthUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
					.getUserId();
		} else {
			return "system";
		}
	}

	public LocalDateTime getLastSyncTimeStamp() {
		LastSync lastSyncDto = lastSyncRepository.findLastSync();
		return (lastSyncDto == null) ? null : lastSyncDto.getLastSync();
	}
	
	public void saveLatestSyncTimeStamp(LocalDateTime LatestSync) {
		
		if(lastSyncRepository.count()>10) {
			lastSyncRepository.deleteOldEntry();
		}
		LastSync latestSync = new LastSync();
		latestSync.setLastSync(LatestSync);
		latestSync.setCreatedBy(getUser());
		latestSync.setCreatedDateTime(LatestSync);
		lastSyncRepository.save(latestSync);
	}
	
}
