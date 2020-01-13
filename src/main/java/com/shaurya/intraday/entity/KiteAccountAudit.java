package com.shaurya.intraday.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "kite_account_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KiteAccountAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "account_id")
  private Integer accountId;
  @Column(name = "fund")
  private Integer fund;

}
