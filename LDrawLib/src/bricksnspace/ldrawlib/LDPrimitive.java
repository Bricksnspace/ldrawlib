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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import bricksnspace.j3dgeom.Matrix3D;
import bricksnspace.j3dgeom.Point3D;



/**
 * 
 * class to store primitive drawing instructions for LDraw parts
 * 
 * @author Mario Pascucci
 *
 */
public class LDPrimitive {
	
	private int id;
	private LDrawCommand type;
	private String partId;
	private LDrawColor color;				// color for !COLOUR primitive
	private int colorIndex;					// color for primitive (as LDraw id)
	private int step = 0;						// step index
	private float[] vertex = null;			/*
	 							 * vertices stored as follow:
	 							 * p[0] = x1, p[1] = y1, p[2] = z1
	 							 * p[3] = x2, p[4] = y2, p[5] = z2
	 							 * ...
	 							 */
	public static final Matrix3D IDENTITY = new Matrix3D(); 
	private Matrix3D matrix = IDENTITY;			// transformation matrix
	private boolean invert;
	//private LDrawPart ldrPart;
	private ConnectionPoint[] connPoints; 
	
	// global id
	private static int globalId = 0;
//	private static Map<Integer,LDPrimitive> globalList = new HashMap<Integer,LDPrimitive>();

	
	
	private LDPrimitive(LDPrimitive p) {
		
		type = p.getType();
		partId = p.getLdrawId();
		color = p.getColor();
		colorIndex = p.getColorIndex();
		invert = p.isInvert();
//		if (p.getNormalFV() != null)
//			normal = p.getNormalFV().clone();
		if (p.getPointsFV() != null)
			vertex = p.getPointsFV().clone();
		if (p.getType() == LDrawCommand.REFERENCE)
			matrix = p.getTransformation().getCopy();
		//ldrPart = p.ldrPart;
		id = p.id;
		step = p.step;
		if (p.connPoints != null) {
			connPoints = p.connPoints.clone();
		}
	}
	
	
	private LDPrimitive(String ldrid) {
		
		type = LDrawCommand.REFERENCE;
		partId = ldrid;
		colorIndex = LDrawColor.INVALID_COLOR;
		invert = false;
		matrix = new Matrix3D();
	}
	
	
	// private constructor (as Effective Java 2nd ed. Items 1,13,14,15) 
	private LDPrimitive(LDrawCommand type, int c, String ldrid) {
		
		id = getUniqueId();
		this.type = type;
		switch (this.type) {
		case LINE:
			vertex = new float[6];
			break;
		case TRIANGLE:
			vertex = new float[9];
			break;
		case QUAD:
			vertex = new float[12];
		case REFERENCE:
			partId = ldrid;
			break;
		case AUXLINE:
			vertex = new float[12];
			break;
		case STEP:
		case BFC_INVERTNEXT:
		case BFC_CCW:
		case BFC_CW:
		case COMMENT:
		case KEYWORDS:
		case META_UNKNOWN:
		case CATEGORY:
		case EMPTY:
		case HISTORY:
			c = LDrawColor.INVALID_COLOR;
			break;
		case UNKNOWN:
			partId = ldrid;
			c = LDrawColor.INVALID_COLOR;
			break;
		default:
			throw new IllegalArgumentException("[LDPrimitive] Primitive type not allowed: "+this.type.toString());
		}
		//ldrPart = LDrawPart.getPartFromPrimitive(this);
		this.colorIndex = c;
	}

	
	private LDPrimitive(LDrawColor c) {
		
		if (c == null) {
			throw new NullPointerException("[LDPrimitive] Primitive color type cannot be null");
		}
		id = getUniqueId();
		partId = "Local color "+c.getId();
		type = LDrawCommand.COLOUR;
		color = c;
		//ldrPart = LDrawPart.getPartFromPrimitive(this);
	}

	
	private static synchronized int getUniqueId() {
		
		return ++globalId;
	}
	
	
	/**
	 * returns true if primitive is a geometric or reference "line" in DAT file
	 * @return true if "drawable"
	 */
	public boolean isDrawable() {
		
		return type == LDrawCommand.REFERENCE || type == LDrawCommand.TRIANGLE 
				|| type == LDrawCommand.QUAD || type == LDrawCommand.LINE 
				|| type == LDrawCommand.AUXLINE;
	}

	
	@Override
	public String toString() {
		return "LDPrimitive [id="+id+" t=" + type + ", partId=" + partId + ", c_idx=" + colorIndex
				+ ", p=" + Arrays.toString(vertex)
				+ ", invert=" + invert + "]";
	}

	
	public static LDPrimitive cmdColour(LDrawColor c) {
		
		return new LDPrimitive(c);
	}
	
	
	public static LDPrimitive newColour(LDrawColor c, String line) {
		
		LDPrimitive l = new LDPrimitive(c);
		l.partId = line;
		return l;
	}
	
	
	public static LDPrimitive errorPrimitive(String why) {
		
		return new LDPrimitive(LDrawCommand.UNKNOWN,LDrawColor.INVALID_COLOR,why);
	}
	


