/**
 * 
 */
package com.shaurya.intraday.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * @author Shaurya
 *
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EquityMargin {
	@JsonProperty("margin")
	public double margin;
	@JsonProperty("co_lower")
	public double coLower;
	@JsonProperty("mis_multiplier")
	public double misMultiplier;
	@JsonProperty("tradingsymbol")
	public String symbol;
	@JsonProperty("co_upper")
	public double coUpper;
	@JsonProperty("nrml_margin")
	public double nrmlMargin;
	@JsonProperty("mis_margin")
	public double misMargin;
	
}
