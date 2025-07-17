package io.mosip.pms.encryptutility.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Entity representing the partner_contact table.
 */
@Entity
@Table(name = "partner_contact")
@Getter
@Setter
@NoArgsConstructor
public class PartnerContact implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    private String address;

    @Column(name = "contact_no")
    private String contactNo;

    @Column(name = "cr_by")
    private String crBy;

    @Column(name = "cr_dtimes")
    private LocalDateTime crDtimes;

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

    @Column(name = "upd_by")
    private String updBy;

    @Column(name = "upd_dtimes")
    private LocalDateTime updDtimes;

    @Column(name = "partner_id")
    private String partnerId;
}
