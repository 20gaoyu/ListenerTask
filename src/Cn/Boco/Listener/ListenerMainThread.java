package Cn.Boco.Listener;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import Cn.Boco.Until.Util;

public class ListenerMainThread {
	public static Logger logger = Logger.getLogger(ListenerMainThread.class);

	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		PropertyConfigurator.configure("conf/log4j.properties");
		Connection con = Util.getConnection();
		while (con == null) {
			try {
				Thread.currentThread().sleep(60000l);
				con = Util.getConnection();
			} catch (InterruptedException e) {
				logger.warn("�̱߳�������");
			}
		}
		String NowDate = Util.DateToString(new Date());

		String sql = "select a.priority_id,a.effect_time,a.effect_start_time,a.file_name,a.effect_end_time,a.order_id,a.execute_time,a.job_cycle_desc,a.cycle_count,a.excute_date,a.content_sql,case when a.database_type='hive' then nvl(e.conn_jdbc_str_hive,e.conn_jdbc_str) else e.conn_jdbc_str end conn_jdbc_str from sys_sharedata_order_new a,sys_sharedata_ftp d,sys_sharedata_conn e where a.conn_key=e.conn_key  and a.ftp_id = d.ftp_id and (a.calltype_id = 3 or a.calltype_id=4) and a.is_pass = 1 and a.isdel=0 and a.finish_time >= to_date('"
				+ NowDate + "','YYYYMMDDHH24MISS')";

