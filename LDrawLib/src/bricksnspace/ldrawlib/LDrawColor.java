/*
	Copyright 2014 Mario Pascucci <mpascucci@gmail.com>
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

import java.awt.Color;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * LDraw color system
 * 
 * @author Mario Pascucci
 *
 */
public class LDrawColor {
	
	
	public static final String LDRCONFIG = "LDConfig.ldr";
	public static final int INVALID_COLOR = -1;
	public static final int CURRENT = 16;
	public static final int EDGE = 24;
	// utility recurrent colors
	public static final int BLACK = 0; 
	public static final int BLUE = 1;
	public static final int GREEN = 2;
	public static final int RED = 4;
	public static final int LTGRAY = 7;
	public static final int BRGREEN = 10; 
	public static final int YELLOW = 14;
	public static final int WHITE = 15;
	public static final int TR_BROWN = 40;
	

	private int id;
	private Color c;
	private Color edge;
	private String name;
	private LDrawColorType type = LDrawColorType.USERDEF;
	private static Map<Integer,LDrawColor> ldrColors = new HashMap<Integer,LDrawColor>();

	
	private LDrawColor(int id,Color c, Color e, LDrawColorType t, String n) {
		
		this.id = id;
		this.c = c;
		type = t;
		edge = e;
		name = n;
	}
	
	
	
	static private Color getBlendedColor(int id) {
		
		if (id < 256 || id > 511)
			throw new IllegalArgumentException("[LDrawColor] Invalid blended color id.");
		int h = (id - 256) / 16;
		int l = (id - 256) % 16;
		Color ch = ldrColors.get(h).c;
		Color cl = ldrColors.get(l).c;
		return new Color((ch.getRed()+cl.getRed())/2,(ch.getGreen()+cl.getGreen())/2,(ch.getBlue()+cl.getBlue())/2);
	}
	
	
	
	/** 
	 * Returns part color identified by <b>id</b> as LDraw color index or direct color 
	 * 
	 * @param id LDraw color index or a direct color
	 * @return Color (AWT) for part or <b>Color.BLACK</b> if index is invalid/unknown
	 */
	static public Color getColorById(int id) {

		if (ldrColors.containsKey(id)) {
			Color c = ldrColors.get(id).c;
			if (c != null)
				return c;
		}
		else if (id >= 0x100 && id < 0x200) {
			// it is a blended color
			return getBlendedColor(id);
		}
		else if (id >= 0x2000000 && id < 0x3000000)
			return new Color(id-0x2000000);
		Logger.getGlobal().warning("Invalid color: "+id);
		return Color.BLACK;
	}
	
	
	/** 
	 * Returns edge color identified by <b>id</b> as LDraw color index 
	 * 
	 * @param id LDraw color index or a direct color
	 * @return Color (AWT) for edge or <b>Color.RED</b> if index is invalid
	 */
	static public Color getEdgeColorById(int id) {
		
		if (ldrColors.containsKey(id)) {
			Color c = ldrColors.get(id).edge;
			if (c != null)
				return c;
		}
		else if (id >= 0x100 && id < 0x200) {
			// it is a blended color
			Logger.getGlobal().warning("Invalid blended edge color: "+id);
			return getBlendedColor(id);
		}
		else if (id >= 0x2000000 && id < 0x3000000) {
			Logger.getGlobal().warning("Invalid direct edge color: "+id);
			// this can't  happens, but...
			return new Color(id-0x2000000);
		}
		Logger.getGlobal().warning("Invalid edge color: "+id);
		return Color.RED;
	}
	
	
	/**
	 * Checks if <b>id</b> is a valid color index in LDraw.
	 * 
	 * If <b>id</b> is a "direct color" with 0x2RRGGBB syntax return true
	 * 
	 * @param id
	 * @return <b>true</b> if index is valid
	 */
	static public boolean isLDrawColor(int id) {
		
		if (ldrColors.containsKey(id)) 
			return true;
		else if (id >= 0x100 && id < 0x200)
			// blended color
			return true;
		else if (id >= 0x2000000 && id < 0x3000000)
			// is a direct color
			return true;
		return false;
	}
	
	
	/**
	 * Checks if <b>id</b> is an official LDraw color
	 * @param id color index to check
	 * @return true if color is in library color definition file
	 */
	static public boolean isOfficialColor(int id) {
		
		return ldrColors.containsKey(id);
	}
	
	
	
	/**
	 * Returns an LDraw color, a blended color or a direct user color with index <b>id</b>
	 * 
	 * @param id LDraw color index or an int value of direct color as "0x2rrggbb" 
	 * @return LDraw color with index <b>id</b> or newly created direct color
	 */
	static public LDrawColor getById(int id) {
		
		if (ldrColors.containsKey(id)) { 
			return ldrColors.get(id);
		}
		else if (id >= 0x100 && id < 0x200) {
			// blended color
			return newLDrawColor("Blended_color_"+id,id,LDrawColorType.USERDEF,getBlendedColor(id),getColorById(EDGE));
		}
		else if (id >= 0x2000000 && id < 0x3000000) {
			// it is a direct color
			return newLDrawColor("Direct_color_"+id,id,LDrawColorType.USERDEF,new Color(id-0x2000000),getColorById(EDGE));
		}
		Logger.getGlobal().warning("[LdrawColor] Illegal color specification: "+id);
		return ldrColors.get(INVALID_COLOR);
	}

	
	
	
	public int getId() {
		return id;
	}


