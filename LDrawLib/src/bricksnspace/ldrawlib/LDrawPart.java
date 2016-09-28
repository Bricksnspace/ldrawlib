/*
	Copyright 2013-2015 Mario Pascucci <mpascucci@gmail.com>
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





import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


/**
 * A generic part from LDraw standard part definition
 * 
 * Part can be a model, a primitive, a single LDraw primitive, in a recursive fashion
 * Part with only one primitive is 1:1 to a single LDraw file line (excluding header)
 * 
 * @author Mario Pascucci
 *
 */
public class LDrawPart {

	private int id;
	private String ldrawid;
	private String description;
	private String author;
	private String partName;
	private String category;
	private String keywords;
	private Date lastUpdate;
	private String partTypeString = "";
	private String license = "";
	private LDrawPartType partType = LDrawPartType.MODEL;
	private List<LDPrimitive> primitives = new ArrayList<LDPrimitive>();
	private Map<Integer,LDPrimitive> partById = new HashMap<Integer,LDPrimitive>();
	private LDStep stepper = new LDStep();
	private static LDrawLib ldrlib = null;
	private static Map<String,LDrawPart> partCache = new HashMap<String,LDrawPart>();
	private static Map<String,LDrawPart> customPartCache = new HashMap<String,LDrawPart>();
	private static Map<String,LDrawPart> internalUsePartCache = new HashMap<String,LDrawPart>();
	private static int globalId = 0;

	

	/**
	 * Private constructor for LDraw Part factory
	 * 
	 * @param ldrid is LDraw part name (LDraw is case-sensitive, so use lowercase!)
	 * @param onlyMetadata if true reads only metadata, not primitives
	 * @throws IOException if LDraw library files isn't readable
	 * @throws LDrawException if there are parse or format error
	 */
	private LDrawPart(String ldrid, boolean onlyMetadata) throws IOException, LDrawException {
		
		if (ldrlib == null) {
			throw new IllegalStateException("[LDrawPart] LDraw library not initialized");
		}
		ldrawid = ldrid;
		id = getUniqueId();
        LineNumberReader ldf = ldrlib.getPart(ldrawid);
		parse(ldf, onlyMetadata);
		ldf.close();
	}
	
	
	
	/**
	 * Reads a part definition from a String
	 * @param ldrid is LDraw part name (LDraw is case-sensitive, so use lowercase!)
	 * @param datLines contains lines in LDraw format
	 * @throws IOException
	 * @throws LDrawException if there are parse or format error
	 */
	private LDrawPart(String ldrid, String datLines) throws IOException, LDrawException {
		
		ldrawid = ldrid;
		id = getUniqueId();
        LineNumberReader ldf = new LineNumberReader(new StringReader(datLines));
		parse(ldf, false);
		ldf.close();
	}
	
	
	/**
	 * creates unofficial/submodel/custom part
	 */
	private LDrawPart() {

		ldrawid = "";
		description = "";
		author = "";
		partName = "";
		category = "";
		keywords = "";
	}
	
	

	/**
	 * creates a copy with new global id and new primitives
	 */
	public LDrawPart getCopy() {
		
		LDrawPart p = new LDrawPart();
		p.id = getUniqueId();
		p.author = author;
		p.category = category;
		p.description = description;
		p.keywords = keywords;
		if (lastUpdate != null) {
			p.lastUpdate = new Date(lastUpdate.getTime());
		}
		p.ldrawid = ldrawid;
		p.partName = partName;
		p.setPartType(partType);
		p.partTypeString = partTypeString;
		p.license = license;
		for (LDPrimitive pr: primitives) {
			p.addPart(pr.getCopy());
		}
		return p;
	}

	
	
