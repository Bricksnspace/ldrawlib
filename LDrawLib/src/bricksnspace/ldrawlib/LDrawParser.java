/*
	Copyright 2014-2015 Mario Pascucci <mpascucci@gmail.com>
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for LDraw files
 * 
 * @author mario
 *
 */
public class LDrawParser {
	
	private static Pattern filePattern = Pattern.compile("\\s*0\\s+file\\s+(.*)\\s*\\Z", 
			Pattern.CASE_INSENSITIVE);
	
	private static Pattern partPattern = Pattern.compile(
			"\\s*1\\s+" +		// line type 1 = part
			"([0-9a-fx]+)\\s+" +	// color index
			"([-.e0-9]+)\\s+" +	// x, y, z coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// a, b, c
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// d, e, f
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// g, h, i of 3x3 transformation matrix
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"(.*)\\s*\\Z",		// part/file name
			Pattern.CASE_INSENSITIVE);

	private static Pattern linePattern = Pattern.compile(
			"\\s*2\\s+" +		// line type 2 = line
			"([0-9a-fx]+)\\s+" +	// color index
			"([-.e0-9]+)\\s+" +	// x, y, z coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x1, y1, z1 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)" +
			"\\s*\\Z",
			Pattern.CASE_INSENSITIVE);

	private static Pattern trianglePattern = Pattern.compile(
			"\\s*3\\s+" +		// line type 3 = triangle
			"([0-9a-fx]+)\\s+" +	// color index
			"([-.e0-9]+)\\s+" +	// x, y, z coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x1, y1, z1 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x2, y2, z2 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)" +
			"\\s*\\Z",
			Pattern.CASE_INSENSITIVE);

	private static Pattern quadPattern = Pattern.compile(
			"\\s*4\\s+" +		// line type 4 = quad
			"([0-9a-fx]+)\\s+" +	// color index
			"([-.e0-9]+)\\s+" +	// x, y, z coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x1, y1, z1 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x2, y2, z2 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x3, y3, z3 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)" +
			"\\s*\\Z",
			Pattern.CASE_INSENSITIVE);

	private static Pattern auxLinePattern = Pattern.compile(
			"\\s*5\\s+" +		// line type 5 = line
			"([0-9a-fx]+)\\s+" +	// color index
			"([-.e0-9]+)\\s+" +	// x, y, z coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x1, y1, z1 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x2, y2, z2 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)\\s+" +	// x3, y3, z3 coordinates
			"([-.e0-9]+)\\s+" +
			"([-.e0-9]+)" +
			"\\s*\\Z",
			Pattern.CASE_INSENSITIVE);

	private static Pattern colourPattern = Pattern.compile(
			"\\s*0\\s+!COLOUR\\s+" +		// 0 !colour statement 
			"([a-z][a-z0-9_]*)\\s+",			// colour identifier
			Pattern.CASE_INSENSITIVE);

	private static Pattern historyPattern = Pattern.compile(
			"\\s*0\\s+!HISTORY\\s+" +		// 0 !history statement 
			"([0-9]{4}-[0-9]{2}-[0-9]{2})\\s+" +			// date as YYYY-MM-DD
			"(.*)\\Z",
			Pattern.CASE_INSENSITIVE);

	
	
	/**
	 * Parses a string representing a color index or a direct color with syntax "0x2RRGGBB"
	 * 
	 * @param s string contains characters to parse
	 * @return integer as color index or a direct color (0x2rrggbb -> decimal) or INVALID_COLOR
	 */
	public static int parseColorIndex(String s) {

		int color;
		
        try {
        	if (s.startsWith("0x")) {
        		// it is a direct color
        		color = Integer.parseInt(s.substring(2), 16);
        	}
        	else {
        		color = Integer.parseInt(s);
        	}
        } catch (NumberFormatException ex) {
        	Logger.getGlobal().log(Level.WARNING, "Invalid color specification: "+s+"\n"+ex.getLocalizedMessage());
        	color = LDrawColor.INVALID_COLOR;
        }
        return color;
	}
	
	
	
