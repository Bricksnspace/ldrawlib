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

/**
 * Type of LDraw Part from library or custom/subpart/primitive
 * @author Mario Pascucci
 *
 */
public enum LDrawPartType {
	/** official part from library */
	OFFICIAL,			
	/** official subpart from library (in s\ folder) */
	SUBPART,
	/** official primitive from library (in p\ folder) */
	PRIMITIVE, PRIMITIVE48, PRIMITIVE8,
	/** unofficial part from unofficial library */
	UNOFFICIAL,	UNOFF48, UNOFF8,
	/** unofficial subpart from unofficial library (in s\ folder) */
	UNOFF_SUB,			
	/** unofficial primitive from unofficial library (in p\ folder) */
	UNOFF_PRIM,			
	/** basic geometric primitive (line, triangle, quad...) */
	GEOM_PRIMITIVE,		
	/** rendering comand/option (STEP, !COLOUR, SAVE, ecc...) */
	COMMAND,			
	/** an LDraw model */
	MODEL,
	/** a submodel/subfile */
	SUBMODEL,
	/** a shortcut, i.e. a grouped parts in one main part like 73200.dat */
	SHORTCUT,
	/** unofficial shortcut */
	UNOFF_SHORTCUT,
	/** Unknown/non-existent part */
	NONE;

	
	
	public String asString() {
		
		switch (this) {
		case MODEL:
			return "Model";
		case OFFICIAL:
			return "Part";
		case PRIMITIVE:
			return "Primitive";
		case SHORTCUT:
			return "Shortcut";
		case SUBMODEL:
			return "Submodel";
		case SUBPART:
			return "Subpart";
		case UNOFFICIAL:
			return "Unofficial_Part";
		case UNOFF_PRIM:
			return "Unofficial_Primitive";
		case UNOFF_SHORTCUT:
			return "Unofficial_Shortcut";
		case UNOFF_SUB:
			return "Unofficial_Subpart";
		case COMMAND:
			return "Ldraw Meta command";
		case GEOM_PRIMITIVE:
			return "Geometric Primitive";
		case PRIMITIVE48:
			return "48_Primitive";
		case PRIMITIVE8:
			return "8_Primitive";
		case UNOFF48:
			return "Unofficial_48_Primitive";
		case UNOFF8:
			return "Unofficial_8_Primitive";
		case NONE:
			return "Model";
		}
		return "Model";
	}
	
	
	

}
