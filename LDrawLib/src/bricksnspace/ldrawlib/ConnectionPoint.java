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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

import bricksnspace.j3dgeom.Matrix3D;
import bricksnspace.j3dgeom.Point3D;


/**
 * Connection points for parts
 * 
 * @author Mario Pascucci
 *
 */
public class ConnectionPoint {

	private static int globalId = 0;

	private Point3D p1,p2; 			// segment for connection limit and orientation
	private Point3D delta;			// offset relative to part local origin
	private ConnectionTypes c;				// enum for connection type
	private int partId;				// part id that own this connection
	private int id;
	public static final String CONNZIP = "ldrconn.zip";
	public static final String CONNFOLDER = "conn/";
	public static final String ZIPFOLDER = "";
	public static final String CONNEXT = ".cxml";
	public static final String CONNURL = "http://sourceforge.net/projects/jbrickbuilder/files/jbrickbuilder/connections/ldrconn.zip/download";
	public static final String CONNVERSION = "http://jbrickbuilder.sourceforge.net/CONNVERSION";
	public static final String CONNAUTO = "autoconnect.csv"; 

	private static Map<String,List<ConnectionPoint>> connectionsCache = 
			new HashMap<String,List<ConnectionPoint>>();

	private static Set<String> autoConnSet = new TreeSet<String>();
	