	/**
	 * Parses a line type 1 (a part or sub-model reference)
	 * @param l string to parse (a complete line)
	 * @param invert if part must be considered "inverted" ({@link http://www.ldraw.org/article/415})
	 * @return LDPrimitive containing part reference, transform matrix, color and inversion flag
	 */
	public static LDPrimitive parseLineType1(String l, boolean invert) {
		
        Matcher partMatch = partPattern.matcher(l);
        if (partMatch.lookingAt()) {
        	try {
	        	return LDPrimitive.newPart(
	        			parseColorIndex(partMatch.group(1)),
	        			partMatch.group(14).trim(),
	        			invert,
	                	Float.parseFloat(partMatch.group(5)), 	// a 
	                	Float.parseFloat(partMatch.group(6)),	// b
	                	Float.parseFloat(partMatch.group(7)),	// c 
	                	Float.parseFloat(partMatch.group(8)), 	// d
	                	Float.parseFloat(partMatch.group(9)),	// e 
	                	Float.parseFloat(partMatch.group(10)),	// f 
	                	Float.parseFloat(partMatch.group(11)),	// g
	                	Float.parseFloat(partMatch.group(12)),	// h
	                	Float.parseFloat(partMatch.group(13)), 	// i
	                	Float.parseFloat(partMatch.group(2)), 	// x
	                	Float.parseFloat(partMatch.group(3)), 	// y
	                	Float.parseFloat(partMatch.group(4))  	// z
	                	);
        	}
        	catch (NumberFormatException ex) {
        		throw new IllegalArgumentException("Invalid number: ",ex);
        	}
        	catch (IndexOutOfBoundsException ex) {
        		throw new IllegalArgumentException("Invalid line type 1: "+ex.getLocalizedMessage()+"\n"+
        				partMatch.groupCount()+" - "+partMatch.group());
        	}
        }
        else {
    		throw new IllegalArgumentException("Parse error: "+l);
        }
	}
	
	
	
	
	/**
	 * Parses a line type 2 (a two point line)
	 * @param l string to parse (a complete line)
	 * @return LDPrimitive containing line points and color
	 */
	public static LDPrimitive parseLineType2(String l) {
		
        Matcher lineMatch = linePattern.matcher(l);
        if (lineMatch.lookingAt()) {
        	try {
	        	return LDPrimitive.newLine(
	        			parseColorIndex(lineMatch.group(1)),
	                	Float.parseFloat(lineMatch.group(2)), 	// x 
	                	Float.parseFloat(lineMatch.group(3)),	// y
	                	Float.parseFloat(lineMatch.group(4)),	// z 
	                	Float.parseFloat(lineMatch.group(5)), 	// x1
	                	Float.parseFloat(lineMatch.group(6)),	// y1
	                	Float.parseFloat(lineMatch.group(7))	// z1
	                	);
        	}
        	catch (NumberFormatException ex) {
        		throw new IllegalArgumentException("Invalid number: ",ex);
        	}
        	catch (IndexOutOfBoundsException ex) {
        		throw new IllegalArgumentException("Invalid line type 2: "+ex.getLocalizedMessage());
        	}
        }
        else {
        	throw new IllegalArgumentException("Parse error: "+l);
        }
	}

	

	/**
	 * Parses a line type 3 (a triangle)
	 * @param l string to parse (a complete line)
	 * @param invert if triangle must be considered "inverted" ({@link http://www.ldraw.org/article/415})
	 * @return LDPrimitive containing triangle points, inversion flag and color
	 */
	public static LDPrimitive parseLineType3(String l, boolean invert) {
		
        Matcher triangleMatch = trianglePattern.matcher(l);
        if (triangleMatch.lookingAt()) {
        	try {
	        	return LDPrimitive.newTriangle(
	        			parseColorIndex(triangleMatch.group(1)),
	        			invert,
	                	Float.parseFloat(triangleMatch.group(2)), 	// x 
	                	Float.parseFloat(triangleMatch.group(3)),	// y
	                	Float.parseFloat(triangleMatch.group(4)),	// z 
	                	Float.parseFloat(triangleMatch.group(5)), 	// x1
	                	Float.parseFloat(triangleMatch.group(6)),	// y1
	                	Float.parseFloat(triangleMatch.group(7)),	// z1
	                	Float.parseFloat(triangleMatch.group(8)), 	// x2
	                	Float.parseFloat(triangleMatch.group(9)),	// y2
	                	Float.parseFloat(triangleMatch.group(10))	// z2
	                	);
        	}
        	catch (NumberFormatException ex) {
        		throw new IllegalArgumentException("Invalid number: ",ex);
        	}
        	catch (IndexOutOfBoundsException ex) {
        		throw new IllegalArgumentException("Invalid line type 3: "+ex.getLocalizedMessage());
        	}
        }
        else {
        	throw new IllegalArgumentException("Parse error: "+l);
        }
	}

	
	
