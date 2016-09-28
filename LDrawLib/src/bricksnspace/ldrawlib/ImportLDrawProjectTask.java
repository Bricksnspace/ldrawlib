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


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import javax.swing.SwingWorker;






/*
 * Imports in background an LDraw project file
 * @see javax.swing.SwingWorker
 */
public class ImportLDrawProjectTask extends SwingWorker<Integer, Void> {

	File ldr;
	private LDrawPart mainModel;	// model imported
	private String internalLog = "";		// internal log for errors and warnings
	private boolean warnings = false;
	private boolean isMpd;

	
	public ImportLDrawProjectTask(File dat) {
		
		ldr = dat;
	}

	
	public LDrawPart getModel() {
		return mainModel;
	}
	

	
	public boolean isWarnings() {
		return warnings;
	}


	public boolean isMpd() {
		return isMpd;
	}


	public String getInternalLog() {
		return internalLog;
	}


	private void addLogLine(String file, int line, String message) {

		warnings = true;
		if (line != 0)
			internalLog += "[" +file + "] line# "+ line + "> " + message + "\n";
		else 
			internalLog += "[" +file + "] >" + message + "\n";
			
	}
	

	
	@Override
	protected Integer doInBackground() throws IOException {
		
		int count=0;
		String modelDir,part;
		LDrawPart currModel = null;
		LDPrimitive p = null;
		//LDrawPartType partType;
		
		LineNumberReader lnr = new LineNumberReader(new FileReader(ldr));
		String line;
		int lineNo = 0;
		while ((line = lnr.readLine()) != null) {
			lineNo++;
		}
		lnr.close();
		// we cheats about complete
		lineNo += 10;
		setProgress(0);
		isMpd = false;
		modelDir = ldr.getParent();
		lnr = new LineNumberReader(new FileReader(ldr));
		while ((line = lnr.readLine()) != null) {
			LDrawCommand type = LDrawParser.parseCommand(line);
			if (type == LDrawCommand.MPDFILE) {
				isMpd = true;
				break;
			}
		}
		lnr.close();
		lnr = new LineNumberReader(new FileReader(ldr));
		boolean isFirstModel = true;
		boolean isMainModel = false;
		boolean isSubModel = false;
		if (isMpd) {
			// gets all sub-model and custom part in MPD
			// and save as LDrawCustomPart
			boolean firstLine = false;
			boolean isSkip = false;
			//partType = LDrawPartType.UNKNOWN;
			while ((line = lnr.readLine()) != null) {
				LDrawCommand type = LDrawParser.parseCommand(line);
				if (isSkip && type != LDrawCommand.MPDFILE && type != LDrawCommand.MPDNOFILE) {
					continue;
				}
				if (!isMainModel && !isSubModel &&
						type != LDrawCommand.MPDFILE && type != LDrawCommand.MPDNOFILE) {
					addLogLine(ldr.getName(), lnr.getLineNumber(), 
                   			"Invalid MPD file format: primitive or command outside FILE..NOFILE block:"+line );
					continue;
				}
				try {
					switch (type) {
					case MPDFILE:
						// if a new FILE command is found without NOFILE first...
						if (isMainModel) {
							mainModel = currModel;
						}
						isMainModel = false;
						isSubModel = false;
						isSkip = false;
						part = LDrawParser.parseMpdFile(line);
	                    if (isFirstModel) {
	                    	isFirstModel = false;
	                    	isMainModel = true;
	                    }
	                    else {
	                    	isSubModel = true;
	                    }
	                    if (LDrawPart.existsCustomPart(part)) {
	                        //------------------- duplicate submodel name
	                    	addLogLine(ldr.getName(), lnr.getLineNumber(), 
	                    			"[Warn] Duplicate name '" + part + "' in MPD");
	                    	//System.out.println("[Warn] Duplicate sub-model name '" + part + "' in MPD");
	                    }
	                    // avoid overwriting of internal use parts
	                    if (!LDrawPart.existsInternalUsePart(part)) {
	                    	//System.out.println("create: "+part);   // DB
	                    	currModel = LDrawPart.newCustomPart(part);
	                    	if (isFirstModel) {
	                    		currModel.setPartType(LDrawPartType.MODEL);
	                    	}
	                    	else {
	                    		currModel.setPartType(LDrawPartType.SUBMODEL);
	                    	}
	                    }
	                    else {
	                    	isSubModel = false;
	                    	isMainModel = false;
	                    	isSkip = true;
	                    }
	                    firstLine = true;
	                    break;
					case MPDNOFILE:
						if (!isSubModel && !isMainModel && ! isSkip) {
	                    	addLogLine(ldr.getName(), lnr.getLineNumber(), 
	                    			"Displaced 'NOFILE' in MPD");   
						}
						if (!isSkip && isMainModel) {
							mainModel = currModel;
						} 
						isMainModel = false;
						isSubModel = false;
						isSkip = false;
						break;
					case FILETYPE:
						currModel.setPartType(LDrawParser.parsePartType(line));
						currModel.setPartTypeString(line);
						break;
					case AUTHOR:
						currModel.setAuthor(LDrawParser.parseAuthor(line));
						break;
					case NAME:
						currModel.setPartName(LDrawParser.parsePartName(line));
						break;
					case TRIANGLE:
					case LINE:
					case AUXLINE:
					case QUAD:
					case COMMENT:
					case EMPTY:
						break;
					case META_UNKNOWN:
						if (firstLine) {
							currModel.setDescription(LDrawParser.parseDescription(line));
							firstLine = false;
						}
						break;
					default:
						break;
					}
				}
				catch (LDrawException exc) {
					addLogLine(ldr.getName(), lnr.getLineNumber(), exc.getLocalizedMessage());
				}
			}
			lnr.close();
			// id an MPD file has no "NOFILE" and only one model
			if (isMainModel) {
				mainModel = currModel;
			} 
			lnr = new LineNumberReader(new FileReader(ldr));
			// now read model file
			isSubModel = false;
			isFirstModel = true;
			isMainModel = false;
			boolean isClockWise = false;
			boolean invNext = false;
			isSkip = false;
			firstLine = false;
			//partType = LDrawPartType.UNKNOWN;
			while ((line = lnr.readLine()) != null) {
				setProgress(lnr.getLineNumber()*100/lineNo);
				LDrawCommand type = LDrawParser.parseCommand(line);
				if (isSkip && type != LDrawCommand.MPDFILE && type != LDrawCommand.MPDNOFILE) {
					continue;
				}
				if (!isMainModel && !isSubModel &&
						type != LDrawCommand.MPDFILE && type != LDrawCommand.MPDNOFILE) {
					addLogLine(ldr.getName(), lnr.getLineNumber(), 
                   			"Invalid MPD file format: primitive or command outside FILE..NOFILE block:"+line );
					continue;
				}
				//System.out.println(type + "-"+line);
				try {
					switch (type) {
					case MPDFILE:
						// in case FILE command is found without NOFILE first
	                    if (isSubModel) {
	                        //--------- we are in submodel, close and prepare next submodel
	                        isSubModel = false;
	                        isSkip = false;
	                        if (isMainModel) {
	                        	// end loading main model
	                        	isMainModel = false;
	                        }
	                    }
						isClockWise = false;
						invNext = false;
						part = LDrawParser.parseMpdFile(line);
						//System.out.println(part);
	                    isSubModel = true;
	                    if (isFirstModel) {
	                    	isFirstModel = false;
	                    	isMainModel = true;
	                    }
                    	if (!LDrawPart.existsInternalUsePart(part)) {
                    		currModel = LDrawPart.getCustomPart(part);
                    	}
                    	else {
                    		isMainModel = false;
                    		isSubModel = false;
                    		isSkip = true;
                    	}
	                    firstLine = true;
	                    break;
					case MPDNOFILE:
						isSkip = false;
	                    if (isSubModel) {
	                        //--------- we are in submodel, close and prepare next submodel
	                        isSubModel = false;
	                        if (isMainModel) {
	                        	// end loading main model
	                        	isMainModel = false;
	                        }
	                    }
	                    break;  // no parts alone admitted in MPD files, so we are always in submodel
					case KEYWORDS:
						p = LDPrimitive.newKeywords(LDrawParser.parseKeywords(line));
                   		currModel.addPart(p);
                    	break;						
					case CATEGORY:
						p = LDPrimitive.newCategory(LDrawParser.parseCategory(line));
                   		currModel.addPart(p);
                    	break;						
					case COMMENT:
						p = LDPrimitive.newComment(line);
                    	currModel.addPart(p);
                    	break;
					case LICENSE:
						currModel.setLicense(line);
						break;
					case HISTORY:
						p = LDPrimitive.newHistory(line);
                    	currModel.addPart(p);
                    	break;						
					case EMPTY:
						p = LDPrimitive.newEmpty();
                   		currModel.addPart(p);
                    	break;
					case COLOUR:
						currModel.addPart(
									LDPrimitive.newColour(LDrawParser.parseColour(line),line));
						break;
					case META_UNKNOWN:
						if (firstLine) {
							firstLine = false;
							break;
						}
						p = LDPrimitive.newMetaUnk(LDrawParser.parseMetaUnk(line));
                    	currModel.addPart(p);
                    	break;						
					case STEP:
                    	currModel.nextStep();
                    	break;
					case BFC_CCW:
						p = LDPrimitive.newBfcCcw();
                    	currModel.addPart(p);
						isClockWise = false;
						break;
					case BFC_CW:
						p = LDPrimitive.newBfcCw();
                   		currModel.addPart(p);
						isClockWise = true;
						break;
					case BFC_INVERTNEXT:
						p = LDPrimitive.newBfcInvertnext();
                   		currModel.addPart(p);
						invNext = true;
						break;
					case REFERENCE:
						p = LDrawParser.parseLineType1(line,invNext);
						invNext = false;
	                    if (LDrawPart.existsPart(p.getLdrawId())) {
    						//System.out.println("MainModel - " +p);
                    		// add to main model
                    		currModel.addPart(p);
	                    	count++;
	                    }
	                    else {
                    		addLogLine(currModel.getLdrawId(), lnr.getLineNumber(), 
	                        			"Unknown submodel or part: "+p.getLdrawId());
	                    }
						break;
					case TRIANGLE: 
						p = LDrawParser.parseLineType3(line,isClockWise);
                   		// add to model
                   		currModel.addPart(p);
                    	count++;
                    	break;
					case AUXLINE:
						p = LDrawParser.parseLineType5(line);
                   		// add to model
                   		currModel.addPart(p);
                    	count++;
						break;
					case LINE:
						p = LDrawParser.parseLineType2(line);
                   		// add to model
                   		currModel.addPart(p);
                    	count++;
						break;
					case QUAD:
						p = LDrawParser.parseLineType4(line,isClockWise);
                   		// add to model
                   		currModel.addPart(p);
                    	count++;
						break;
					default:
						break;
					}
				}
				catch (LDrawException exc) {
					addLogLine(ldr.getName(), lnr.getLineNumber(), exc.getLocalizedMessage());
				}
			}
			// recalc connections... yes.
			for (LDPrimitive prim: mainModel.getPrimitives()) {
				if (LDrawPart.existsCustomPart(prim.getLdrawId())) {
					prim.recalcConnPoints();
				}
			}
		}
		else {		// it is LDR/DAT format
			mainModel = LDrawPart.newCustomPart(ldr.getName());
			mainModel.setPartType(LDrawPartType.MODEL);
			boolean isClockWise = false;
			boolean invNext = false;
			boolean firstLine = true;
			while ((line = lnr.readLine()) != null) {
				//System.out.println(line);
				setProgress(lnr.getLineNumber()*100/lineNo);
				LDrawCommand type = LDrawParser.parseCommand(line);
				try {
					switch (type) {
					case STEP:
						mainModel.nextStep();
                   		break;
					case BFC_CCW:
						mainModel.addPart(LDPrimitive.newBfcCcw());
						isClockWise = false;
						break;
					case BFC_CW:
						mainModel.addPart(LDPrimitive.newBfcCw());
						isClockWise = true;
						break;
					case AUTHOR:
						mainModel.setAuthor(LDrawParser.parseAuthor(line));
						break;
					case LICENSE:
						mainModel.setLicense(line);
						break;
					case BFC_INVERTNEXT:
						mainModel.addPart(LDPrimitive.newBfcInvertnext());
						invNext = true;
						break;
					case REFERENCE:
						p = LDrawParser.parseLineType1(line,invNext);
	                    if (!LDrawPart.isLdrPart(p.getLdrawId())) {
	                    	// not a LDraw part, checks if it is a submodel
	                		File ld = new File(modelDir,p.getLdrawId());
	                		if (!ld.exists()) {
	                			// old part or error in file
	                        	addLogLine(ldr.getName(), lnr.getLineNumber(), 
	                        			"Unknown part: "+p.getLdrawId());   
	                		}
	                		else {
	                			count++;
	                			//System.out.println("SubModel: " + part);
	                			if (!LDrawPart.existsPart(p.getLdrawId())) {
	                				// 	a new submodel             
	                				LDrawPart pt = LDrawPart.newCustomPart(p.getLdrawId());
	                				expandSubFile(pt,modelDir,false);
	                			}
	                			mainModel.addPart(LDPrimitive.newGlobalPart(p.getLdrawId(), p.getColorIndex(), p.getTransformation()));
	                		}
	                    }
	                    else {
	                    	count++;
	                    	//System.out.println("Part: " + part);
	                    	mainModel.addPart(p);
	                    }
	                    invNext = false;
	                    break;
					case COLOUR:
						mainModel.addPart(
								LDPrimitive.newColour(LDrawParser.parseColour(line),line));
						break;						
					case AUXLINE:
						p = LDrawParser.parseLineType5(line);
	               		// add to main model
	               		mainModel.addPart(p);;
	                   	count++;
						break;
					case LINE:
						p = LDrawParser.parseLineType2(line);
	              		// add to main model
	               		mainModel.addPart(p);;
	                   	count++;
						break;
					case TRIANGLE: 
						p = LDrawParser.parseLineType3(line,isClockWise);
                   		mainModel.addPart(p);
                    	count++;
                    	break;
					case QUAD:
						p = LDrawParser.parseLineType4(line,isClockWise);
                   		mainModel.addPart(p);
                    	count++;
						break;
					case META_UNKNOWN:
						if (firstLine) {
							mainModel.setDescription(LDrawParser.parseDescription(line));
							firstLine = false;
						}
						else {
							mainModel.addPart(LDPrimitive.newMetaUnk(LDrawParser.parseMetaUnk(line)));
						}
						break;
					case KEYWORDS:
						p = LDPrimitive.newKeywords(LDrawParser.parseKeywords(line));
                   		mainModel.addPart(p);
                    	break;						
					case CATEGORY:
						p = LDPrimitive.newCategory(LDrawParser.parseCategory(line));
                   		mainModel.addPart(p);
                    	break;						
					case COMMENT:
						p = LDPrimitive.newComment(line);
                   		mainModel.addPart(p);
                    	break;				
					case HISTORY:
						p = LDPrimitive.newHistory(line);
                   		mainModel.addPart(p);
                    	break;				
					case EMPTY:
                   		mainModel.addPart(LDPrimitive.newEmpty());
                    	break;										
					case FILETYPE:
						mainModel.setPartType(LDrawParser.parsePartType(line));
						mainModel.setPartTypeString(line);
						break;
					case NAME:
						mainModel.setPartName(LDrawParser.parsePartName(line));
						break;
					default:
						break;
					}
				}
				catch (LDrawException exc) {
					addLogLine(ldr.getName(), lnr.getLineNumber(), exc.getLocalizedMessage());
				}
			}
		}
		try {
			lnr.close();
		} catch (IOException ex) {
			;
		}
		//System.out.println(mainModel.getPartList());
		return count;
	}

	

