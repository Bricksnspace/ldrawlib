/*
	Copyright 2017 Mario Pascucci <mpascucci@gmail.com>
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
 * LDraw color type
 * 
 * @author Mario Pascucci
 *
 */
public enum LDrawColorType {
	
	/** User defined */
	USERDEF,
	/** Solid color */
	SOLID,
	/** Transparent */
	TRANSPARENT,
	/** Pearlescent or flat */
	PEARL,
	/** Metal (paint or ink) */
	METAL,
	/** Chrome metallic */
	CHROME,
	/** Milky translucent or glow */
	MILKY,
	/** Glitter or speckle */
	GLITTER,
	/** Rubber parts */
	RUBBER,
	/** Internal or special use */
	INTERNAL;
	
	
	
	@Override
	public String toString() {
		
		switch (this) {
		case CHROME:
			return "Chrome";
		case GLITTER:
			return "Glitter/Speckle";
		case INTERNAL:
			return "Internal/Special use";
		case METAL:
			return "Metal";
		case MILKY:
			return "Milky/Glow";
		case PEARL:
			return "Pearl";
		case RUBBER:
			return "Rubber";
		case SOLID:
			return "Solid";
		case TRANSPARENT:
			return "Transparent";
		case USERDEF:
			return "User defined";
		}
		// unreachable code...
		return "Unknown";
	}
	
}