	/**
	 * Parses a line type 4 (a quad)
	 * @param l string to parse (a complete line)
	 * @param invert if quad must be considered "inverted" ({@link http://www.ldraw.org/article/415})
	 * @return LDPrimitive containing quad points, inversion flag and color
	 */
	public static LDPrimitive parseLineType4(String l, boolean invert) {
		
        Matcher quadMatch = quadPattern.matcher(l);
        if (quadMatch.lookingAt()) {
        	try {
	        	return LDPrimitive.newQuad(
	        			parseColorIndex(quadMatch.group(1)),
	        			invert,
	                	Float.parseFloat(quadMatch.group(2)), 	// x 
	                	Float.parseFloat(quadMatch.group(3)),	// y
	                	Float.parseFloat(quadMatch.group(4)),	// z 
	                	Float.parseFloat(quadMatch.group(5)), 	// x1
	                	Float.parseFloat(quadMatch.group(6)),	// y1
	                	Float.parseFloat(quadMatch.group(7)),	// z1
	                	Float.parseFloat(quadMatch.group(8)), 	// x2
	                	Float.parseFloat(quadMatch.group(9)),	// y2
	                	Float.parseFloat(quadMatch.group(10)),	// z2
	                	Float.parseFloat(quadMatch.group(11)), 	// x3
	                	Float.parseFloat(quadMatch.group(12)),	// y3
	                	Float.parseFloat(quadMatch.group(13))	// z3
	                	);
        	}
        	catch (NumberFormatException ex) {
        		throw new IllegalArgumentException("Invalid number: ",ex);
        	}
        	catch (IndexOutOfBoundsException ex) {
        		throw new IllegalArgumentException("Invalid line type 4: "+ex.getLocalizedMessage());
        	}
        }
        else {
        	throw new IllegalArgumentException("Parse error: "+l);
        }
	}


	
	/**
	 * Parses a line type 5 (a two point auxiliary line)
	 * @param l string to parse (a complete line)
	 * @return LDPrimitive containing line points and color
	 */
	public static LDPrimitive parseLineType5(String l) {
		
        Matcher auxLineMatch = auxLinePattern.matcher(l);
        if (auxLineMatch.lookingAt()) {
        	try {
	        	return LDPrimitive.newAuxLine(
	        			parseColorIndex(auxLineMatch.group(1)),
	                	Float.parseFloat(auxLineMatch.group(2)), 	// x 
	                	Float.parseFloat(auxLineMatch.group(3)),	// y
	                	Float.parseFloat(auxLineMatch.group(4)),	// z 
	                	Float.parseFloat(auxLineMatch.group(5)), 	// x1
	                	Float.parseFloat(auxLineMatch.group(6)),	// y1
	                	Float.parseFloat(auxLineMatch.group(7)),	// z1
	                	Float.parseFloat(auxLineMatch.group(8)), 	// x2
	                	Float.parseFloat(auxLineMatch.group(9)),	// y2
	                	Float.parseFloat(auxLineMatch.group(10)),	// z2
	                	Float.parseFloat(auxLineMatch.group(11)), 	// x3
	                	Float.parseFloat(auxLineMatch.group(12)),	// y3
	                	Float.parseFloat(auxLineMatch.group(13))	// z3
	                	);
        	}
        	catch (NumberFormatException ex) {
        		throw new IllegalArgumentException("Invalid number: ",ex);
        	}
        	catch (IndexOutOfBoundsException ex) {
        		throw new IllegalArgumentException("Invalid line type 5: "+ex.getLocalizedMessage());
        	}
        }
        else {
        	throw new IllegalArgumentException("Parse error: "+l);
        }
	}

	
	private static int searchToken(String[] l, String token) {
		
		int i;
		
		for (i=0;i<l.length;i++) {
			if (l[i].equalsIgnoreCase(token))
				return i<l.length-1?i+1:-1;
		}
		return -1;
	}
	

	
	private static int searchKeyword(String[] l, String kw) {
		
		int i;
		
		for (i=0;i<l.length;i++) {
			if (l[i].equalsIgnoreCase(kw))
				return i;
		}
		return -1;
	}
	

	
	public static LDrawCommand parseCommand(String l) {
		
	    String[] ld = l.trim().split("\\s+");
	    if (ld.length < 1) {
	        return LDrawCommand.EMPTY;
	    }
	    else if (ld.length == 1) {
	    	return LDrawCommand.COMMENT;
	    }
	    else if (ld[0].equals("0")) {
	        if (ld[1].equals("//")) {
	        	// it is a comment
	            return LDrawCommand.COMMENT;
	        }
	        else if (ld[1].equalsIgnoreCase("!colour")) {
	        	// it is a colour spec
	        	return LDrawCommand.COLOUR;
	        }
	        else if (ld[1].equalsIgnoreCase("bfc")) {
	        	if (ld[2].equalsIgnoreCase("invertnext"))
	        		return LDrawCommand.BFC_INVERTNEXT;
	        	if (ld[2].equalsIgnoreCase("certify")) { 
                    if (ld.length == 4 && ld[3].equalsIgnoreCase("cw")) {
                        return LDrawCommand.BFC_CW;
                    }
                    else if (ld.length == 4 && ld[3].equalsIgnoreCase("ccw")) {
                    	return LDrawCommand.BFC_CCW;
                    }
                    else if (ld.length == 4 && ld[3].equalsIgnoreCase("invertnext")) {
                    	Logger.getGlobal().warning("Deprecated command BFC CERTIFY INVERTNEXT");
                    	return LDrawCommand.BFC_INVERTNEXT;
                    }
	        	}
                if (ld[2].equalsIgnoreCase("cw"))
                    	return LDrawCommand.BFC_CW;
                if (ld[2].equalsIgnoreCase("ccw"))
                	return LDrawCommand.BFC_CCW;
                return LDrawCommand.COMMENT;
	        }
	        else if (ld[1].equalsIgnoreCase("file")) {
	        	return LDrawCommand.MPDFILE;
	        }
	        else if (ld[1].equalsIgnoreCase("nofile")) {
	        	return LDrawCommand.MPDNOFILE;
	        }
	        else if (ld[1].equalsIgnoreCase("!category")) {
	        	return LDrawCommand.CATEGORY;
	        }
	        else if (ld[1].equalsIgnoreCase("!keywords")) {
	        	return LDrawCommand.KEYWORDS;
	        }
	        else if (ld[1].equalsIgnoreCase("!license")) {
	        	return LDrawCommand.LICENSE;
	        }
	        else if (ld[1].equalsIgnoreCase("name:")) {
	        	return LDrawCommand.NAME;
	        }
	        else if (ld[1].equalsIgnoreCase("author:")) {
	        	return LDrawCommand.AUTHOR;
	        }
	        else if (ld[1].equalsIgnoreCase("step")) {
	        	return LDrawCommand.STEP;
	        }
	        else if (ld[1].equalsIgnoreCase("!history")) {
	        	return LDrawCommand.HISTORY;
	        }
	        else if (ld[1].equalsIgnoreCase("!LDRAW_ORG") || ld[1].equalsIgnoreCase("LDRAW_ORG") ||
	        		ld[1].equalsIgnoreCase("Official") || ld[1].equalsIgnoreCase("Unofficial") ||
	        		ld[1].equalsIgnoreCase("Un-official")) {
	        	return LDrawCommand.FILETYPE;
	        }
	        return LDrawCommand.META_UNKNOWN;
	    }
	    else if (ld[0].equals("1")) {
	    	return LDrawCommand.REFERENCE;
	    }
	    else if (ld[0].equals("2")) {
	    	return LDrawCommand.LINE;
	    }
	    else if (ld[0].equals("3")) {
	    	return LDrawCommand.TRIANGLE;
	    }
	    else if (ld[0].equals("4")) {
	    	return LDrawCommand.QUAD;
	    }
	    else if (ld[0].equals("5")) {
	    	return LDrawCommand.AUXLINE;
	    }
	    return LDrawCommand.UNKNOWN;
	}

	
	/**
	 * Return description: must be the first non-empty line in a LDraw file
	 * @param l
	 * @return description as a string
	 */
	public static String parseDescription(String l) {
		
		try {
			return l.substring(l.indexOf('0')+1).trim();
		}
		catch (IndexOutOfBoundsException ex) {
			return "";
		}
	}
	
	
	public static String parseMetaUnk(String l) {

		try {
			return l.substring(l.indexOf("0")+1).trim();
		}
		catch (IndexOutOfBoundsException ex) {
			return "";
		}
	}
	
	
	
