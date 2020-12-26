package com.shaurya.intraday.entity;

import java.util.Objects;
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
public class KiteAccountAudit implements Comparable<KiteAccountAudit>{

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "account_id")
  private Integer accountId;
  @Column(name = "fund")
  private Long fund;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KiteAccountAudit that = (KiteAccountAudit) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public int compareTo(KiteAccountAudit o) {
    Integer a = this.id;
    Integer b = o.id;
    if (a > b) {
      return 1;
    } else if (a < b) {
      return -1;
    } else {
      return 0;
    }
  }
}
