package io.mosip.pms.ida.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAbsentContent;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.CryptoUtil;
import io.mosip.pms.ida.dto.DataShareResponseDto;
import io.mosip.pms.ida.dto.KeyPairGenerateResponseDto;


@Component
public class CertUtil {

	public static final String CERTIFICATE_TYPE = "X.509";
	
	private static final Logger LOGGER = UtilityLogger.getLogger(CertUtil.class);
	
	@Value("${pms.certs.datashare.policyId}")
	private String policyId;

	@Value("${pms.certs.datashare.subscriberId}")
	private String subscriberId;
	
	@Autowired
	RestUtil restUtil;
	
	@Autowired
	private Environment environment;
	
	@Autowired
	private ObjectMapper mapper;
	
	private String rootCert;
	
	private String pmsCert;
	
	public String getPmsCert() throws Exception {
		if(pmsCert==null) {
			pmsCert = getCertificate("PARTNER",null);
		}
		return pmsCert;
	}
	
	public String getRootCert() throws Exception {
		if(rootCert==null) {
			rootCert = getCertificate("ROOT",null);
		}
		return rootCert;
	}

	public Certificate convertToCertificate(String certData) {
		try {
			StringReader strReader = new StringReader(certData);
			PemReader pemReader = new PemReader(strReader);
			PemObject pemObject = pemReader.readPemObject();
			byte[] certBytes = pemObject.getContent();
			CertificateFactory certFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
			return certFactory.generateCertificate(new ByteArrayInputStream(certBytes));
		} catch(IOException | CertificateException e) {
			LOGGER.error("Error occured while Converting certData to X509Certificate " );
		}
		return null;
	}
	
	 public String buildP7BCertificateChain(String resignedCert) throws Exception {
		 List<Certificate> chain = new ArrayList<>();
		 chain.add(convertToCertificate(resignedCert));
		 chain.add(convertToCertificate(getPmsCert()));
		 chain.add(convertToCertificate(getRootCert()));
		 return buildCertChain(chain.toArray(new Certificate[0]));
		}
	
	 private static String buildCertChain(Certificate[] chain) {
        
        try {
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();
            JcaCertStore jcaStore = new JcaCertStore(Arrays.asList(chain));
            generator.addCertificates(jcaStore);

            CMSTypedData cmsTypedData = new CMSAbsentContent();
            CMSSignedData cmsSignedData = generator.generate(cmsTypedData);
            return CryptoUtil.encodeToURLSafeBase64(cmsSignedData.getEncoded());
        } catch(CertificateEncodingException | CMSException | IOException e) {
            LOGGER.error("Error generating p7b certificates chain.  " + e.toString());
        }
		return null;
    }
	 
	 public String getDataShareurl(String cert) throws Exception {
		    String certsChain = buildP7BCertificateChain(cert);
			MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			String fileName = "certsChain";
			map.add("name", fileName);
			map.add("filename", fileName);
			ByteArrayResource contentsAsResource = new ByteArrayResource(certsChain.getBytes()) {
				@Override
				public String getFilename() {
					return fileName;
				}
			};
			map.add("file", contentsAsResource);
			List<String> pathSegments = new ArrayList<>();
			pathSegments.add(policyId);
			pathSegments.add(subscriberId);
			DataShareResponseDto response = restUtil.postApi(
					environment.getProperty("pmp.certificaticate.datashare.rest.uri"), pathSegments, "", "",
					MediaType.MULTIPART_FORM_DATA, map, DataShareResponseDto.class);
			if (response == null) {
				LOGGER.error("Error occured while parsing the response " );
			}
			if ((response.getErrors() != null && response.getErrors().size() > 0)) {
//				throw new PartnerServiceException(response.getErrors().get(0).getErrorCode(),
//						response.getErrors().get(0).getMessage());
			}
			System.out.println(response.getDataShare().getUrl());
			return response.getDataShare().getUrl();
		}
	 
	 public String getCertificate(String appId, String refId) throws Exception {
			List<String> queryParamNames = new ArrayList<String>();
			queryParamNames.add("applicationId");
			List<String> queryParamValues = new ArrayList<String>();
			queryParamValues.add(appId);
			if(refId!=null) {
				queryParamNames.add("referenceId");
				queryParamValues.add(refId);
			}
			Map<String, Object> getApiResponse = restUtil
					.getApi(environment.getProperty("pmp.certificate.get.rest.uri"),null, String.join(",", queryParamNames),String.join(",", queryParamValues) , Map.class);
			KeyPairGenerateResponseDto responseObject = null;
			try {
				responseObject = mapper.readValue(mapper.writeValueAsString(getApiResponse.get("response")),
						KeyPairGenerateResponseDto.class);
			} catch (IOException e) {
				LOGGER.error("Error occured while parsing the response ", e);
			}
			if (responseObject == null && getApiResponse.containsKey("errors")) {
				LOGGER.error("Error occured while parsing the response " );
			}
			if (responseObject == null) {
				LOGGER.error("Response is null ");
			}

			return responseObject.getCertificate();
		}

	 public Map<String,String> getPartnerDomainMap(){
			Map<String,String> map = new HashMap<>();
			map.put("Auth_Partner","Auth");
			map.put("FTM_Partner","FTM");
			map.put("MISP_Partner","MISP");
			map.put("Device_Provider","Device");
			return map;
	 }
}
