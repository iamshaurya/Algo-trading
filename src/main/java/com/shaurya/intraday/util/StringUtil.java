package com.shaurya.intraday.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author prashantkumar
 *
 */
public class StringUtil {

	public static <T, C extends Collection<T>> String convertCollectionToDelimetedString(final C coll) {
		if (coll == null || coll.isEmpty()) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		for (final T t : coll) {
			sb.append(t.toString());
			sb.append(',');
		}
		final String res = sb.toString();
		if (res.charAt(res.length() - 1) == ',') {
			return res.substring(0, res.length() - 1);
		}
		return sb.toString();
	}

	public static <T> String convertListToDelimetedString(final List<T> idList) {
		final StringBuilder sb = new StringBuilder();
		if (idList != null && !idList.isEmpty()) {
			sb.append(idList.get(0));
			for (int i = 1; i < idList.size(); i++) {
				if (idList.get(i) != null) {
					sb.append(',');
					sb.append(idList.get(i));
				}
			}
		}
		else {
			sb.append("-1");
		}
		return sb.toString();
	}

	public static String getStackTraceInStringFmt(final Throwable ex) {
		final Writer writer = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(writer);
		ex.printStackTrace(printWriter);
		return writer.toString();

	}

	/**
	 * Check if string is empty or not.
	 */
	public static boolean isEmpty(final String s) {
		return s == null || s.trim().length() == 0;

	}

}
