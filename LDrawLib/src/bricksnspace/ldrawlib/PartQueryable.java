/*
	Copyright 2015-2017 Mario Pascucci <mpascucci@gmail.com>
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
 * 
 * Interface to allow query for hidden parts in connection search
 * so you can ignore hidden parts for connecting new parts<br/>
 * <br/>
 * Editors may implement this interface to allow selectively exclude parts for connecting a new one<br/>
 * If it is not necessary, add a function that return always <code>false</code>.
 * 
 * @author Mario Pascucci
 *
 */
public interface PartQueryable {

	/**
	 * Checks if part is available in connection search
	 * @param id part identifier (handle) 
	 * @return true if part is hidden and must be ignored in connection search
	 */
	public boolean isHidden(int id);
}
