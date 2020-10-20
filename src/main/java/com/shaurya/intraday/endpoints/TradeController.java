/**
 *
 */
package com.shaurya.intraday.endpoints;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.model.UpdateStrategyDto;
import com.shaurya.intraday.trade.service.SetupServiceImpl;
import com.shaurya.intraday.trade.service.TradeService;
import com.shaurya.intraday.util.HelperUtil;
import com.shaurya.intraday.util.MailSender;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Shaurya
 *
 */
@Slf4j
@RestController
@RequestMapping("/v1")
public class TradeController {

  @Autowired
  private SetupServiceImpl setupService;
  @Autowired
  private TradeService tradeService;
  @Autowired
  private MailAccount mailAccount;

  @RequestMapping(value = "/startup/login", method = RequestMethod.GET)
  public ResponseEntity<Boolean> startUpLogin() {
    setupService.startupLogin();
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/startup", method = RequestMethod.GET)
  public ResponseEntity<Boolean> startUp() {
    try {
      setupService.startup();
    } catch (KiteException | IOException | JSONException e) {
      String reason = "startup failed by cron because :: " + e.getCause();
      log.error("startup failed {}", reason);
      MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.STARTUP_FALIED, reason,
          mailAccount);
    }
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/shutdown", method = RequestMethod.GET)
  public ResponseEntity<Boolean> shutdown() {
    try {
      setupService.shutdown();
    } catch (IOException | KiteException e) {
      String reason = "shutdown failed by cron because :: " + e.getCause();
      log.error("shutdown failed {}", reason);
      MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.SHUTDOWN_FALIED, reason,
          mailAccount);
    }
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/test", method = RequestMethod.POST)
  public ResponseEntity<List<Candle>> test(
      final @RequestParam(value = "security", required = true) Long security,
      final @RequestParam(value = "intervalType", required = true) IntervalType intervalType,
      final @RequestParam(value = "from", required = true) @DateTimeFormat(pattern = "dd-MM-yyyy") Date from,
      final @RequestParam(value = "to", required = true) @DateTimeFormat(pattern = "dd-MM-yyyy") Date to,
      final @RequestParam(value = "candleCount", required = true) Integer candleCount) {
    return new ResponseEntity<List<Candle>>(
        tradeService.getPrevDayCandles(security, IntervalType.MINUTE_15, from, to, candleCount),
        HttpStatus.OK);
  }

  @RequestMapping(value = "/test/indicator", method = RequestMethod.GET)
  public ResponseEntity<String> testIndicator() {
    try {
      tradeService.testIndicator();
    } catch (IOException | KiteException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return new ResponseEntity<>("Check mail", HttpStatus.OK);
  }

  @RequestMapping(value = "/test/simulation", method = RequestMethod.GET)
  public ResponseEntity<String> simulation(
      final @RequestParam(value = "security", required = true) Long security) {
    tradeService.simulation(security);
    return new ResponseEntity<>("Check mail", HttpStatus.OK);
  }

  @RequestMapping(value = "/checkBalance", method = RequestMethod.GET)
  public ResponseEntity<Double> checkBalance() throws IOException, KiteException {
    return new ResponseEntity<Double>(tradeService.checkBalance(), HttpStatus.OK);
  }

  @RequestMapping(value = "/strategies", method = RequestMethod.GET)
  public ResponseEntity<Map<StrategyType, List<StrategyModel>>> getStrategies()
      throws IOException, KiteException {
    Map<StrategyModel, StrategyType> strategyTypeMap = tradeService.getTradeStrategy();
    if (!CollectionUtils.isEmpty(strategyTypeMap)) {
      Map<StrategyType, List<StrategyModel>> reverseMap = new HashMap<>();
      for (Entry<StrategyModel, StrategyType> e : strategyTypeMap.entrySet()) {
        if (reverseMap.get(e.getValue()) == null) {
          reverseMap.put(e.getValue(), new ArrayList<>());
        }
        List<StrategyModel> strategyModelList = reverseMap.get(e.getValue());
        strategyModelList.add(e.getKey());
        reverseMap.put(e.getValue(), strategyModelList);
      }
      return new ResponseEntity<>(reverseMap, HttpStatus.OK);
    }
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @RequestMapping(value = "/update/strategies", method = RequestMethod.GET)
  public ResponseEntity<String> updateStrategies(@RequestParam List<String> name)
      throws IOException, KiteException {
    if (name == null || CollectionUtils.isEmpty(name)) {
      tradeService.updateAllStockToMonitorStock();
      return new ResponseEntity<>("Request or names can not be null", HttpStatus.BAD_REQUEST);
    }
    Map<Long, String> tokenNameMap = tradeService.getNameTokenMap();
    if (CollectionUtils.isEmpty(tokenNameMap)) {
      return new ResponseEntity<>("token-name map can not be null", HttpStatus.BAD_REQUEST);
    }
    Map<String, Long> reverseTokenNameMap = new HashMap<>();
    for (Entry<Long, String> e : tokenNameMap.entrySet()) {
      reverseTokenNameMap.put(e.getValue(), e.getKey());
    }
    List<Long> tokenList = new ArrayList<>();
    for (String s : name) {
      if (reverseTokenNameMap.get(s) == null) {
        return new ResponseEntity<>("No token found for " + s + " please check spelling!",
            HttpStatus.BAD_REQUEST);
      }
      tokenList.add(reverseTokenNameMap.get(s));
    }
    updateNextDayStocks(tokenList);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/update/strategies/bulk", method = RequestMethod.POST)
  public ResponseEntity<String> updateStrategiesBulk(
      @RequestParam(value = "file", required = true) MultipartFile file)
      throws IOException, KiteException {
    List<String> name = readStockFromXlsx(file.getInputStream());
    if (name == null || CollectionUtils.isEmpty(name)) {
      tradeService.updateAllStockToMonitorStock();
      return new ResponseEntity<>("Request or names can not be null", HttpStatus.BAD_REQUEST);
    }
    Map<Long, String> tokenNameMap = tradeService.getNameTokenMap();
    if (CollectionUtils.isEmpty(tokenNameMap)) {
      return new ResponseEntity<>("token-name map can not be null", HttpStatus.BAD_REQUEST);
    }
    Map<String, Long> reverseTokenNameMap = new HashMap<>();
    for (Entry<Long, String> e : tokenNameMap.entrySet()) {
      reverseTokenNameMap.put(e.getValue(), e.getKey());
    }
    List<Long> tokenList = new ArrayList<>();
    for (String s : name) {
      if (reverseTokenNameMap.get(s) == null) {
        return new ResponseEntity<>("No token found for " + s + " please check spelling!",
            HttpStatus.BAD_REQUEST);
      }
      tokenList.add(reverseTokenNameMap.get(s));
    }
    updateNextDayStocks(tokenList);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private List<String> readStockFromXlsx(InputStream file) {
    List<String> stockList = new ArrayList<>();
    try {
      FileInputStream excelFile = (FileInputStream) file;
      Workbook workbook = new XSSFWorkbook(excelFile);
      Sheet datatypeSheet = workbook.getSheetAt(0);
      Iterator<Row> iterator = datatypeSheet.iterator();

      while (iterator.hasNext()) {

        Row currentRow = iterator.next();
        Iterator<Cell> cellIterator = currentRow.iterator();

        while (cellIterator.hasNext()) {

          Cell currentCell = cellIterator.next();
          if (currentCell.getColumnIndex() == 2 && !currentCell.getStringCellValue()
              .contains("NIFTY")) {
            stockList.add(currentCell.getStringCellValue());
          }
        }

      }
    } catch (FileNotFoundException e) {
      log.error("no file found {}", e);
    } catch (IOException e) {
      log.error("error in parsing xlsx file {}", e);
    }
    return stockList;
  }

  private void updateNextDayStocks(final List<Long> eligibleStocks) {
    tradeService.updateAllStockToMonitorStock();
    if (eligibleStocks.size() > 0) {
      //Double marginPortion = Math.min(0.05 / eligibleStocks.size(), 0.005);
      tradeService.updateTradeStocks(eligibleStocks, 0.005);
    }
  }

}