	private static ZipFile connZip = null;
	

	
	/*
	 *  connection rules:
	 *  
	 *  - axle are bidirectional
	 *  - axlehole are single oriented, i.e. can connect only on one orientation
	 *  - peghole are single oriented
	 *  - bar are bidirectional
	 *  - clip are bidirectional
	 */
	
	
	public static void init() throws IOException {
		
		connZip = new ZipFile(new File(CONNZIP));
		ConnectionTypes.initTypes(connZip.getInputStream(
				connZip.getEntry(ConnectionTypes.connFileName)));
		ConnectionTypes.initAutodetect(connZip.getInputStream(
				connZip.getEntry(ConnectionTypes.autodetectFileName)));
		try {
			initAutodetected(connZip.getInputStream(connZip.getEntry(CONNAUTO)));
		} catch (Exception e) {
			// ignore if no autoconnect.csv file found
		}
		try {
			LDFlexPart.initFlexParts(connZip.getInputStream(connZip.getEntry(LDFlexPart.INITFILE)));
		} catch (Exception e) {
			// ignore if no flexpart file found
		}
	}
	
	
	public static void initFromFile() throws IOException {
		
		connZip = null; //new ZipFile(new File(CONNZIP));
		ConnectionTypes.initTypes(new FileInputStream(ConnectionTypes.connFileName));
		ConnectionTypes.initAutodetect(new FileInputStream(ConnectionTypes.autodetectFileName));
		try {
			initAutodetected(new FileInputStream(CONNAUTO));
		} catch (IOException e) {
			// ignore if no autoconnect.csv file found
		}
		try {
			LDFlexPart.initFlexParts(new FileInputStream(LDFlexPart.INITFILE));
		} catch (IOException e) {
			// ignore if no flexpart file found
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * closes old zipfile 
	 * 
	 *  to use with connections update to avoid resource leak
	 */
	public static void release() {

		if (connZip != null) {
			try {
				connZip.close();
			} catch (IOException e) {
				// ignored
			}
		}
	}
	
	
	private ConnectionPoint() {
		
		// private empty constructor
	}
	
	
	private ConnectionPoint(int partId) {
		
		this.partId = partId;
		id = getUniqueId();
	}
	
	

    private static synchronized int getUniqueId() {
		
		return ++globalId;
	}
	
    
    public void setP1(Point3D p) {
    	
    	p1 = new Point3D(p);
    	delta = new Point3D(-p.x,-p.y,-p.z);
    }
    
    
    public void setP2(Point3D p) {
    	
    	p2 = new Point3D(p);
    }
    
    
    public ConnectionPoint getClone(int partId) {
    	
    	ConnectionPoint cp = new ConnectionPoint(partId);
    	cp.p1 = new Point3D(this.p1);
    	cp.p2 = new Point3D(this.p2);
    	cp.delta = new Point3D(this.delta);
    	cp.c = this.c;
    	return cp;
    }
    
    
    
    /**
     * Creates a dummy/commodity connection point with invalid connection type
     * <p>
     * Do NOT use as a normal connection point
     * @param p1
     * @param p2
     * @return
     */
    public static ConnectionPoint getDummy(Point3D p1, Point3D p2) {
    	ConnectionPoint cp = new ConnectionPoint();
    	cp.p1 = p1;
    	cp.p2 = p2;
    	cp.delta = new Point3D(0, 0, 0);
    	cp.c = null;
    	return cp;
    }
    
    
    public int getId() {
    	return id;
    }
    
	
	public Point3D getP1() {
		return p1;
	}


	public Point3D getP2() {
		return p2;
	}


	public Point3D getDelta() {
		return delta;
	}


	public ConnectionTypes getType() {
		return c;
	}

	
	public int getPartId() {
		return partId;
	}
	
	
//	public Point3D getPartPlacement() {
//		
//		return new Point3D(p1.x-delta.x,p1.y-delta.y,p1.z-delta.z);
//	}
//
//
	@Override
	public String toString() {
		return "CPoint [id="+id+", p1=" + p1 + ", p2=" + p2 + " d="+delta+ " , c=" + c
				+ ", ptId=" + partId + "]";
	}


	
	public static void addAutodetected(String s) {
		
		if (s.length() > 0) {
			autoConnSet.add(s);
		}
	}
	
	
	public static void delAutodetected(String s) {
		
		if (s.length() > 0) {
			autoConnSet.remove(s);
		}		
	}
	
	
	
	public static void saveAutoconnList(File f) throws IOException {
		
		OutputStreamWriter o = new OutputStreamWriter(new FileOutputStream(f));
		for (String s:autoConnSet) {
			o.write(s);
			o.write('\n');
		}
		o.close();
	}
	
	
	
	
	private static void initAutodetected(InputStream a) throws IOException {
		
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(a));
		String s;
		while ((s = lnr.readLine()) != null) {
			autoConnSet.add(s);
		}
	}
	
	
	
	
	public static String getPartMainName(String ldrid) {

		ldrid = ldrid.toLowerCase();
		if (ldrid.matches("[0-9]+[a-oq-z]{0,1}p.+.dat") 
				&& ConnectionTypes.getPrimitiveConns(ldrid) == null) {
			// it is a printed part, get "main" part"
			return ldrid.substring(0, ldrid.indexOf('p'));
		}
		else {
			// no part marker
			if (ldrid.toLowerCase().endsWith(".dat")) {
				return ldrid.substring(0, ldrid.length()-4);
			}
			else {
				return ldrid;
			}
		}
	}
	
	
	
	private static void extractConnections(int partId,List<ConnectionPoint> cp,Collection<LDPrimitive> pt, 
			Matrix3D m, boolean invert) {

		//System.out.println(pt);
		for (LDPrimitive p : pt) {
			switch (p.getType()) {
			case REFERENCE:
			// sub-part
				//System.out.println(p.getLdrawId()); //DB
				//System.out.println(p.getId()+" c:"+localColor+" inv:"+invert+ " isInvert:"+p.isInvert());
				String part = p.getLdrawId().toLowerCase();
				int idxs = part.lastIndexOf('/');
				if (idxs != -1) {
					part = part.substring(idxs+1);
				}
				Matrix3D transform = p.getTransformation().transform(m);
				// uses autodetection database
				ConnectionPoint[] clp = ConnectionTypes.getPrimitiveConns(part);
				if (clp != null) {
					//System.out.println(clp.length);	// DB
					// add connections to list
					for (ConnectionPoint cnp : clp) {
						ConnectionPoint cn = new ConnectionPoint(partId);
						cn.c = cnp.getType();
						cn.p1 = new Point3D(transform.transformPoint(cnp.p1.x, cnp.p1.y, cnp.p1.z));
						cn.p2 = new Point3D(transform.transformPoint(cnp.p2.x, cnp.p2.y, cnp.p2.z));
						cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
						cn.addConnection(cp, ConnectionTypes.needsDupCheck(part));
					}
					continue;
				}
				// look for "printed" parts
				// it isn't needed to check for primitive in autodetect, there was checked
				// in the above "if"
				String filepart = getPartMainName(p.getLdrawId());
//				if (p.getLdrawId().toLowerCase().matches("[0-9]+[a-oq-z]{0,1}p.+.dat") //p.getLdrawId().toLowerCase().indexOf('p') >= 0 
//						) {
//					// it is a printed part, get "main" part"
//					filepart = p.getLdrawId().substring(0, p.getLdrawId().toLowerCase().indexOf('p'));
//				}
//				else {
//					// no part marker
//					if (p.getLdrawId().toLowerCase().endsWith(".dat")) {
//						filepart = p.getLdrawId().toLowerCase().substring(0, p.getLdrawId().length()-4);
//					}
//					else {
//						filepart = p.getLdrawId().toLowerCase();
//					}
//				}
				//System.out.println(p.getLdrawId()+" - "+filepart);  //DB
				// if part connections isn't in cache
				if (!connectionsCache.containsKey(filepart)) {
					try {
						// try first if a connection file exists
						if (connZip != null) {
							ZipEntry ze = connZip.getEntry(ZIPFOLDER+filepart+CONNEXT);
							if (ze!= null) {
								InputStream cf = connZip.getInputStream(ze);
								connectionsCache.put(filepart,readFromFile(0, cf));
							}
						}
						else {
							File cf = new File(CONNFOLDER,filepart+CONNEXT);
							if (cf.isFile() && cf.canRead()) {
								InputStream cfis = new FileInputStream(cf);
								connectionsCache.put(filepart,readFromFile(0, cfis));
							}
						}
					} catch (XMLStreamException e) {
						System.out.println("Error reading connection file: "+p.getLdrawId());
						e.printStackTrace();
					} catch (IOException e) {
						System.out.println("Error reading connection file: "+p.getLdrawId());
						e.printStackTrace();
					}
				}
				if (connectionsCache.containsKey(filepart)) {
					for (ConnectionPoint cpt: connectionsCache.get(filepart)) {
						if (cpt.c.getName().equals("R_STUD")) {
							cpt.transform(transform).addConnection(cp, true);
						}
						else {
							cpt.transform(transform).addConnection(cp, false);
						}
					}
					continue;
				}
				// system
				// studs
//				if (part.equals("stud.dat") || part.equals("stud2.dat")
//						|| part.equals("stud10.dat")
//						|| part.equals("stud2a.dat")) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("STUD");
//					cn.p1 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, -4, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
////					System.out.println("Stud: x1=" + cn.p1[0]+" y1="+cn.p1[1]+" z1="+cn.p1[2] +
////							" x2=" + cn.p2[0]+" y2="+cn.p2[1]+" z2="+cn.p2[2]);
//					cn.addConnection(cp, false);
//					// open stud (like technic bricks) 
//					if (part.equals("stud2.dat")
//							|| part.equals("stud2a.dat")
//							) {
//						cn = new ConnectionPoint(partId);
//						cn.c = ConnectionTypes.get("CLIP");
//						cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//						cn.p2 = new Point3D(transform.transformPoint(0, 0, 0));
//						cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//						cn.addConnection(cp, false);
//						cn = new ConnectionPoint(1);
//						cn.c = ConnectionTypes.get("STUDJ");
//						cn.p1 = new Point3D(transform.transformPoint(0, 0, 0));
//						cn.p2 = new Point3D(transform.transformPoint(0, -4, 0));
//						cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//						cn.addConnection(cp, false);
//					}
//					continue;
//				}
//				// under tube
//				if (part.equals("stud4.dat") 
//						|| part.equals("stud4a.dat") 
//						|| part.equals("stud4h.dat")
//						|| part.equals("stud4o.dat")
//						|| part.equals("stud4f4s.dat")
//						|| part.equals("stud4f4n.dat")
//						|| part.equals("stud18a.dat")
//						) {
//					// center connection, in tube
//					ConnectionPoint cn = new ConnectionPoint(partId);
////					float dy = 0;
////					if (transform.getE() != 0) { 
////						System.out.println(transform);
////						dy = -4-4/p.getTransformation().getE();	// max depth for stud in tube/tube connection
////					}
//					//System.out.println(part+"-"+transform);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, -2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
////					System.out.println("Tube: x1=" + cn.p1.x+" y1="+cn.p1.y+" z1="+cn.p1.z +
////							" x2=" + cn.p2.x+" y2="+cn.p2.y+" z2="+cn.p2.z);
//					cn.addConnection(cp, false);
//					// top right connection, around tube
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(10, -4, 10));
//					cn.p2 = new Point3D(transform.transformPoint(10, -2, 10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					// top right connection, around tube
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(-10, -4, 10));
//					cn.p2 = new Point3D(transform.transformPoint(-10, -2, 10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(10, -4, -10));
//					cn.p2 = new Point3D(transform.transformPoint(10, -2, -10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(-10, -4, -10));
//					cn.p2 = new Point3D(transform.transformPoint(-10, -2, -10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					// under studs with peghole
//					if (part.equals("stud4h.dat") 
//							|| part.equals("stud18a.dat")) {
//						cn = new ConnectionPoint(partId);
//						cn.c = ConnectionTypes.get("PEGHOLE");
//						cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//						cn.p2 = new Point3D(transform.transformPoint(0, 4, 0));
//						cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//						cn.addConnection(cp, false);
//						cn = new ConnectionPoint(partId);
//						cn.c = ConnectionTypes.get("PEGHOLE");
//						cn.p1 = new Point3D(transform.transformPoint(0, 4, 0));
//						cn.p2 = new Point3D(transform.transformPoint(0, -4, 0));
//						cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//						cn.addConnection(cp, false);
//					}
//					continue;
//				}
//				// under stud thin (like 3004, 3008, 3010)
//				if (part.equals("stud3.dat")
//						) {
////					float dy = 0;
////					if (m.getE() != 0) { 
////						dy = -4-4/p.getTransformation().getE();	// max depth for stud in tube/tube connection
////					}
////					System.out.println(dy);
//					ConnectionPoint cn = new ConnectionPoint(1);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(10, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(10, -2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					cn = new ConnectionPoint(1);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(-10, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(-10, -2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					cn = new ConnectionPoint(1);
//					cn.c = ConnectionTypes.get("R_STUDJ");
//					cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, -2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// under stud thin (like 3701)
//				if (part.equals("stud3a.dat")
//						) {
////					float dy = 0;
////					if (m.getE() != 0) { 
////						dy = -4+4/p.getTransformation().getE();	// max depth for stud in tube/tube connection
////					}
////					System.out.println(dy);
//					ConnectionPoint cn = new ConnectionPoint(1);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(10, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(10, -2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					cn = new ConnectionPoint(1);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(-10, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(-10, -2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, true);
//					cn = new ConnectionPoint(1);
//					cn.c = ConnectionTypes.get("R_STUDJ");
//					cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, -2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// technic
//				// axleholes
//				if (part.equals("axlehole.dat")
//						|| part.equals("axlehol4.dat")
//						|| part.equals("axlehol5.dat")
//						|| part.equals("axlehol8.dat")
//						|| part.equals("axl2hole.dat")
//						|| part.equals("axl2hol8.dat")
//						|| part.equals("axl3hole.dat")
//						|| part.equals("axl3hol8.dat")
//						) {
//					// axlehole originating on 0,0,0, extending for 1 LDU
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("R_AXLE");
//					cn.p1 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, 1, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// axle
//				if (part.equals("axle.dat")) {
//					// axle originating on 0,0,0, extending for 1 LDU
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("AXLE");
//					cn.p1 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, 1, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
////					// normal axle is bidirectional
////					ConnectionPoint cr = new ConnectionPoint(partId);
////					cr.c = ConnectionTypes.get("AXLE");
////					cr.p1 = cn.p2;
////					cr.p2 = cn.p1;
////					cr.delta = new Point3D(-cr.p1.x,-cr.p1.y,-cr.p1.z);
////					cr.addConnection(cp, false);
//					continue;
//				}
//				// pin and pegholes
//				// peghole, like #3701 
//				if (part.equals("peghole.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("PEGHOLE");
//					cn.p1 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, 10, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// pin round
//				if (part.equals("connect.dat") 
//						|| part.equals("confric.dat")
//						|| part.equals("confric5.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("PIN");
//					cn.p1 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, -20, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// clips
//				if (part.equals("clip1.dat")
//						|| part.equals("clip4.dat")
//						|| part.equals("clip5.dat")
//						|| part.equals("clip7.dat")
//						) {
//					// clip vertical for bar/handle
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLIP");
//					cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, 4, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
////					// normal axle is bidirectional
////					ConnectionPoint cr = new ConnectionPoint(partId);
////					cr.c = ConnectionTypes.get("CLIP");
////					cr.p1 = cn.p2;
////					cr.p2 = cn.p1;
////					cr.delta = new Point3D(-cr.p1.x,-cr.p1.y,-cr.p1.z);
////					cr.addConnection(cp, false);
//					continue;
//				}
//				if (part.equals("clip2.dat")
//						|| part.equals("clip9.dat")
//						|| part.equals("clip10.dat")
//						) {
//					// clip vertical for bar/handle
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLIP");
//					cn.p1 = new Point3D(transform.transformPoint(0, -4, -10));
//					cn.p2 = new Point3D(transform.transformPoint(0, 4, -10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					// normal axle is bidirectional
////					ConnectionPoint cr = new ConnectionPoint(partId);
////					cr.c = ConnectionTypes.get("CLIP");
////					cr.p1 = cn.p2;
////					cr.p2 = cn.p1;
////					cr.delta = new Point3D(-cr.p1.x,-cr.p1.y,-cr.p1.z);
////					cr.addConnection(cp, false);
//					continue;
//				}
//				if (part.equals("clip3.dat")
//						|| part.equals("clip6.dat")
//						) {
//					// clip horizontal for bar/handle
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLIP");
//					cn.p1 = new Point3D(transform.transformPoint(4, 2, 0));
//					cn.p2 = new Point3D(transform.transformPoint(-4, 2, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					// normal axle is bidirectional
////					ConnectionPoint cr = new ConnectionPoint(partId);
////					cr.c = ConnectionTypes.get("CLIP");
////					cr.p1 = cn.p2;
////					cr.p2 = cn.p1;
////					cr.delta = new Point3D(-cr.p1.x,-cr.p1.y,-cr.p1.z);
////					cr.addConnection(cp, false);
//					continue;
//				}
//				if (part.equals("clip8.dat")
//						|| part.equals("clip11.dat")
//						) {
//					// clip horizontal top for bar/handle
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLIP");
//					cn.p1 = new Point3D(transform.transformPoint(0, -6, -4));
//					cn.p2 = new Point3D(transform.transformPoint(0, -6, 4));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					// normal axle is bidirectional
////					ConnectionPoint cr = new ConnectionPoint(partId);
////					cr.c = ConnectionTypes.get("CLIP");
////					cr.p1 = cn.p2;
////					cr.p2 = cn.p1;
////					cr.delta = new Point3D(-cr.p1.x,-cr.p1.y,-cr.p1.z);
////					cr.addConnection(cp, false);
//					continue;
//				}
//				// hinge click locking
//				if (part.equals("clh1.dat")
//						|| part.equals("clh3.dat")
//						|| part.equals("clh6.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFINGER");
//					cn.p1 = new Point3D(transform.transformPoint(-4, 0, -10));
//					cn.p2 = new Point3D(transform.transformPoint(4, 0, -10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFINGER");
//					cn.p1 = new Point3D(transform.transformPoint(4, 0, -10));
//					cn.p2 = new Point3D(transform.transformPoint(-4, 0, -10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// short version
//				if (part.equals("clh8.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFINGER");
//					cn.p1 = new Point3D(transform.transformPoint(-4, 0, -7));
//					cn.p2 = new Point3D(transform.transformPoint(4, 0, -7));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFINGER");
//					cn.p1 = new Point3D(transform.transformPoint(4, 0, -7));
//					cn.p2 = new Point3D(transform.transformPoint(-4, 0, -7));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// short vertical version
//				if (part.equals("clh9.dat")
//						|| part.equals("clh2.dat")
//						|| part.equals("clh12.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFINGER");
//					cn.p1 = new Point3D(transform.transformPoint(-4, -7, 0));
//					cn.p2 = new Point3D(transform.transformPoint(4, -7, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFINGER");
//					cn.p1 = new Point3D(transform.transformPoint(4, -7, 0));
//					cn.p2 = new Point3D(transform.transformPoint(-4, -7, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// hinge click fork
//				if (part.equals("clh4.dat")
//						|| part.equals("clh10.dat")
//						|| part.equals("clh11.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFORK");
//					cn.p1 = new Point3D(transform.transformPoint(4, 0, 4));
//					cn.p2 = new Point3D(transform.transformPoint(-4, 0, 4));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// hinge click fork for arms
//				if (part.equals("clh5.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLHFORK");
//					cn.p1 = new Point3D(transform.transformPoint(4, 0, 10));
//					cn.p2 = new Point3D(transform.transformPoint(-4, 0, 10));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// wheel pin
//				if (part.equals("wpin.dat")
//						|| part.equals("wpin2.dat")
//						|| part.equals("wpin2a.dat")
//						|| part.equals("wpin4.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("WHEELPIN");
//					cn.p1 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, -12, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// wheel pinhole
//				if (part.equals("wpinhole.dat")
//						|| part.equals("wpinhol2.dat")
//						) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("R_WHEELPIN");
//					cn.p1 = new Point3D(transform.transformPoint(0, -1, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// under tube for minifig headgear 
//				if (part.equals("stud16.dat")) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("R_STUD");
//					cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, 0, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
//				// shields handle
//				// it is possible to insert a bar between handle and shield
//				if (part.equals("handle.dat")
//						|| part.equals("handle2.dat")) {
//					ConnectionPoint cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("BAR");
//					cn.p1 = new Point3D(transform.transformPoint(7, 0, 4));
//					cn.p2 = new Point3D(transform.transformPoint(-7, 0, 4));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					cn = new ConnectionPoint(partId);
//					cn.c = ConnectionTypes.get("CLIP");
//					cn.p1 = new Point3D(transform.transformPoint(0, -4, 0));
//					cn.p2 = new Point3D(transform.transformPoint(0, 4, 0));
//					cn.delta = new Point3D(-cn.p1.x,-cn.p1.y,-cn.p1.z);
//					cn.addConnection(cp, false);
//					continue;
//				}
				LDrawPart ldpt = LDrawPart.getPart(p.getLdrawId());
				if (ldpt != null) { 
					extractConnections(partId,cp,ldpt.getPrimitives(), 
							transform, p.isInvert()^invert);
				}
				break;
			default:
				//System.out.println("[LDRenderedPart] Unknown primitive:\n"+p.toString());
				break;
			}
		}
	}

	
	
	
	private void addConnection(List<ConnectionPoint> cp, boolean check) {
		
		//System.out.println("Called: "+check+"-"+c+"-"+p1);  // DB
		if (check) {
			// checks if duplicated
			for (ConnectionPoint ch : cp) {
				if (ch.c != c)		// different connection type 
					continue;
				//System.out.println(Math.abs(p1.y-ch.p1.y)+" - "+Math.abs(p1.y-ch.p1.y));
				if (Math.abs(p1.x-ch.p1.x) < 0.001 &&
						Math.abs(p1.y-ch.p1.y) < 0.001 &&
						Math.abs(p1.z-ch.p1.z) < 0.001 &&
						Math.abs(p2.x-ch.p2.x) < 0.001 &&
						Math.abs(p2.y-ch.p2.y) < 0.001 &&
						Math.abs(p2.z-ch.p2.z) < 0.001) {
					// it is a duplicated connection point
					//System.out.println("Duplicated! x1=" + p1.x+" y1="+p1.y+" z1="+p1.z+" id="+ch.id);
					return;
				}
			}
		}
		cp.add(this);
	}
	
	
	
	public ConnectionPoint transform(Matrix3D m) {
		
		ConnectionPoint cp = new ConnectionPoint(getPartId());
		cp.c = c;
		cp.p1 = p1.transform(m);
		cp.p2 = p2.transform(m);
		// delta is relative to the origin, so use transformNormal(x,y,z)
		// to recalc delta relative to p1 after a transformation
		//cp.delta = new Point3D(m.transformNormal(delta.x,delta.y,delta.z)); 
		Point3D p = p1.vector(Point3D.ORIGIN);
		cp.delta = new Point3D(m.transformNormal(p.x, p.y, p.z));
		return cp;
	}
	
	
	
	public ConnectionPoint fastMove(Point3D pos) {
		
		ConnectionPoint cp = new ConnectionPoint(getPartId());
		cp.c = c;
		cp.delta = new Point3D(delta);
		cp.p1 = p1.translate(pos);
		cp.p2 = p2.translate(pos);
		return cp;
	}
	
	
	/**
	 * reads connections from a cxml file
	 * @param cxml
	 * @return list of connections
	 * @throws XMLStreamException 
	 * @throws FileNotFoundException 
	 */
	public static List<ConnectionPoint> readFromFile(int partId, InputStream cxml) throws FileNotFoundException, XMLStreamException {
		
		XMLInputFactory xmlFact = XMLInputFactory.newInstance();
		xmlFact.setProperty(XMLInputFactory.IS_COALESCING,true);
		XMLEventReader xer = xmlFact.createXMLEventReader(cxml);
		boolean isDoc = false;
		boolean isConnList = false;
		boolean isConn = false;
		int type = -1;
		List<ConnectionPoint> connList = new ArrayList<ConnectionPoint>();
		ConnectionPoint cp;
		Point3D p1 = null,p2 = null,delta= null;
		String tag;
		while (xer.hasNext()) {
			XMLEvent e = xer.nextEvent();
			switch (e.getEventType()) {
			case XMLEvent.START_DOCUMENT:
				isDoc = true;
				break;
			case XMLEvent.START_ELEMENT:
				tag = e.asStartElement().getName().getLocalPart();
				if (tag.equals("connections") && isDoc) {
					isConnList = true;
				}
				else if (tag.equals("cpoint") && isConnList) {
					try {
						type = ConnectionTypes.getByName(e.asStartElement().getAttributeByName(new QName("type")).getValue().toUpperCase().trim()).getId();
						isConn = true;
					}
					catch (NullPointerException ex) {
						System.out.println("Unknown connection type (line#"+e.getLocation().getLineNumber()+"): "+e);
					}
				}
				else if (tag.equals("base") && isConn) {
					try {
						float x = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("x")).getValue());
						float y = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("y")).getValue());
						float z = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("z")).getValue());
						p1 = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						p1 = new Point3D(0, 0, 0);
					}
					delta = p1.vector(Point3D.ORIGIN);
				}
				else if (tag.equals("dir") && isConn) {
					try {
						float x = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("x")).getValue());
						float y = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("y")).getValue());
						float z = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("z")).getValue());
						p2 = new Point3D(x, y, z);
					}
					catch (NumberFormatException ex) {
						p2 = new Point3D(0, 0, 0);
					}
				}
