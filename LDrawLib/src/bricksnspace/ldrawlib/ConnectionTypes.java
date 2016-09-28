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



import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import bricksnspace.j3dgeom.Point3D;

/**
 * Reads and handles connections definition file
 * 
 * @author Mario Pascucci
 *
 */
public class ConnectionTypes {

	
	public static String connFileName = "connectionDefs.xml";
	public static String autodetectFileName = "autodetectDefs.xml";
	
	
	/*
	 * connectionType object
	 */
	private int id;		// connection id 
	private int cc;		// coupling connection id
	private String name;	// short name
	private ConnectionFamily cf;	// connection family (Rail,Vector,Point)
	private String description;		// description
	
	private static int globalId = 0;
	
	/* 
	 * these are used to define connection types
	 */
	// maps shortnames with connections
	private static Map<String,ConnectionTypes> connIndexByName = new HashMap<String, ConnectionTypes>();
	// convert index to connection
	private static Map<Integer,ConnectionTypes> connById = new HashMap<Integer, ConnectionTypes>();
	
	/*
	 * now connection autodetect data
	 */
	// maps primitive with connection placement
	private static Map<String,ConnectionPoint[]> connByPrimitive = new HashMap<String, ConnectionPoint[]>();
	// maps primitive and duplicate check needs
	private static Map<String,Boolean> needDupCheck = new HashMap<String, Boolean>();
	

	
	private ConnectionTypes() {
		// empty connection
		id = getUniqueId();
	}
	
	
    private static synchronized int getUniqueId() {
		
		return ++globalId;
	}
	
    

	
	
	
	static void initTypes(InputStream conns) throws IOException {

		try {
			XMLInputFactory xmlFact = XMLInputFactory.newInstance();
			xmlFact.setProperty(XMLInputFactory.IS_COALESCING,true);
			XMLEventReader xer = xmlFact.createXMLEventReader(conns);
			boolean isXml = false;
			boolean isConnDefs = false;
			boolean isConn = false;
			boolean isStud = false;    	// a mnemonic for connector "head" 
			boolean isTube = false;		// a mnemonic for connector "socket"
			String studName = "", studType = "", studDescr = "", 
					tubeName = "", tubeType = "", tubeDescr = "";
			while (xer.hasNext()) {
				XMLEvent e = xer.nextEvent();
				switch (e.getEventType()) {
				case XMLEvent.START_DOCUMENT:
					isXml = true;
					break;
				case XMLEvent.START_ELEMENT:
					String tag = e.asStartElement().getName().getLocalPart();
					if (tag.equals("connectiondefs") && isXml) {
						// is a connection definition file
						isConnDefs = true;
					}
					else if (tag.equals("conndef") && isConnDefs) {
						// start of connection definition
						studName = "INVALID";
						studType = "";
						studDescr = "Invalid";
						tubeName = "INVALID";
						tubeType = "";
						tubeDescr = "Invalid";
						isConn = true;
					}
					else if (tag.equals("conn") && isConn) {
						// read connection
						if (!isStud) {
							// reads first definition
							studName = e.asStartElement().getAttributeByName(new QName("name")).getValue().trim().toUpperCase();
							studType = e.asStartElement().getAttributeByName(new QName("type")).getValue().trim();
							studDescr = e.asStartElement().getAttributeByName(new QName("descr")).getValue().trim();
							if (studName.length() == 0 || studDescr.length() == 0) {
								throw new IllegalArgumentException("Invalid XML format, name or descr empty: "+e.toString());
							}
							if (!studType.equalsIgnoreCase("R") 
									&& !studType.equalsIgnoreCase("V") 
									&& !studType.equalsIgnoreCase("P")) {
								throw new IllegalArgumentException("Invalid XML format, type must be R|V|P: "+e.toString());
							}
							isStud = true;
						}
						else if (!isTube && isStud) {
							tubeName = e.asStartElement().getAttributeByName(new QName("name")).getValue().trim().toUpperCase();
							tubeType = e.asStartElement().getAttributeByName(new QName("type")).getValue().trim();
							tubeDescr = e.asStartElement().getAttributeByName(new QName("descr")).getValue().trim();
							if (tubeName.length() == 0 || tubeDescr.length() == 0) {
								throw new IllegalArgumentException("Invalid XML format, name or descr empty: "+e.toString());
							}
							if (!tubeType.equalsIgnoreCase("R") 
									&& !tubeType.equalsIgnoreCase("V") 
									&& !tubeType.equalsIgnoreCase("P")) {
								throw new IllegalArgumentException("Invalid XML format, type must be R|V|P: "+e.toString());
							}
							isTube = true;
						}
						else {
							// error!
							throw new IllegalArgumentException("Invalid XML format, more than two coupled connection type: "+e.toString());
						}
					}
					break;
				case XMLEvent.END_ELEMENT:
					String endtag = e.asEndElement().getName().getLocalPart();
					if (endtag.equals("conndef")) {
						// it is a connection definition end
						if (!isTube || ! isStud) {
							throw new IllegalArgumentException("Invalid XML format, connection must be paired (line: "+e.getLocation().getLineNumber()+")");
						}
						ConnectionTypes cStud = new ConnectionTypes();
						ConnectionTypes cTube = new ConnectionTypes();
						// add coupling
						cStud.cc = cTube.id;
						cTube.cc = cStud.id;
						// add to known connections
						cStud.name = studName;
						cStud.description = studDescr;
						if (studType.equalsIgnoreCase("R"))
							cStud.cf = ConnectionFamily.RAIL;
						else if (studType.equalsIgnoreCase("V"))
							cStud.cf = ConnectionFamily.VECTOR;
						else if (studType.equalsIgnoreCase("P"))
							cStud.cf = ConnectionFamily.POINT;
						// add second connection
						cTube.name = tubeName;
						cTube.description = tubeDescr;
						if (tubeType.equalsIgnoreCase("R"))
							cTube.cf = ConnectionFamily.RAIL;
						else if (tubeType.equalsIgnoreCase("V"))
							cTube.cf = ConnectionFamily.VECTOR;
						else if (tubeType.equalsIgnoreCase("P"))
							cTube.cf = ConnectionFamily.POINT;
						connIndexByName.put(cStud.name, cStud);
						connIndexByName.put(cTube.name, cTube);
						connById.put(cStud.id, cStud);
						connById.put(cTube.id, cTube);
						isTube = false;
						isStud = false;
						isConn = false;
					}
					else if (endtag.equals("connectiondefs")) {
						// end of connection definition list
						isConnDefs = false;
					}
					break;
				}
			}
			xer.close();
		}
		catch (XMLStreamException ex) {
			throw new IOException("Error reading XML", ex);
		}
	
	}
	
	
	static void initAutodetect(InputStream conns) throws IOException {

		try {
			XMLInputFactory xmlFact = XMLInputFactory.newInstance();
			xmlFact.setProperty(XMLInputFactory.IS_COALESCING,true);
			XMLEventReader xer = xmlFact.createXMLEventReader(conns);
			boolean isXml = false;
			boolean isAutodetect = false;
			boolean isPrimitive = false;
			ArrayList<String> primName = new ArrayList<String>();
			boolean needsCheck = false;
			ArrayList<ConnectionPoint> cp = new ArrayList<ConnectionPoint>();
			while (xer.hasNext()) {
				XMLEvent e = xer.nextEvent();
				switch (e.getEventType()) {
				case XMLEvent.START_DOCUMENT:
					isXml = true;
					break;
				case XMLEvent.START_ELEMENT:
					String tag = e.asStartElement().getName().getLocalPart();
					if (tag.equals("autodetect") && isXml) {
						// start of autodetect list
						isAutodetect = true;
					}
					else if (tag.equals("primitive") && isAutodetect) {
						primName.clear();
						cp.clear();
						isPrimitive = true;
						if (e.asStartElement().getAttributeByName(new QName("checkdup")) != null) {
							needsCheck = e.asStartElement().getAttributeByName(new QName("checkdup")).getValue().trim().equals("1");
						}
						else {
							needsCheck = false;
						}
					}
					else if (tag.equals("name") && isPrimitive) {
						String name = e.asStartElement().getAttributeByName(new QName("p")).getValue().trim();
						primName.add(name);
					}
					else if (tag.equals("cp") && isPrimitive) {
						// reads type
						String type = e.asStartElement().getAttributeByName(new QName("type")).getValue().trim().toUpperCase();
						ConnectionPoint c = ConnectionPoint.createCPoint(getByName(type).id);
						try {
							// reads vector base point
							float x = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("bx")).getValue());
							float y = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("by")).getValue());
							float z = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("bz")).getValue());
							c.setP1(new Point3D(x, y, z));
						}
						catch (NumberFormatException ex) {
							Logger.getAnonymousLogger().severe("Autodetect XML def" +
									"(line #"+e.getLocation().getLineNumber()+") -> Wrong coordinate value\n"+
									ex.getLocalizedMessage());
							c.setP1(new Point3D(0,0,0));
						}
						try {
							// reads vector head point
							float hx = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("hx")).getValue());
							float hy = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("hy")).getValue());
							float hz = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("hz")).getValue());
							c.setP2(new Point3D(hx, hy, hz));
						}
						catch (NumberFormatException ex) {
							Logger.getAnonymousLogger().severe("Autodetect XML def" +
									"(line #"+e.getLocation().getLineNumber()+") -> Wrong coordinate value\n"+
									ex.getLocalizedMessage());
							c.setP2(new Point3D(0,0,0));
						}
						cp.add(c);
					}
					break;
				case XMLEvent.END_ELEMENT:
					String endtag = e.asEndElement().getName().getLocalPart();
					if (endtag.equals("autodetect")) {
						isAutodetect = false;
					}
					else if (endtag.equals("primitive")) {
						// create new autodetect record
						for (String name : primName) {
							connByPrimitive.put(name, cp.toArray(new ConnectionPoint[0]));
							needDupCheck.put(name, needsCheck);
						}
						isPrimitive = false;
					}
					break;
				}
			}
			xer.close();
		}
		catch (XMLStreamException ex) {
			throw new IOException("Error reading XML", ex);
		}
	
	}
	
	
	public static ConnectionTypes getByName(String name) {
		return connIndexByName.get(name);
	}
	
	
	public static ConnectionTypes getById(int id) {
		return connById.get(id);
	}

	
	public String getName() { return name; }
	
	public int getId() { return id; }
	
	public int getOpposite() { return cc; }
	
	public ConnectionFamily getFamily() { return cf; }
	
	@Override
	public String toString() { return description; }
	
	
	public static Collection<ConnectionTypes> listTypes() {
		return connIndexByName.values();
	}
	
	
	public static Set<Integer> listById() {
		return connById.keySet();
	}
	
	
	public static ConnectionPoint[] getPrimitiveConns(String prim) {
		return connByPrimitive.get(prim);
	}
	
	
	public static boolean needsDupCheck(String prim) {
		return needDupCheck.get(prim);
	}
	
	
}
