package Cn.Boco.Until;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.log4j.Logger;

public class Util {

	public static String encoding = System.getProperty("file.encoding");
	public static Logger logger = Logger.getLogger(Util.class);
	public static String propertyfile = "conf/db.properties";
	public static Properties p;
	public static String DRIVER;
	public static String URL;
	public static String USER;
	public static String PASSWD;
	public static String DbEncouding;
	SimpleDateFormat sdf = new SimpleDateFormat(" yyyy-MM-dd HH:mm:ss ");
	static {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			Class.forName("com.mysql.jdbc.Driver");
			Class.forName("org.apache.hive.jdbc.HiveDriver");
		} catch (ClassNotFoundException e) {
			logger.error("没有数据库驱动包", e);
		}
		InputStream in = null;
		p = new Properties();
		try {
			in = new FileInputStream(new File(propertyfile));
			p.load(in);
			in.close();
		} catch (Exception e) {
			logger.error("获取配置文件失败:", e);
		}
		DRIVER = "" == (String) p.get("driver") ? "oracle.jdbc.driver.OracleDriver" : (String) p.get("driver");
		URL = "" == (String) p.get("url") ? "jdbc:oracle:thin:@10.12.1.128:1521:obidb2" : (String) p.get("url");
		USER = "" == (String) p.get("username") ? "sysdss_dc" : (String) p.get("username");
		PASSWD = "" == (String) p.get("password") ? "sysdss_dc" : (String) p.get("password");
		DbEncouding = "" == (String) p.get("DbEncouding") ? "GBK" : (String) p.get("DbEncouding");
	}

	public static Connection getConnection() {
		Connection connection;

		try {
			Class.forName(DRIVER);
			connection = DriverManager.getConnection(URL, USER, PASSWD);
			return connection;
		} catch (Exception e) {
			logger.warn("无法连接ORACLE 异常信息", e);
			return null;
		}
	}

	public static void release(ResultSet rs, Statement st, Connection con) {
		try {
			if (rs != null) {
				rs.close();
				rs = null;
			}
			if (st != null) {
				st.close();
				st = null;
			}
			if (con != null) {
				con.close();
				con = null;
			}
		} catch (SQLException e) {
			logger.error("无法正确关闭SQL连接", e);
		} finally {
			try {
				if (con != null) {
					con.close();
					con = null;
				}
			} catch (SQLException e) {
				logger.error("无法正确关闭SQL", e);
			}
		}
	}

	public static String getData(String format) {
		SimpleDateFormat df = new SimpleDateFormat(format);// 设置日期格式
		return df.format(new Date());// new Date()为获取当前系统时间
	}

	public static String BlobToString(Blob bl) {
		try {
			if (bl == null) {
				return null;
			}
			//--2016-09-08 增加字符集
			InputStreamReader in = new InputStreamReader(bl.getBinaryStream(), DbEncouding);
			BufferedReader bf = new BufferedReader(in);
			String temp = "";
			StringBuffer strsql = new StringBuffer();
			while ((temp = bf.readLine()) != null) {
				strsql.append(temp);
			}
			in.close();
			// System.out.println("bolbsql:" + strsql.toString());
			return strsql.toString();
		} catch (SQLException e) {
			logger.error("无法正确接解析BLOB数据", e);
			return null;
		} catch (IOException e) {
			logger.error("无法从缓冲流中读取一行", e);
			return null;
		}
	}

	/**
	 * 
	 * @param executetime
	 *            00:00:00
	 * @param executedate
	 *            阿拉伯数字
	 * @param desc
	 * @param count
	 * @param actstart
	 *            00:00:00
	 * @param actend
	 *            00:00:00
	 * @param start
	 * @return
	 * @throws ParseException
	 */
	public static List<String> computertime(String executetime, String executedate, String desc, Integer count,
			String actstart, String actend, Date start) throws ParseException {
		int flag_ = 0;
		Date startdate = getStarttime();// 获取开始时间当天0点
		Date enddate = getendtime();// 获取开始时间当天23点59
		Date fstartdate = getStarttime();// 获取开始时间 不用于计算 只用于比较
		Date fenddate = getendtime();

		String startstr = DateToString(start);// 开始时间的字符串类型订单生效第一天
		List<String> params = new ArrayList<String>();
		try {
			String executetime_1 = executetime.replaceAll("[: -]", "");
			if ("分钟".equals(desc)) {
				flag_ = 1;
				startstr = startstr.substring(12);
				startdate = new Date(startdate.getTime() + Long.parseLong(startstr) * 1000l);
				if ("".equals(actstart) || actstart == null) {// 那么就是全天匹配
					while ((startdate.getTime() - start.getTime()) % (count.longValue() * 1000l) != 0) {
						startdate = new Date(startdate.getTime() + 1000l * 60l);// 加一分钟
					}
					while (startdate.getTime() <= enddate.getTime()) {
						String param = DateToString(startdate);
						params.add(param);
						startdate = new Date(startdate.getTime() + count.longValue() * 60l * 1000l);// 加间隔时间
					}
				} else {// 匹配活动区间的
					startdate = Add(startdate, actstart);
					enddate = new Date(Add(startdate, actend).getTime() + 999l);
					// starttime订单开始的有效期第一天
					while ((startdate.getTime() - start.getTime()) % (count.longValue() * 1000l) != 0) {
						startdate = new Date(startdate.getTime() + 1000l * 60l);// 加一分钟
					}
					while (startdate.getTime() <= enddate.getTime()) {
						String param = DateToString(startdate);
						params.add(param);
						startdate = new Date(startdate.getTime() + count.longValue() * 60l * 1000l);// 加间隔时间
					}
				}
			}
			if ("小时".equals(desc)) {
				actstart = "";
				flag_ = 1;
				startstr = startstr.substring(10);
				// int executetime_1 = Integer.parseInt(executetime.substring(0,
				// 2));
				// System.out.println("生效时间的小时整数 " + executetime_1);

				// 当前时间和生效第一天相同，
				long m = ((long) (Integer.parseInt(startstr) / 100)) * 60l * 1000l;
				long s = ((long) (Integer.parseInt(startstr.substring(2)))) * 1000l;

				startdate = new Date(startdate.getTime() + m + s);
				// start 开始时间的字符串类型订单生效第一天
				// count循环周期
				if ("".equals(actstart) || actstart == null) {// 那么就是全天匹配
					while ((startdate.getTime() - start.getTime()) % (count.longValue() * 3600l * 1000l) != 0) {
						startdate = new Date(startdate.getTime() + 1000l * 3600l);// 加一小时
					}
					while (startdate.getTime() <= enddate.getTime()) {
						String param = DateToString(startdate);
						// 拼接上具体执行时间点
						param = param.substring(0, param.length() - 4)
								+ executetime_1.substring(executetime_1.length() - 4);
						// System.out.println(param);
						params.add(param);
						startdate = new Date(startdate.getTime() + count.longValue() * 3600l * 1000l);// 加间隔时间
					}
				} else {// 匹配活动区间的
					enddate = new Date(Add(startdate, actend).getTime() + 999l);
					startdate = Add(startdate, actstart);

					while ((startdate.getTime() - start.getTime()) % (count.longValue() * 3600l * 1000l) != 0) {
						startdate = new Date(startdate.getTime() + 1000l * 60l);// 加一分钟
					}
					while (startdate.getTime() <= enddate.getTime()) {
						String param = DateToString(startdate);
						param = param.substring(0, param.length() - 4)
								+ executetime_1.substring(executetime_1.length() - 4);
						params.add(param);
						startdate = new Date(startdate.getTime() + count.longValue() * 3600l * 1000l);// 加间隔时间
					}
				}
			}
			if ("日".equals(desc)) {
				flag_ = 1;
				startstr = startstr.substring(8);
				long s = ((long) (Integer.parseInt(startstr.substring(4, 6)))) * 1000l;
				long m = ((long) (Integer.parseInt(startstr.substring(2, 4)))) * 60l * 1000l;
				long h = ((long) (Integer.parseInt(startstr.substring(0, 2)))) * 3600l * 1000l;
				startdate = new Date(startdate.getTime() + m + s + h);
				if (((startdate.getTime() - start.getTime()) / 1000) % (count.longValue() * 24l * 3600l) != 0) {// 说明今天无任务
					return null;
				} else {
					if (!"".equals(executetime) && executetime != null) {// 根据扩展时间算出时间
						startdate = getStarttime(); // 00:00:00
						/* 这里可能出现截取字符串异常 */

						s = ((long) (Integer.parseInt(executetime.substring(6, 8)))) * 1000l;
						m = ((long) (Integer.parseInt(executetime.substring(3, 5)))) * 60l * 1000l;
						h = ((long) (Integer.parseInt(executetime.substring(0, 2)))) * 3600l * 1000l;
						startdate = new Date(startdate.getTime() + s + m + h);
						String param = DateToString(startdate);
						param = param.substring(0, param.length() - 6)
								+ executetime_1.substring(executetime_1.length() - 6);
						params.add(param);
						return params;
					} else {// 直接返回0点时间
						String param = DateToString(startdate);
						param = param.substring(0, param.length() - 6)
								+ executetime.substring(executetime.length() - 6);
						params.add(param);
						return params;
					}
				}
			}
			if ("周".equals(desc)) {
				flag_ = 1;
				Date d1 = Week(1, start);
				Date d2 = Week(1, new Date());
				if ((d2.getTime() - d1.getTime()) % (count.longValue() * 7l * 24l * 3600l * 1000l) != 0) {// 本周无任务
					return null;
				} else {
					if ("".equals(executedate) || executedate == null) {// 如果没有扩展的日期
						// 那么就以开始时间的周几为准
						if ("".equals(executetime) || executetime == null) {// 如果没有扩展时间那么就以当初的时间为准
							// 获取当初时间的时分秒
							startstr = startstr.substring(8);
							long s = ((long) (Integer.parseInt(startstr.substring(4, 6)))) * 1000l;
							long m = ((long) (Integer.parseInt(startstr.substring(2, 4)))) * 60l * 1000l;
							long h = ((long) (Integer.parseInt(startstr.substring(0, 2)))) * 3600l * 1000l;
							startdate = new Date(startdate.getTime() + m + s + h);// 20140505020202
							enddate = new Date(Week(1, enddate).getTime() + 24l * 3600l * 1000l - 1l);
							while ((startdate.getTime() - start.getTime())
									% (count.longValue() * 7l * 24l * 3600l * 1000l) != 0) {
								startdate = new Date(startdate.getTime() + 1000l * 3600l * 24l);// 加一天
								if (startdate.getTime() > enddate.getTime()) {
									break;
								}
							}
							String param = DateToString(startdate);
							if (startdate.getTime() <= fenddate.getTime()
									&& startdate.getTime() >= fstartdate.getTime()) {
								params.add(param);
								return params;
							} else {
								return null;
							}
						} else {// 那么就以扩展时间为准
							long s = ((long) (Integer.parseInt(executetime.substring(6, 8)))) * 1000l;
							long m = ((long) (Integer.parseInt(executetime.substring(3, 5)))) * 60l * 1000l;
							long h = ((long) (Integer.parseInt(executetime.substring(0, 2)))) * 3600l * 1000l;
							startdate = new Date(startdate.getTime() + s + m + h);// 现在是周一的扩展时间
							enddate = new Date(Week(1, enddate).getTime() + 24l * 3600l * 1000l - 1l);
							while ((startdate.getTime() - start.getTime())
									% (count.longValue() * 7l * 24l * 3600l * 1000l) != 0) {
								startdate = new Date(startdate.getTime() + 1000l * 3600l * 24l);// 加一天
								if (startdate.getTime() > enddate.getTime()) {
									break;
								}
							}
							String param = DateToString(startdate);
							if (startdate.getTime() <= fenddate.getTime()
									&& startdate.getTime() >= fstartdate.getTime()) {
								params.add(param);
								return params;
							} else {
								return null;
							}
						}
					} else {// 有周几的安排
						if ("".equals(executetime) || executetime == null) {// 直接用周几的安排
							// 但是
							// 时分秒还是用当初的
							if (Integer.parseInt(executedate) >= 7) {// 如果是周7
								startdate = Week(1, startdate);// 锁定这一天
							} else {
								startdate = Week(Integer.parseInt(executedate), startdate);// 锁定这一天
							}
							// 获取当初时间的时分秒
							startstr = startstr.substring(8);
							long s = ((long) (Integer.parseInt(startstr.substring(4, 6)))) * 1000l;
							long m = ((long) (Integer.parseInt(startstr.substring(2, 4)))) * 60l * 1000l;
							long h = ((long) (Integer.parseInt(startstr.substring(0, 2)))) * 3600l * 1000l;
							startdate = new Date(startdate.getTime() + m + s + h);// 20140505020202
							String param = DateToString(startdate);
							if (startdate.getTime() <= fenddate.getTime()
									&& startdate.getTime() >= fstartdate.getTime()) {
								params.add(param);
								return params;
							} else {
								return null;
							}
						} else {//
							if (Integer.parseInt(executedate) >= 7) {// 如果是周7
								startdate = Week(1, startdate);// 锁定这一天
							} else {
								startdate = Week(Integer.parseInt(executedate), startdate);// 锁定这一天
							}
							// 获取规定的时分秒
							long s = ((long) (Integer.parseInt(executetime.substring(6, 8)))) * 1000l;
							long m = ((long) (Integer.parseInt(executetime.substring(3, 5)))) * 60l * 1000l;
							long h = ((long) (Integer.parseInt(executetime.substring(0, 2)))) * 3600l * 1000l;
							startdate = new Date(startdate.getTime() + s + m + h);// 现在是周一的扩展时间
							String param = DateToString(startdate);
							if (startdate.getTime() <= fenddate.getTime()
									&& startdate.getTime() >= fstartdate.getTime()) {
								params.add(param);
								return params;
							} else {
								return null;
							}
						}
					}
				}
			}
			if ("月".equals(desc)) {
				flag_ = 1;
				if (computerMonth(start, new Date()) % count != 0) {// 那么当前月无任务
					return null;
				} else {// 当前月有任务 计算出这个时间
					startdate = getMonthStarttime();
					if ("".equals(executedate) || executedate == null) {// 如果无扩展日期
						// 那么时间就是开始日期的时间
						Integer day = Integer.parseInt(startstr.substring(6, 8));
						startdate = new Date(startdate.getTime() + day.longValue() * 24l * 3600l * 1000l);

					} else {
						Integer day = Integer.parseInt(executedate);
						startdate = new Date(startdate.getTime() + day.longValue() * 24l * 3600l * 1000l);
					}
					if ("".equals(executetime) || executetime == null) {
						// 获取当初时间的时分秒
						startstr = startstr.substring(8);
						long s = ((long) (Integer.parseInt(startstr.substring(4, 6)))) * 1000l;
						long m = ((long) (Integer.parseInt(startstr.substring(2, 4)))) * 60l * 1000l;
						long h = ((long) (Integer.parseInt(startstr.substring(0, 2)) - 24)) * 3600l * 1000l;
						startdate = new Date(startdate.getTime() + m + s + h);
					} else {

						// 获取规定的时分秒
						long s = ((long) (Integer.parseInt(executetime.substring(6, 8)))) * 1000l;
						long m = ((long) (Integer.parseInt(executetime.substring(3, 5)))) * 60l * 1000l;
						long h = ((long) (Integer.parseInt(executetime.substring(0, 2)) - 24)) * 3600l * 1000l;
						startdate = new Date(startdate.getTime() + s + m + h);//

					}

					if (startdate.getTime() <= fenddate.getTime() && startdate.getTime() >= fstartdate.getTime()) {
						String param = DateToString(startdate);
						params.add(param);
						return params;
					} else {
						return null;
					}
				}
			}
		} catch (NullPointerException e) {
			logger.warn("Util类的computertime方法 空指针异常", e);
		} catch (ArrayIndexOutOfBoundsException e) {
			logger.warn("Util类的computertime方法数组越界异常", e);
		} catch (NumberFormatException e) {
			logger.warn("Util类的computertime方法   数据格式异常，试图把一字符串非法转换成数值（或相反）", e);
		} catch (StringIndexOutOfBoundsException e) {
			logger.warn("Utile类Exception", e);
		}
		if (flag_ == 0) {
			logger.warn("Util类的computertime方法 传入非法的日期循环单位  不是小时 分 日 周 月 导致无法计算时间");
		}
		return params;
	}

	public static Date StringToDate(String dateStr, String formatStr) {
		SimpleDateFormat dd = new SimpleDateFormat(formatStr);
		Date date = null;
		try {
			date = dd.parse(dateStr);
			return date;
		} catch (ParseException e) {
			logger.error("把" + dateStr + "按照" + formatStr + "格式时候转换出错", e);

		}
		return null;
	}

	public static String DateToString(Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		return sdf.format(date);
	}

	public static Date getStarttime() {
		Calendar currentDate = new GregorianCalendar();
		currentDate.set(Calendar.HOUR_OF_DAY, 0);
		currentDate.set(Calendar.MINUTE, 0);
		currentDate.set(Calendar.SECOND, 0);
		currentDate.set(Calendar.MILLISECOND, 0);
		Date date = currentDate.getTime();
		currentDate.clone();
		return date;
	}

	public static Date getMonthStarttime() {
		Date d = new Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMM", Locale.CHINA);
		try {
			return format.parse(format.format(d));
		} catch (ParseException e) {
			Util.logger.warn("无法解析年月", e);// 基本不可能发生
		}
		return null;
	}

	public static Date getendtime() {
		Calendar currentDate = new GregorianCalendar();
		currentDate.set(Calendar.HOUR_OF_DAY, 23);
		currentDate.set(Calendar.MINUTE, 59);
		currentDate.set(Calendar.SECOND, 59);
		currentDate.set(Calendar.MILLISECOND, 999);
		Date date = currentDate.getTime();
		currentDate.clone();
		return date;
	}

	public static Date Add(Date date, String time) {
		String[] strs = time.split(":");
		Integer h = Integer.parseInt(strs[0]);
		Integer m = Integer.parseInt(strs[1]);
		Integer s = Integer.parseInt(strs[2]);
		Date mydate = new Date(
				date.getTime() + h.longValue() * 3600l * 1000l + m.longValue() * 60l * 1000l + s.longValue() * 1000l);
		return mydate;
	}

	/**
	 * 算出一個日期的周幾 返回date类型 以周1为第一天
	 * 
	 * @param i
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static Date Week(Integer i, Date date) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
		Calendar cl = Calendar.getInstance(Locale.CHINA);
		cl.setFirstDayOfWeek(Calendar.MONDAY);
		cl.setTime(date);
		cl.set(Calendar.DAY_OF_WEEK, i + 1);
		Date d = format.parse(format.format(cl.getTime()));
		cl.clone();
		return d;
	}

	/**
	 * 计算出应该是当前日期所属月份的那一天执行
	 * 
	 * @param i
	 * @param date
	 * @return
	 * @throws ParseException
	 */
	public static Date Month(Integer i, Date date) throws ParseException {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
		Calendar cl = Calendar.getInstance(Locale.CHINA);
		String yyyymmdd = format.format(date);
		Integer Year = Integer.parseInt(yyyymmdd.substring(0, 4));
		Integer Month = Integer.parseInt(yyyymmdd.substring(4, 6));
		// Integer Day = Integer.parseInt(yyyymmdd.substring(6, 8));
		boolean isrun = false;// 判断是否是闰年
		if (Year % 4 == 0) {
			isrun = true;
		}
		Integer a = 0;
		if (Month == 1 || Month == 3 || Month == 5 || Month == 7 || Month == 8 || Month == 10 || Month == 12) {
			if (i > 31) {
				a = 31;
			}
		}
		if (Month == 4 || Month == 6 || Month == 9 || Month == 11) {
			if (i > 30) {
				a = 30;
			}
		}
		if (Month == 2) {
			if (isrun) {
				if (i > 29) {
					a = 29;
				}
			} else {
				if (i > 28) {
					a = 28;
				}
			}
		}
		cl.setFirstDayOfWeek(Calendar.MONDAY);
		// 当前时间，貌似多余，其实是为了所有可能的系统一致
		cl.setTime(date);
		cl.set(Calendar.DAY_OF_MONTH, a);
		Date d = format.parse(format.format(cl.getTime()));
		cl.clone();
		return d;
	}

	/**
	 * 计算两个日期相差多少月
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static Integer computerMonth(Date start, Date end) {
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.CHINA);
		String yyyymmdd1 = format.format(start);
		String yyyymmdd2 = format.format(end);
		Integer Year1 = Integer.parseInt(yyyymmdd1.substring(0, 4));
		Integer Month1 = Integer.parseInt(yyyymmdd1.substring(4, 6));
		Integer Year2 = Integer.parseInt(yyyymmdd2.substring(0, 4));
		Integer Month2 = Integer.parseInt(yyyymmdd2.substring(4, 6));
		return (Year2 - Year1) * 12 + Month2 - Month1;
	}

	// day_id @[日,-1,yyyyMMdd]#
	// where day_id=[日,-1,yyyyMMdd]
	// fields :日编码@[$日,0,YYYYMMDD]#小时编码@[$小时,-1,HH]#
	// date=20160113000000
	public static String[] getSql(String metasql, String fields, String date, String fileName) {
		String[] resultArr = new String[2];
		String sql = metasql;
		try {
			String[] splits = fields.split("#");

			Date d = StringToDate(date, "yyyyMMddHHmmss");
			if (d == null) {
				return null;
			}
			// 日编码@[$日,0,YYYYMMDD]#小时编码@[$小时,-1,HH]#
			// 小时编码@[$小时,-1,HH]# 日编码@[$日,0,YYYYMMDD]
			if (splits.length > 1) {
				int tmpHour = Integer.parseInt(date.substring(8, 10));
				if (getSubString(fields, 0, 1).trim().indexOf("$小时") > 0) {
					if (Integer.parseInt(getSubString(fields, 1, 2).trim()) + tmpHour < 0) {
						d.setTime(d.getTime() - 1000L * 60 * 60 * 24);
					}
				} else if (getSubString(fields, 2, 3).trim().indexOf("$小时") > 0) {
					if (Integer.parseInt(getSubString(fields, 3, 4).trim()) + tmpHour < 0) {
						d.setTime(d.getTime() - 1000L * 60 * 60 * 24);
					}
				}

				if (splits[0].indexOf("$小时") > 0) {
					String tmpString = splits[1];
					splits[1] = splits[0];
					splits[0] = tmpString;

				}
			}

			// day_id @[日,-1,yyyyMMdd]
			for (int k = 0; k < splits.length; k++) {
				// System.out.println(splits[k]);
				String field = splits[k];

				String[] strs = field.split("@");// day_id @ [日,-1,yyyyMMdd]
				String son = strs[1].substring(2);// 日,-1,yyyyMMdd]
				son = son.substring(0, son.length() - 1);// 日,-1,yyyyMMdd
				// System.out.println(son);
				String[] formats = son.split(",");
				String desc = formats[0];// 日
				Integer count = Integer.parseInt(formats[1]);// -1
				String formatstr = formats[2];// yyyyMMdd
				if ("YYYYMMDD".equalsIgnoreCase(formatstr)) {
					formatstr = "yyyyMMdd";
				} else if ("YYYYMM".equalsIgnoreCase(formatstr)) {
					formatstr = "yyyyMM";
				} else if ("YYYYWW".equalsIgnoreCase(formatstr)) {
					formatstr = "yyyyww";
				} else if ("MI".equalsIgnoreCase(formatstr)) {
					formatstr = "yyyyMMddHHmm";
				}
				String value = computerdate(formatstr, desc, count, d);// 计算出值
				sql = sql.replace(strs[1], value);

				fileName += value;
			}
		} catch (Exception e) {
			String relust = "SQL语句" + sql + "字段" + fields + "参照时间" + date;
			logger.warn("存在非法字段" + relust + "正确格式请参照  day_id=[日,-1,yyyyMMdd]", e);
			return null;
		}

		resultArr[0] = sql;
		resultArr[1] = fileName;

		return resultArr;
	}

	public static String computerdate(String formatstr, String desc, Integer count, Date date) {
		Calendar c = Calendar.getInstance();
		c.setTime(date); // 设置当前日期
		if (desc.equals("秒")) {
			c.add(Calendar.SECOND, count); // 日期分钟加1,Calendar.DATE(天),Calendar.HOUR(小时)
		}
		if (desc.contains("分")) {
			c.add(Calendar.MINUTE, count); // 日期分钟加1,Calendar.DATE(天),Calendar.HOUR(小时)
		}
		if (desc.equals("小时")) {
			c.add(Calendar.HOUR, count); // 日期分钟加1,Calendar.DATE(天),Calendar.HOUR(小时)
		}
		if (desc.equals("日")) {
			c.add(Calendar.DATE, count); // 日期分钟加1,Calendar.DATE(天),Calendar.HOUR(小时)
		}
		if (desc.equals("周")) {
			c.add(Calendar.DATE, 7 * count); // 日期分钟加1,Calendar.DATE(天),Calendar.HOUR(小时)
		}
		if (desc.equals("月")) {
			c.add(Calendar.MONTH, count); // 日期分钟加1,Calendar.DATE(天),Calendar.HOUR(小时)
		}
		SimpleDateFormat format = new SimpleDateFormat(formatstr, Locale.CHINA);

		Date d = c.getTime();
		c.clone();
		return format.format(d);
	}

	public static int taskCount(String taskId) {
		Connection con = getConnection();
		Statement st = null;
		ResultSet rs = null;
		int count = 0;
		try {
			String sql = "select count(*) from sys_sharedata_task where task_id='" + taskId + "'";
			st = con.createStatement();
			rs = st.executeQuery(sql);
			while (rs.next()) {
				count = Integer.parseInt(rs.getString(1));
			}
			con.commit();
		} catch (Exception e) {
			logger.warn("查询task记录数失败", e);
		} finally {
			release(rs, st, con);
		}
		return count;
	}

	public static List<String> taskIdList() {
		Connection con = getConnection();
		Statement st = null;
		ResultSet rs = null;
		List<String> list = new ArrayList<String>();
		try {
			String sql = "select task_id from sys_sharedata_task";
			st = con.createStatement();
			rs = st.executeQuery(sql);
			while (rs.next()) {
				list.add(rs.getString(1));
			}
			// con.commit();
		} catch (Exception e) {
			logger.warn("查询taskid列表失败", e);
		} finally {
			release(rs, st, con);
		}
		return list;
	}

	public static void prestatesql(String sql, String value) {
		Connection con = getConnection();
		PreparedStatement ps;
		try {
			ps = con.prepareStatement(sql);
			ps.setBlob(1, new ByteArrayInputStream(value.getBytes(DbEncouding)));
			ps.execute();
			con.commit();
		} catch (SQLException e) {
			logger.warn("在插入Task表时候 SQL语句出错   sql语句为" + sql + "参数值" + value + "异常信息为", e);
		} catch (UnsupportedEncodingException e) {
			logger.warn("在插入Task表时候 不支持 指定的编码格式", e);
		} finally {
			release(null, null, con);
		}
	}

	public static String getSubString(String srcString, int begin, int end) {
		int fromIndex = 0;
		int toIndex = 0;
		for (int k = 0; k < begin; k++) {
			fromIndex = srcString.indexOf(",", fromIndex) + 1;
		}

		for (int k = 0; k < end; k++) {
			toIndex = srcString.indexOf(",", toIndex) + 1;
		}
		toIndex = toIndex - 1;

		return srcString.substring(fromIndex, toIndex);
	}

	public static void main(String[] args) {
		String excutetime = "00:00:30";
		String excutedate = "";
		String cycledesc = "分钟";
		int cyclecount = 15;
		String actstart = "";
		String actend = "";
		Date starttime = StringToDate("2016-6-23", "yyyy-MM-dd");
		String strsql = "SELECT FP0 , FP1 , FP2 , FP3 , C_FP0 , C_FP1 , C_FP2 , C_FP3 , EQP_INT_ID , EQP_LABEL , EQP_ALIAS , NE_IP , ALARM_SOURCE , EQP_OBJECT_CLASS , VENDOR_ID , EQP_VERSION , NE_LABEL , OBJECT_CLASS , NE_STATUS , ALARM_NE_STATUS , LOCATE_INFO , EVENT_TIME , CANCEL_TIME , TIME_STAMP , VENDOR_TYPE , VENDOR_SEVERITY , ORG_SEVERITY , PROBABLE_CAUSE , STANDARD_ALARM_ID , ACTIVE_STATUS , ACK_FLAG , ACK_TIME , ACK_USER , ALARM_TITLE_TEXT , STANDARD_ALARM_NAME , PROBABLE_CAUSE_TXT , ALARM_TEXT , PROFESSIONAL_TYPE , LOGIC_ALARM_TYPE , LOGIC_SUB_ALARM_TYPE , EFFECT_NE , EFFECT_SERVICE , ORG_TYPE , SEND_JT_FLAG , ALARM_ORIGIN , PROVINCE_NAME , REGION_NAME , CITY_NAME , OFFICE_SITE , ALARM_ACT_COUNT , CORRELATE_ALARM_FLAG , BUSINESS_SYSTEM , GCSS_CLIENT_NAME , GCSS_CLIENT_LEVEL , GCSS_SERVICE_LEVEL , SHEET_SEND_STATUS , SHEET_STATUS , SHEET_NO , ALARM_REMARK , SUB_ALARM_TYPE , NE_SUB_TYPE FROM OP_RE_ST_ALARM_LIST WHERE SUBSTR(TO_CHAR(EVENT_TIME,'YYYYMMDDHHMMSS'),0,12)>=[$分钟,-15,MI] AND SUBSTR(TO_CHAR(EVENT_TIME,'YYYYMMDDHHMMSS'),0,12)<[$分钟,0,MI]";

		try {
			List<String> strs = Util.computertime(excutetime, excutedate, cycledesc, cyclecount, actstart, actend,
					starttime);

			for (String str : strs) {
				System.out.println(str);
				String fields = "分钟@[$分钟,0,MI]#分钟@[$分钟,-15,MI]";
				String[] resultArr = Util.getSql(strsql, fields, str, "111");
				System.out.println(resultArr[0]);
				System.out.println(resultArr[1]);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}

	}
}
