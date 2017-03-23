/*
	Copyright 2014-2017 Mario Pascucci <mpascucci@gmail.com>
	This file is part of LDrawLib

	LDrawLib is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	LDrawLib is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with LDrawLib.  If not, see <http://www.gnu.org/licenses/>.

*/


package bricksnspace.ldrawlib;




import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;

import bricksnspace.dbconnector.DBConnector;



/**
 * LDraw parts database and search functions
 * 
 * @author Mario Pascucci
 *
 */
public class LDrawLibDB {

	
	/*
	 * done: select only from "enabled" library parts
	 * done: functions to enable/disable a single library
	 * TODO: functions to change official flag
	 * done: search results ordered on priority
	 */
	
	
	// database vars
	protected static DBConnector db;
	//Connection conn;
	boolean inited = false;
	boolean needUpgrade = false;
	private PreparedStatement insertPS;
	private boolean includeObsolete = false;
	private boolean includeColored = false;
	private PreparedStatement updatedPartsPS;
	private final static String fieldsOrder = "ldrid,partname,author,description,category,keywords,obsolete,colored,lastupdate,ldcategory,official,priority,enabled";
	private final static String table = "ldrawlibdb";
	private final static String FTSfields = "LDRID,PARTNAME,DESCRIPTION,CATEGORY,KEYWORDS,ldcategory";
	private final static String DBVERCONSTANT = "LDRAWDBVER";
	private final static int DBVERSION = 1;

	

	public LDrawLibDB(DBConnector dbc) throws SQLException {
		
		if (dbc == null)
			throw new IllegalArgumentException("[LDrawDB] undefined DBConnector");
		db = dbc;
		// checks for new or already populated database
		if (!db.checkTable(table)) {
			// is a new database
			createNewTables();
			db.setDbVersion(DBVERCONSTANT, DBVERSION);
		}
		else { 
			// checks for database upgrade
			if (db.needsUpgrade(DBVERCONSTANT, DBVERSION)) {
				switch (db.getDbVersion(DBVERCONSTANT)) {
				case -1:
					upgradeFromMinus1();
					break;
				}
			}
		}
	    	    
	    inited = initDB();
	}


	/**
	 * Contain all operation and queries to upgrade database tables and schema 
	 * to current version
	 * @throws SQLException
	 */
	private static void upgradeFromMinus1() throws SQLException {
		
		Statement st;
		
		st = db.createStatement();
		st.execute("DROP TABLE IF EXISTS ldrparts");
		db.setDbVersion(DBVERCONSTANT, DBVERSION);
	}
	
	
	
	
	private boolean initDB() throws SQLException {
		
		Statement st;
		
		st = db.createStatement();
		
		try {
			ResultSet rs = st.executeQuery("SELECT * FROM FTL_SEARCH_DATA('brick',0,0) " +
					"WHERE table='"+table.toUpperCase()+"' LIMIT 1");
			if (!rs.next()) {
				db.deleteFTS(table);
				db.createFTS(table, FTSfields);
			}
		} catch (SQLException ex) {
			// there is a problem with FT indexes, so rebuild all
			Logger.getGlobal().log(Level.WARNING,"[initDB] Try to rebuild full text index.", ex);
			db.deleteFTS(table);
			db.createFTS(table, FTSfields);
		}
		updatedPartsPS = db.prepareStatement("SELECT id," + fieldsOrder +
				" FROM "+table+" WHERE lastupdate >= ? AND NOT colored AND NOT obsolete AND enabled ORDER BY priority ASC");
		return true;
	}
	
	
	



