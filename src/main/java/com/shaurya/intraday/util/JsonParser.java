package com.shaurya.intraday.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author shaurya
 *
 */
public class JsonParser {

	/**
	 * This method will return customized list of Objects from a given list of json
	 * strings
	 *
	 * @param json
	 * @param class1
	 * @return
	 */
	public static <T> List<T> generalJsonListToObjectList(final String json, final Class<?> cls) {
		final ObjectMapper mapper = new ObjectMapper();
		List<T> objectList = null;

		try {
			objectList = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, cls));

		}
		catch (final IOException e) {
		}
		return objectList;
	}

	public static <T> Set<T> generalJsonListToObjectSet(final String json, final Class<?> cls) {
		final ObjectMapper mapper = new ObjectMapper();
		Set<T> objectList = null;

		try {
			objectList = mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(Set.class, cls));

		}
		catch (final IOException e) {
		}
		return objectList;
	}

	/**
	 * This method will return customized model object from a given json String here the
	 * input type for the method is json string and class name of the domain that you want
	 * output as
	 *
	 * @param json - json String that needs to be converted
	 * @param cls - .class name for the pojo
	 * @return
	 */
	public static <T> T generalJsonToObject(final String json, final Class<?> cls) {
		final ObjectMapper mapper = new ObjectMapper();
		T object = null;

		try {
			object = (T) mapper.readValue(json, cls);
		}
		catch (final IOException e) {
			 e.printStackTrace();
		}
		return object;
	}

	/**
	 * converts Object into map format where fieldName is string and its value is value of
	 * the map
	 *
	 * @param object
	 * @return
	 */
	public static Map<String, Object> getMapOutOfBean(final Object object) {
		// BeanMap
		final ObjectMapper mapper = new ObjectMapper();
		final Map<String, Object> dataMap = mapper.convertValue(object, Map.class);
		return dataMap;
	}

	/**
	 * This method will convert any model objectList into json string The key for the json
	 * string is being mapped from getters of your pojo
	 *
	 * @author prashantkumar
	 * @param object
	 * @return Json from Object
	 */
	public static String objectListToJson(final List<? extends Object> list) {
		final ObjectMapper mapper = new ObjectMapper();
		String jsonInString = null;
		try {
			final OutputStream out = new ByteArrayOutputStream();
			mapper.writeValue(out, list);
			// final byte[] data = ((ByteArrayOutputStream) out).toByteArray();
			jsonInString = out.toString();
		}
		catch (final IOException e) {
			// e.printStackTrace();
		}
		return jsonInString;
	}

	/**
	 * This method will convert any model object into json string The key for the json
	 * string is being mapped from getters of your pojo
	 *
	 * @author prashantkumar
	 * @param object
	 * @return Json from Object
	 */
	public static String objectToJson(final Object obj) {
		final ObjectMapper mapper = new ObjectMapper();
		String jsonInString = null;
		try {
			jsonInString = mapper.writeValueAsString(obj);
		}
		catch (final IOException e) {
			// logger.error("Failed to parse object into json");
			// e.printStackTrace();
		}
		return jsonInString;
	}

}
