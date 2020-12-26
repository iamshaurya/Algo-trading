package com.shaurya.intraday.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreOpenResponse {

  private Integer declines;
  private Integer noChange;
  private Integer advances;
  private List<StockPreOpen> data;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class StockPreOpen implements Comparable<StockPreOpen> {

    private String symbol;
    private Double perChn;

    @Override
    public int compareTo(StockPreOpen o) {
      Double a = Math.abs(this.perChn);
      Double b = Math.abs(o.perChn);
      if (a > b) {
        return -1;
      } else if (a < b) {
        return 1;
      } else {
        return 0;
      }
    }
  }

}
