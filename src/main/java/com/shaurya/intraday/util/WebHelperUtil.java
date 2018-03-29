/**
 * 
 */
package com.shaurya.intraday.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;

/**
 * @author Shaurya
 *
 */
public class WebHelperUtil {
	public static File downloadCSV(String url, List<NameValuePair> nvp) throws ParseException, IOException {
		File file = null;
		HttpResponse response = (HttpResponse) HttpClientService.executeGetRequest(url, new ArrayList<>());
		if (response != null && response.getStatusLine().getStatusCode() == 200) {
			String resStr = EntityUtils.toString(response.getEntity());
			String[] line = resStr.split("\\r?\\n");
			file = new File("/tmp/" + System.currentTimeMillis() + "_nse_200");
			file.createNewFile();
			FileWriter writer = new FileWriter(file);
			for (String l : line) {
				writer.write(l);
				writer.write("\n");
			}
			writer.flush();
			writer.close();
		}
		return file;
	}
	
	public static <T> List<T> executeGetRequest(String url, List<NameValuePair> nvp, Class<?> clzz)
			throws ParseException, IOException{
		List<T> list = new ArrayList<>();
		HttpResponse response = (HttpResponse) HttpClientService.executeGetRequest(url, new ArrayList<>());
		if (response != null && response.getStatusLine().getStatusCode() == 200) {
			String resStr = EntityUtils.toString(response.getEntity());
			list = JsonParser.generalJsonListToObjectList(resStr, clzz);
		}
		return list;
	}
}