	public static LDPrimitive newLine(int color, 
			float x1, float y1, float z1,
			float x2, float y2, float z2) {
		
		LDPrimitive l = new LDPrimitive(LDrawCommand.LINE,color,null);
		l.vertex[0] = x1;
		l.vertex[1] = y1;
		l.vertex[2] = z1;
		l.vertex[3] = x2;
		l.vertex[4] = y2;
		l.vertex[5] = z2;
		return l;
	}

	
	public static LDPrimitive newAuxLine(int color, 
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3,
			float x4, float y4, float z4) {
		
		LDPrimitive l = new LDPrimitive(LDrawCommand.AUXLINE,color,null);
		l.vertex[0] = x1;
		l.vertex[1] = y1;
		l.vertex[2] = z1;
		l.vertex[3] = x2;
		l.vertex[4] = y2;
		l.vertex[5] = z2;
		l.vertex[6] = x3;
		l.vertex[7] = y3;
		l.vertex[8] = z3;
		l.vertex[9] = x4;
		l.vertex[10] = y4;
		l.vertex[11] = z4;
		return l;
	}

	
	/*
	 * design primitives
	 */
	
//	/**
//	 * creates a new STEP primitive
//	 * @return
//	 */
//	public static LDPrimitive newStep() {
//		return new LDPrimitive(LDrawCommand.STEP,LDrawColor.INVALID_COLOR,null);
//	}
	
	
	public static LDPrimitive newBfcInvertnext() {
		return new LDPrimitive(LDrawCommand.BFC_INVERTNEXT,LDrawColor.INVALID_COLOR,null);
	}
	
	public static LDPrimitive newBfcCw() {
		return new LDPrimitive(LDrawCommand.BFC_CW,LDrawColor.INVALID_COLOR,null);
	}
	
	public static LDPrimitive newBfcCcw() {
		return new LDPrimitive(LDrawCommand.BFC_CCW,LDrawColor.INVALID_COLOR,null);
	}
	
	public static LDPrimitive newEmpty() {
		return new LDPrimitive(LDrawCommand.EMPTY,LDrawColor.INVALID_COLOR,null);
	}
	
	public static LDPrimitive newKeywords(String k) {
		LDPrimitive l = new LDPrimitive(LDrawCommand.KEYWORDS,LDrawColor.INVALID_COLOR,null);
		l.partId = k;
		return l;
	}
	
	public static LDPrimitive newCategory(String k) {
		LDPrimitive l = new LDPrimitive(LDrawCommand.CATEGORY,LDrawColor.INVALID_COLOR,null);
		l.partId = k;
		return l;
	}
	
	public static LDPrimitive newComment(String k) {
		LDPrimitive l = new LDPrimitive(LDrawCommand.COMMENT,LDrawColor.INVALID_COLOR,null);
		l.partId = k;
		return l;
	}
	
	public static LDPrimitive newHistory(String k) {
		LDPrimitive l = new LDPrimitive(LDrawCommand.HISTORY,LDrawColor.INVALID_COLOR,null);
		l.partId = k;
		return l;
	}
	
