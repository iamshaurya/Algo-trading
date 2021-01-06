package com.shaurya.intraday.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PreOpenModel {

  private Double change;
  private Boolean isFullGapUp;

}
