
/*
	Copyright 2013-2016 Mario Pascucci <mpascucci@gmail.com>
	This file is part of LDrawLib.

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



import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import bricksnspace.dbconnector.DBConnector;


/**
 * A library to read LDraw primitives, parts and other files from folder or zipfiles.
 * 
 * Requires only Official part library to work. If supplied, uses unofficial library and user library, too.
 * First library in list is an official library and is enabled/used as default library.
 * To disable (momentary or permanently) use of unofficial parts, use setOfficialOnly(true).
 * Use a database to do full text search and search on metadata. 
 * Database isn't necessary to use library in programs, only if you need a complex part search.
 * 
 */

/*
 * 
 */



public class LDrawLib { 

	private List<LDLibrary> ldLibs;
	public static final int OFFICIALINDEX = 0;
	
	private boolean officialOnly = false;
	private boolean useDatabase = false;
	private LDrawLibDB ldrDB = null;
	public static final String LDRAWENV = "LDRAWBASEDIR"; 

	
	
	/**
	 * Creates a list of LDraw libraries, ordered by priority.
	 * Requires one and only one LDraw library, and library MUST be an official library.
	 * Does not update database.
	 * 
	 * @param official path to main official library (can be a folder or a zipfile)
	 * @param dbc database connector, if null no database is used
	 * @throws ZipException 
	 * @throws IOException if cannot read library content
	 * @throws SQLException 
	 */
	public LDrawLib(String official,DBConnector dbc) throws ZipException, IOException, SQLException {
		
		ldLibs = new ArrayList<LDLibrary>();
		if (dbc != null) {
			// use database for part search
			ldrDB = new LDrawLibDB(dbc);
			useDatabase = true;
		}
		LDLibrary l = new LDLibrary(official, true);
		if (!l.isLDrawStd())
			throw new IOException("[LDrawLib] Library isn't an official library.\nPath:"+official);
		ldLibs.add(l);
		LDrawColor.readFromLibrary(this);
		LDrawPart.setLdrlib(this);
	}
	
	
	/**
	 * Replace official library<br/>
	 * Does not update database.
	 * @param lib path to new library
	 * @return true if replace was successful, false if library isn't official
	 * @throws ZipException
	 * @throws IOException
	 */
	protected boolean replaceOfficial(String lib) throws ZipException, IOException {
		
		LDLibrary l = new LDLibrary(lib, true);
		if (!l.isLDrawStd())
			return false;
		ldLibs.set(OFFICIALINDEX, l);
		LDrawColor.readFromLibrary(this);
		return true;
	}
	
	
	
	
	
	
	/**
	 * @return the ldrDB or null if database isn't used
	 */
	public LDrawLibDB getLdrDB() {
		return ldrDB;
	}



	/**
	 * @return true if part database is enabled
	 */
	public boolean isDatabaseEnabled() {
		return useDatabase;
	}



	/**
	 * Disable part database in library
	 */
	public void disableDatabase() {
		this.useDatabase = false;
	}



	/**
	 * Enable part database in library. 
	 */
	public void enableDatabase() {
		if (ldrDB == null)
			throw new IllegalStateException("[LDrawLib] Database not inited");
		this.useDatabase = true;
	}

	
	
	
//	public int addStdLDrawLib() throws ZipException, IOException {
//		
//		if (!isAlreadyInstalled()) {
//			throw new IllegalStateException("[addStdLDrawLib] No standard LDraw library in system environment.");
//		}
//		LDLibrary l = new LDLibrary(System.getenv(LDRAWENV), true);
//		if (l.isLDrawStd()) {
//			ldLibs.add(l);
//			return ldLibs.indexOf(l);
//		}
//		Logger.getGlobal().log(Level.SEVERE,"[addStdLDrawLib] Environment variable point to an invalid library.");
//		throw new IOException("[addStdLDrawLib] Environment variable point to an invalid library.");
//	}
	
	

	
	
	
	/**
	 * Appends an unofficial library to list, defining if it's enabled
	 * Does not update database
	 * 
	 * @param lib path to library (folder or zipfile)
	 * @param enabled true if library is enabled
	 * @throws ZipException
	 * @throws IOException if cannot read library content
	 */
	public int addLDLib(String lib, boolean enabled) throws ZipException, IOException {
		
		LDLibrary l = new LDLibrary(lib, false);
		if (!enabled) 
			l.disable();
		ldLibs.add(l);
		return ldLibs.indexOf(l);
//		officialIndex = getOfficialIndex();
	}

	
	
	
	
	/**
	 * Removes a library from list
	 * 
	 * @param index position in list of library to remove 
	 */
	public void removeLDLib(int index) {
		
		if (index == OFFICIALINDEX) {
			Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Attempt to remove default library");
			return;
		}
		if (useDatabase) {
			try {
				ldrDB.prepareUpdate(index);
				for (int i=index+1;i<ldLibs.size();i++) {
					ldrDB.moveLib(i, i-1);
				}
				ldrDB.endUpdate();
				ldLibs.remove(index);
			}
			catch (SQLException sqlex) {
				Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Error in removing library index:"+index, sqlex);
				try {
					ldrDB.abortUpdate();
				} catch (SQLException e) {
					Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Can't rollback in failed removing library index:"+index, sqlex);
				}
			}
		}
	}
	
	
	