	public static String parseCategory(String l) {

		try {
			return l.substring(l.toUpperCase().indexOf("!CATEGORY")+10).trim();
		}
		catch (IndexOutOfBoundsException ex) {
			return "";
		}
	}
	
	
	public static String parseKeywords(String l) {
		
		try {
			return l.substring(l.toUpperCase().indexOf("!KEYWORDS")+10).trim();
		}
		catch (IndexOutOfBoundsException ex) {
			return "";
		}
	}
	
	
	public static String parsePartName(String l) {
		
		try {
			return l.substring(l.toUpperCase().indexOf("NAME:")+6).trim();
		}
		catch (IndexOutOfBoundsException ex) {
			return "";
		}
	}
	
	

	public static String parseAuthor(String l) {
		
		try {
			return l.substring(l.toUpperCase().indexOf("AUTHOR:")+8).trim();
		}
		catch (IndexOutOfBoundsException ex) {
			return "";
		}
	}
	
	
	
	public static LDrawPartType parsePartType(String l) {
		
		boolean official = true;
	    String[] ld = l.trim().split("\\s+");
	    if (ld.length <= 2) {
	    	Logger.getGlobal().warning("Malformed part type command: " + l);
	    	return LDrawPartType.MODEL;
	    }
	    if (ld.length >= 4 && ld[1].equalsIgnoreCase("Official") && ld[2].equalsIgnoreCase("LCAD")) {
	    	if (ld.length < 5) {
	    		Logger.getGlobal().warning("Malformed part type command: " + l);
		    	return LDrawPartType.MODEL;
	    	}
	    }
	    if (ld[1].equalsIgnoreCase("Unofficial") || ld[1].equalsIgnoreCase("Un-official")) {
	    	official = false;
	    }
	    String type;
	    if (ld[1].equalsIgnoreCase("Official") && ld[2].equalsIgnoreCase("LCAD")) {
	    	type = ld[3]; 
	    }
	    else {
	    	type = ld[2];
	    }
	    if (type.equalsIgnoreCase("Part")) {
	    	if (official)
	    		return LDrawPartType.OFFICIAL;
	    	else 
	    		return LDrawPartType.UNOFFICIAL;
	    }
	    else if (type.equalsIgnoreCase("Subpart")) {
	    	if (official)
	    		return LDrawPartType.SUBPART;
	    	else 
	    		return LDrawPartType.UNOFF_SUB;
	    }
	    else if (type.equalsIgnoreCase("Primitive")) {
	    	if (official)
	    		return LDrawPartType.PRIMITIVE;
	    	else 
	    		return LDrawPartType.UNOFF_PRIM;
	    }
	    else if (type.equalsIgnoreCase("48_Primitive") ||
	    		type.equalsIgnoreCase("Hi-Res")) {
	    	if (official)
	    		return LDrawPartType.PRIMITIVE48;
	    	else 
	    		return LDrawPartType.UNOFF48;
	    }
	    else if (type.equalsIgnoreCase("8_Primitive")) {
	    	if (official)
	    		return LDrawPartType.PRIMITIVE8;
	    	else 
	    		return LDrawPartType.UNOFF8;
	    }
	    else if (type.equalsIgnoreCase("Shortcut")) {
	    	if (official) 
	    		return LDrawPartType.SHORTCUT;
	    	else
	    		return LDrawPartType.UNOFF_SHORTCUT;
	    }
	    else if (type.equalsIgnoreCase("File")) {
	    	return LDrawPartType.MODEL;
	    }
	    else if (type.equalsIgnoreCase("Model")) {
    		return LDrawPartType.MODEL;
	    }
	    else if (type.equalsIgnoreCase("Submodel")) {
    		return LDrawPartType.SUBMODEL;
	    }
	    else if (type.equalsIgnoreCase("Element")) {
	    	Logger.getGlobal().warning("Deprecated/obsolete type: " + l);
	    	if (official)
	    		return LDrawPartType.SUBPART;
	    	else
	    		return LDrawPartType.UNOFF_SUB;
	    }
	    else if (type.equalsIgnoreCase("Sub-part")) {
	    	Logger.getGlobal().warning("Deprecated/obsolete type: " + l);
	    	if (official)
	    		return LDrawPartType.SUBPART;
	    	else 
	    		return LDrawPartType.UNOFF_SUB;
	    }
	    else if (type.equalsIgnoreCase("Alias")) {
	    	Logger.getGlobal().warning("Deprecated/obsolete type: " + l);
	    	if (official)
	    		return LDrawPartType.OFFICIAL;
	    	else 
	    		return LDrawPartType.UNOFFICIAL;
	    }
	    else if (type.equalsIgnoreCase("Cross-reference")) {
	    	Logger.getGlobal().warning("Deprecated/obsolete type: " + l);
	    	if (official)
	    		return LDrawPartType.SUBPART;
	    	else 
	    		return LDrawPartType.UNOFF_SUB;
	    }
	    else if (type.equalsIgnoreCase("Unofficial_Part")) {
	    	return LDrawPartType.UNOFFICIAL;
	    }
	    else if (type.equalsIgnoreCase("Unofficial_Subpart")) {
	    	return LDrawPartType.UNOFF_SUB;
	    }
	    else if (type.equalsIgnoreCase("Unofficial_Primitive")) {
	    	return LDrawPartType.UNOFF_PRIM;
	    }
	    else if (type.equalsIgnoreCase("Unofficial_48_Primitive")) {
	    	return LDrawPartType.UNOFF48;
	    }
	    else if (type.equalsIgnoreCase("Unofficial_8_Primitive")) {
	    	return LDrawPartType.UNOFF8;
	    }
	    else if (type.equalsIgnoreCase("Unofficial_Shortcut")) {
	    	return LDrawPartType.UNOFF_SHORTCUT;
	    }
	    else if (type.equalsIgnoreCase("Unofficial_Model")) {
	    	return LDrawPartType.MODEL;
	    }
	    return LDrawPartType.MODEL;
	}
	
	
	
