/**
	Copyright 2016-2017 Mario Pascucci <mpascucci@gmail.com>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * An object connected to an LDraw library or repository
 * 
 * @author Mario Pascucci
 *
 */
public class LDLibrary {

	
	/** library is a folder with files */
	public static final int FOLDER = 1;
	/** library is a zipped file */
	public static final int ZIPFILE = 2;
	
	/** use default resolution primitives */
	public static final int STDRES = 0;
	/** use low resolution primitives */
	public static final int LORES = 1;
	/** use high resolution primitives */
	public static final int HIRES = 2;
	
	// paths in official or unofficial library files/folders
	private static final String ZIPPATH = "ldraw/";
	private static final String PARTPATH = "parts/";
	private static final String SUBPARTPATH = "s/";
	private static final String PRIMITIVESPATH = "p/";
	private static final String HIRESPRIMITIVESPATH = "p/48/";
	private static final String LORESPRIMITIVESPATH = "p/8/";
	private static final String MODELSPATH = "models/";
	
	/** defines resolution for primitives */
	private static int RESOLUTION = STDRES;
	
	/** true if library is an official library */
	private boolean official = false;
	
	/** true if library is used	*/
	private boolean enabled = true;
	
	/** true if library follows LDraw library folder/subfolder (e.g. parts/ p/ p/48/ etc) standards structure */
	private boolean isLDrawStd = false;
	
	private int type = ZIPFILE;
	private File libPath = null;
	private ZipFile libZip = null;
	private Map<String,String> partList = new HashMap<String,String>();
	
	
	
	/**
	 * returns a new LDraw library source from string
	 * checks if library follows LDraw library structure 
	 * and if library is really official
	 * @param path pathname to library folder or zipfile
	 * @param official true if library is official LDraw library
	 * @throws IOException if library is marked official but it isn't
	 * @throws ZipException 
	 */
	public LDLibrary(String path, boolean official) throws ZipException, IOException {
		
		boolean hasPrimitives = false, hasParts = false, hasSubParts = false;
		
		libPath = new File(path);
		if (libPath.isDirectory() && libPath.canRead()) {
			// it is a folder/directory
			type = FOLDER;
			// scan base folder
			for (File f : libPath.listFiles()) {
				if (f.isFile() && f.canRead()) {
					String key = f.getName().toLowerCase();
					if (partList.get(key) != null) {
						continue;
					}
					partList.put(key, f.getAbsolutePath());
				}
			}
			// scan p folder
			File p = new File(libPath.getPath(),PRIMITIVESPATH);
			if (p.isDirectory() && p.canRead()) { 
				for (File f : p.listFiles()) {
					if (f.isFile() && f.canRead()) {
						String key = PRIMITIVESPATH + f.getName().toLowerCase();
						if (partList.get(key) != null) {
							continue;
						}
						partList.put(key, f.getAbsolutePath());
						hasPrimitives = true;
					}
				}
			}
			// scan p/48 folder
			p = new File(libPath.getPath(),HIRESPRIMITIVESPATH);
			if (p.isDirectory() && p.canRead()) { 
				for (File f : p.listFiles()) {
					if (f.isFile() && f.canRead()) {
						String key = HIRESPRIMITIVESPATH + f.getName().toLowerCase();
						if (partList.get(key) != null) {
							continue;
						}
						partList.put(key, f.getAbsolutePath());
					}
				}
			}
			// scan p/8 folder
			p = new File(libPath.getPath(),LORESPRIMITIVESPATH);
			if (p.isDirectory() && p.canRead()) { 
				for (File f : p.listFiles()) {
					if (f.isFile() && f.canRead()) {
						String key = LORESPRIMITIVESPATH + f.getName().toLowerCase();
						if (partList.get(key) != null) {
							continue;
						}
						partList.put(key, f.getAbsolutePath());
					}
				}
			}
			// scan parts folder
			p = new File(libPath.getPath(),PARTPATH);
			if (p.isDirectory() && p.canRead()) { 
				for (File f : p.listFiles()) {
					if (f.isFile() && f.canRead()) {
						String key = PARTPATH + f.getName().toLowerCase();
						if (partList.get(key) != null) {
							continue;
						}
						partList.put(key, f.getAbsolutePath());
						hasParts = true;
					}
				}
			}
			// scan parts/s/ folder
			p = new File(libPath.getPath(),PARTPATH + SUBPARTPATH);
			if (p.isDirectory() && p.canRead()) { 
				for (File f : p.listFiles()) {
					if (f.isFile() && f.canRead()) {
						String key = PARTPATH + SUBPARTPATH + f.getName().toLowerCase();
						if (partList.get(key) != null) {
							continue;
						}
						partList.put(key, f.getAbsolutePath());
						hasSubParts = true;
					}
				}
			}
		}
		else if (libPath.isFile() && libPath.canRead()) {
			// it is a file/zipfile
			type = ZIPFILE;
			libZip = new ZipFile(libPath);
			Enumeration<? extends ZipEntry> en = libZip.entries();
			while (en.hasMoreElements()) {
				ZipEntry ze = en.nextElement();
				//System.out.println(ze.toString());
				if (!ze.isDirectory()) {
					// if a duplicate exists, first win
					String key = ze.getName().toLowerCase();
					// strip "ldraw/" from start of key to uniforms values. 
					if (key.startsWith(ZIPPATH))
						key = key.substring(ZIPPATH.length());
					if (partList.get(key) != null) {
						continue;
					}
					partList.put(key, ze.getName());
					if (key.startsWith(PARTPATH+SUBPARTPATH)) {
						hasSubParts = true;
						continue;
					}
					if (key.startsWith(PARTPATH)) {
						hasParts = true;
						continue;
					}
					if (key.startsWith(PRIMITIVESPATH)) {
						hasPrimitives = true;
						continue;
					}
					//System.out.println(key+" - "+ze.getName());
				}
			}

		}
		else 
			throw new IOException("'"+path+"' isnt't a file or a directory, or cannot read it.");
		if (hasParts && hasPrimitives && hasSubParts) {
			// library follows standard LDraw folder structure
			isLDrawStd = true;
		}
		if (official) {
			if (!isLDrawStd || !partList.containsKey(LDrawColor.LDRCONFIG.toLowerCase())) {
				throw new IOException("[LDLibrary] Library marked as official does not appears as official LDraw library.\nLibrary: "+path);
			}
		}
		this.official = official;
	}
	
	
	