	public static LDPrimitive newMetaUnk(String k) {
		LDPrimitive l = new LDPrimitive(LDrawCommand.META_UNKNOWN,LDrawColor.INVALID_COLOR,null);
		l.partId = k;
		return l;
	}
	

	
//	/* 
//	 * compute normal, not normalized.
//	 */
//	public static float[] calcNormal(
//			float x,float y,float z,
//			float x1,float y1,float z1,
//			float x2,float y2,float z2) {
//
//		float v1x,v1y,v1z,v2x,v2y,v2z,xn,yn,zn;
//
//		v1x = x1 - x;
//		v1y = y1 - y;
//		v1z = z1 - z;
//		v2x = x2 - x1;
//		v2y = y2 - y1;
//		v2z = z2 - z1;
//		xn = v1y * v2z - v1z * v2y;
//		yn = v1z * v2x - v1x * v2z;
//		zn = v1x * v2y - v1y * v2x;
//		return new float[] {xn,yn,zn};
//	}


	
	public static LDPrimitive newTriangle(int color, boolean isClockWise,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3) {
		
		LDPrimitive l = new LDPrimitive(LDrawCommand.TRIANGLE,color,null);
		l.vertex[0] = x1;
		l.vertex[1] = y1;
		l.vertex[2] = z1;
		l.vertex[3] = x2;
		l.vertex[4] = y2;
		l.vertex[5] = z2;
		l.vertex[6] = x3;
		l.vertex[7] = y3;
		l.vertex[8] = z3;
		l.invert = isClockWise;
//		if (!isClockWise) {
//			l.normal = calcNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3);
//		}
//		else {
//			l.normal = calcNormal(x3, y3, z3, x2, y2, z2, x1, y1, z1);
//		}
		return l;
	}
	


	public static LDPrimitive newQuad(int color, boolean isClockWise,
			float x1, float y1, float z1,
			float x2, float y2, float z2,
			float x3, float y3, float z3,
			float x4, float y4, float z4) {
		
		LDPrimitive l = new LDPrimitive(LDrawCommand.QUAD,color,null);
		l.vertex[0] = x1;
		l.vertex[1] = y1;
		l.vertex[2] = z1;
		l.vertex[3] = x2;
		l.vertex[4] = y2;
		l.vertex[5] = z2;
		l.vertex[6] = x3;
		l.vertex[7] = y3;
		l.vertex[8] = z3;
		l.vertex[9] = x4;
		l.vertex[10] = y4;
		l.vertex[11] = z4;
		l.invert = isClockWise;
//		if (!isClockWise) {
//			l.normal = calcNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3);
//		}
//		else {
//			l.normal = calcNormal(x3, y3, z3, x2, y2, z2, x1, y1, z1);
//		}
		return l;
	}
	

	
	public static LDPrimitive newGlobalPart(String id, int color, Matrix3D m) {

		LDPrimitive l = new LDPrimitive(LDrawCommand.REFERENCE,color,id);
		l.invert = false;
		l.matrix = m.getCopy();
		// part has connection points, must be computed
		l.connPoints = ConnectionPoint.getConnections(l);
		return l;
	}
	

	public static LDPrimitive newPart(int color, String id, boolean invert,
			float a, float b, float c,
			float d, float e, float f,
			float g, float h, float i,
			float x, float y, float z
			) {
		
		LDPrimitive l = new LDPrimitive(LDrawCommand.REFERENCE,color,id);
		l.invert = invert;
		l.matrix = new Matrix3D(
				a,b,c,
				d,e,f,
				g,h,i,
				x,y,z);
		l.connPoints = ConnectionPoint.getConnections(l);
		return l;
	}
	
	
	public static LDPrimitive getDummyPart(String id) {
		
		return new LDPrimitive(id);
	}
	
	
    public List<LDPrimitive> getPrimitives() {
    	
    	List<LDPrimitive> primitives = new ArrayList<LDPrimitive>();
		primitives.add(this);
		return primitives;
    }
    

//    public static LDPrimitive getByGlobalId(int gid) {
//    	
//    	return globalList.get(gid);
//    }
//	
//	
    /**
     *  returns an exact clone, same global id 
     */
	public LDPrimitive getClone() {
		
		return new LDPrimitive(this);
	}
	

	/** 
	 * return a copy, clone with new global id
	 * @return
	 */
	public LDPrimitive getCopy() {
		
		LDPrimitive p = new LDPrimitive(this);
		p.id = getUniqueId();
		return p;
	}
	

	public int getId() {
		return id;
	}
	

	public LDrawColor getColor() {
		
		return color;
	}
	
	
	
	public int getColorIndex() {
		
		return colorIndex;
	}
	
	
	public LDPrimitive setColorIndex(int index) {
		
		LDPrimitive p = new LDPrimitive(this);
		p.colorIndex = index;
		return p;
	}

	
	public int getStep() {
		return step;
	}


