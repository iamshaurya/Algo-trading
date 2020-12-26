package com.shaurya.intraday.model;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StockBeta implements Comparable<StockBeta>{

  private Double beta;
  private String name;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StockBeta stockBeta = (StockBeta) o;
    return Objects.equals(name, stockBeta.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public int compareTo(StockBeta o) {
    Double a = this.beta;
    Double b = o.beta;
    if (a > b) {
      return -1;
    } else if (a < b) {
      return 1;
    } else {
      return 0;
    }
  }
}
