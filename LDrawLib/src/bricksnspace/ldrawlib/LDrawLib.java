
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



import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;


/**
 * A library to read LDraw primitives, parts and other files from folder or zipfiles.
 * 
 * Requires only Official part library to work. If supplied, uses unofficial library and user library, too.
 * To disable (momentary or permanently) use of unofficial parts, use setOfficialOnly(true)
 * 
 */





public class LDrawLib { 

	private List<LDLibrary> ldLibs;
	private int officialIndex = -1;
	
	private boolean officialOnly = false;
	public static final String LDRAWENV = "LDRAWBASEDIR"; 

	
	
	/**
	 * Creates a list of LDraw libraries, ordered by priority.
	 * Requires one and only one official LDraw library.
	 * 
	 * @param official path to main official library (can be a folder or a zipfile)
	 * @throws ZipException 
	 * @throws IOException if cannot read library content
	 */
	public LDrawLib(String official) throws ZipException, IOException {
		
		ldLibs = new ArrayList<LDLibrary>();
		ldLibs.add(new LDLibrary(official, true));
		officialIndex = 0;
	}
	
	
	
	/**
	 * Creates an empty list of LDraw libraries, ordered by priority.
	 * 
	 */
	public LDrawLib() {
		
		ldLibs = new ArrayList<LDLibrary>();
	}
	
	
	
	/**
	 * Appends a library to list
	 * It can't be an official library
	 *
	 * @param lib path to library (can be a folder or a zipfile)
	 * @throws ZipException
	 * @throws IOException if cannot read library content
	 */
	public void addLDLib(String lib) throws ZipException, IOException {
		
		ldLibs.add(new LDLibrary(lib, false));
		officialIndex = getOfficialIndex();
	}
	
	
	
	/**
	 * Appends a library to list, defining if it's official and enabled
	 * 
	 * @param lib path to library (folder or zipfile)
	 * @param official true if library is official 
	 * @param enabled true if library is enabled
	 * @throws ZipException
	 * @throws IOException if cannot read library content
	 */
	public void addLDLib(String lib, boolean official, boolean enabled) throws ZipException, IOException {
		
		LDLibrary l = new LDLibrary(lib, official);
		if (!enabled) 
			l.disable();
		ldLibs.add(l);
		officialIndex = getOfficialIndex();
	}
	
	
	
	
	/**
	 * Removes a library from list
	 * 
	 * @param index position in list of library to remove 
	 */
	public void removeLDLib(int index) {
		
		ldLibs.remove(index);
		officialIndex = getOfficialIndex();
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
	 * Change library status from official to unofficial and vice-versa
	 * @param index library handle
	 * @param isOfficial true if library is official
	 */
	public void setOfficial(int index, boolean isOfficial) {
		
		ldLibs.get(index).setOfficial(isOfficial);
		officialIndex = getOfficialIndex();
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
		officialIndex = getOfficialIndex();
	}
	
	
	
	/**
	 * Disable (ignore) a library
	 * @param index library to disable
	 */
	public void disable(int index) {
		
		ldLibs.get(index).disable();
		officialIndex = getOfficialIndex();
	}
	
	
	
	/**
	 * Elevates priority for a library (swap n with n-1)
	 * 
	 * @param index library to elevate
	 */
	public void upPriority(int index) {
		
		LDLibrary a,b;
		if (index == 0) {
			// it is already at max priority
			return;
		}
		a = ldLibs.get(index);
		b = ldLibs.set(index-1, a);
		ldLibs.set(index, b);
		officialIndex = getOfficialIndex();
	}
	
	
	
	
	/**
	 * Lowers priority for a library (swap n with n+1)
	 * 
	 * @param index library to lowers
	 */
	public void downPriority(int index) {
		
		LDLibrary a,b;
		if ((index + 1) >= ldLibs.size()) {
			// it is already at minimum priority
			return;
		}
		a = ldLibs.get(index+1);
		b = ldLibs.set(index, a);
		ldLibs.set(index+1, b);
		officialIndex = getOfficialIndex();
	}
	
	
	
	
	/** 
	 * Returns index of first official library in list
	 * @return index of official library, -1 if no official library defined
	 */
	public int getOfficialLibrary() {
		
		return officialIndex;
	}
	
	
	
	
	/**
	 * Updates official library index in event of add/remove/disable/change priority in library list.
	 * Ignore disable library.
	 * @return index of first official library or -1 if no official lib in list. 
	 */
	private int getOfficialIndex() {
		
		for (int i=0;i<ldLibs.size();i++) {
			if (!ldLibs.get(i).isEnabled())
				continue;
			if (ldLibs.get(i).isOfficial())
				return i;
		}
		return -1;
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
			return ldLibs.get(officialIndex).checkPart(ldrawId);
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
			LineNumberReader l = ldLibs.get(officialIndex).getFile(path);
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

		if (officialIndex > 0)
			throw new IllegalStateException("[LDrawLib] No official library in library set.");
		LineNumberReader l = ldLibs.get(officialIndex).getFile(path);
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
			LineNumberReader l = ldLibs.get(officialIndex).getPart(ldrawid);
			if (l == null)
				System.err.println("[LDLibrary] Unable to get part "+ldrawid+" from official library");
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
		System.err.println("[LDLibrary] Unable to get part "+ldrawid);
    	return null;
	}
	
	
	
	// Some utility functions
	
	/**
	 * Do a rough sanity check on a file or folder to see if it is an official LDraw library
	 * @param f file/folder to check
	 * @return true if success
	 */
	public static boolean officialSanityCheck(File f) {
		
		if (f.getName().toLowerCase().endsWith(".zip")) {
			// a zip library is selected
			ZipFile z;
			try {
				z = new ZipFile(f);
			} catch (IOException e) {
				z = null;
			}
			if (z == null || z.getEntry("ldraw/p/stud.dat") == null) {
				return false;
			}
			return true;
		}
		else {
			// user selected a file/folder inside library path
			File l = new File(f.getParent(),"parts");
			if (l.exists() && l.isDirectory()) {
				// found library path
				return true;
			}
			else {
				// if user selected library folder
				l = new File(f, "parts");
				if (l.exists() && l.isDirectory()) {
					// found library path
					return true;
				}			
				else {
					return false;
				}
			}
		}

	}
	

}