	public boolean isInited() {
		return inited;
	}

	
	

	
	/**
	 * Create table in new database. Also create indexes on ldrid and ldcategory
	 * 
	 * @throws SQLException
	 */
	public void createNewTables() throws SQLException {

		Statement st = db.createStatement();
		
		st.execute("CREATE TABLE IF NOT EXISTS "+table+" (" +
				"id INT PRIMARY KEY AUTO_INCREMENT, " +
				"ldrid VARCHAR(64)," +
				"partname VARCHAR(64)," +
				"author VARCHAR(64)," +
				"description VARCHAR(255)," +
				"category VARCHAR(255)," +
				"keywords VARCHAR(1023)," +
				"obsolete BOOL," +
				"colored BOOL," +
				"lastupdate TIMESTAMP," +
				"ldcategory VARCHAR(64)," +
				"official BOOL," + 
				"priority INT," +
				"enabled BOOL" +
				"); COMMIT ");
		// creates indexes to speedup search
		st.executeUpdate("CREATE INDEX IF NOT EXISTS ldr_ldrid ON "+table+"(ldrid)");
		st.executeUpdate("CREATE INDEX IF NOT EXISTS ldr_ldcat ON "+table+"(ldcategory)");
	}

	
	
	/**
	 * Prepare for update. Uses transaction to handle update failures
	 * @throws SQLException
	 */
	public void prepareUpdate(int index) throws SQLException {
		
		Statement st;

		db.deleteFTS(table);
		insertPS = db.prepareStatement("INSERT INTO " + table +
				" ("+fieldsOrder+") VALUES " +
				"(?,?,?,?,?,?,?,?,?,?,?,?,?)" +
				";");
		db.autocommitDisable();
		st = db.createStatement();
		st.executeUpdate("DROP INDEX IF EXISTS ldr_ldrid");
		st.executeUpdate("DROP INDEX IF EXISTS ldr_ldcat");
		removeLib(index);
	}
	
	
	
	/**
	 * Successfully ends an update
	 * @throws SQLException
	 */
	public void endUpdate() throws SQLException {
		
		Statement st = db.createStatement();

		st.executeUpdate("CREATE INDEX IF NOT EXISTS ldr_ldrid ON "+table+"(ldrid)");
		st.executeUpdate("CREATE INDEX IF NOT EXISTS ldr_ldcat ON "+table+"(ldcategory)");
		db.commit();
		db.autocommitEnable();
		db.createFTS(table, FTSfields);
		insertPS.close();
	}
	
	
	
	/**
	 * Rollback on failed update
	 * @throws SQLException
	 */
	public void abortUpdate() throws SQLException {
		
		db.rollback();
		// FIXME: verify that indexes are "rolled back" by aborted transaction
		db.autocommitEnable();
		insertPS.close();
		db.createFTS(table, FTSfields);
	}
	
	
	
	/**
	 * Refresh all indexes
	 * @throws SQLException
	 */
	public void refreshIndexes() throws SQLException {
		
		db.deleteFTS(table);
		db.createFTS(table, FTSfields);
		Statement st = db.createStatement();
		st.executeUpdate("DROP INDEX IF EXISTS ldr_ldrid");
		st.executeUpdate("DROP INDEX IF EXISTS ldr_ldcat");
		st.executeUpdate("CREATE INDEX IF NOT EXISTS ldr_ldrid ON "+table+"(ldrid)");
		st.executeUpdate("CREATE INDEX IF NOT EXISTS ldr_ldcat ON "+table+"(ldcategory)");		
	}
	
	
	
	
	/**
	 * Remove all parts from table
	 * @throws SQLException
	 */
	public void clearParts() throws SQLException {
		
		Statement st = db.createStatement();
		// delete all rows
		st.execute("DELETE FROM "+table);
	}
	
	
	
	/**
	 * Remove all parts on a library selected by priority index 
	 * @param priority index of library to remove
	 * @throws SQLException
	 */
	protected void removeLib(int priority) throws SQLException {
		
		Statement st = db.createStatement();
		// delete all rows 
		st.execute("DELETE FROM "+table+" WHERE priority="+priority);
	}
	
	
	
