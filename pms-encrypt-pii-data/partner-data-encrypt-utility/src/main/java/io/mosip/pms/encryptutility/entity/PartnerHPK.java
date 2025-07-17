package io.mosip.pms.encryptutility.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PartnerHPK implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "eff_dtimes")
    private Date effDtimes;
}
