/**
 * 
 */
package com.shaurya.intraday.strategy;

import java.util.List;

import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.StrategyModel;

/**
 * @author Shaurya
 *
 */
public interface Strategy {
	public void initializeSetup(List<Candle> cList);
	
	public void destroySetup();

	public StrategyModel processTrades(Candle candle, StrategyModel openTrade, boolean updateSetup);
	
	public void updateSetup(Candle candle);
}
