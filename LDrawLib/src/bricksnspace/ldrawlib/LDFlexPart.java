/*
	Copyright 2015 Mario Pascucci <mpascucci@gmail.com>
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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import bricksnspace.j3dgeom.Point3D;

/**
 * Flexible parts definition
 * 
 * @author Mario Pascucci
 *
 */
public class LDFlexPart {

	
	public static final String INITFILE = "flexpartsDefs.xml";
	
	
	enum FlexType {
		CONT, SEGM
	}
	

	
	private static Map<String,LDFlexPart> flexParts = new HashMap<String, LDFlexPart>();  
	
	
	String name;
	String start;
	String mid;
	String end;
	ConnectionPoint startVector;
	ConnectionPoint midVector;
	ConnectionPoint endVector;
//	boolean orientStart;
//	boolean orientEnd;
	float maxLength;
	float overlapLen;
	FlexType type;
	float howRigid;
	

	
	
	private LDFlexPart() {
		
	}
	
	
	
	@Override
	public String toString() {
		return String
				.format("LDFlexPart [name=%s, start=%s, mid=%s, end=%s, maxLength=%s, type=%s, howRigid=%s]",
						name, start, mid, end, maxLength, type, howRigid);
	}



	public String getName() {
		return name;
	}



	public String getStart() {
		return start;
	}



	public String getMid() {
		return mid;
	}



	public String getEnd() {
		return end;
	}



	public ConnectionPoint getStartVector() {
		return startVector;
	}



	public ConnectionPoint getMidVector() {
		return midVector;
	}



	public ConnectionPoint getEndVector() {
		return endVector;
	}



//	public boolean isOrientStart() {
//		return orientStart;
//	}
//
//
//
//	public boolean isOrientEnd() {
//		return orientEnd;
//	}
//
//
//
	public float getMaxLength() {
		return maxLength;
	}



	public float getOverlapLen() {
		return overlapLen;
	}



	public boolean isContinue() {
		return type == FlexType.CONT;
	}



	public float getHowRigid() {
		return howRigid;
	}



	private static void addFlexPart(LDFlexPart p) {
		
		flexParts.put(p.name, p);
	}
	
	
	
	public static LDFlexPart getFlexPart(String ldrid) {
		
		return flexParts.get(ldrid);
	}
	

	
	public static boolean isFlexPart(String ldrid) {
		
		//System.out.println(ldrid);   // DB
		return flexParts.containsKey(ldrid);
	}
	
	
	
