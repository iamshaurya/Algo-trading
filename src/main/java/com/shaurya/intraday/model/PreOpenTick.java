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
public class PreOpenTick implements Comparable<PreOpenTick> {

  private Double preOpenGap;
  private Long instrumentToken;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PreOpenTick preOpenTick = (PreOpenTick) o;
    return Objects.equals(instrumentToken, preOpenTick.instrumentToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(instrumentToken);
  }

  @Override
  public int compareTo(PreOpenTick o) {
    Double a = this.preOpenGap;
    Double b = o.preOpenGap;
    if (a > b) {
      return -1;
    } else if (a < b) {
      return 1;
    } else {
      return 0;
    }
  }
}