	/**
	 * Library path as string 
	 * @return library path
	 */
	public String libPath() {
		
		return libPath.getPath();
	}
	
	
	/**
	 * set library official flag
	 * @param isOfficial true if library is official
	 */
	public void setOfficial(boolean isOfficial) {
		
		official = isOfficial;
	}
	
	
	
	/**
	 * @return the official
	 */
	public boolean isOfficial() {
		return official;
	}

	
	
	/**
	 * @return true if library follows LDraw standard directory structures 
	 */
	public boolean isLDrawStd() {
		return isLDrawStd;
	}



	/**
	 * Enable library
	 */
	public void enable() {
		enabled = true;
	}
	
	
	/**
	 * Disable library
	 */
	public void disable() {
		enabled = false;
	}
	

	/**
	 * Checks if library is enabled. Is up to LDrawLib class to ignore disabled library.
	 * @return true if library is enabled.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	

	/**
	 * @return the rESOLUTION
	 */
	public static int getRESOLUTION() {
		return RESOLUTION;
	}


	/**
	 * Force using high resolution primitives
	 */
	public static void setHiRes() {
		RESOLUTION = HIRES;
	}


	/**
	 * Force using low resolution primitives
	 */
	public static void setLoRes() {
		RESOLUTION = LORES;
	}


	/** 
	 * restore primitive standard usage
	 */
	public static void setStdRes() {
		RESOLUTION = STDRES;
	}


	/**
	 * Gets type of library (folder or zipfile)
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	
	
	/**
	 * Returns all parts in this library
	 * @return all parts in string form
	 */
	public Collection<String> getAllParts() {
		
		return partList.keySet();
	}
	
	
	