	/**
	 * reads flex part definition from xml file
	 * @param xml
	 * @return list of connections
	 * @throws XMLStreamException 
	 * @throws FileNotFoundException 
	 */
	public static void initFlexParts(InputStream xml) throws FileNotFoundException, XMLStreamException {
		
		XMLInputFactory xmlFact = XMLInputFactory.newInstance();
		xmlFact.setProperty(XMLInputFactory.IS_COALESCING,true);
		XMLEventReader xer = xmlFact.createXMLEventReader(xml);
		boolean isDoc = false;
		boolean isFlexList = false;
		boolean isPartList = false;
		boolean isAuxList = false;
		boolean isAuxPart = false;
		boolean isPart = false;
		boolean isStart = false;
		boolean isMid = false;
		boolean isEnd = false;
		LDFlexPart fp = null;
		String auxpart=null, dat=null;
		String tag;
		while (xer.hasNext()) {
			XMLEvent e = xer.nextEvent();
			//System.out.println(e);
			switch (e.getEventType()) {
			case XMLEvent.START_DOCUMENT:
				isDoc = true;
				break;
			case XMLEvent.START_ELEMENT:
				tag = e.asStartElement().getName().getLocalPart();
				if (tag.equals("flexparts") && isDoc) {
					isFlexList = true;
				}
				else if (tag.equals("auxparts") && isFlexList) {
					isAuxList = true;
				}
				else if (tag.equals("auxp") && isAuxList) {
					auxpart = e.asStartElement().getAttributeByName(new QName("name")).getValue().toLowerCase().trim();
					if (!auxpart.startsWith("__")) {
						Logger.getAnonymousLogger().log(Level.WARNING, "Auxiliary part name not starting with '__'");
					}
					isAuxPart = true;
				}
				else if (tag.equals("partlist") && isFlexList) {
					isPartList = true;
				}
				else if (tag.equals("part") && isPartList) {
					isPart = true;
					fp = new LDFlexPart();
					// name="32580.dat" type="cont" rigid="30" len="60" extend="1" elastic="0"
					fp.name = e.asStartElement().getAttributeByName(new QName("name")).getValue().toLowerCase().trim();
					fp.type = FlexType.valueOf(e.asStartElement().getAttributeByName(new QName("type")).getValue().toUpperCase().trim());
					try {
						fp.howRigid = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("rigid")).getValue());
					} catch (NumberFormatException e1) {
						fp.howRigid = 10;
						Logger.getAnonymousLogger().log(Level.WARNING, e1.getLocalizedMessage());
					}
					try {
						fp.maxLength = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("len")).getValue());
					} catch (NumberFormatException e1) {
						fp.maxLength = 10000;
						Logger.getAnonymousLogger().log(Level.WARNING, e1.getLocalizedMessage());
					}
				}
				else if (tag.equals("start") && isPart) {
					fp.start = e.asStartElement().getAttributeByName(new QName("name")).getValue().toLowerCase().trim();
//					if (e.asStartElement().getAttributeByName(new QName("orient")) != null) {
//						fp.orientStart = e.asStartElement().getAttributeByName(new QName("orient")).getValue().trim().equals("1");
//					}
//					else {
//						fp.orientStart = false;
//					}
					Point3D base,head;
					String[] coords = e.asStartElement().getAttributeByName(new QName("b")).getValue().split(",");
					try {
						float x = Float.parseFloat(coords[0]);
						float y = Float.parseFloat(coords[1]);
						float z = Float.parseFloat(coords[2]);
						base = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						base = new Point3D(0, 0, 0);
					}					
					coords = e.asStartElement().getAttributeByName(new QName("h")).getValue().split(",");
					try {
						float x = Float.parseFloat(coords[0]);
						float y = Float.parseFloat(coords[1]);
						float z = Float.parseFloat(coords[2]);
						head = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						head = new Point3D(0, 0, 0);
					}
					fp.startVector = ConnectionPoint.getDummy(base,head);
					isStart = true;
				}
				else if (tag.equals("mid") && isPart) {
					fp.mid = e.asStartElement().getAttributeByName(new QName("name")).getValue().toLowerCase().trim();
					if (e.asStartElement().getAttributeByName(new QName("overlap")).getValue() != null) {
						try {
							fp.overlapLen = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("overlap")).getValue());
						} catch (NumberFormatException e1) {
							fp.overlapLen = 0;
							Logger.getAnonymousLogger().log(Level.WARNING, e1.getLocalizedMessage());
						}
					}
					Point3D base,head;
					String[] coords = e.asStartElement().getAttributeByName(new QName("b")).getValue().split(",");
					try {
						float x = Float.parseFloat(coords[0]);
						float y = Float.parseFloat(coords[1]);
						float z = Float.parseFloat(coords[2]);
						base = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						base = new Point3D(0, 0, 0);
					}					
					coords = e.asStartElement().getAttributeByName(new QName("h")).getValue().split(",");
					try {
						float x = Float.parseFloat(coords[0]);
						float y = Float.parseFloat(coords[1]);
						float z = Float.parseFloat(coords[2]);
						head = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						head = new Point3D(0, 0, 0);
					}
					fp.midVector = ConnectionPoint.getDummy(base, head);
					isMid = true;
				}
				else if (tag.equals("end") && isPart) {
					fp.end = e.asStartElement().getAttributeByName(new QName("name")).getValue().toLowerCase().trim();
//					if (e.asStartElement().getAttributeByName(new QName("orient")) != null) {
//						fp.orientEnd = e.asStartElement().getAttributeByName(new QName("orient")).getValue().trim().equals("1");
//					}
//					else {
//						fp.orientEnd = false;
//					}
					Point3D base,head;
					String[] coords = e.asStartElement().getAttributeByName(new QName("b")).getValue().split(",");
					try {
						float x = Float.parseFloat(coords[0]);
						float y = Float.parseFloat(coords[1]);
						float z = Float.parseFloat(coords[2]);
						base = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						base = new Point3D(0, 0, 0);
					}					
					coords = e.asStartElement().getAttributeByName(new QName("h")).getValue().split(",");
					try {
						float x = Float.parseFloat(coords[0]);
						float y = Float.parseFloat(coords[1]);
						float z = Float.parseFloat(coords[2]);
						head = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						head = new Point3D(0, 0, 0);
					}
					fp.endVector = ConnectionPoint.getDummy(base, head);
					isEnd = true;
				}
				break;
			case XMLEvent.CHARACTERS:
				if (isAuxPart) {
					dat = e.asCharacters().getData();
				}
				break;
			case XMLEvent.END_ELEMENT:
				tag = e.asEndElement().getName().getLocalPart();
				if (tag.equals("auxp") && isAuxPart) {
					LDrawPart p = LDrawPart.newPartFromString(auxpart, dat);
					p.registerInternalUsePart(auxpart);
					//System.out.println(p);
					dat = null;
					auxpart = null;
					isAuxPart = false;
				}
				else if (tag.equals("part")) {
					if (isStart && isMid && isEnd) {
						addFlexPart(fp);
						//System.out.println(fp);
					}
					else {
						Logger.getAnonymousLogger().log(Level.WARNING,"Missing Start,Mid or end tag");
					}
					fp = null;
					isStart = false;
					isMid = false;
					isEnd = false;
					isPart = false;
				}
				else if (tag.equals("partList")) {
					isPartList = false;
				}
				else if (tag.equals("auxparts") && isAuxList) {
					isAuxList = false;
				}
				else if (tag.equals("flexparts") && isFlexList) {
					isFlexList = false;
				}
			}
		}
		xer.close();
	}
	
	
	

}