	protected void moveLib(int priorityFrom, int priorityTo) throws SQLException {
		
		if (priorityFrom == 0 || priorityTo == 0 || priorityFrom == priorityTo)
			throw new IllegalArgumentException("[LDrawLibDB] Invalid priority value in [moveLib]\n"
					+ "From: "+priorityFrom+" To: "+priorityTo);
		Statement st = db.createStatement();
		try {
			st.execute("UPDATE "+table+" SET priority="+priorityTo+" WHERE priority="+priorityFrom);
		}
		catch (SQLException sqlex) {
			throw new SQLException("[LDrawLibDB] Unable to change priority\n"
					+ "From: "+priorityFrom+" To: "+priorityTo, sqlex);
		}

	}
	
	
	
	/**
	 * Enable a library selected by priority index
	 * @param priority index to enable
	 * @throws SQLException
	 */
	protected void enableLib(int priority) throws SQLException {
		
		Statement st = db.createStatement();
		// delete all rows 
		st.execute("UPDATE "+table+" SET enabled=true WHERE priority="+priority);		
	}
	
	
	/**
	 * Disable a library selected by priority index
	 * @param priority index to disable 
	 * @throws SQLException
	 */
	protected void disableLib(int priority) throws SQLException {
		
		Statement st = db.createStatement();
		// delete all rows 
		st.execute("UPDATE "+table+" SET enabled=false WHERE priority="+priority);		
	}
	
	
	
	protected void changePriority(int priorityFrom, int priorityTo) throws SQLException {
		
		if (priorityFrom == 0 || priorityTo == 0 || priorityFrom == priorityTo)
			throw new IllegalArgumentException("[LDrawLibDB] Invalid priority value in [changePriority]\n"
					+ "From: "+priorityFrom+" To: "+priorityTo);
		db.autocommitDisable();
		Statement st = db.createStatement();
		try {
			// set priority to a dummy value
			st.execute("UPDATE "+table+" SET priority=-2 WHERE priority="+priorityFrom);
			st.execute("UPDATE "+table+" SET priority="+priorityFrom+" WHERE priority="+priorityTo);
			st.execute("UPDATE "+table+" SET priority="+priorityTo+" WHERE priority=-2");
		}
		catch (SQLException sqlex) {
			db.rollback();
			db.autocommitEnable();
			throw new SQLException("[LDrawLibDB] Unable to change priority\n"
					+ "From: "+priorityFrom+" To: "+priorityTo, sqlex);
		}
		db.commit();
		db.autocommitEnable();
	}
	
	
	
	/**
	 * Change official flag on a library selected by priority index
	 * @param priority index to change
	 * @param official flag
	 * @throws SQLException
	 */
	protected void setOfficial(int priority, boolean official) throws SQLException {
		
		Statement st = db.createStatement();
		// delete all rows 
		st.execute("UPDATE "+table+" SET official="+official+" WHERE priority="+priority);		
	}
	
	
	
	
	/**
	 * Checks if part is in database
	 * @param name part id (with .dat extension)
	 * @return true if part is in database
	 * @throws SQLException
	 * @throws IllegalStateException if a duplicate part is found in database
	 */
	public boolean partInDB(String name) throws SQLException {
		
		Statement st;
		ResultSet rs;
		
		st = db.createStatement();
		rs = st.executeQuery("SELECT count(*) FROM "+table+" WHERE ldrid='"+name+"' AND enabled");
		rs.next();
		int num = rs.getInt(1);
		if (num > 1)
			throw new IllegalStateException("[LDrawDB] duplicate part in database: '"+name+"'" );
		return num == 1;
	}
	
	

	public boolean partIsOfficial(String name) throws SQLException {
		
		Statement st;
		ResultSet rs;
		
		st = db.createStatement();
		rs = st.executeQuery("SELECT official FROM "+table+" WHERE ldrid='"+name+"' AND enabled");
		if (rs.next())
			return rs.getBoolean(1);
		return false;
	}
	
	