	/**
	 * return a LDrawPart object from primitive that isn't a reference
	 * @param p original primitive
	 */
	private LDrawPart(LDPrimitive p) {
		
		id = getUniqueId();
		switch (p.getType()) {
		case AUXLINE:
			setPartType(LDrawPartType.GEOM_PRIMITIVE);
			ldrawid = "AuxLine";
			category = "Primitives";
			description = "LDraw auxiliary line primitive";
			//matrix = new Matrix3D();
			addPart(p);
			break;
		case COLOUR:
			setPartType(LDrawPartType.COMMAND);
			ldrawid = "Colour";
			category = "Commands";
			description = "LDraw !Colour statement";
			addPart(p);
			break;
		case LINE:
			setPartType(LDrawPartType.GEOM_PRIMITIVE);
			ldrawid = "Line";
			category = "Primitives";
			description = "LDraw line primitive";
			//matrix = new Matrix3D();
			addPart(p);
			break;
		case QUAD:
			setPartType(LDrawPartType.GEOM_PRIMITIVE);
			ldrawid = "Quad";
			category = "Primitives";
			description = "LDraw quad primitive";
			//matrix = new Matrix3D();
			addPart(p);
			break;
		case STEP:
			setPartType(LDrawPartType.COMMAND);
			ldrawid = "Step";
			category = "Commands";
			description = "LDraw STEP statement";
			addPart(p);
			break;
		case TRIANGLE:
			setPartType(LDrawPartType.GEOM_PRIMITIVE);
			ldrawid = "Triangle";
			category = "Primitives";
			description = "LDraw triangle primitive";
			addPart(p);
			break;
		default:
			break;
		}
	}
	
	
	
	public static void listcache() {
		
		System.out.println("LDraw Part cache: "+ partCache.size() +" ----------------------");
		for (LDrawPart p : partCache.values()) {
			System.out.println(p);
		}
		System.out.println("LDraw User Part cache: "+ customPartCache.size() +" ----------------------");
		for (LDrawPart p : customPartCache.values()) {
			System.out.println(p);
		}
	}
	
	
    public static Collection<LDrawPart> getAllCustomParts() {
		return customPartCache.values();
	}


	@Override
	public String toString() {
    	if (getPartType() == LDrawPartType.GEOM_PRIMITIVE) {
    		return "LDrawPart [id=" + id + 
    				", ldrawid=" + ldrawid + 
    				", type=" + getPartType() +
    				", name=" + description + 
    				", primitives=" + primitives.get(0) + 
    				"]";
    	}
		return "LDrawPart [id=" + id + 
				", ldrawid=" + ldrawid + 
				", type=" + getPartType() +
				", name=" + description + 
				", primitives=" + primitives.size() + 
				"]";
	}



    private static synchronized int getUniqueId() {
		
		return ++globalId;
	}
	


    
    
    public static void clearCache() {
		
		partCache.clear();
	}


    

	public static void setLdrlib(LDrawLib ldrlib) {

		if (ldrlib == null)
			throw new NullPointerException("[LDrawPart] LDraw library can't be null");
    	LDrawPart.ldrlib = ldrlib;
	}



	/////////////////////
	//
	// Official libraries parts
	//
	/////////////////////