	/**
	 * Checks if a LDraw parts, sub-parts or primitive is in current library
	 *  
	 * @param ldrawId LDraw part id as string, with full path and ".dat" appended
	 * @return LDrawPartType != NONE if part <strong>ldrawId</strong> exists in library
	 */
	public LDrawPartType checkPart(String ldrawId) {
		
		String[] part;

		// part search is case insensitive
		String ldrid = ldrawId.toLowerCase();
        ldrid = ldrid.replace('\\', '/');   // in ZIP path separator is always '/'
        part = ldrid.split("/");
        if (part.length > 1) {
        	if (part[0].equals("s")) { 
                if (partList.containsKey(PARTPATH+ldrid)) {
                	if (official)
                		return LDrawPartType.SUBPART;
                	else
                		return LDrawPartType.UNOFF_SUB;                	
                }
        	}
        	// hi-res primitive
        	else if (part[0].equals("48")) {
                if (partList.containsKey(PRIMITIVESPATH+ldrid)) {
                	if (official) 
                		return LDrawPartType.PRIMITIVE48;
                	else
                		return LDrawPartType.UNOFF48;
                }
        	}
        	// lo-res primitive
        	else if (part[0].equals("8")) {
                if (partList.containsKey(PRIMITIVESPATH+ldrid)) {
                	if (official) 
                		return LDrawPartType.PRIMITIVE8;
                	else
                		return LDrawPartType.UNOFF8;
                }
        	}
        }
        else {
       		if (partList.containsKey(PRIMITIVESPATH+ldrid)) {
       			if (official) 
       				return LDrawPartType.PRIMITIVE;
       			else 
       				return LDrawPartType.UNOFF_PRIM;
       		}
            if (partList.containsKey(PARTPATH+ldrid)) { 
            	if (official) 
            		return LDrawPartType.OFFICIAL;
            	else 
            		return LDrawPartType.UNOFFICIAL;
            }
            if (partList.containsKey(MODELSPATH+ldrid)) { 
           		return LDrawPartType.MODEL;
            }
            if (partList.containsKey(ldrid)) {
            	return LDrawPartType.UNOFFICIAL;
            }
        }
        return LDrawPartType.NONE; 

	}
	

	
	
	/** 
	 * Gets a file from LDraw official Library
	 * 
	 * If library is folder, return a file by complete path under library folder
	 * If library is Zipfile, return a file by path without "ldraw/" prefix, i.e. "ldraw/" prefix must be omitted
	 * 
	 * @param pathname pathname into library 
	 * @return a LineNumberReader or null if file is not found
	 * @throws IOException 
	 */
	public LineNumberReader getFile(String path) throws IOException {
		
		String pathname = path.toLowerCase();
        pathname = pathname.replace('\\', '/');
        if (type == FOLDER) {
        	try {
        		return new LineNumberReader(new FileReader(new File(libPath,path)));
        	}
        	catch (FileNotFoundException ex) {
        		return null;
        	}
        }
        ZipEntry ze = libZip.getEntry(partList.get(pathname));
        if (ze != null)
			try {
				return new LineNumberReader(new InputStreamReader(libZip.getInputStream(ze)));
			} catch (IOException e) {
				throw new IOException("[LDrawLib] Unable to get file "+pathname+ " from zipfile\n"+e.getLocalizedMessage());
			}
		else {
			return null;
		}
	}

	
	

	/**
	 * Gets a LineNumberReader for part identified by "ldrid"
	 * 
	 * @param ldrid LDraw part ID as full pathname with ".dat" suffix 
	 * @return a LineNumberReader or null if part is not found
	 */
	public LineNumberReader getPart(String ldrawid) {

		String[] part;
		String ze = null;
		
		String ldrid = ldrawid.toLowerCase();
        ldrid = ldrid.replace('\\', '/');
        part = ldrid.split("/");
        
        if (part.length > 1) {
        	if (part[0].equals("s")) {
                ze = partList.get(PARTPATH+ldrid);
        	}
        	else if (part[0].equals("48") || part[0].equals("8")) {
                ze = partList.get(PRIMITIVESPATH+ldrid); 	
         	}
        }
        else {
        	// try primitive in "p/" folder
        	if (RESOLUTION == LORES) {
        		ze = partList.get(LORESPRIMITIVESPATH+ldrid);
        		if (ze == null)  // fallback 
            		ze = partList.get(PRIMITIVESPATH+ldrid);
 	       	}
	    	else if (RESOLUTION == HIRES) {
	    		ze = partList.get(HIRESPRIMITIVESPATH+ldrid);
        		if (ze == null)  // fallback 
            		ze = partList.get(PRIMITIVESPATH+ldrid);
	    	}
	    	else {
        		ze = partList.get(PRIMITIVESPATH+ldrid);
        	}
        	// try part in "parts/" folder
            if (ze == null) {
                ze = partList.get(PARTPATH+ldrid); 		
            }
            // try models path
            if (ze == null) {
                ze = partList.get(MODELSPATH+ldrid); 	
            }
            // try root path
            if (ze == null) {
                ze = partList.get(ldrid); 	
            }
        }
        if (ze != null)
			try {
				if (type == FOLDER) {
					return new LineNumberReader(new FileReader(ze));
				}
				else {
					return new LineNumberReader(new InputStreamReader(libZip.getInputStream(libZip.getEntry(ze))));
				}
			} catch (IOException e) {
				Logger.getGlobal().log(Level.SEVERE,"[LDLibrary] Unable to get part "+ldrid+" "+e.getLocalizedMessage(),e);
				return null;
			}
		else {
			//System.err.println("[LDLibrary] Unable to get part "+ldrid);
        	return null;
		}
	}

	
	
	
}
