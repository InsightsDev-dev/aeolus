/*
 * #!
 * %
 * Copyright (C) 2014 - 2015 Humboldt-Universität zu Berlin
 * %
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #_
 */
package de.hub.cs.dbis.lrb.datatypes;

import storm.lrb.tools.Constants;





/**
 * A {@link PositionReport} from the LRB data generator.
 * 
 * Position reports do have the following attributes: TYPE=0, TIME, VID, Spd, XWay, Lane, Dir, Seg, Pos
 * <ul>
 * <li>TYPE: the tuple type ID</li>
 * <li>TIME: 'the timestamp of the input tuple that triggered the tuple to be generated' (in milliseconds)</li>
 * <li>VID: the unique vehicle ID</li>
 * <li>Spd: the speed of the vehicle (0...100)</li>
 * <li>XWay: the ID of the expressway the vehicle is driving on (1...L-1)</li>
 * <li>Lane: the ID of the lane the vehicle is using (0...4)</li>
 * <li>Dir: the direction the vehicle is driving (0 for Eastbound; 1 for Westbound)</li>
 * <li>Seg: the ID of the expressway segment the vehicle in on (0...99)</li>
 * <li>Pos: the position in feet of the vehicle (distance to expressway Westbound point; 0...527999</li>
 * </ul>
 * 
 * TODO LAV = latest average velocity
 * 
 * @author mjsax
 */
public final class PositionReport extends AbstractInputTuple {
	private final static long serialVersionUID = -4386109322233754497L;
	
	// attribute indexes
	/** The index of the speed attribute. */
	public final static int SPD_IDX = 3;
	
	/** The index of the express way attribute. */
	public final static int XWAY_IDX = 4;
	
	/** The index of the land attribute. */
	public final static int LANE_IDX = 5;
	
	/** The index of the direction attribute. */
	public final static int DIR_IDX = 6;
	
	/** The index of the segment attribute. */
	public final static int SEG_IDX = 7;
	
	/** The index of the position attribute. */
	public final static int POS_IDX = 8;
	
	
	
	public PositionReport() {
		super();
	}
	
	/**
	 * Instantiates a new position record for the given attributes.
	 * 
	 * @param time
	 *            the time at which the position record was emitted (in milliseconds)
	 * @param vid
	 *            the vehicle identifier
	 * @param speed
	 *            the current speed of the vehicle
	 * @param xway
	 *            the current expressway
	 * @param lane
	 *            the lane of the expressway
	 * @param diretion
	 *            the traveling direction
	 * @param segment
	 *            the mile-long segment of the highway
	 * @param position
	 *            the horizontal position on the expressway
	 */
	public PositionReport(Long time, Integer vid, Integer speed, Integer xway, Short lane, Short diretion,
		Short segment, Integer position) {
		super(AbstractLRBTuple.POSITION_REPORT, time, vid);
		
		assert (speed != null);
		assert (xway != null);
		assert (lane != null);
		assert (diretion != null);
		assert (segment != null);
		assert (position != null);
		
		super.add(SPD_IDX, speed);
		super.add(XWAY_IDX, xway);
		super.add(LANE_IDX, lane);
		super.add(DIR_IDX, diretion);
		super.add(SEG_IDX, segment);
		super.add(POS_IDX, position);
		
		assert (super.size() == 9);
	}
	
	
	
	/**
	 * Returns the vehicle's speed of this {@link PositionReport}.
	 * 
	 * @return the speed of this position report
	 */
	public final Integer getSpeed() {
		return (Integer)super.get(SPD_IDX);
	}
	
	/**
	 * Returns the expressway ID of this {@link PositionReport}.
	 * 
	 * @return the VID of this position report
	 */
	public final Integer getXWay() {
		return (Integer)super.get(XWAY_IDX);
	}
	
	/**
	 * Returns the lane of this {@link PositionReport}.
	 * 
	 * @return the VID of this position report
	 */
	public final Short getLane() {
		return (Short)super.get(LANE_IDX);
	}
	
	/**
	 * Returns the vehicle's direction of this {@link PositionReport}.
	 * 
	 * @return the VID of this position report
	 */
	public final Short getDirection() {
		return (Short)super.get(DIR_IDX);
	}
	
	/**
	 * Returns the segment of this {@link PositionReport}.
	 * 
	 * @return the VID of this position report
	 */
	public final Short getSegment() {
		return (Short)super.get(SEG_IDX);
	}
	
	/**
	 * Returns the vehicle's position of this {@link PositionReport}.
	 * 
	 * @return the VID of this position report
	 */
	public final Integer getPosition() {
		return (Integer)super.get(POS_IDX);
	}
	
	/**
	 * TODO check if needed (only if used multiple times)
	 * 
	 * @return TODO
	 */
	public boolean isOnExitLane() {
		return this.getLane().shortValue() == Constants.EXIT_LANE;
	}
	
}
