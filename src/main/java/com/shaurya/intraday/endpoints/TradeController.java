/**
 *
 */
package com.shaurya.intraday.endpoints;

import com.shaurya.intraday.constant.Constants;
import com.shaurya.intraday.enums.IntervalType;
import com.shaurya.intraday.enums.StrategyType;
import com.shaurya.intraday.model.Candle;
import com.shaurya.intraday.model.MailAccount;
import com.shaurya.intraday.model.PreOpenResponse;
import com.shaurya.intraday.model.PreOpenResponse.StockPreOpen;
import com.shaurya.intraday.model.StockBeta;
import com.shaurya.intraday.model.StrategyModel;
import com.shaurya.intraday.trade.service.LiveTickerService;
import com.shaurya.intraday.trade.service.SetupServiceImpl;
import com.shaurya.intraday.trade.service.TradeService;
import com.shaurya.intraday.util.HttpClientService;
import com.shaurya.intraday.util.JsonParser;
import com.shaurya.intraday.util.MailSender;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
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

  private static final String volatility_sdf = "ddMMyyyy";

  @Autowired
  private LiveTickerService liveTickerService;
  @Autowired
  private SetupServiceImpl setupService;
  @Autowired
  private TradeService tradeService;
  @Autowired
  private MailAccount mailAccount;
  @Value("${nse.stock.list.url}")
  private String niftyStocksUrl;
  @Value("${nse.stock.high.beta.list.url}")
  private String highBetaUrl;
  @Value("${nse.daily.volatilty.report.url}")
  private String niftyDailyVolatilityUrl;
  @Value("${nse.daily.preopen.url}")
  private String niftyDailyPreOpenUrl;

  @RequestMapping(value = "/startup/login", method = RequestMethod.GET)
  public ResponseEntity<Boolean> startUpLogin() {
    setupService.startupLogin();
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/startup", method = RequestMethod.GET)
  public ResponseEntity<Boolean> startUp() {
    try {
      liveTickerService.filterPreOpenStock();
    } catch (Exception e) {
      String reason = "startup failed by cron because :: " + e.getCause();
      log.error("startup failed {}", reason);
      MailSender.sendMail(Constants.TO_MAIL, Constants.TO_NAME, Constants.STARTUP_FALIED, reason,
          mailAccount);
    }
    return new ResponseEntity<>(true, HttpStatus.OK);
  }

  @RequestMapping(value = "/preopen", method = RequestMethod.GET)
  public ResponseEntity<List<String>> preopen() throws IOException {
    List<String> stockNames = getTopPreopenStock();
    return new ResponseEntity<>(stockNames, HttpStatus.OK);
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
  public ResponseEntity<String> updateStrategies()
      throws IOException, KiteException {
    List<StockBeta> stockBetas = getTodaysVolatileStocks();
    tradeService.updateAllStockToMonitorStock();
    updateStockBetaValue(stockBetas);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  private void updateStockBetaValue(List<StockBeta> stockBetas) {
    for (StockBeta sb : stockBetas) {
      tradeService.updateStockBetaValue(sb.getName(), sb.getBeta());
    }
  }

  @RequestMapping(value = "/holidays", method = RequestMethod.GET)
  public ResponseEntity<Boolean> getHolidayList(
      final @RequestParam(value = "date", required = true) String dateStr)
      throws ParseException {
    Set<Date> holidays = tradeService.getHolidayDates();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date date = sdf.parse(dateStr);
    return new ResponseEntity<>(holidays.contains(date), HttpStatus.OK);
  }

  private List<StockBeta> getTodaysVolatileStocks() throws IOException {
    Map<String, Double> nifty50StockMap = new HashMap<>();
    HttpResponse niftyStockListResponse = (HttpResponse) HttpClientService
        .executeGetRequest(niftyStocksUrl, new ArrayList<>());
    if (niftyStockListResponse != null
        && niftyStockListResponse.getStatusLine().getStatusCode() == 200) {
      String niftyStockListResponseStr = EntityUtils
          .toString(niftyStockListResponse.getEntity());
      String[] niftyStockArr = niftyStockListResponseStr.split("\n");
      for (int i = 1; i < niftyStockArr.length; i++) {
        String[] row = niftyStockArr[i].split(",");
        nifty50StockMap.put(row[2], 0.0);
      }
    }
    List<StockBeta> stockBetas = new ArrayList<>();
    SimpleDateFormat sdf = new SimpleDateFormat(volatility_sdf);
    String dailyVoltilityListUrl = niftyDailyVolatilityUrl + sdf.format(new Date()) + ".CSV";
    HttpResponse dailyVolatilityResponse = (HttpResponse) HttpClientService
        .executeGetRequest(dailyVoltilityListUrl, new ArrayList<>());
    if (dailyVolatilityResponse != null
        && dailyVolatilityResponse.getStatusLine().getStatusCode() == 200) {
      String dailyVolatilityResponseStr = EntityUtils
          .toString(dailyVolatilityResponse.getEntity());
      String[] dailyVolatilityArr = dailyVolatilityResponseStr.split("\n");
      for (int i = 1; i < dailyVolatilityArr.length; i++) {
        String[] row = dailyVolatilityArr[i].split(",");
        if (nifty50StockMap.get(row[1]) != null && Double.valueOf(row[2]) >= 50.0) {
          StockBeta sb = new StockBeta(Double.valueOf(row[row.length - 2]) * 100, row[1]);
          stockBetas.add(sb);
        }
      }
    }
    Collections.sort(stockBetas);
    return stockBetas;
  }

  private List<String> getTopPreopenStock() throws IOException {
    List<String> stockName = new ArrayList<>();

    List<StockPreOpen> preOpenStockList = new ArrayList<>();
    Map<String, String> headers = new HashMap<>();
    headers.put("Connection", "keep-alive");
    headers.put("User-Agent",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
    headers.put("Accept-Language", "en-GB,en-US;q=0.9,en;q=0.8");
    HttpResponse preOpenStockResponse = (HttpResponse) HttpClientService
        .executeGetRequestWithHeaders(niftyDailyPreOpenUrl, new ArrayList<>(), headers);
    if (preOpenStockResponse != null
        && preOpenStockResponse.getStatusLine().getStatusCode() == 200) {
      String preOpenStockResponseStr = EntityUtils
          .toString(preOpenStockResponse.getEntity());
      PreOpenResponse preOpenResponse = JsonParser
          .generalJsonToObject(preOpenStockResponseStr, PreOpenResponse.class);
      preOpenStockList.addAll(preOpenResponse.getData());
    }

    Collections.sort(preOpenStockList);
    for (int i = 0; i < 10; i++) {
      stockName.add(preOpenStockList.get(i).getSymbol());
    }
    return stockName;
  }

  @RequestMapping(value = "/update/strategies/bulk", method = RequestMethod.POST)
  public ResponseEntity<String> updateStrategiesBulk(
      @RequestParam(value = "file", required = true) MultipartFile file)
      throws IOException, KiteException {
    Map<String, Double> name = readStockFromCsv(file.getInputStream());
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
    Map<Long, Double> tokenList = new HashMap<>();
    for (Entry<String, Double> s : name.entrySet()) {
      if (reverseTokenNameMap.get(s.getKey()) == null) {
        return new ResponseEntity<>("No token found for " + s + " please check spelling!",
            HttpStatus.BAD_REQUEST);
      }
      tokenList.put(reverseTokenNameMap.get(s.getKey()), s.getValue());
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
          if (currentCell.getRowIndex() > 1 && currentCell.getColumnIndex() == 2 && !currentCell
              .getStringCellValue()
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

  private Map<String, Double> readStockFromCsv(InputStream file) {
    Map<String, Double> stockList = new HashMap<>();
    try {
      BufferedReader oldBr = new BufferedReader(new InputStreamReader(file));
      String data = null;
      int i = 0;
      while ((data = oldBr.readLine()) != null) {
        if (i == 0) {
          i++;
          continue;
        }
        i++;
        String[] dataArr = data.split(",");
        stockList.put(dataArr[1], Double.valueOf(dataArr[5]));
      }
    } catch (FileNotFoundException e) {
      log.error("no file found {}", e);
    } catch (IOException e) {
      log.error("error in parsing xlsx file {}", e);
    }
    return stockList;
  }

  private void updateNextDayStocks(final Map<Long, Double> eligibleStocks) {
    tradeService.updateAllStockToMonitorStock();
    if (eligibleStocks.size() > 0) {
      for (Entry<Long, Double> e : eligibleStocks.entrySet()) {
        tradeService.updateTradeStocks(e.getKey(), e.getValue(), 0.005);
      }
    }
  }

  private void updateNextDayStocks(final List<Long> eligibleStocks) {
    tradeService.updateAllStockToMonitorStock();
    if (eligibleStocks.size() > 0) {
      tradeService.updateTradeStocks(eligibleStocks, 0.005);
    }
  }

}
