/**
 *
 */
package com.shaurya.intraday.entity;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Table(name = "trade_ledger")
@Entity
@Getter
@Setter
public class Trade {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "position_type")
  private Byte positionType;
  @Column(name = "atr")
  private Double atr;
  @Column(name = "sl")
  private Double sl;
  @Column(name = "tp")
  private Double tp;
  @Column(name = "trade_entry_price")
  private Double tradeEntryPrice;
  @Column(name = "trade_exit_price")
  private Double tradeExitPrice;
  @Column(name = "security_name")
  private String securityName;
  @Column(name = "security_code")
  private Long securityCode;
  @Column(name = "order_id")
  private String orderId;
  @Column(name = "quantity")
  private Integer quantity;
  @Column(name = "trade_date")
  private Date tradeDate;
  @Column(name = "status")
  private Byte status;
  @Column(name = "trade_exit_reason")
  private Byte tradeExitReason;
  @Column(name = "risk_reward")
  private Double riskToReward;
  @Column(name = "pl")
  private Double pl;
  @Column(name = "risk")
  private Double risk;
  @Column(name = "current_equity")
	private Double currentEquity;
}