	public Color getColor() {
		return c;
	}


	public Color getEdge() {
		return edge;
	}


	public String getName() {
		return name;
	}

	
	/**
	 * @return the type
	 */
	public LDrawColorType getType() {
		return type;
	}



	/**
	 * @param type the type to set
	 */
	public void setType(LDrawColorType type) {
		this.type = type;
	}



	public static Set<Integer> getAllColors() {
		
		return ldrColors.keySet();
	}

	
	/**
	 * Create a new LDrawColor object
	 * No checks are done about duplicated index, color and name
	 * 
	 * @param name color identifier 
	 * @param index	integer index of color
	 * @param mainColor	an AWT Color for polygons 
	 * @param edgeColor	an AWT Color for edges
	 * @return LDrawColor object
	 * @throws IllegalArgumentException if name is empty or null, if mainColor is null, if edgeColor is null
	 */
	static LDrawColor newLDrawColor(String name, int index, LDrawColorType type, Color mainColor, Color edgeColor) {
		
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException("[LDrawLib.LDrawColor] Color identifier can't be empty");
		if (mainColor == null || edgeColor == null) 
			throw new IllegalArgumentException("[LDrawLib.LDrawColor] Main and Edge color can't be null");
		return new LDrawColor(index, mainColor, edgeColor, type, name);
	}
	
	
	
//	/**
//	 * create a new direct user color with syntax 0x2RRGGBB
//	 * 
//	 * @param dc string containing direct color in format "0x2rrggbb"
//	 * @return index for created color, i.e. int value for 0x2rrggbb
//	 * @throws IllegalArgumentException if color specification is invalid
//	 */
//	public static int newUserColor(String dc) {
//		
//		if (userColors.get(dc) == null) {
//	    	if (dc.startsWith("0x2")) {
//	    		try {
//	    			int r = Integer.parseInt(dc.substring(3, 5),16);
//	    			int g = Integer.parseInt(dc.substring(5, 7),16);
//	    			int b = Integer.parseInt(dc.substring(7),16);
//	    			int i = Integer.parseInt(dc.substring(2), 16);
//		    		LDrawColor c = new LDrawColor(i,new Color(r,g,b),Color.BLACK,"Direct Color "+dc);
//		    		userColors.put(i, c);
//		    		return i;
//	    		}
//	    		catch (NumberFormatException e) {
//	    			throw new IllegalArgumentException("[LDrawLib] Invalid direct color specification: "+dc);
//	    		}
//	    	}
//	    	else {
//	    		throw new IllegalArgumentException("[LDrawLib] Invalid direct color specification: "+dc);
//	    	}
//		}
//		else return userColors.get(dc).id;
//	}
//	
//	
	/**
	 * reads standard color list from LDConfig.ldr file inside LDraw library
	 * 
	 * @param ldlib initialized LDraw library
	 * @throws IOException if file isn't readable or there are errors in file syntax
	 * @throws IllegalArgumentException if ldlib is null
	 */
	public static void readFromLibrary(LDrawLib ldlib) throws IOException {
		
		String l;
		Map<Integer,LDrawColor> nc = new HashMap<Integer, LDrawColor>();
		
		
		if (ldlib == null) {
			throw new IllegalArgumentException("[LDRawLib.readFromLibrary] LDraw library object cannot be null");
		}
		LineNumberReader lnr = ldlib.getOfficialFile(LDRCONFIG);
		try {
			while ((l = lnr.readLine()) != null) {
				LDrawCommand cmd = LDrawParser.parseCommand(l);
				if (cmd != LDrawCommand.COLOUR)
					continue;
				try {
					LDrawColor ldc = LDrawParser.parseColour(l);
					nc.put(ldc.id, ldc);
				} catch (IllegalArgumentException e) {
					Logger.getGlobal().severe("[LDRawColor.readFromLibrary] "+LDRCONFIG+
							"(line#"+ lnr.getLineNumber()+ ") -> Unable to parse !COLOUR definition in library\n"+
							e.getLocalizedMessage());
				}
			}
		} catch (IOException exc) {
			throw new IOException("[LDrawLib] Unable to read LDraw colors\n"
					+ "File: " + LDRCONFIG + " line #: "+lnr.getLineNumber());
		}
		// put invalid color code and color schema (deep black with red lines)
		nc.put(-1, new LDrawColor(-1, Color.BLACK, Color.RED, LDrawColorType.USERDEF, "Invalid_color"));
		ldrColors = nc;
	}
	
	
}