	public void setStep(int step) {
		this.step = step;
	}


	public String getLdrawId() {
		
		return partId;
	}
	
	
	public Matrix3D getTransformation() {
		
		if (type != LDrawCommand.REFERENCE)
			throw new IllegalArgumentException("Line type != 1 doesn't have a transformation matrix");
		return matrix;
	}
	
	/**
	 * Add a transformation to this primitive
	 * Also compute transformation for connection points, if any 
	 * @param m transformation matrix
	 */
	public LDPrimitive transform(Matrix3D m) {
		
		if (type != LDrawCommand.REFERENCE)
			throw new IllegalArgumentException("Cannot set a transformation for line type != 1");
		LDPrimitive p = new LDPrimitive(this);
		p.matrix = matrix.transform(m);
		if (type == LDrawCommand.REFERENCE && connPoints != null && connPoints.length > 0) {
			// part has connection points, must be recomputed
			p.recalcConnPoints();
		}
		return p;
	}
	
	
	/**
	 * Set transformation for this primitive, replacing previous transformation
	 * Also compute transformation for connection points, if any 
	 * @param m new transformation matrix
	 */
	public LDPrimitive setTransform(Matrix3D m) {
		
		if (type != LDrawCommand.REFERENCE)
			throw new IllegalArgumentException("Cannot set a transformation for line type != 1");
		LDPrimitive p = new LDPrimitive(this);
		p.matrix = m.getCopy();
		if (type == LDrawCommand.REFERENCE && connPoints != null && connPoints.length > 0) {
			// part has connection points, must be recomputed
			p.recalcConnPoints();
		}
		return p;
	}
	
	
	/**
	 * Set a quick transformation for moving this primitive
	 * Also compute transformation for connection points, if any 
	 * @param pt point where to move
	 */
	public LDPrimitive moveTo(Point3D pt) {
		
		if (type != LDrawCommand.REFERENCE)
			throw new IllegalArgumentException("Cannot set a transformation for line type != 1");
		LDPrimitive p = new LDPrimitive(this);
		p.matrix = matrix.moveTo(pt);
		if (type == LDrawCommand.REFERENCE && connPoints != null && connPoints.length > 0) {
			// part has connection points, must be recomputed
			p.recalcConnPoints();
		}
		return p;
	}
	
	
	/**
	 * Set a quick transformation for moving this primitive
	 * Also compute transformation for connection points, if any 
	 * @param x coordinates where to move
	 * @param y
	 * @param z
	 */
	public LDPrimitive moveTo(float x, float y, float z) {
		
		if (type != LDrawCommand.REFERENCE)
			throw new IllegalArgumentException("Cannot set a transformation for line type != 1");
		LDPrimitive p = new LDPrimitive(this);
		p.matrix = matrix.moveTo(x,y,z);
		if (type == LDrawCommand.REFERENCE && connPoints != null && connPoints.length > 0) {
			// part has connection points, must be recomputed
			p.recalcConnPoints();
		}
		return p;
	}
	
	

	public ConnectionPoint[] getConnPoints() {
		return connPoints;
	}



	/**
	 * recalculates connection points
	 */
	public void recalcConnPoints() {
		this.connPoints = ConnectionPoint.getConnections(this);
	}




	public boolean isInvert() {
		
		return invert;
	}
	
	
	public LDrawCommand getType() {
		
		return type;
	}
	
	
	public float[] getPointsFV() {
		
		return vertex;
	}
	
//	
//	public float[] getNormalFV() {
//		
//		return normal;
//	}
	
	//////////////////////////////////////////////////
	// linked LDraw Part info
	
//	public String getPartName() {
//		
//		return ldrPart.getPartName();
//	}
//	
	
