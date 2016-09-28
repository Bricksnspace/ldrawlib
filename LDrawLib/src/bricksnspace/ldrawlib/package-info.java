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

/**
 * Low level access to LDraw part file libraries
 * <p>
 * Opens and gets parts from library files (official and unofficial) 
 * right from ZIP file or unzipped folder.
 * See http://www.ldraw.org/ for LDraw file specifications
 * 
 * @author Mario Pascucci
 *
 */
package bricksnspace.ldrawlib;



/*
 * done: remove LDLogger->useless, use "anonymous" logger
 * done: create object "LDStep" (an arraylist of arraylists) for STEP handling
 * done: modify LDrawPart.addpart(LDPrimitive p) to -> addPart(LDprimitive p) with step handling
 * 			if step == 0 -> no step handling
 * 			else -> handle step (insert in LDStep list, checks for old step)
 * 
 * TODO: dialog to list and select libraries
 * FIXME: can add more official libs?
 * TODO: !history checks for updates
 * Question: adds last modified date in cxml file? No, better checks for last date for connection update 
 * 
 * LDraw parts with anomalies
 * 
 * 3842a.dat  		uses BFC CERTIFY INVERTNEXT
 * 3842b.dat  		uses BFC CERTIFY INVERTNEXT
 * 
 * s/3068s101.dat  	uses color 391 (unknown)
 * 973psk.dat		uses color 391 (unknown)
 * 
 * 4150p02.dat		uses color 354 (unknown)
 * 973p7b.dat		uses color 354 (unknown)
 * 
 * s/2528s01.dat 	line 385 has a triangle with 4 vertices
 * 
 */