	/**
	 * Gets a LDraw part from library, parsing file if necessary
	 * 
	 * DO NOT USE parts returned as a part to place in a model, always make a copy or
	 * use LDPrimitive methods
	 * @param ldrid string with LDraw id (with ".dat"), case insensitive
	 * @return a LDrawPart object or null if part doesn't exists
	 * @throws IOException if cannot read part from file/disk
	 * @throws LDrawException if an error occurs in parsing part/subpart/primitive
	 */
	private static LDrawPart getLDrawPart(String ldrid) {
		
		if (ldrlib == null) {
			throw new IllegalStateException("[LDrawPart] LDraw library not initialized");
		}
		String id = ldrid.toLowerCase();
		// it is in cache?
		LDrawPart p = partCache.get(id);
		if (p == null) {
			// first seen part
			if (ldrlib.checkPart(id) == LDrawPartType.NONE) {
				// unknown part
				return null;
			}
			try {
				p = new LDrawPart(id, false);
			} catch (IOException e) {
				return null;
			} catch (LDrawException e) {
				return null;
			}
			// put in cache
			partCache.put(id, p);
			return p;
		}
		else {
			// part in cache
			return p;
		}
	}
	
	
	/**
	 * Returns an LDraw part metadata from library, parsing file if necessary
	 * 
	 * DO NOT USE parts returned as a part to place in a model, this "part" contains only
	 * metadata, no primitives
	 * @param ldrid string with LDraw id (with ".dat"), case insensitive
	 * @return a LDrawPart object
	 * @throws IOException if cannot read part from file/disk
	 * @throws LDrawException if an error occurs in parsing part/subpart/primitive
	 */
	public static LDrawPart getLDrawPartMeta(String ldrid) throws IOException, LDrawException {
		
		if (ldrlib == null) {
			throw new IllegalStateException("[LDrawPart] LDraw library not initialized");
		}
		String id = ldrid.toLowerCase();
		// it is a part in library?
		if (ldrlib.checkPart(id) == LDrawPartType.NONE) {
			// unknown part
			return null;
		}
		LDrawPart p = new LDrawPart(id, true);
		return p;
	}
	
	
	
	
	/**
	 * Checks if part is in official library
	 * @param ldrid part id
	 * @return true if is an official library part
	 */
	public static boolean isLdrPart(String ldrid) {
		
		if (ldrlib == null) {
			throw new IllegalStateException("[LDrawPart] LDraw library not initialized");
		}
		return ldrlib.checkPart(ldrid.toLowerCase()) != LDrawPartType.NONE;
	}

	
	
	/**
	 * Checks if a part exists in library, custom or internal parts
	 * @param ldrid string with LDraw id (with ".dat"), case insensitive
	 * @return a LDrawPart object
	 * @throws IOException if cannot read part from file/disk
	 * @throws LDrawException if an error occurs in parsing part/subpart/primitive
	 */
	public static boolean existsPart(String name) {
		
		if (name == null || name.length() == 0) {
			return false;
		}
		return isLdrPart(name) || existsCustomPart(name) || existsInternalUsePart(name);
	}
	
	
	