	public String getDescription() {

		switch (this.type) {
		case LINE:
			return "LINE primitive";
		case TRIANGLE:
			return "TRIANGLE primitive";
		case QUAD:
			return "QUAD primitive";
		case REFERENCE:
			return LDrawPart.getPart(partId).getDescription();
		case AUXLINE:
			return "AUXLINE primitive";
		case STEP:
		case BFC_INVERTNEXT:
		case BFC_CCW:
		case BFC_CW:
		case COMMENT:
		case KEYWORDS:
		case META_UNKNOWN:
		case CATEGORY:
		case EMPTY:
		case HISTORY:
		case AUTHOR:
		case COLOUR:
		case FILETYPE:
		case LDRAW_ORG:
		case LICENSE:
		case MPDFILE:
		case MPDNOFILE:
		case NAME:
		case SAVE:
			return "Special/Command";
		case UNKNOWN:
		default:
			return "UNKNOWN";
		}

	}
	
	
	
	/**
	 * nicely formats a float for LDraw files standard
	 * - no trailing decimal zeroes
	 * - max 5 decimals 
	 * @param n float to format
	 * @return
	 */
	private String fancyFloat(float n) {
		
		String s = String.format(Locale.US, " %.5f", n);
		if (s.equals(" -0.00000")) return " 0";
		if (s.endsWith(".00000")) return s.substring(0, s.length()-6);
		if (s.endsWith("0000")) return s.substring(0, s.length()-4);
		if (s.endsWith("000")) return s.substring(0, s.length()-3);
		if (s.endsWith("00")) return s.substring(0, s.length()-2);
		if (s.endsWith("0")) return s.substring(0, s.length()-1);
		return s;
	}
	
	
	////////////////////////////////////////////////
	// file write helpers
	