		try {
			List<String> taskList = Util.taskIdList();
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(sql);
			while (rs.next()) {
				try {
					String orderid = rs.getString("order_id");
					// ����ִ��ʱ�䣺����3�㣬�Ͳ�������һ������������
					String excutetime = rs.getString("execute_time");
					// ����������/��/��/Сʱ/����
					String cycledesc = rs.getString("job_cycle_desc");
					// ��ǰ�ӳٶ��ٸ����ڵ�λ
					Integer cyclecount = rs.getInt("cycle_count");
					// �º��ܵĴ�ڼ���ִ�У�����������ֶ�Ϊ��
					String excutedate = rs.getString("excute_date");
					Blob bl = rs.getBlob("content_sql");
					String strsql = Util.BlobToString(bl);
					String metainfo = rs.getString("conn_jdbc_str");
					Integer priority = rs.getInt("priority_id");
					// ��������Чʱ�䣬��Ч�ڣ�������ִ��ʱ��Ϊ׼
					Date starttime = rs.getDate("effect_time");
					String actstart = rs.getString("effect_start_time");
					String actend = rs.getString("effect_end_time");

					String oralce_fileName = rs.getString("file_name");
					String create_fileName = "";

					String mysql = "select column_field,param_value from sys_sharedata_order_column where order_id='"
							+ orderid + "' and param_value<> '-1' and  param_value<> '0'";
					List<String> dates = new ArrayList<String>();
					try {
						dates = Util.computertime(excutetime, excutedate, cycledesc, cyclecount, actstart, actend,
								starttime);

					} catch (ParseException e) {
						String errorsql = "insert into sys_sharedata_task(task_id,state,result) values(1,'����',?)";
						logger.error(
								"ԴSQL ��� ��select a.priority_id,a.effect_time,a.effect_start_time,a.effect_end_time,a.order_id,a.execute_time,a.job_cycle_desc,a.cycle_count,a.excute_date,a.content_sql,e.conn_jdbc_str from sys_sharedata_order_new a,sys_sharedata_ftp d,sys_sharedata_conn e where a.conn_key=e.conn_key  and a.ftp_id = d.ftp_id and a.calltype_id = 3 and a.is_pass = 1 and a.finish_time >= to_date('"
										+ NowDate + "','YYYYMMDDHH24MISS')   �ڽ�������SQL������  ���������ֶ��Ƿ񲻺Ϸ�  executime"
										+ excutetime + "executedate" + excutedate + "ѭ����λ" + cycledesc + "ѭ�����"
										+ cyclecount + "�ʱ�� " + actstart + "�����ʱ��" + actend + "�����쳣��Ϣ",
								e);
						Util.prestatesql(errorsql,
								"SQL:   select a.priority_id,a.effect_time,a.effect_start_time,a.effect_end_time,a.order_id,a.execute_time,a.job_cycle_desc,a.cycle_count,a.excute_date,a.content_sql,e.conn_jdbc_str from sys_sharedata_order_new a,sys_sharedata_ftp d,sys_sharedata_conn e where a.conn_key=e.conn_key  and a.ftp_id = d.ftp_id and a.calltype_id = 3 and a.is_pass = 1  and a.finish_time >= to_date('"
										+ NowDate
										+ "','YYYYMMDDHH24MISS') In the generated SQL statement error, please check the following fields are invalid  executime:"
										+ excutetime + "  executedate" + excutedate + "   CycleUnit:" + cycledesc
										+ "  CycleInterval" + cyclecount + "   actiontimestart " + actstart
										+ "   actiontimeend" + actend + " exception:" + e.toString());
						continue;
					}
					StringBuilder sb = new StringBuilder();
					Connection mycon = Util.getConnection();
					int c = 0;
					while (mycon == null) {
						try {
							Thread.currentThread().sleep(60000l);
							mycon = Util.getConnection();
							c++;
							System.out.println("retry conn :" + c);
						} catch (InterruptedException e) {
							logger.warn("�̱߳�ǿ�ƽ����ն�");
						}
					}
					try {
						Statement myst = mycon.createStatement();
						ResultSet myrs = myst.executeQuery(mysql);
						while (myrs.next()) {
							// ���� [$��,-1,yyyyMMdd]��[$��,-2,yyyyMMdd
							String wrongStr = myrs.getString("param_value");
							if (wrongStr.indexOf("��") > 0) {
								wrongStr = wrongStr.substring(0, wrongStr.indexOf("��"));
							}
							sb.append(myrs.getString("column_field")).append("@");
							sb.append(wrongStr).append("#");
						}
						Util.release(myrs, myst, mycon);
					} catch (SQLException e) {
						Util.release(null, null, mycon);
						logger.warn("SQL�쳣 SQL���Ϊ" + mysql + "�쳣Ϊ" + e.toString());
						continue;
					}
					String fields = "";
					if (sb.toString().length() > 1) {
						fields = sb.substring(0, sb.length() - 1);
					}
					if (dates != null && dates.size() > 0) { // �õ��˿�ʼִ��ʱ��
						for (String date : dates) {// �Ϳ�ʼ���뵽���ݿ�
							if (strsql == null) {
								String _errsql = "insert into sys_sharedata_task(result,state,task_id) values(?,'����',1)";
								String _errinfo = "ԴSQLȱʧ  ����ԴSQL��BLOB�д�����JAVA�޷���������ΪString����";
								logger.warn(_errinfo + "SQL���" + sql + "  ��ǰ��ORDERID" + orderid);
								Util.prestatesql(_errsql,
										"Source SQL BLOB are missing or source of SQL errors, resulting in JAVA can not properly resolve the String type BLOB     SQL:"
												+ sql + "  ORDERID:" + orderid);
								break;
							}
							String sonsql = "";
							if ("".equals(fields)) {
								sonsql = strsql;
							} else {
								// �滻sql��ִ��ʱ��
								String[] resultArr = Util.getSql(strsql, fields, date, oralce_fileName);
								if (resultArr != null) {
									sonsql = resultArr[0];
									create_fileName = resultArr[1];
								}
							}
							String help = date.substring(4, 14) + orderid.replaceAll("-", "");
							String _taskid = help;
							if (sonsql != null) {
								String insertsql = "insert into sys_sharedata_task(order_id,start_time,content_sql,conection_str,state,load_time,priority,task_id,file_name) values('"
										+ orderid + "',to_date('" + date + "','yyyyMMddHH24MIss'),?,'" + metainfo
										+ "','δִ��',sysdate,'" + priority + "','" + _taskid + "','" + create_fileName
										+ "')";
								// logger.info("sql=" + sonsql);
								// int taskIdCount = Util.taskCount(_taskid);
								if (taskList.contains(_taskid)) {
									logger.info("����task_id=" + _taskid + "�Ѿ����ڣ�");
								} else {
									logger.info("��������task_id=" + _taskid);
									Util.prestatesql(insertsql, sonsql);

								}
							} else {
								String lsql = "insert into sys_sharedata_task(task_id,state,result) values(1,'����',?)";
								Util.prestatesql(lsql, "The task can not be inserted correctly    SQL:" + strsql
										+ "fields " + fields + "  Time:" + date + "    DATEFORMAT:YYYYMMDDHH24MISS");
							}
						}
					}
				} catch (NumberFormatException e) {
					logger.warn("Sql:" + sql + " �м��й����NUBER�������� ������ ��������Խ�� Exception", e);
					String errorsql = "insert into sys_sharedata_task(result,state,task_id) values(?,'����',1)";
					Util.prestatesql(errorsql, "Sql:  " + sql
							+ "Number type is too big  or ArrayIndexOutOfBoundsException  Exception" + e);
					continue;
				}
			}
			logger.info("���������������");
		} catch (SQLException e) {
			logger.error(" ��������SQL����Ƿ��д�  " + sql, e);
		}
		Util.release(null, null, con);
	}
}
