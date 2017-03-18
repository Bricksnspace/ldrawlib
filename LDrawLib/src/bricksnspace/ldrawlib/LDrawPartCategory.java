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

//import bricksnspace.ldrawlib.LDrawPart;

/**
 * Heuristic to identify part category
 * 
 * @author Mario Pascucci
 * 
 */
public enum LDrawPartCategory {

	/** animal */
	ANIMAL,
	/** antenna */
	ANTENNA,
	/** arch */
	ARCH,
	/** bar */
	BAR,
	/** baseplates */
	BASEPLATE,
	/** boat */
	BOAT,
	/** bracket */
	BRACKET,
	/** plain bricks */
	BRICK,
	/** brick, modified */
	BRICK_MOD,
	/** round bricks */
	BRICK_ROUND,
	/** decorated bricks */
	BRICK_DEC,
	/** car */
	CAR,
	/** cone */
	CONE,
	/** container */
	CONTAINER,
	/** cylinder */
	CYLINDER,
	/** cylinder decorated */
	CYLINDER_DEC,
	/** dish */
	DISH,
	/** dish, decorated */
	DISH_DEC,
	/** door */
	DOOR,
	/** door, decorated */
	DOOR_DEC,
	/** electric */
	ELECTRIC,
	/** mindstorms elements */
	ELECTRIC_MS,
	/** power functions */
	ELECTRIC_PF,
	/** technic electric elements */
	ELECTRIC_TECH,
	/** trains electric elements */
	ELECTRIC_TRAIN,
	/** fence */
	FENCE,
	/** flag */
	FLAG, 
	/** flag, decorated */
	FLAG_DEC,
	/** friends */
	FRIENDS,
	/** friends hair */
	FRIENDS_HAIR,
	/** friends head */
	FRIENDS_HEAD,
	/** friends torso */
	FRIENDS_TORSO,
	/** friends legs/hips */
	FRIENDS_LEGS,
	/** glass */
	GLASS,
	/** Hinge */
	HINGE,
	/** hinge locking */
	HINGE_LOCK,
	/** magnet */
	MAGNET,
	/** minifig */
	MINIFIG,
	/** head, decorated */
	MF_HEAD,MF_HEAD_DEC,
	/** mf hairs */
	MF_HAIR,
	/** minifig torso */
	MF_TORSO,
	/** minifig torso, SW */
	MF_TORSO_SW,
	/** mf arm/hand */
	MF_ARM,
	/** mf hips/legs */
	MF_LEGS,
	/** mf hats/helmets */
	MF_HAT,
	/** minifig food */
	MF_FOOD,
	/** mf guns and weapons */
	MF_WEAPON,
	/** mf shield */
	MF_SHIELD,
	/** monorail */
	MONORAIL,
	/** motor (non-electric) */
	MOTOR,
	/** panel */
	PANEL,
	/** panel, decorated */
	PANEL_DEC,
	/** plane */
	PLANE,
	/** plant */
	PLANT,
	/** plate */
	PLATE,
	/** propellor */
	PROPELLOR,
	/** roadsign */
	ROADSIGN,
	/** rock */
	ROCK,
	/** slope */
	SLOPE,
	/** curved slope */
	SLOPE_DEC,
	/** staircase */
	STAIRCASE,
	/** support */
	SUPPORT,
	/** TAIL */
	TAIL,
	/** generic Technic */
	TECHNIC,
	/** Technic axle */
	TECH_AXLE,
	/** ball joints */
	TECH_BUSH,
	/** Technic brick */
	TECH_BRICK,
	/** technic connector */
	TECH_CONN,
	/** technic joiner */
	TECH_PANEL,
	/** technic beam/liftarm */
	TECH_BEAM,
	/** technic cross block */
	TECH_GEAR,
	/** technic pin */
	TECH_PIN,
	/** technic pneumatic */
	TECH_PNEUMATIC,
	/** technic bionicle */
	BIONICLE,
	/** technic bionicle weapon */
	ACTION_FIG,
	/** tile */
	TILE,
	/** tile, decorated */
	TILE_DEC,
	/** train */
	TRAIN,
	/** trucks, excavator, crane */
	TRUCKS,
	/** turntable */
	TURNTABLE,
	/** tyre */
	TYRE,
	/** wedge */
	WEDGE,
	/** wheel */
	WHEEL,
	/** window */
	WINDOW,
	/** windscreen */
	WINDSCREEN,
	/** wings */
	WING,
	/** sticker */
	STICKER,
	SUBPART,
	ALIAS,
	OBSOLETE,
	DUPLO,FABULAND,MURSTEN,QUATRO,ZNAP,
	OTHER;
	
