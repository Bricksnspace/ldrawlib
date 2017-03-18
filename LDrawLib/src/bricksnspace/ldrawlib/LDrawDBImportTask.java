/*
	Copyright 2014 Mario Pascucci <mpascucci@gmail.com>
	This file is part of LDrawDB

	LDrawDB is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	LDrawDB is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with LDrawDB.  If not, see <http://www.gnu.org/licenses/>.

*/


package bricksnspace.ldrawlib;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;



/**
 * @author Mario Pascucci
 *
 */
public class LDrawDBImportTask extends SwingWorker<Integer, Void> {

	
	LDrawLib ldr = null;
	LDrawLibDB ldrdb = null;
	//boolean useAllParts;
	int index;
	
	
	/**
	 * Import parts in database
	 * @param l	LDraw library
	 * @param index	index of library in list
	 * @param useAllParts	if true imports all ".dat" files in library
	 */
	public LDrawDBImportTask(LDrawLib l, int index) {
		
		ldr = l;
		ldrdb = ldr.getLdrDB();
//		this.useAllParts = useAllParts;
		this.index = index;
	}

	
	@Override
	protected Integer doInBackground() throws Exception {

		setProgress(0);
//		int i = 0;
//		int parts = 0;
//		// Prepare for table update
//		ldrdb.prepareUpdate();
//		try {
//			for (int j=0;j<ldr.count();j++) {
//				// insert only official parts or all, excluding disabled libraries
//				if ((officialOnly && !ldr.isOfficial(j)) || !ldr.isEnabled(j)) {
//					continue;
//				}
//				Collection<String> allParts = ldr.getAllParts(j);
//				for (String part:allParts) {
//					i++;
//					setProgress(i*100/allParts.size());
//					if (part.toLowerCase().indexOf("parts") >= 0) {
//						// it is a part
//						if (part.toLowerCase().indexOf("parts/s/") == -1) {
//							// it isn't a subpart
//							File partpath = new File(part);
//							LDrawPart p = LDrawPart.getLDrawPartMeta(partpath.getName());
//							ldrdb.insert(p,j,ldr.isOfficial(j),ldr.isEnabled(j));
//							parts++;
//						}
//					}
//				}
//			}
//		}
		int parts = 0;
		int i = 0;
		setProgress(0);
		// disabled library?
//		if (!ldr.isEnabled(index)) {
//			Logger.getGlobal().log(Level.WARNING, "[LDrawDB] Importing disabled library?");
//		}
		ldrdb.prepareUpdate(index);
		try {
			Collection<String> allParts = ldr.getAllParts(index);
			if (ldr.isLDrawStd(index)) {
				// import only parts in parts/ path, but not in parts/s/
				for (String part:allParts) {
					i++;
					setProgress(i*100/allParts.size());
					if (part.toLowerCase().indexOf("parts/") >= 0) {
						// it is a part
						if (part.toLowerCase().indexOf("parts/s/") == -1) {
							// it isn't a subpart
							File partpath = new File(part);
							LDrawPart p = LDrawPart.getLDrawPartMeta(partpath.getName());
							ldrdb.insert(p,index,ldr.isOfficial(index),ldr.isEnabled(index));
							parts++;
						}
					}
				}
			}
			else {
				// import all .dat files
				for (String part:allParts) {
					i++;
					setProgress(i*100/allParts.size());
					if (part.toLowerCase().endsWith(".dat")) {
						// it is a part
						File partpath = new File(part);
						LDrawPart p = LDrawPart.getLDrawPartMeta(partpath.getName());
						if (p == null) {
							Logger.getGlobal().log(Level.SEVERE,"[LDrawDBImportTask] Unable to find part: "+part);
							continue;
						}
						ldrdb.insert(p,index,ldr.isOfficial(index),ldr.isEnabled(index));
						parts++;
					}
				}
				
				
			}
		}
		catch (IOException ioex) {
			Logger.getGlobal().log(Level.SEVERE, "[LDrawDBImportTask] Error reading LDraw parts", ioex);
			ldrdb.abortUpdate();
			throw ioex;
		}
		catch (SQLException sqlex) {
			Logger.getGlobal().log(Level.SEVERE, "[LDrawDBImportTask] Error in part database", sqlex);
			ldrdb.abortUpdate();
			throw sqlex;
		}
		ldrdb.endUpdate();
		return parts;
	}
	
	
}