	/**
	 * returns number of libraries in list, regardless its status
	 * @return number of libraries 
	 */
	public int count() {
		
		return ldLibs.size();
	}
	
	
	/**
	 * Return type of library as integer
	 * @param index library to query
	 * @return FOLDER or ZIPFILE integer constant
	 */
	public int getType(int index) {
		
		return ldLibs.get(index).getType();
	}
	
	
	
	/**
	 * Returns path of selected library
	 * @param index library to query
	 * @return a path as a string 
	 */
	public String getPath(int index) {
		
		return ldLibs.get(index).libPath();
	}
	

	
	/**
	 * Return true if library follows LDraw standard for folder structure inside zipfile/folder
	 * @param index library handle
	 * @return true if library follows LDraw folder structure
	 */
	public boolean isLDrawStd(int index) {
		
		return ldLibs.get(index).isLDrawStd();
	}
	
	
	
	/**
	 * Returns true if selected library is "official"
	 * @param index library to query
	 * @return true if library is an official library
	 */
	public boolean isOfficial(int index) {
		
		return ldLibs.get(index).isOfficial();
	}
	
	
	
	
	/**
	 * checks if a library is enabled
	 * 
	 * @param index library to query
	 * @return true if library is enabled
	 */
	public boolean isEnabled(int index) {
		
		return ldLibs.get(index).isEnabled();
	}
	
	
	/**
	 * enable a library
	 * @param index library to enable
	 */
	public void enable(int index) {
		
		ldLibs.get(index).enable();
		if (useDatabase) {
			try {
				ldrDB.enableLib(index);
			}
			catch (SQLException sqlex) {
				Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Error in enabling library index:"+index, sqlex);
			}
		}
	}
	
	
	
	/**
	 * Disable (ignore) a library
	 * @param index library to disable
	 */
	public void disable(int index) {
		
		if (index == OFFICIALINDEX) {
			Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Attempt to disable default library");
			return;			
		}
		ldLibs.get(index).disable();
		if (useDatabase) {
			try {
				ldrDB.disableLib(index);
			}
			catch (SQLException sqlex) {
				Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Error in disabling library index:"+index, sqlex);
			}
		}
	}
	
	
	
	/**
	 * Elevates priority for a library (swap n with n-1)
	 * 
	 * @param index library to elevate
	 */
	public void upPriority(int index) {
		
		LDLibrary a,b;
		if (index <= 1) {
			// it is already at max priority
			return;
		}
		a = ldLibs.get(index);
		b = ldLibs.set(index-1, a);
		ldLibs.set(index, b);
		if (useDatabase) {
			try {
				ldrDB.changePriority(index,index-1);
			}
			catch (SQLException sqlex) {
				Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Error in elevating priority: "+index, sqlex);
			}
		}
	}
	
	
	
	
	/**
	 * Lowers priority for a library (swap n with n+1)
	 * 
	 * @param index library to lowers
	 */
	public void downPriority(int index) {
		
		if (index == OFFICIALINDEX) {
			Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Attempt to lower priority of default library");
			return;			
		}
		LDLibrary a,b;
		if ((index + 1) >= ldLibs.size()) {
			// it is already at minimum priority
			return;
		}
		a = ldLibs.get(index+1);
		b = ldLibs.set(index, a);
		ldLibs.set(index+1, b);
		if (useDatabase) {
			try {
				ldrDB.changePriority(index,index+1);
			}
			catch (SQLException sqlex) {
				Logger.getGlobal().log(Level.SEVERE,"[LDrawLib] Error in lowering priority: "+index, sqlex);
			}
		}
	}
	
		
	
	
	
	public Collection<String> getAllParts(int index) {
		
		return ldLibs.get(index).getAllParts();
	}
	
	
	
	/**
	 * Force retrieval of low-resolution primitives, if available.
	 * If requested primitive is already lo- or hi- res this 
	 * setting is ignored 
	 */
	public static void forceLoRes() {
		
		LDLibrary.setLoRes();
	}
	
	
	/**
	 * Force retrieval of high-resolution primitives, if available
	 * If requested primitive is already lo- or hi- res this 
	 * setting is ignored 
	 */
	public static void forceHiRes() {
		
		LDLibrary.setHiRes();
	}
	
	
	/**
	 * Reset to standard resolution
	 */
	public static void resetStdRes() {
		
		LDLibrary.setStdRes();
	}
	
	
	
	/**
	 * Checks if LDrawLib looking for parts only in official library
	 * 
	 * @return true if using only official parts
	 */
	public boolean isOfficialOnly() {
		return officialOnly;
	}


	/**
	 * Enable or disable use of unofficial parts library
	 * 
	 * @param officialOnly if true uses only official parts
	 */
	public void setOfficialOnly(boolean officialOnly) {
		this.officialOnly = officialOnly;
	}