	private void expandSubFile(LDrawPart model, String modelDir, boolean invert) throws IOException {

		String line;
		
		File ld = new File(modelDir,model.getLdrawId());
		// System.out.println(model); // DB
		LDPrimitive p;
		boolean isClockWise = false;
		boolean invNext = false;
		boolean firstLine = true;
		model.setPartType(LDrawPartType.SUBMODEL);
		LineNumberReader lnr = new LineNumberReader(new FileReader(ld));
		while ((line = lnr.readLine()) != null) {
			LDrawCommand type = LDrawParser.parseCommand(line);
			try {
				switch (type) {
				case STEP:
					model.nextStep();
					break;
				case BFC_CCW:
					model.addPart(LDPrimitive.newBfcCcw());
					isClockWise = false;
					break;
				case BFC_CW:
					model.addPart(LDPrimitive.newBfcCw());
					isClockWise = true;
					break;
				case BFC_INVERTNEXT:
					model.addPart(LDPrimitive.newBfcInvertnext());
					invNext = true;
					break;
				case REFERENCE:
					p = LDrawParser.parseLineType1(line,invNext);
	                if (!LDrawPart.existsPart(p.getLdrawId())) {
	                	// not a known part, checks if it is a submodel
	            		File subFile = new File(modelDir,p.getLdrawId());
	            		if (!subFile.exists() || !subFile.canRead()) {
	            			// old part or error in file
	                    	addLogLine(ldr.getName(), lnr.getLineNumber(), 
	                    			"Unknown part: "+p.getLdrawId());   
	            		}
	            		else {
	            			//System.out.println("SubModel: " + part);
	            			model.addPart(p);
            				// 	a new submodel             
            				LDrawPart pt = LDrawPart.newCustomPart(p.getLdrawId());
            				expandSubFile(pt,modelDir,false);
	            		}
	                }
	                else {
	                	//System.out.println("Part: " + part);
	                	model.addPart(p);
	                }
	                invNext = false;
	                break;
				case AUXLINE:
					p = LDrawParser.parseLineType5(line);
	           		// add to main model
					model.addPart(p);
	 				break;
				case LINE:
					p = LDrawParser.parseLineType2(line);
	          		// add to main model
					model.addPart(p);
	 				break;
				case TRIANGLE: 
					p = LDrawParser.parseLineType3(line,isClockWise);
               		model.addPart(p);
                	break;
				case QUAD:
					p = LDrawParser.parseLineType4(line,isClockWise);
              		model.addPart(p);
					break;
				case META_UNKNOWN:
					if (firstLine) {
						model.setDescription(LDrawParser.parseDescription(line));
						firstLine = false;
					}
					else {
						model.addPart(LDPrimitive.newMetaUnk(LDrawParser.parseMetaUnk(line)));
					}
					break;
				case KEYWORDS:
					p = LDPrimitive.newKeywords(LDrawParser.parseKeywords(line));
               		model.addPart(p);
                	break;						
				case CATEGORY:
					p = LDPrimitive.newCategory(LDrawParser.parseCategory(line));
               		model.addPart(p);
                	break;						
				case COMMENT:
					p = LDPrimitive.newComment(line);
               		model.addPart(p);
                	break;						
				case HISTORY:
					p = LDPrimitive.newHistory(line);
               		model.addPart(p);
                	break;				
				case EMPTY:
               		model.addPart(LDPrimitive.newEmpty());
                	break;
				case COLOUR:
					model.addPart(
							LDPrimitive.newColour(LDrawParser.parseColour(line),line));
				case FILETYPE:
					model.setPartType(LDrawParser.parsePartType(line));
					model.setPartTypeString(line);
					break;
				case NAME:
					model.setPartName(LDrawParser.parsePartName(line));
					break;
				case AUTHOR:
					model.setAuthor(LDrawParser.parseAuthor(line));
					break;
				case LICENSE:
					model.setLicense(line);
				default:
					break;
				}
			}
			catch (LDrawException exc) {
				addLogLine(ldr.getName(), lnr.getLineNumber(), exc.getLocalizedMessage());
			}
		}
	    lnr.close();        
	}
	
}