	/**
	 * Gets a LDraw part from library, custom or internal parts
	 * 
	 * DO NOT USE parts returned as a part to place in a model, always make a copy or
	 * use LDPrimitive methods
	 * @param ldrid string with LDraw id (with ".dat"), case insensitive
	 * @return a LDrawPart object
	 * @throws IOException if cannot read part from file/disk
	 * @throws LDrawException if an error occurs in parsing part/subpart/primitive
	 */
	public static LDrawPart getPart(String name) {
		
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException("[getPart] Part name is null or empty");
		}
		LDrawPart p = getLDrawPart(name);
		if (p == null) {
			p = getCustomPart(name);
		}
		if (p == null) {
			p = getInternalUsePart(name);
		}
		if (p == null) {
			Logger.getAnonymousLogger().warning("No such part: "+name);
		}
		return p;
	}
	
	
	
	/////////////////////
	//
	// Part and primitives handling
	//
	/////////////////////
	
	
	public int getId() {
		return id;
	}

	
	public String getPartName() {
		return partName;
	}


	public void setPartName(String partName) {
		if (isOfficial())
			throw new IllegalArgumentException("[LDrawPart.setPartName] Cannot modify official part reference");
		this.partName = partName;
	}


	public String getAuthor() {
		return author;
	}


	public void setAuthor(String author) {
		this.author = author;
	}


	public String getDescription() {
		return description;
	}



	public String getCategory() {
		return category;
	}


	public Date getLastUpdate() {
		return lastUpdate;
	}


	public String getKeywords() {
		return keywords;
	}



	public boolean isOfficial() {
		return getPartType() == LDrawPartType.OFFICIAL || getPartType() == LDrawPartType.PRIMITIVE ||
				getPartType() == LDrawPartType.PRIMITIVE48 || getPartType() == LDrawPartType.PRIMITIVE8 ||
				getPartType() == LDrawPartType.SHORTCUT || getPartType() == LDrawPartType.SUBPART;
	}


	/**
	 * @return a string for LDraw brick ID
	 */
	public String getLdrawId() {
		return ldrawid;
	}
	
	
	
	public String getPartTypeString() {
		return partTypeString;
	}



	public void setPartTypeString(String partTypeString) {
		this.partTypeString = partTypeString;
	}



	public String getLicense() {
		return license;
	}



	public void setLicense(String license) {
		this.license = license;
	}



	public List<LDPrimitive> getPrimitives() {
		return primitives;
	}

	
	public LDrawPartType getPartType() {
		return partType;
	}


	public void setPartType(LDrawPartType partType) {
		//System.out.println(partName+"-"+partType);
		this.partType = partType;
		if (getPartTypeString() == "") {
			setPartTypeString("0 !LDRAW_ORG "+getPartType().asString());
		}
	}

	
	/**
	 * Add or replace part if exists with same id
	 * @param p part to add/replace
	 * @return old part or null if part is new
	 */
	public LDPrimitive addPart(LDPrimitive p) {

		LDPrimitive old = partById.put(p.getId(), p);
		// if replaces an old part
		if (old != null) {
			// put it in same position
			int i = primitives.indexOf(old);
			stepper.delPart(old);
			primitives.set(i, p);
		}
		else {
			// add new part to list
			primitives.add(p);
		}
		stepper.addPart(p);
		return old;
	}

	
	
	
	/**
	 * Removes part p from LDraw part
	 * @param p part to remove
	 * @return removed part
	 */
	public LDPrimitive delPart(LDPrimitive p) {
		
		primitives.remove(p);
		stepper.delPart(p);
		return partById.remove(p.getId());
	}
	
	
	/**
	 * Removes part identified by id from LDraw part
	 * @param id part ID to remove
	 * @return removed part
	 */
	public LDPrimitive delPartById(int id) {
		
		primitives.remove(partById.get(id));
		stepper.delPart(partById.get(id));
		return partById.remove(id);
	}
	
	
	
	public LDPrimitive getPartById(int id) {
		
		return partById.get(id);
	}

	
	/////////////////////
	//
	// 0 STEP handling
	//
	/////////////////////

	
	
	/**
	 * Utility function for STEP
	 * <p>
	 * If this is first step, moves all parts added until now to first step 
	 */
	public int nextStep() {
		
		stepper.nextStep();
		if (stepper.getCurrStep() == 1) {
			// it is first step, moves all parts to step #1
			for (LDPrimitive p: primitives) {
				stepper.addPart(p);
			}
			// go to step #2
			stepper.nextStep();
		}
		return stepper.getCurrStep();
	}
	
	
	
	public int prevStep() {
		
		return stepper.prevStep();
	}


	
	public Collection<LDPrimitive> getPartsInStep(int n) {
		
		return stepper.getStep(n);
	}
	
	
	public void moveToCurrStep(LDPrimitive p) {
		
		stepper.delPart(p);
		stepper.addPart(p);
	}
	
	
	public void moveToStep(LDPrimitive p, int s) {
		
		stepper.moveToStep(p, s);
	}
	
	
	
	public int getCurrStep() {
		return stepper.getCurrStep();
	}
	
	
	
	public int getNumSteps() {
		return stepper.getNumSteps();
	}
	
	
	
	public int goFirstStep() {
		return stepper.goFirstStep();
	}


	public int goLastStep() {
		return stepper.goLastStep();
	}


	
	
	
	/////////////////////
	//
	// Internal use part
	//
	/////////////////////


	public void registerInternalUsePart(String ldrid) {
		
		if (isOfficial()) {
			throw new IllegalArgumentException("[LDrawPart.registerCustomPart] Cannot register official part as custom");
		}
		if (ldrid == null || ldrid == "") {
			throw new IllegalStateException("[LDrawPart.registerCustomPart] LDraw ID is empty");
		}
		ldrawid = ldrid;
		internalUsePartCache.put(ldrawid, this);
		ConnectionPoint.removeFromCache(ldrawid);
	}
	
	
	
	/**
	 * Creates a new internal use part, initially empty
	 * <p>
	 * Register part in cache as a "custom part"
	 * @param name part name (includes ".dat" or ".ldr")
	 * @return empty part
	 */
	public static LDrawPart newInternalUsePart(String name) {
		
		LDrawPart p = new LDrawPart();
		p.id = getUniqueId();
		p.ldrawid = name;
		p.partName = name;
		internalUsePartCache.put(name, p);
		ConnectionPoint.removeFromCache(name);
		return p;
	}
	

	
	public static LDrawPart getInternalUsePart(String part) {
		
		return internalUsePartCache.get(part);
	}
	
	
	
	public static boolean existsInternalUsePart(String name) {
		
		return internalUsePartCache.containsKey(name);
	}

    
	
	
	/////////////////////
	//
	// Generic part creation 
	//
	/////////////////////
	

	
    /**
     * Return a LDraw part object
     * If part is a reference to part or primitive, get it from library or custom parts
     * If it is a command or a geometric primitive
     * @param p
     * @return
     */
