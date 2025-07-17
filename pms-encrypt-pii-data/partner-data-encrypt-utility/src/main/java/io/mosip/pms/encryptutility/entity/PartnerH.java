package io.mosip.pms.encryptutility.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "partner_h")
@Getter
@Setter
@NoArgsConstructor
public class PartnerH implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private PartnerHPK id;

    private String address;

    @Column(name = "approval_status")
    private String approvalStatus;

    @Column(name = "certificate_alias")
    private String certificateAlias;

    @Column(name = "contact_no")
    private String contactNo;

    @Column(name = "cr_by")
    private String crBy;

    @Column(name = "cr_dtimes")
    private Timestamp crDtimes;

    @Column(name = "del_dtimes")
    private Timestamp delDtimes;

    @Column(name = "email_id")
    private String emailId;

    @Column(name = "email_id_hash")
    private String emailIdHash;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    private String name;

    @Column(name = "partner_type_code")
    private String partnerTypeCode;

    @Column(name = "policy_group_id")
    private String policyGroupId;

    @Column(name = "upd_by")
    private String updBy;

    @Column(name = "upd_dtimes")
    private Timestamp updDtimes;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "addl_info")
    private String additionalInfo;
}