//				else if (tag.equals("orient") && isConn) {
//					try {
//						float x = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("x")).getValue());
//						float y = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("y")).getValue());
//						float z = Float.parseFloat(e.asStartElement().getAttributeByName(new QName("z")).getValue());
//						o = new Point3D(x, y, z);
//					}
//					catch (NumberFormatException ex) {
//						o = new Point3D(0, 0, 0);
//					}
//				}
				break;
			case XMLEvent.END_ELEMENT:
				tag = e.asEndElement().getName().getLocalPart();
				if (tag.equals("cpoint")) {
					cp = createCPoint(type);
					cp.setP1(p1);
					cp.setP2(p2);
					cp.delta = delta;
					connList.add(cp);
					isConn = false;
				}
				else if (tag.equals("connections")) {
					isConnList = false;
				}
			}
		}
		xer.close();
		return connList;
	}
	
	
	
	private static List<ConnectionPoint> computeConnectionPoints(LDPrimitive pt) {
		
		ArrayList<ConnectionPoint> cp = new ArrayList<ConnectionPoint>();
		//System.out.println(pt.getLdrawid()+" - "+pt.getPartType()+" - "+pt.getPrimitives().size());
		extractConnections(pt.getId(),cp, pt.getPrimitives(), /*new Matrix3D()*/pt.getTransformation(), false);
		//detectConnections(pt.getLdrawid(), cp, new Matrix3D());
		//System.out.println(cp); // DB
		return cp;
	}
	
	
	
	/**
	 * returns true if part has connection defined by file
	 * @param ldrid LDraw id of part to check for connections
	 * @return
	 */
	public static boolean existsConnectionFile(String ldrid) {
		
		
		if (ConnectionTypes.getPrimitiveConns(ldrid) != null) 
			return false;
		String filepart = getPartMainName(ldrid);
//		if (ldrid.toLowerCase().matches("[0-9]+[a-oq-z]*p.+.dat") 
//				&& ConnectionTypes.getPrimitiveConns(ldrid) == null) {
//			// it is a printed part, get "main" part"
//			filepart = ldrid.substring(0, ldrid.toLowerCase().indexOf('p'));
//		}
//		else {
//			// no part marker
//			if (ldrid.toLowerCase().endsWith(".dat")) {
//				filepart = ldrid.toLowerCase().substring(0, ldrid.length()-4);
//			}
//			else {
//				filepart = ldrid.toLowerCase();
//			}
//		}
		if (connZip != null) {
			ZipEntry ze = connZip.getEntry(ZIPFOLDER+filepart+CONNEXT);
			return ze!= null;
		}
		else {
			File cf = new File(CONNFOLDER,filepart+CONNEXT);
			if (cf.isFile() && cf.canRead()) {
				return true;
			}
		}
		return false;
	}
	
	
	
	public static boolean isAutoconnChecked(String ldrid) {
		
		
		if (ConnectionTypes.getPrimitiveConns(ldrid) != null) 
			return true;
		String filepart = getPartMainName(ldrid);
//		if (ldrid.toLowerCase().matches("[0-9]+[a-oq-z]*p.+.dat") 
//				&& ConnectionTypes.getPrimitiveConns(ldrid) == null) {
//			// it is a printed part, get "main" part"
//			filepart = ldrid.substring(0, ldrid.toLowerCase().indexOf('p'));
//		}
//		else {
//			// no part marker
//			if (ldrid.toLowerCase().endsWith(".dat")) {
//				filepart = ldrid.toLowerCase().substring(0, ldrid.length()-4);
//			}
//			else {
//				filepart = ldrid.toLowerCase();
//			}
//		}
		return autoConnSet.contains(filepart);
	}

	
	
	
	/**
	 * Computes connection points for a primitive as a part
	 * @param p
	 */
	public static ConnectionPoint[] getConnections(LDPrimitive p) {
		
		if (p.getType() != LDrawCommand.REFERENCE)
			return new ConnectionPoint[0];
		String filepart;
		if (ConnectionTypes.getPrimitiveConns(p.getLdrawId()) != null) {
			// it is a primitive with ".dat" extension. To avoid false detect we put a 
			// unreachable name in cache
			filepart = p.getLdrawId().toLowerCase().substring(0, p.getLdrawId().length()-4);
		}
		else {
			filepart = getPartMainName(p.getLdrawId());
			//System.out.println(filepart);
		}
//		if (p.getLdrawId().toLowerCase().matches("[0-9]+[a-oq-z]*p.+.dat") //p.getLdrawId().toLowerCase().indexOf('p') >= 0 
//				&& ConnectionTypes.getPrimitiveConns(p.getLdrawId()) == null) {
//			// it is a printed part, get "main" part"
//			filepart = p.getLdrawId().substring(0, p.getLdrawId().toLowerCase().indexOf('p'));
//		}
//		else {
//			// no part marker
//			if (p.getLdrawId().toLowerCase().endsWith(".dat")) {
//				filepart = p.getLdrawId().toLowerCase().substring(0, p.getLdrawId().length()-4);
//			}
//			else {
//				filepart = p.getLdrawId().toLowerCase();
//			}
//		}
		//System.out.println(p.getLdrawId()+" - "+filepart);  //DB
		// if part connections isn't in cache
		if (!connectionsCache.containsKey(filepart)) {
			boolean found = false;
			try {
				// try first if a connection file exists
				if (connZip != null) {
					ZipEntry ze = connZip.getEntry(ZIPFOLDER+filepart+CONNEXT);
					if (ze!= null) {
						InputStream cf = connZip.getInputStream(ze);
						connectionsCache.put(filepart,readFromFile(0, cf));
						found = true;
					}
				}
				else {
					File cf = new File(CONNFOLDER,filepart+CONNEXT);
					if (cf.isFile() && cf.canRead()) {
						InputStream cfis = new FileInputStream(cf);
						connectionsCache.put(filepart,readFromFile(0, cfis));
						found = true;
					}
				}
			} catch (XMLStreamException e) {
				System.out.println("Error reading connection file: "+p.getLdrawId());
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("Error reading connection file: "+p.getLdrawId());
				e.printStackTrace();
			}
			// if no file were found
			if (!found) {
				// try detect connections from primitives
				LDrawPart ldp = LDrawPart.getPart(p.getLdrawId());
				if (ldp != null) {
					if (ldp.getPrimitives().size() != 0) {
						//System.out.println("Stored: "+filepart+" "+p.getLdrawId());  // DB
			//			throw new NullPointerException();
						connectionsCache.put(filepart, 
								computeConnectionPoints(LDPrimitive.getDummyPart(p.getLdrawId())));
					}
				}
			}
		}
		// System.out.println("Req: "+filepart);    // DB
		List<ConnectionPoint> lcp = connectionsCache.get(filepart);
		ConnectionPoint[] listCp;
		if (lcp == null) {
			listCp = new ConnectionPoint[0];
			return listCp;
		}
		else {
			listCp = new ConnectionPoint[lcp.size()];
		}
		int index = 0;
		for (ConnectionPoint cp: lcp) {
			// transform connection point
//			if (cp.c.getName().equals("CLIP")) {
//				System.out.println(cp);   // DB
//			}
			ConnectionPoint transformedCp = cp.getClone(p.getId()).transform(p.getTransformation());
			listCp[index++] = transformedCp;
		}
		return listCp;
	}
	
	
	
	static void removeFromCache(String ldrawId) {
		
		if (connectionsCache.containsKey(ldrawId)) {
			connectionsCache.remove(ldrawId);
		}
	}
	
	
	
	
	public static void clearCache() {
		
		connectionsCache.clear();
	}
	
	
	
	/**
	 * create new connection point of type ct
	 * @param ct type of connection
	 * @return empty connection point of type ct
	 */
	public static ConnectionPoint createCPoint(int ct) {
		ConnectionPoint p = new ConnectionPoint();
		p.id = getUniqueId();
		p.c = ConnectionTypes.getById(ct);
		return p;
	}


	public void XMLWrite(XMLStreamWriter xsw) throws XMLStreamException {
		
		xsw.writeStartElement("cpoint");
		xsw.writeAttribute("type",c.getName());
		xsw.writeCharacters("\n");
		xsw.writeStartElement("base");
		xsw.writeAttribute("x",Float.toString(getP1().x));
		xsw.writeAttribute("y",Float.toString(getP1().y));
		xsw.writeAttribute("z",Float.toString(getP1().z));
		xsw.writeEndElement();
		xsw.writeCharacters("\n");
		xsw.writeStartElement("dir");
		xsw.writeAttribute("x",Float.toString(getP2().x));
		xsw.writeAttribute("y",Float.toString(getP2().y));
		xsw.writeAttribute("z",Float.toString(getP2().z));
		xsw.writeEndElement();
		xsw.writeCharacters("\n");
		xsw.writeEndElement();
		xsw.writeCharacters("\n");
	}

	

	
}
