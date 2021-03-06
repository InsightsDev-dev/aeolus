/*
 * #!
 * %
 * Copyright (C) 2014 - 2016 Humboldt-Universität zu Berlin
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
package de.hub.cs.dbis.lrb.types;

import backtype.storm.tuple.Fields;
import de.hub.cs.dbis.lrb.queries.utils.TopologyControl;





/**
 * A {@link DailyExpenditureRequest} from the LRB data generator.<br />
 * <br />
 * Daily expenditure requests do have the following attributes: TYPE=3, TIME, VID, XWay, QID, day
 * <ul>
 * <li>TYPE: the tuple type ID</li>
 * <li>TIME: 'the timestamp of the input tuple that triggered the tuple to be generated' (in LRB seconds)</li>
 * <li>VID: the unique vehicle ID making the request</li>
 * <li>XWay: the ID of the expressway on which an expenditure total is desired (1...L-1)</li>
 * <li>QID: the unique request ID</li>
 * <li>Day: the day on which an expenditure total is desired (1 is yesterday, 69 is 10 weeks ago)</li>
 * </ul>
 * 
 * @author mjsax
 * @author richtekp
 */
public class DailyExpenditureRequest extends AbstractInputTuple {
	private static final long serialVersionUID = 5710564296782458284L;
	
	// attribute indexes
	/** The index of the express way attribute. */
	public final static int XWAY_IDX = 3;
	
	/** The index of the query identifier attribute. */
	public final static int QID_IDX = 4;
	
	/** The index of the day attribute. */
	public final static int DAY_IDX = 5;
	
	
	
	public DailyExpenditureRequest() {
		super();
	}
	
	/**
	 * Instantiates a new daily expenditure request for the given attributes.
	 * 
	 * @param time
	 *            the time at which the request was issued (in LRB seconds)
	 * @param vid
	 *            the vehicle identifier
	 * @param xway
	 *            the express way of the request
	 * @param qid
	 *            the query identifier
	 * @param day
	 *            the day of the request
	 */
	public DailyExpenditureRequest(Short time, Integer vid, Integer xway, Integer qid, Short day) {
		super(AbstractLRBTuple.DAILY_EXPENDITURE_REQUEST, time, vid);
		
		assert (xway != null);
		assert (qid != null);
		assert (day != null);
		
		super.add(XWAY_IDX, xway);
		super.add(QID_IDX, qid);
		super.add(DAY_IDX, day);
		
		assert (super.size() == 6);
	}
	
	/**
	 * Returns the expressway ID of this {@link DailyExpenditureRequest}.
	 * 
	 * @return the VID of this request
	 */
	public final Integer getXWay() {
		return (Integer)super.get(XWAY_IDX);
	}
	
	/**
	 * Returns the query ID of this {@link DailyExpenditureRequest}.
	 * 
	 * @return the QID of this request
	 */
	public final Integer getQid() {
		return (Integer)super.get(QID_IDX);
	}
	
	/**
	 * Returns the day of this {@link DailyExpenditureRequest}.
	 * 
	 * @return the day of this request
	 */
	public final Short getDay() {
		return (Short)super.get(DAY_IDX);
	}
	
	/**
	 * Returns the schema of a {@link DailyExpenditureRequest}.
	 * 
	 * @return the schema of a {@link DailyExpenditureRequest}
	 */
	public static Fields getSchema() {
		return new Fields(TopologyControl.TYPE_FIELD_NAME, TopologyControl.TIMESTAMP_FIELD_NAME,
			TopologyControl.VEHICLE_ID_FIELD_NAME, TopologyControl.XWAY_FIELD_NAME,
			TopologyControl.QUERY_ID_FIELD_NAME, TopologyControl.DAY_FIELD_NAME);
	}
	
}