//	public static LDrawPart getPartFromPrimitive(LDPrimitive p) {
//		
//		LDrawPart part;
//		if (p.getType() != LDrawCommand.REFERENCE) {
//			part = new LDrawPart(p);
//		}
//		else {
//			part = getPart(p.getLdrawId());
//		}
//		return part;
//	}

	
 




	public static LDrawPart getPartMeta(String ldrawid, String partName, String author,
			String description, String category, String keywords) {
		
		LDrawPart p = new LDrawPart();
		p.ldrawid = ldrawid;
		p.partName = partName;
		p.author = author;
		p.description = description;
		p.category = category;
		p.keywords = keywords;
		return p;
	}
	
	

	
	public static LDrawPart newEmptyPart() {
		
		LDrawPart p = new LDrawPart();
		p.id = getUniqueId();
		return p;
	}
	
	

	/**
	 * Creates a new part parsing LDraw lines from a string
	 * <p>
	 * Does NOT register part in cache
	 * @param name part name (includes ".dat" or ".ldr")
	 * @param datLines contains part definition in LDraw format
	 * @return new part from string or null if there are errors
	 */
	public static LDrawPart newPartFromString(String name, String datLines) {
		
		LDrawPart p;
		try {
			p = new LDrawPart(name,datLines);
		} catch (IOException e) {
			Logger.getAnonymousLogger().severe("[newCustomPartFromString] Error reading "+name);
			return null;
		} catch (LDrawException e) {
			Logger.getAnonymousLogger().severe("[newCustomPartFromString] Error reading "+name);
			return null;
		}
		return p;
	}
	


	/////////////////////
	//
	// Custom parts
	//
	/////////////////////
	
	
	/**
	 * Creates a new custom part, initially empty
	 * <p>
	 * Register part in cache as a "custom part"
	 * @param name part name (includes ".dat" or ".ldr")
	 * @return empty part
	 */
	public static LDrawPart newCustomPart(String name) {
		
		LDrawPart p = new LDrawPart();
		p.id = getUniqueId();
		p.ldrawid = name;
		p.partName = name;
		customPartCache.put(name, p);
		ConnectionPoint.removeFromCache(name);
		return p;
	}
	
	

	public void registerCustomPart(String ldrid) {
		
		if (isOfficial()) {
			throw new IllegalArgumentException("[LDrawPart.registerCustomPart] Cannot register official part as custom");
		}
		if (ldrid == null || ldrid == "") {
			throw new IllegalStateException("[LDrawPart.registerCustomPart] LDraw ID is empty");
		}
		ldrawid = ldrid;
		customPartCache.put(ldrawid, this);
		ConnectionPoint.removeFromCache(ldrawid);
	}
	

	
	
	public static void delCustomPart(String name) {
		
		customPartCache.remove(name);
		ConnectionPoint.removeFromCache(name);
	}
	
	
	public static LDrawPart getCustomPart(String name) {
		
		return customPartCache.get(name);
	}

	
	
	public static boolean existsCustomPart(String name) {
		
		return customPartCache.containsKey(name);
	}

    
	
	public static void clearCustomParts() {
		
		customPartCache.clear();
	}
	
	
	

	
	/////////////////////
	//
	// I/O for parts
	//
	/////////////////////
	
	
	
    private void parse(LineNumberReader ldf, boolean onlyMeta) throws IOException, LDrawException {
    	
    	String l;
        boolean invNext = false;
        boolean isClockWise = false;
        boolean firstLine = true;

        keywords = "";

        if (ldf == null) {
        	return;
        }

		while ((l = ldf.readLine()) != null) {
			//System.out.println("linea: "+ldf.getLineNumber()+" -- "+l);
			try {
				LDrawCommand cmd = LDrawParser.parseCommand(l);
				switch (cmd) {
				case AUTHOR:
					author = LDrawParser.parseAuthor(l);
					break;
				case AUXLINE:
					if (! onlyMeta)
						addPart(LDrawParser.parseLineType5(l));
					break;
				case BFC_CCW:
					isClockWise = false;
					break;
				case BFC_CW:
					isClockWise = true;
					break;
				case BFC_INVERTNEXT:
					invNext = true;
					break;
				case CATEGORY:
					category = LDrawParser.parseCategory(l);
					break;
				case COLOUR:
					if (! onlyMeta)
						addPart(LDPrimitive.cmdColour(LDrawParser.parseColour(l)));
					break;
				case COMMENT:
					break;
				case EMPTY:
					break;
				case HISTORY:
					Date d = LDrawParser.parseHistory(l);
					if (lastUpdate == null || d.after(lastUpdate)) {
						// hack to avoid !HISTORY dates in future
						if (d.before(Calendar.getInstance().getTime())) {
							lastUpdate = d;
						}
					}
					break;
				case KEYWORDS:
					keywords += " " + LDrawParser.parseKeywords(l);
					break;
				case LICENSE:
					license = l;
					break;
				case LINE:
					if (! onlyMeta)
						addPart(LDrawParser.parseLineType2(l));
					break;
				case MPDFILE:
					Logger.getAnonymousLogger().warning("[LDrawLib] Illegal command FILE ("+ldrawid+
							") line#"+ ldf.getLineNumber());
					break;
				case MPDNOFILE:
					Logger.getAnonymousLogger().warning("[LDrawLib] Illegal command NOFILE ("+ldrawid+
							") line#"+ ldf.getLineNumber());
					break;
				case NAME:
					partName = LDrawParser.parsePartName(l);
					break;
				case FILETYPE:
					setPartType(LDrawParser.parsePartType(l));
					setPartTypeString(l);
					break;
				case QUAD:
					if (! onlyMeta)
						addPart(LDrawParser.parseLineType4(l,isClockWise));
					break;
				case REFERENCE:
					if (! onlyMeta)
						addPart(LDrawParser.parseLineType1(l, invNext));
					invNext = false;
					break;
				case SAVE:
					Logger.getAnonymousLogger().warning("[LDrawLib] Illegal command SAVE ("+ldrawid+
							") line#"+ ldf.getLineNumber());
					break;
				case STEP:
					Logger.getAnonymousLogger().warning("[LDrawLib] Illegal command STEP ("+ldrawid+
						") line#"+ ldf.getLineNumber());
					break;
				case TRIANGLE:
					if (! onlyMeta)
						addPart(LDrawParser.parseLineType3(l,isClockWise));
					break;
				case UNKNOWN:
					break;
				case META_UNKNOWN:
					if (firstLine) {
						description = LDrawParser.parseDescription(l);
						firstLine = false;
					}
					break;
				default:
					break;
				}
			} catch (LDrawException lde) {
				Logger.getAnonymousLogger().severe("[LDrawLib] Parsing error ("+ldrawid+
						") line#"+ ldf.getLineNumber() +" -> "+ lde.getLocalizedMessage());
			}
        }
    }


	public void setDescription(String parseDescription) {

		description = parseDescription;
	}

	
	private void writeParts(BufferedWriter bw) throws IOException {
		
		if (stepper.hasStep()) {
			for (int i=1;i<=stepper.getTotalSteps();i++) {
				List<LDPrimitive> s = stepper.getStep(i);
				if (s.size() == 0) {
					continue;
				}
				for (LDPrimitive p:s) {
					bw.write(p.getAsLdrLine());
				}
				if (i<stepper.getTotalSteps()) {
					bw.write("0 STEP\n");
				}
			}
		}
		else {
			for (LDPrimitive p:primitives){
				bw.write(p.getAsLdrLine());
			}
		}		
	}
	
	
	
	public void saveAsLdr(File f) throws IOException {
		
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));
		bw.write(getDatHeader());
		writeParts(bw);