	public static LDrawPartCategory getCategory(LDrawPart p) {
		
		String d = p.getDescription().toUpperCase();
		boolean decorated = d.indexOf("PATTERN") >= 6 
				|| d.indexOf("STICKER") >= 6 
				|| d.indexOf("PAT.") >= 6 
				|| p.getLdrawId().indexOf("p") >= 1;
		if (d.startsWith("FIGURE ")) {
			d = d.substring(7);
			if (d.startsWith("FRIEND ")) {
				d = "FRIENDS " + d.substring(7);
				p.setDescription("Friends" + p.getDescription().substring(7));
			}
		}
		String[] tokens = d.split("\\s+");
		LDrawPartCategory mainType;
		try {
			mainType = LDrawPartCategory.valueOf(tokens[0]);
		} catch (IllegalArgumentException e) {
			mainType = OTHER;
		}
		switch (mainType) {
		case BRICK:
			if (decorated) {
				return BRICK_DEC;
			}
			if (d.indexOf("ROUND") >= 0) {
				return BRICK_ROUND;
			}
			if (tokens.length == 4 || (tokens.length == 6 && tokens[4].equals("X"))) {
				return BRICK;
			}
			return BRICK_MOD;
		case CYLINDER:
			if (decorated) {
				return CYLINDER_DEC;
			}
			break;
		case DISH:
			if (decorated) {
				return DISH_DEC;
			}
			break;
		case DOOR:
			if (decorated) {
				return DOOR_DEC;
			}
			break;
		case ELECTRIC:
			if (tokens[1].equals("MINDSTORMS")) {
				return ELECTRIC_MS;
			}
			if (tokens[1].equals("TECHNIC")) {
				return ELECTRIC_TECH;
			}
			if (tokens[1].equals("POWER") && (tokens.length > 2 && tokens[2].equals("FUNCTIONS"))) {
				return ELECTRIC_PF;
			}
			if (tokens[1].equals("TRAIN")) {
				return ELECTRIC_TRAIN;
			}
			break;
		case FLAG:
			if (decorated) {
				return FLAG_DEC;
			}
			break;
		case FRIENDS:
			if (tokens[1].equals("HAIR")) {
				return FRIENDS_HAIR;
			}
			if (tokens.length > 2 && tokens[2].equals("HEAD")) {
				return FRIENDS_HEAD;
			}
			if (tokens[1].equals("HIPS")
					|| tokens[1].equals("LEGS")) {
				return FRIENDS_LEGS;
			}
			if (tokens.length > 2 && tokens[2].equals("TORSO")) {
				return FRIENDS_TORSO;
			}
			break;
		case HINGE:
			if (d.indexOf("LOCKING") >= 0) {
				return HINGE_LOCK;
			}
			break;
		case MINIFIG:
			if (tokens[1].equals("HEAD")) {
				return decorated?MF_HEAD_DEC:MF_HEAD;
			}
			if (tokens[1].equals("TORSO")
					|| (tokens.length > 2 && tokens[2].equals("TORSO"))
					) {
				if (d.indexOf(" SW ") >= 5) {
					return MF_TORSO_SW;
				}
				return MF_TORSO;
			}
			if (tokens[1].equals("FOOD")) {
				return MF_FOOD;
			}
			if (tokens[1].equals("HIPS")
					|| tokens[1].equals("LEG")
					|| tokens[1].equals("LEGS")
					) {
				return MF_LEGS;
			}
			if (tokens[1].equals("HAT")
					|| tokens[1].equals("HELMET")
					|| tokens[1].equals("CAP")
					|| tokens[1].equals("HEADDRESS")
					|| (tokens.length > 2 && tokens[2].equals("HELMET"))
					) {
				return MF_HAT;
			}
			if (tokens[1].equals("GUN")
					|| tokens[1].equals("SWORD")
					|| tokens[1].equals("WEAPON")
					|| (tokens.length > 2 && tokens[2].equals("GUN"))
					|| tokens[1].indexOf("GUN") >= 0
					) {
				return MF_WEAPON;
			}
			if (tokens[1].equals("HAIR")) {
				return MF_HAIR;
			}
			if (tokens[1].equals("ARM")
					|| (tokens.length > 2 && tokens[2].equals("ARM"))
					|| (tokens.length == 2 && tokens[1].equals("HAND"))
					) {
				return MF_ARM;
			}
			if (tokens[1].equals("SHIELD")) {
				return MF_SHIELD;
			}
			break;
		case PANEL:
			if (decorated) {
				return PANEL_DEC;
			}
			break;
		case SLOPE:
			if (decorated) {
				return SLOPE_DEC;
			}
			break;
		case TECHNIC:
			if (tokens[1].equals("AXLE")) {
				return TECH_AXLE;
			}
			if (tokens[1].equals("BUSH")) {
				return TECH_BUSH;
			}
			if (tokens[1].equals("BEAM")) {
				return TECH_BEAM;
			}
			if (tokens[1].equals("BRICK")) {
				return TECH_BRICK;
			}
			if (tokens[1].equals("CONNECTOR")
					|| tokens[1].equals("CROSS")
					|| (tokens.length > 2 && tokens[2].equals("CONNECTOR"))
					) {
				return TECH_CONN;
			}
			if (tokens[1].equals("GEAR")
					|| tokens[1].equals("GEARBOX")
					) {
				return TECH_GEAR;
			}
			if (tokens[1].equals("PANEL")) {
				return TECH_PANEL;
			}
			if (tokens[1].equals("PIN")) {
				return TECH_PIN;
			}
			if (tokens[1].equals("BIONICLE")) {
				return BIONICLE;
			}
			if (tokens[1].equals("ACTION")) {
				return ACTION_FIG;
			}
			if (tokens[1].equals("FLEX-SYSTEM")
					|| tokens[1].equals("PNEUMATIC") 
					) {
				return TECH_PNEUMATIC;
			}
			break;
		case TILE:
			if (decorated) {
				return TILE_DEC;
			}
			break;
		case OTHER:
			if (tokens[0].startsWith("~MOVE") || d.indexOf("OBSOLETE") >= 2) {
				return OBSOLETE;
			}
			if (tokens[0].startsWith("~")) {
				return SUBPART;
			}
			if (tokens[0].startsWith("=")) {
				return ALIAS;
			}
			if (tokens[0].equals("ARM")) {
				if (tokens.length > 1 && tokens[1].equals("SKELETON")) {
					return MF_ARM;
				}
				return TRUCKS;
			}
			if (tokens[0].equals("GATE")) {
				return FENCE;
			}
			if (tokens[0].equals("CLAW")) {
				return BAR;
			}
			if (tokens[0].equals("COCKPIT")
					|| tokens[0].equals("LANDING")
					) {
				return PLANE;
			}
			if (tokens[0].equals("BIKE")
					|| tokens[0].equals("MOTORCYCLE")
					) {
				return CAR;
			}
			if (tokens[0].equals("CRANE")
					|| tokens[0].equals("EXCAVATOR")
					|| tokens[0].equals("CONVEYOR")
					|| tokens[0].equals("BRUSH")
					|| tokens[0].equals("EXHAUST")
					|| tokens[0].equals("FORKLIFT")
					|| tokens[0].equals("HOSE")
					|| tokens[0].equals("HOOK")
					|| tokens[0].equals("JACK")
					|| tokens[0].equals("LADDER")
					|| tokens[0].equals("TIPPER")
					|| tokens[0].equals("TRACTOR")
					|| tokens[0].equals("TRAILER")
					|| tokens[0].equals("WINCH")
					) {
				return TRUCKS;
			}
			if (tokens[0].equals("BARREL")
					|| tokens[0].equals("BUCKET")
					|| tokens[0].equals("COCOON")
					|| tokens[0].equals("HANDLE")
					) {
				return CONTAINER;
			}
			if (tokens[0].equals("ROLLER")) {
				return DOOR;
			}
			if (tokens[0].equals("ROOF")) {
				return SLOPE;
			}
			if (tokens[0].equals("LAMPPOST")
					|| tokens[0].equals("SIGNPOST")
					) {
				return ROADSIGN;
			}
			if (tokens[0].equals("BONE")
					|| (tokens.length > 1 && tokens[1].equals("INGOT"))
					|| tokens[0].equals("CLUB")
					|| (tokens.length > 1 && tokens[1].equals("THERMOMETER"))
					) {
				return MINIFIG;
			}
			if (tokens[0].equals("RACK")) {
				return TECH_GEAR;
			}
			if (tokens[0].equals("SAIL")) {
				return BOAT;
			}
			if (tokens[0].equals("JET")) {
				return PROPELLOR;
			}
			if (tokens[0].equals("SLIDE")) {
				return STAIRCASE;
			}
			break;
		default:
			return mainType;
		}
		return mainType;
	}
	
	
	@Override
	public String toString() {
		switch (this) {
		case ARCH:
			return "Arch";
		case ANIMAL:
			return "Animal";
		case ANTENNA:
			return "Antenna";
		case BASEPLATE:
			return "Baseplate";
		case BAR:
			return "Bar";
		case BOAT:
			return "Boat";
		case BRACKET:
			return "Bracket";
		case BRICK: 
			return "Brick";
		case BRICK_MOD:
			return "Brick, modified";
		case BRICK_DEC:
			return "Brick, decorated";
		case BRICK_ROUND:
			return "Brick, round";
		case CAR:
			return "Car";
		case CONE: 
			return "Cone";
		case CONTAINER:
			return "Container";
		case CYLINDER:
			return "Cylinder";
		case CYLINDER_DEC:
			return "Cylinder, decorated";
		case DISH:
			return "Dish";
		case DISH_DEC:
			return "Dish, decorated";
		case DOOR:
			return "Door";
		case DOOR_DEC:
			return "Door, decorated";
		case ELECTRIC:
			return "Electric";
		case ELECTRIC_MS:
			return "Electric, Mindstorms";
		case ELECTRIC_TECH:
			return "Electric, Technic";
		case ELECTRIC_TRAIN:
			return "Electric, trains";
		case ELECTRIC_PF:
			return "Electric, Power Func";
		case FENCE:
			return "Fence";
		case FLAG:
			return "Flag";
		case FLAG_DEC:
			return "Flag, decorated";
		case FRIENDS:
			return "Friends";
		case FRIENDS_HAIR:
			return "Friends hair";
		case FRIENDS_HEAD:
			return "Friends head";
		case FRIENDS_TORSO:
			return "Friends torso";
		case FRIENDS_LEGS:
			return "Friends hips/legs";
		case GLASS:
			return "Glass";
		case HINGE:
			return "Hinge";
		case HINGE_LOCK:
			return "Hinge, locking";
		case MAGNET:
			return "Magnet";
		case MINIFIG:
			return "Minifig";
		case MF_HEAD:
			return "Minifig head";
		case MF_HEAD_DEC:
			return "Minifig head, decorated";
		case MF_TORSO:
			return "Minifig torso";
		case MF_TORSO_SW:
			return "Minifig torso SW";
		case MF_ARM:
			return "Minifig arm/hand";
		case MF_HAIR:
			return "Minifig hair";
		case MF_LEGS:
			return "Minifig hips/legs";
		case MF_HAT:
			return "Minifig hat/helmet";
		case MF_WEAPON:
			return "Minifig weapons";
		case MF_SHIELD:
			return "Minifig shield";
		case MF_FOOD:
			return "Minifig food";
		case MONORAIL:
			return "Monorail";
		case MOTOR:
			return "Motor (non-electric)";
		case PANEL:
			return "Panel";
		case PANEL_DEC:
			return "Panel, decorated";
		case PLANE:
			return "Plane";
		case PLANT:
			return "Plant";
		case PLATE:
			return "Plate";
		case PROPELLOR:
			return "Propellor";
		case ROADSIGN:
			return "Roadsign";
		case ROCK:
			return "Rock";
		case SLOPE:
			return "Slope";
		case SLOPE_DEC:
			return "Slope, decorated";
		case STAIRCASE:
			return "Staircase";
		case SUPPORT:
			return "Support";
		case TAIL:
			return "Tail";
		case TECH_AXLE:
			return "Technic axle";
		case TECH_BUSH:
			return "Technic bush";
		case TECH_BRICK:
			return "Technic brick";
		case TECH_CONN:
			return "Technic connector";
		case TECH_GEAR:
			return "Technic gear";
		case TECH_PANEL:
			return "Technic panel";
		case TECH_BEAM:
			return "Technic beam/liftarm";
		case TECH_PIN:
			return "Technic pin";
		case TECH_PNEUMATIC:
			return "Technic pneumatic";
		case BIONICLE:
			return "Technic, Bionicle";
		case ACTION_FIG:
			return "Technic, Action Figure";
		case TECHNIC:
			return "Technic (other)";
		case TILE:
			return "Tile";
		case TILE_DEC:
			return "Tile, decorated";
		case TRUCKS:
			return "Truck, crane, digger";
		case TRAIN:
			return "Train";
		case TURNTABLE:
			return "Turntable";
		case TYRE:
			return "Tyre";
		case WEDGE:
			return "Wedge";
		case WHEEL:
			return "Wheel";
		case WINDOW:
			return "Window";
		case WINDSCREEN:
			return "Windscreen";
		case WING:
			return "Wing";
		case STICKER:
			return "Sticker";
		case ALIAS:
			return "Part alias";
		case SUBPART:
			return "Subparts";
		case OBSOLETE:
			return "Obsolete parts/codes";
			/* other bricks, not used at the moment */
		case DUPLO:
			return "Duplo brick";
		case FABULAND:
			return "Fabuland";
		case MURSTEN:
			return "Mursten brick";
		case QUATRO:
			return "Quatro brick";
		case ZNAP:
			return "ZNAP brick";
		case OTHER:
			return "Other";
		}
		return "Unknown";
	}
	
}
