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

/**
 * Type of META commands
 * 
 * @author mario
 *
 */
public enum LDrawCommand {
	COLOUR, SAVE, STEP,
	BFC_INVERTNEXT, BFC_CW, BFC_CCW,
	CATEGORY, KEYWORDS, MPDFILE, MPDNOFILE, 
	NAME, AUTHOR, EMPTY, COMMENT,
	REFERENCE, LINE, TRIANGLE, QUAD, AUXLINE,
	UNKNOWN, META_UNKNOWN, FILETYPE, LDRAW_ORG, HISTORY, 
	LICENSE
}