	public String getAsLdrLine() {
		
		String c;
		if (colorIndex >= 0x2000000) {
			c = String.format("0x%X", colorIndex);
		}
		else {
			c = Integer.toString(colorIndex);
		}
		//System.out.println(type+" - "+partId);  // DB
		switch (type) {
		case REFERENCE:
			// from http://stackoverflow.com/a/14126736
			// for "nice" number output
			return "1 "+ c + 
				fancyFloat(matrix.getX()) + fancyFloat(matrix.getY()) + fancyFloat(matrix.getZ()) + 
				fancyFloat(matrix.getA()) + fancyFloat(matrix.getB()) + fancyFloat(matrix.getC()) +
				fancyFloat(matrix.getD()) + fancyFloat(matrix.getE()) + fancyFloat(matrix.getF()) +
				fancyFloat(matrix.getG()) + fancyFloat(matrix.getH()) + fancyFloat(matrix.getI()) +
				" " + partId + "\n";
		case AUXLINE:
			return "5 "+ c +
					fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]) +
					fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]) +
					fancyFloat(vertex[6]) + fancyFloat(vertex[7]) + fancyFloat(vertex[8]) +
					fancyFloat(vertex[9]) + fancyFloat(vertex[10]) + fancyFloat(vertex[11]) +
					"\n";
		case BFC_CCW:
			return "0 BFC CERTIFY CCW\n";
		case BFC_CW:
			return "0 BFC CERTIFY CW\n";
		case BFC_INVERTNEXT:
			return "0 BFC INVERTNEXT\n";
		case CATEGORY:
			return "0 !CATEGORY "+partId+"\n";
		case COLOUR:
			break;
		case COMMENT:
			return partId + "\n";
		case EMPTY:
			return "\n";
		case FILETYPE:
			break;
		case KEYWORDS:
			return "0 !KEYWORDS "+partId+"\n";
		case LINE:
			return "2 "+ c +
					fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]) +
					fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]) +
					"\n";
		case META_UNKNOWN:
			return "0 " + partId + "\n";
		case MPDFILE:
			break;
		case MPDNOFILE:
			break;
		case NAME:
			break;
		case QUAD:
			return "4 "+ c +
					fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]) +
					fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]) +
					fancyFloat(vertex[6]) + fancyFloat(vertex[7]) + fancyFloat(vertex[8]) +
					fancyFloat(vertex[9]) + fancyFloat(vertex[10]) + fancyFloat(vertex[11]) +
					"\n";
		case SAVE:
			break;
		case STEP:
			return "0 STEP\n";
		case TRIANGLE:
			return "3 "+ c +
					fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]) +
					fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]) +
					fancyFloat(vertex[6]) + fancyFloat(vertex[7]) + fancyFloat(vertex[8]) +
					"\n";
		case UNKNOWN:
			break;
		default:
			break;
			
		}
		return "";
	}
	
    

	public void xmlWrite(XMLStreamWriter x) throws XMLStreamException {
		
		boolean invert = false;
		
		String c;
		if (colorIndex >= 0x2000000) {
			c = String.format("0x%X", colorIndex);
		}
		else {
			c = Integer.toString(colorIndex);
		}
		//System.out.println(type+" - "+partId);  // DB
		switch (type) {
		case REFERENCE:
			// from http://stackoverflow.com/a/14126736
			// for "nice" number output
			x.writeStartElement("ref");
			x.writeAttribute("name",getLdrawId());
			if (getColorIndex() != 16) {
				x.writeAttribute("c",c);
			}
			if (invert) {
				x.writeAttribute("invert","1");
				invert = false;
			}
			x.writeAttribute("p",fancyFloat(matrix.getX()) + fancyFloat(matrix.getY()) + fancyFloat(matrix.getZ()));
			x.writeAttribute("t",fancyFloat(matrix.getA()) + fancyFloat(matrix.getB()) + fancyFloat(matrix.getC()) +
					fancyFloat(matrix.getD()) + fancyFloat(matrix.getE()) + fancyFloat(matrix.getF()) +
					fancyFloat(matrix.getG()) + fancyFloat(matrix.getH()) + fancyFloat(matrix.getI()));
			x.writeEndElement();
			x.writeCharacters("\n");
			break;
		case AUXLINE:
			x.writeStartElement("aux");
			if (getColorIndex() != 16) {
				x.writeAttribute("c",c);
			}
			x.writeAttribute("p1",fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]));
			x.writeAttribute("p2",fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]));
			x.writeAttribute("c1",fancyFloat(vertex[6]) + fancyFloat(vertex[7]) + fancyFloat(vertex[8]));
			x.writeAttribute("c2",fancyFloat(vertex[9]) + fancyFloat(vertex[10]) + fancyFloat(vertex[11]));
			x.writeEndElement();
			x.writeCharacters("\n");
			break;
		case BFC_CCW:
			x.writeStartElement("bfc");
			x.writeAttribute("cert","ccw");
			x.writeEndElement();
			x.writeCharacters("\n");
			break;
		case BFC_CW:
			x.writeStartElement("bfc");
			x.writeAttribute("cert","cw");			
			x.writeEndElement();
			x.writeCharacters("\n");
			break;
		case BFC_INVERTNEXT:
			invert = true;
			break;
		case CATEGORY:
			break;
		case COLOUR:
			break;
		case COMMENT:
			break;
		case EMPTY:
			break;
		case FILETYPE:
			break;
		case KEYWORDS:
			break;
		case LINE:
			x.writeStartElement("line");
			if (getColorIndex() != 16) {
				x.writeAttribute("c",c);
			}
			x.writeAttribute("p1",fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]));
			x.writeAttribute("p2",fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]));
			x.writeEndElement();
			x.writeCharacters("\n");
			break;
		case META_UNKNOWN:
			break;
		case MPDFILE:
			break;
		case MPDNOFILE:
			break;
		case NAME:
			break;
		case QUAD:
			x.writeStartElement("quad");
			if (getColorIndex() != 16) {
				x.writeAttribute("c",c);
			}
			x.writeAttribute("p1",fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]));
			x.writeAttribute("p2",fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]));
			x.writeAttribute("p3",fancyFloat(vertex[6]) + fancyFloat(vertex[7]) + fancyFloat(vertex[8]));
			x.writeAttribute("p4",fancyFloat(vertex[9]) + fancyFloat(vertex[10]) + fancyFloat(vertex[11]));
			x.writeEndElement();
			x.writeCharacters("\n");
			break;
		case SAVE:
			break;
		case STEP:
			break;
		case TRIANGLE:
			x.writeStartElement("tri");
			if (getColorIndex() != 16) {
				x.writeAttribute("c",c);
			}
			x.writeAttribute("p1",fancyFloat(vertex[0]) + fancyFloat(vertex[1]) + fancyFloat(vertex[2]));
			x.writeAttribute("p2",fancyFloat(vertex[3]) + fancyFloat(vertex[4]) + fancyFloat(vertex[5]));
			x.writeAttribute("p3",fancyFloat(vertex[6]) + fancyFloat(vertex[7]) + fancyFloat(vertex[8]));
			x.writeEndElement();
			x.writeCharacters("\n");
			break;
		case UNKNOWN:
			break;
		default:
			break;
		}
	}
	
    
	
	
}