	/** 
	 * Checks if a LDraw parts, sub-parts or primitive is in current library
	 *  
	 * @param ldrawId LDraw part id as string, with full path and ".dat" appended
	 * @return LDrawPartType != NONE if part <strong>ldrawId</strong> exists in library
	 */
	public LDrawPartType checkPart(String ldrawId) {
		
		// if only official lib is enabled
		if (officialOnly) {
			return ldLibs.get(OFFICIALINDEX).checkPart(ldrawId);
		}
		for (int i=0;i<ldLibs.size();i++) {
			// if lib is disabled, ignore
			if (!ldLibs.get(i).isEnabled())
				continue;
			LDrawPartType t = ldLibs.get(i).checkPart(ldrawId);
			if (t != LDrawPartType.NONE)
				return t;
		}
		return LDrawPartType.NONE;
	}
	

	
	
	/** 
	 * Gets a file from LDraw Library
	 * 
	 * Stops on first file found
	 * 
	 * @param pathname pathname into library
	 * @return a LineNumberReader or null if file is not found
	 * @throws IOException if file error
	 * @throws FileNotFoundException if file is not in library
	 */
	public LineNumberReader getFile(String path) throws IOException {
		
		// if only official lib is enabled
		if (officialOnly) {
			LineNumberReader l = ldLibs.get(OFFICIALINDEX).getFile(path);
			if (l == null)
				throw new FileNotFoundException("[LDrawLib] File '"+path+"' not found in official library.");
			return l;
		}
		for (int i=0;i<ldLibs.size();i++) {
			// if lib is disabled, ignore
			if (!ldLibs.get(i).isEnabled())
				continue;
			LineNumberReader l = ldLibs.get(i).getFile(path);
			if (l != null)
				return l;
		}
		throw new FileNotFoundException("[LDrawLib] File '"+path+"' not found.");
	}
	
	
	
	/**
	 * Gets a file from LDraw official Library
	 * 
	 * @param path file to search in library 
	 * @return a LineNumberReader 
	 * @throws IOException if read error
	 * @throws IllegalState if library set does not contains an official library
	 * @throws FileNotFoundException if file is not in library
	 */
	public LineNumberReader getOfficialFile(String path) throws IOException {

		if (ldLibs.size() == 0)
			throw new IllegalStateException("[LDrawLib] No official library in library set.");
		LineNumberReader l = ldLibs.get(0).getFile(path);
		if (l == null)
			throw new FileNotFoundException("[LDrawLib] File '"+path+"' not found in official library.");
		return l;
	}

	
	/** 
	 * Gets a LineNumberReader for part identified by "ldrid"
	 * 
	 * @param ldrid LDraw part ID as full pathname with ".dat" suffix 
	 * @return a LineNumberReader or null if part is not found
	 */
	public LineNumberReader getPart(String ldrawid) {

		// if only official lib is enabled
		if (officialOnly) {
			LineNumberReader l = ldLibs.get(OFFICIALINDEX).getPart(ldrawid);
			if (l == null)
				Logger.getGlobal().log(Level.WARNING,"[LDLibrary] Unable to get part "+ldrawid+" from official library");
			return l;
		}
		for (int i=0;i<ldLibs.size();i++) {
			// if lib is disabled, ignore
			if (!ldLibs.get(i).isEnabled())
				continue;
			LineNumberReader l = ldLibs.get(i).getPart(ldrawid);
			if (l != null)
				return l;
		}
		Logger.getGlobal().log(Level.WARNING,"[LDLibrary] Unable to get part "+ldrawid);
    	return null;
	}
	
	
	
	// Some utility functions
	
	
	public static boolean isAlreadyInstalled() {
		
		return System.getenv(LDRAWENV) != null;
	}
	
	
	public static String getInstalled() {
		
		return System.getenv(LDRAWENV);
	}
	
	
//	/**
//	 * Do a rough sanity check on a file or folder to see if it is an official LDraw library
//	 * @param f file/folder to check
//	 * @return true if success
//	 */
//	public static boolean officialSanityCheck(File f) {
//		
//		boolean res = true;
//		
//		if (f.getName().toLowerCase().endsWith(".zip")) {
//			// a zip library is selected
//			ZipFile z;
//			try {
//				z = new ZipFile(f);
//			} catch (IOException e) {
//				return false;
//			}
//			if (z.getEntry("ldraw/p/stud.dat") == null) {
//				if (z.getEntry("parts/s") == null) 
//					res = false;
//				else if (!z.getEntry("parts/s").isDirectory())
//					res = false;
//			}
//			try {
//				z.close();
//			} catch (IOException e) {
//				;
//			}
//			return res;
//		}
//		else {
//			// user selected a file/folder inside library path
//			File l = new File(f.getParent(),"parts");
//			if (l.exists() && l.isDirectory()) {
//				// found library path
//				return true;
//			}
//			else {
//				// if user selected library folder
//				l = new File(f, "parts");
//				if (l.exists() && l.isDirectory()) {
//					// found library path
//					return true;
//				}			
//				else {
//					return false;
//				}
//			}
//		}
//
//	}
	

}