//		if (stepper.hasStep()) {
//			for (int i=1;i<=stepper.getTotalSteps();i++) {
//				Set<LDPrimitive> s = stepper.getStep(i);
//				for (LDPrimitive p:s) {
//					bw.write(p.getAsLdrLine());
//				}
//				if (i<stepper.getTotalSteps()) {
//					bw.write("0 STEP\n");
//				}
//			}
//		}
//		else {
//			for (LDPrimitive p:primitives){
//				bw.write(p.getAsLdrLine());
//			}
//		}
		//bw.write("0 \n");
		bw.close();
	}
	
	
	
	public void saveAsMpd(File f) throws IOException {
		
		Set<String> subfiles = new HashSet<String>();
		checkIncluded(subfiles,this);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"UTF-8"));
		bw.write("0 FILE "+getLdrawId()+"\n");
		bw.write(getDatHeader());
		writeParts(bw);
//		for (LDPrimitive p:primitives){
//			bw.write(p.getAsLdrLine());
//		}
		bw.write("0 NOFILE\n");
		for (String pt: subfiles) {
			LDrawPart ldp = LDrawPart.getPart(pt);
			bw.write("0 FILE "+ldp.getLdrawId()+"\n");
			bw.write(ldp.getDatHeader());
			ldp.writeParts(bw);
//			for (LDPrimitive p:ldp.getPrimitives()) {
//				bw.write(p.getAsLdrLine());
//			}
			bw.write("0 NOFILE\n");			
		}
		bw.close();

	}
	
	
	

	public void exportAsXml(XMLStreamWriter writer) throws XMLStreamException, IOException {
		
		writer.writeCharacters("\n");
		// global start tag
		writer.writeStartElement("ldrawpart");
		writer.writeAttribute("name",getLdrawId());
		writer.writeCharacters("\n");
		for (LDPrimitive p: primitives) {
			// exports all primitives
			p.xmlWrite(writer);
		}
		writer.writeEndElement();
		writer.writeCharacters("\n");
	}
	
	

	

	private void checkIncluded(Set<String> subfiles, LDrawPart p) {
		
		//System.out.println(p.getLdrawid());  // DB
		for (LDPrimitive pt:p.getPrimitives()){
			if (LDrawPart.existsCustomPart(pt.getLdrawId()) || LDrawPart.existsInternalUsePart(pt.getLdrawId())) {
				// it is a custom part/submodel, insert in set for later saving
				subfiles.add(pt.getLdrawId());
				checkIncluded(subfiles, LDrawPart.getPart(pt.getLdrawId()));
			}
		}
	}
	
	
	private String getDatHeader() {
		
		return  "0 "+getDescription()+"\n" +
				"0 Name: "+getPartName() + "\n"+
				(getAuthor() != ""?"0 Author: "+getAuthor() +"\n":"") +
				(getPartTypeString() != ""?getPartTypeString()+"\n":"") +
				(getLicense() != ""?getLicense()+"\n":"");				
	}
	
	
	
    
}