	public void insert(LDrawPart p, int priority, boolean isOfficial, boolean isEnabled) throws SQLException {
		
		// ldrid,partname,author,description,category,keywords,obsolete,colored,lastupdate,ldcategory,official,priority,enabled
		insertPS.setString(1, p.getLdrawId());
		insertPS.setString(2, p.getPartName());
		insertPS.setString(3, p.getAuthor());
		boolean obs = false;
		if (p.getDescription().startsWith("_")) {
			insertPS.setString(4, "(Colored) "+p.getDescription().substring(1));
		}
		else if (p.getDescription().startsWith("~")) {
			if (p.getDescription().toLowerCase().startsWith("~moved to") || 
					p.getDescription().toLowerCase().indexOf("obsolete") >= 0) {
				insertPS.setString(4, "(Obsolete) "+p.getDescription().substring(1));
				obs = true;
			}
			else {
				insertPS.setString(4, "(Subpart) "+p.getDescription().substring(1));
			}
		}
		else if (p.getDescription().startsWith("=")) {
			insertPS.setString(4, "(Alias) "+p.getDescription().substring(1));
		}
		else {
			insertPS.setString(4, p.getDescription());
		}
		insertPS.setBoolean(7, obs);
		insertPS.setString(5, p.getCategory());
		insertPS.setString(6, p.getKeywords());
		insertPS.setBoolean(8, p.getDescription().startsWith("_"));
		if (p.getLastUpdate() != null)
			insertPS.setDate(9, new Date(p.getLastUpdate().getTime()));
		else
			insertPS.setDate(9, new Date(Calendar.getInstance().getTime().getTime()));
		insertPS.setString(10, LDrawPartCategory.getCategory(p).name());
		insertPS.setBoolean(11, isOfficial);
		insertPS.setInt(12, priority);
		insertPS.setBoolean(13, isEnabled);
		insertPS.executeUpdate();
	}

	
	
	
	
	public ArrayList<LDrawPart> get(String filterExpr, int limit) throws SQLException {

		ArrayList<LDrawPart> dat = new ArrayList<LDrawPart>();
		LDrawPart l;
		Statement st;
		ResultSet rs;
		
		if (filterExpr == null) {
			st = db.createStatement();
			rs = st.executeQuery("SELECT id,"+fieldsOrder+" FROM "+table+" WHERE enabled ORDER BY priority ASC");
		}
		else {
			st = db.createStatement();
			rs = st.executeQuery("SELECT id," + fieldsOrder +
				" FROM "+table+" WHERE " + filterExpr +
				(includeColored ? "" : " AND NOT colored") +
				(includeObsolete ? "" : " AND NOT obsolete") +
				" AND enabled" +
				" ORDER BY priority ASC" +
				(limit > 0 ? " LIMIT " + limit:"")
				);
		}
		while (rs.next()) {
			// fetch and assign rows to an Array list
			l = LDrawPart.getPartMeta(
					rs.getString("ldrid"),
					rs.getString("partname"),
					rs.getString("author"),
					rs.getString("description"),
					rs.getString("category"),
					rs.getString("keywords"),
					rs.getBoolean("official")
					);
			dat.add(l);
		}
		//System.out.println(dat.size()); // DB
		return dat;
	}

	
	
//	public LDrawPart getPart(String ldrid) throws SQLException {
//		
//		ArrayList<LDrawPart> ldp = get("ldrid='"+ldrid+"'",3);
//		if (ldp.size() == 1) {
//			return ldp.get(0);
//		}
//		else if (ldp.size() == 0) {
//			return null;
//		}
//		else 
//			throw new IllegalStateException("[LDrawDB] duplicate part in database: '"+ldrid+"'");
//	}
	
	
	
	public void excludeObsolete() {
		
		includeObsolete = false;
	}
	
	
	
	public void excludeColored() {
		
		includeColored = false;
	}
	

	public void includeObsolete() {
		
		includeObsolete = true;
	}
	
	
	