	public static Date parseHistory(String l) {
		
		Matcher dateMatch = historyPattern.matcher(l);
        if (dateMatch.lookingAt()) {
        	try {
        		String d = dateMatch.group(1).trim();
        		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        		formatter.setLenient(false);
        		Date date = formatter.parse(d);
        		return date;
        	}
        	catch (IndexOutOfBoundsException ex) {
        		throw new IllegalArgumentException("No date in !HISTORY line: "+l);
        	} catch (ParseException e) {
        		throw new IllegalArgumentException("Invalid !HISTORY date: "+l);
			}
        }
        else {
    		throw new IllegalArgumentException("Invalid !HISTORY meta: "+l);
        }
		
	}

	
	public static String parseMpdFile(String l) {
		
		String file;
		Matcher fileMatch = filePattern.matcher(l);
        if (fileMatch.lookingAt()) {
        	try {
        		file = fileMatch.group(1).trim();
        	}
        	catch (IndexOutOfBoundsException ex) {
        		throw new IllegalArgumentException("Invalid MPD filename: "+l);
        	}
        }
        else {
    		throw new IllegalArgumentException("Invalid MPD filename: "+l);
        }
        return file;
	}

	
	/**
	 * Parses a !COLOUR statement
	 * @param l string containing statement
	 * @return an LDrawColor object
	 */
	public static LDrawColor parseColour(String l) {
		
		String[] ld = l.trim().split("\\s+");

		Matcher colorMatch = colourPattern.matcher(l);
		LDrawColorType type = LDrawColorType.SOLID;
        if (colorMatch.lookingAt()) {
        	String name = colorMatch.group(1).trim();	// colour identifier
        	int idx = searchToken(ld,"code");
        	int cx = searchToken(ld,"value");
        	int ex = searchToken(ld, "edge");
        	int ax = searchToken(ld, "alpha");
        	if (searchKeyword(ld, "rubber") >= 0) {
        		type = LDrawColorType.RUBBER;
        	}
        	else if (searchKeyword(ld, "chrome") >= 0) {
        		type = LDrawColorType.CHROME;
        	}
        	else if (searchKeyword(ld, "metal") >= 0) {
        		type = LDrawColorType.METAL;
        	}
        	else if (searchKeyword(ld, "pearlescent") >= 0) {
        		type = LDrawColorType.PEARL;
        	}
        	else if (searchKeyword(ld, "glitter") >= 0 || searchKeyword(ld, "speckle") >= 0) {
        		type = LDrawColorType.GLITTER;
        	}
//        	else if (searchToken(ld, "speckle") >= 0) {
//        		type = LDrawColorType.SPECKLE;
//        	}
        	if (idx < 0 || cx < 0 || ex < 0) {
        		throw new IllegalArgumentException("Invalid !COLOUR specification: " + l);
        	}
    		int a = 255;
    		int code = LDrawColor.INVALID_COLOR;
    		if (ax >= 0) {
    			try {
    				a = Integer.parseInt(ld[ax]);
    			}
    			catch (NumberFormatException exc) {
    				throw new IllegalArgumentException("Invalid ALPHA value: " +ld[ax]);
    			}
    		}
    		if (a == 240 && type == LDrawColorType.SOLID) {
    			type = LDrawColorType.MILKY;
    		}
    		if (a == 128 && type == LDrawColorType.SOLID) {
    			type = LDrawColorType.TRANSPARENT;
    		}
    		try {
    			code = Integer.parseInt(ld[idx]);
    		}
    		catch (NumberFormatException exc) {
    			throw new IllegalArgumentException("Invalid CODE value: "+ld[idx]);
    		}
    		Color c, e;
    		try {
        		int r = Integer.parseInt(ld[cx].substring(1, 3),16);
        		int g = Integer.parseInt(ld[cx].substring(3, 5),16);
        		int b = Integer.parseInt(ld[cx].substring(5),16);
        		c = new Color(r,g,b,a);
    		}
    		catch (NumberFormatException exc) {
    			throw new IllegalArgumentException("Invalid VALUE format: "+ld[cx]);
    		}
    		try {
    			if (ld[ex].startsWith("#")) {
    				// it is a RGB color in #RRGGBB hex format
    				int r = Integer.parseInt(ld[ex].substring(1, 3),16);
    				int g = Integer.parseInt(ld[ex].substring(3, 5),16);
    				int b = Integer.parseInt(ld[ex].substring(5),16);
    				e = new Color(r,g,b,a);
    			}
    			else {
    				// it is an LDraw color code
    				e = LDrawColor.getColorById(Integer.parseInt(ld[ex]));
    				if (e == null) {
    					throw new IllegalArgumentException("Invalid EDGE color reference: "+ld[ex]);
    				}
    			}
    		}
    		catch (NumberFormatException nfe) {
    			throw new IllegalArgumentException("Invalid EDGE format: "+ld[ex]);
    		}
    		if (code == 16 || code == 32 || code == 24 || code == 493 || code == 494 || code == 495) {
    			type = LDrawColorType.INTERNAL;
    		}
    		return LDrawColor.newLDrawColor(name, code, type, c, e);
	    }
        throw new IllegalArgumentException("Invalid !COLOUR definition: "+l);
	}
	
	
	
}
