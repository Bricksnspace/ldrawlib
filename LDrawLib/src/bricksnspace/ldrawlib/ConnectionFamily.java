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

/**
 * Connection family definition
 * 
 * @author Mario Pascucci
 *
 */
public enum ConnectionFamily {

	/** 
	 * connection with vector orientation and a single position
	 * 
	 * It has one degree of freedom, rotation around vector axis
	 * Example: stud, Technic pin, tyre/rim assembly, hinges 
	 */
	VECTOR,
	/** 
	 * connection with vector along a rail, position along rail
	 * 
	 * It has two degree of freedom, rotation around vector axis and move along vector
	 */
	RAIL,
	/**
	 * connection with a single point position
	 * 
	 * It has three degree of freedom, rotation along x,y,z axis with origin on connection point
	 */
	POINT;
	
	
}
