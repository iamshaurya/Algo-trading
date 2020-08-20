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
 */
@Table(name = "trade_performance")
@Entity
@Getter
@Setter
public class Performance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;
  @Column(name = "starting_capital")
  private Double startingCapital;
  @Column(name = "current_capital")
  private Double currentCapital;
  @Column(name = "return_percentage")
  private Double returnPercentage;
  @Column(name = "total_win_trade")
  private Integer totalWinningTrade;
  @Column(name = "total_win_r")
  private Double totalWinningR;
  @Column(name = "avg_win_r")
  private Double avgWinningR;
  @Column(name = "total_loss_trade")
  private Integer totalLosingTrade;
  @Column(name = "total_loss_r")
  private Double totalLosingR;
  @Column(name = "avg_loss_r")
  private Double avgLosingR;
  @Column(name = "win_rate")
  private Double winRate;
  @Column(name = "edge")
  private Double edge;
  @Column(name = "sharpe_ratio")
  private Double sharpeRatio;
  @Column(name = "max_drawdown")
  private Double maxDrawDown;
}

