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
 * LDrawLib unchecked Exception
 * 
 * Used for parsing error, invalid part/color requests.
 * N.B.: internal LDraw library errors are reported via Logger, not via Exceptions
 * 
 * @author Mario Pascucci
 *
 */
public class LDrawException extends Exception {

	private static final long serialVersionUID = 2301053348174907145L;

	/**
	 * @param c
	 * @param message
	 */
	public LDrawException(Class<?> c, String message) {

		super("["+c.getName()+"] " + message);
	}

	/**
	 * @param c
	 * @param message
	 * @param cause
	 */
	public LDrawException(Class<?> c, String message, Throwable cause) {
		
		super("["+c.getName()+"] " + message, cause);
	}

}