	public void includeColored() {
		
		includeColored = true;
	}
	
	
	public ArrayList<LDrawPart> getUpdatedAfter(java.util.Date last) throws SQLException {
		
		ResultSet rs;
		LDrawPart l;
		ArrayList<LDrawPart> dat = new ArrayList<LDrawPart>();
		
		updatedPartsPS.setDate(1, new Date(last.getTime()));
		rs = updatedPartsPS.executeQuery();
		while (rs.next()) {
			// fetch and assign rows to an Array list
			l = LDrawPart.getPartMeta(
					rs.getString("ldrid"),
					rs.getString("partname"),
					rs.getString("author"),
					rs.getString("description"),
					rs.getString("category"),
					rs.getString("keywords"),
					rs.getBoolean("official")
					);
			dat.add(l);
		}
		return dat;
	}
	
	
	public ArrayList<LDrawPart> getByCategory(LDrawPartCategory ct) throws SQLException {
		
		return get("ldcategory = '"+ct.name()+"' ",1000);
	}
	

	public ArrayList<LDrawPart> getFTS(String filterExpr, int limit) throws SQLException {

		ArrayList<LDrawPart> dat = new ArrayList<LDrawPart>();
		LDrawPart l;
		Statement st;
		ResultSet rs;
		
		if (filterExpr != null) {
			st = db.createStatement();
			rs = st.executeQuery("SELECT id,"+fieldsOrder+" FROM FTL_SEARCH_DATA('"+filterExpr+"',0,0) f " +
					"LEFT JOIN "+table+" l on (f.keys[0]=l.id) WHERE f.table='"+table.toUpperCase()+"'" +
					(includeColored ? "" : " AND NOT colored")+
					(includeObsolete ? "" : " AND NOT obsolete")+
					" AND enabled" +
					" ORDER BY priority ASC" +
					(limit > 0 ? " LIMIT " + limit:"")
					);
			while (rs.next()) {
				// fetch and assign rows to an Array list
				l = LDrawPart.getPartMeta(
						rs.getString("ldrid"),
						rs.getString("partname"),
						rs.getString("author"),
						rs.getString("description"),
						rs.getString("category"),
						rs.getString("keywords"),
						rs.getBoolean("official")
						);
				dat.add(l);
			}
		}
		return dat;
	}
	
	

	/**
	 * return single part metadata, if exist
	 * @param ldrid part LDraw id, with ".dat"
	 * @return part metadata or null if no part was found
	 * @throws SQLException
	 */
	public static ArrayList<LDrawPart> getPart(String ldrid) throws SQLException {


		Statement st;
		ResultSet rs;
		ArrayList<LDrawPart> dat = new ArrayList<LDrawPart>();
		
		if (db == null) 
			throw new IllegalStateException("[getPart] DBConnector not initialized");
		st = db.createStatement();
		rs = st.executeQuery("SELECT id," + fieldsOrder +
			" FROM "+table+" WHERE ldrid='" + ldrid + "' AND enabled ORDER BY priority ASC");
		while (rs.next()) {
			// fetch and assign rows to an Array list
			dat.add(LDrawPart.getPartMeta(
					rs.getString("ldrid"),
					rs.getString("partname"),
					rs.getString("author"),
					rs.getString("description"),
					rs.getString("category"),
					rs.getString("keywords"),
					rs.getBoolean("official")
					));
		}
		return dat;
	}

	

	
	
	
	public int getPartCount() {

		Statement st;
		ResultSet rs;
		
		try {
			st = db.createStatement();
			rs = st.executeQuery("SELECT COUNT(*) FROM " + table +" WHERE enabled");
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			return -1;
		}
	}

	
	
	public ArrayList<LDrawPart> getNew() throws SQLException {
		
		return get(" lastupdate > TIMESTAMPADD(MINUTE,-15,SELECT MAX(lastupdate) from "+table+")",0);
	}

	
	

	
}
