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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handling for "0 STEP" LDraw instruction
 * 
 * @author Mario Pascucci
 *
 */
public class LDStep {
	
	private List<List<LDPrimitive>> steps = new ArrayList<List<LDPrimitive>>();
	private int currStep = 0;
	
	
	public LDStep() {
	
	}


	/**
	 * gets current step
	 * @return current step or 0 if no step are defined
	 */
	public int getCurrStep() {
		return currStep;
	}


	public void setCurrStep(int currStep) {
		this.currStep = currStep;
	}
	
	
	
	public boolean hasStep() {
		
		return steps.size() > 0;
	}
	
	
	
	public int getNumSteps() {
		
		return steps.size();
	}
	
	
	
	public List<LDPrimitive> getStep(int n) {
		
		if (n > steps.size() || steps.size() == 0 || n == 0) 
			return new ArrayList<LDPrimitive>();
		return steps.get(n-1);
	}
	
	
	
	public int goFirstStep() {
		
		if (hasStep()) {
			currStep = 1;
		}
		return currStep;
	}
	
	
	
	public int goLastStep() {
		
		if (hasStep()) {
			currStep = steps.size();
		}
		return currStep;
	}
	
	
	
	public int getTotalSteps() {
		
		return steps.size();
	}
	
	
	
	public int nextStep() {
		
		// go to next step only if at least one part is in current step
		if (currStep == 0) {
			currStep++;
		}
		else if (currStep == steps.size()) {
			if (steps.get(currStep-1) != null && steps.get(currStep-1).size() != 0) {
				currStep++;
			}
		}
		else if (currStep < steps.size()) {
			currStep++;
		}
		return currStep;
	}
	
	
	public int prevStep() {
		
		if (currStep > 1) {
			currStep--;
		}
		return currStep;
	}
	
	
	/**
	 * Add a part to current step. If no step are defined, do nothing
	 * @param p part to add
	 */
	public void addPart(LDPrimitive p) {
		
		if (currStep == 0) {
			// no step, do nothing
			return;
		}
		else {
			// if no parts in step, create new step
			if (steps.size() < currStep) {
				steps.add(new ArrayList<LDPrimitive>());
			}
			p.setStep(currStep);
			steps.get(currStep-1).add(p);
		}
	}

	
	/**
	 * remove part from its associated step. If no associated step, do nothing
	 * @param p
	 */
	public void delPart(LDPrimitive p) {
		
		if (p.getStep() == 0) {
			// no step assigned, do nothing
			return;
		}
		List<LDPrimitive> s = steps.get(p.getStep()-1);
		if (s == null) {
			Logger.getAnonymousLogger().warning("[LDStep.delPart] Part with undefined step '"+
					p.getStep()+"' "+p);
			return;
		}
		steps.get(p.getStep()-1).remove(p);
		//p.setStep(0);
	}
	
	
	
	
	/**
	 * Add a part to a defined step. If part already belongs to a step, remove first.
	 * @param p part to add
	 * @param s step to add
	 */
	public void moveToStep(LDPrimitive p, int s) {
		
		if (s <= 0 || p == null || s > steps.size()+1) {
			// do nothing
			return;
		}
		// if part already associated to a step, remove first
		if (p.getStep() != 0) {
			delPart(p);
		}
		// if no parts in step, create new step
		//System.out.println(steps.size()+"-"+s);
		if (steps.size() < s) {
			steps.add(new ArrayList<LDPrimitive>());
		}
		p.setStep(s);
		steps.get(s-1).add(p);
	}
	
}
