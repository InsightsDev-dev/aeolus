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
package de.hub.cs.dbis.lrb.operators;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;

import backtype.storm.task.OutputCollector;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import de.hub.cs.dbis.aeolus.testUtils.TestDeclarer;
import de.hub.cs.dbis.aeolus.testUtils.TestOutputCollector;
import de.hub.cs.dbis.aeolus.utils.TimestampMerger;
import de.hub.cs.dbis.lrb.queries.utils.TopologyControl;
import de.hub.cs.dbis.lrb.types.internal.AvgSpeedTuple;
import de.hub.cs.dbis.lrb.types.internal.LavTuple;
import de.hub.cs.dbis.lrb.types.util.SegmentIdentifier;
import de.hub.cs.dbis.lrb.util.Constants;





/**
 * @author richter
 * @author mjsax
 */
public class LastAverageVelocityBoltTest {
	private long seed;
	private Random r;
	
	
	
	@Before
	public void prepare() {
		this.seed = System.currentTimeMillis();
		this.r = new Random(this.seed);
		System.out.println("Test seed: " + this.seed);
	}
	
	
	
	@Test
	public void testExecute() {
		LatestAverageVelocityBolt instance = new LatestAverageVelocityBolt();
		TestOutputCollector collector = new TestOutputCollector();
		instance.prepare(null, null, new OutputCollector(collector));
		
		final short numberOfMinutes = 10;
		final int numberOfHighways = 1 + this.r.nextInt(3);
		final int numberOfSegments = 1 + this.r.nextInt(5);
		
		final Map<SegmentIdentifier, List<Double>> allSpeedsPerSegments = new HashMap<SegmentIdentifier, List<Double>>();
		final List<SegmentIdentifier> allSegments = new LinkedList<SegmentIdentifier>();
		final List<SegmentIdentifier> skippedSegments = new LinkedList<SegmentIdentifier>();
		
		
		Tuple tuple = mock(Tuple.class);
		when(tuple.getSourceStreamId()).thenReturn("streamId");
		OngoingStubbing<List<Object>> tupleStub = when(tuple.getValues());
		
		for(short minute = 1; minute <= numberOfMinutes; ++minute) {
			List<AvgSpeedTuple> input = new LinkedList<AvgSpeedTuple>();
			
			for(int xway = 1; xway <= numberOfHighways; ++xway) {
				for(short segment = 1; segment <= numberOfSegments; ++segment) {
					// randomly skip some AvgSpeedTuple
					if(this.r.nextDouble() < 0.5 / numberOfSegments) {
						input.add(null);
						skippedSegments.add(new SegmentIdentifier(new Integer(xway), new Short(segment),
							Constants.EASTBOUND));
						continue;
					}
					
					int avgSpeed = 1 + this.r.nextInt(Constants.MAX_SPEED);
					input.add(new AvgSpeedTuple(new Short(minute), new Integer(xway), new Short(segment),
						Constants.EASTBOUND, new Double(avgSpeed)));
				}
			}
			
			Collections.shuffle(input, this.r);
			for(AvgSpeedTuple t : input) {
				SegmentIdentifier sid;
				if(t != null) {
					sid = new SegmentIdentifier(t);
				} else {
					sid = skippedSegments.remove(0);
				}
				allSegments.add(sid);
				
				List<Double> speeds = allSpeedsPerSegments.get(sid);
				if(speeds == null) {
					speeds = new LinkedList<Double>();
					allSpeedsPerSegments.put(sid, speeds);
				}
				
				if(t != null) {
					speeds.add(t.getAvgSpeed());
					tupleStub = tupleStub.thenReturn(t);
				} else {
					speeds.add(null);
				}
			}
		}
		
		int executeCounter = 0;
		int lastMinute = 0;
		
		LinkedList<Values> expectedFlushs = null;
		
		for(short minute = 1; minute <= numberOfMinutes; ++minute) {
			boolean firstTupleOfMinute = true;
			
			boolean flushed = false;
			for(int i = 0; i < numberOfHighways * numberOfSegments; ++i) {
				SegmentIdentifier sid = allSegments.remove(0);
				List<Double> speeds = allSpeedsPerSegments.get(sid);
				if(speeds.get(minute - 1) == null) {
					continue;
				}
				
				instance.execute(tuple);
				++executeCounter;
				
				if(!flushed) {
					if(expectedFlushs == null) {
						expectedFlushs = new LinkedList<Values>();
					}
					expectedFlushs.add(new Values(new Short((short)((minute * 60) - 1))));
					flushed = true;
				}
				
				List<LavTuple> expectedResult = new LinkedList<LavTuple>();
				if(firstTupleOfMinute && minute > 1) { // process open (incomplete windows)
					for(int m = minute - 5; m < minute - 1; ++m) {
						if(m < 0 || m <= lastMinute) {
							continue;
						}
						lastMinute = m;
						for(Entry<SegmentIdentifier, List<Double>> e : allSpeedsPerSegments.entrySet()) {
							List<Double> speedList = e.getValue();
							if(speedList.get(m) == null) {
								this.addResult(speedList, (short)(m + 1), expectedResult, e.getKey());
							}
						}
					}
				}
				
				this.addResult(speeds, minute, expectedResult, sid);
				
				assertEquals(2, collector.output.size());
				assertEquals(executeCounter, collector.acked.size());
				assertEquals(expectedFlushs, collector.output.get(TimestampMerger.FLUSH_STREAM_ID));
				
				List<List<Object>> result = collector.output.get(TopologyControl.LAVS_STREAM_ID);
				
				HashSet<List<Object>> ers = new HashSet<List<Object>>();
				HashSet<List<Object>> rs = new HashSet<List<Object>>();
				while(expectedResult.size() > 0) {
					LavTuple t = expectedResult.remove(0);
					ers.add(t);
					
					rs.add(result.remove(0));
					
					while(expectedResult.size() > 0) {
						LavTuple t2 = expectedResult.get(0);
						if(t2.getMinuteNumber() == t.getMinuteNumber()) {
							ers.add(expectedResult.remove(0));
							rs.add(result.remove(0));
						} else {
							break;
						}
					}
					
				}
				
				assertEquals(ers, rs);
				assertEquals(0, result.size());
				
				firstTupleOfMinute = false;
			}
			
			assertEquals(expectedFlushs, collector.output.get(TimestampMerger.FLUSH_STREAM_ID));
		}
		
		Tuple flushTuple = mock(Tuple.class);
		when(flushTuple.getSourceStreamId()).thenReturn(TimestampMerger.FLUSH_STREAM_ID);
		instance.execute(flushTuple);
		++executeCounter;
		
		short minute = numberOfMinutes + 1;
		List<LavTuple> expectedResult = new LinkedList<LavTuple>();
		for(int m = minute - 5; m < minute - 1; ++m) {
			if(m < 0 || m <= lastMinute) {
				continue;
			}
			lastMinute = m;
			boolean lastMinuteEmpty = true;
			for(Entry<SegmentIdentifier, List<Double>> e : allSpeedsPerSegments.entrySet()) {
				List<Double> speedList = e.getValue();
				
				if(speedList.get(m) == null) {
					this.addResult(speedList, (short)(m + 1), expectedResult, e.getKey());
				} else {
					lastMinuteEmpty = false;
				}
			}
			if(lastMinuteEmpty) {
				for(int i = 0; i < numberOfHighways; ++i) {
					expectedResult.remove(expectedResult.size() - 1);
				}
			}
		}
		
		List<List<Object>> result = collector.output.get(TopologyControl.LAVS_STREAM_ID);
		HashSet<List<Object>> ers = new HashSet<List<Object>>();
		HashSet<List<Object>> rs = new HashSet<List<Object>>();
		while(expectedResult.size() > 0) {
			LavTuple t = expectedResult.remove(0);
			ers.add(t);
			
			rs.add(result.remove(0));
			
			while(expectedResult.size() > 0) {
				LavTuple t2 = expectedResult.get(0);
				if(t2.getMinuteNumber() == t.getMinuteNumber()) {
					ers.add(expectedResult.remove(0));
					rs.add(result.remove(0));
				} else {
					break;
				}
			}
			
		}
		if(expectedFlushs == null) {
			expectedFlushs = new LinkedList<Values>();
		}
		expectedFlushs.add(new Values((Object)null));
		
		assertEquals(ers, rs);
		assertEquals(0, result.size());
		assertEquals(executeCounter, collector.acked.size());
		
		assertEquals(2, collector.output.size());
		assertEquals(expectedFlushs, collector.output.get(TimestampMerger.FLUSH_STREAM_ID));
	}
	
	private void addResult(List<Double> speeds, short minute, List<LavTuple> expectedResult, SegmentIdentifier sid) {
		double sum = 0;
		int cnt = 0;
		List<Double> window = speeds.subList(minute > 5 ? minute - 5 : 0, minute);
		for(Double avgs : window) {
			if(avgs == null) {
				continue;
			}
			sum += avgs.intValue();
			++cnt;
		}
		if(cnt == 0) {
			return;
		}
		expectedResult.add(new LavTuple(new Short((short)(((minute + 1) * 60) - 1)), sid.getXWay(), sid.getSegment(),
			sid.getDirection(), new Integer((int)(sum / cnt))));
	}
	
	@Test
	public void testDeclareOutputFields() {
		LatestAverageVelocityBolt bolt = new LatestAverageVelocityBolt();
		
		TestDeclarer declarer = new TestDeclarer();
		bolt.declareOutputFields(declarer);
		
		Assert.assertEquals(2, declarer.streamIdBuffer.size());
		Assert.assertEquals(2, declarer.schemaBuffer.size());
		Assert.assertEquals(2, declarer.directBuffer.size());
		
		Assert.assertEquals(TopologyControl.LAVS_STREAM_ID, declarer.streamIdBuffer.get(0));
		Assert.assertEquals(new Fields(TopologyControl.MINUTE_FIELD_NAME, TopologyControl.XWAY_FIELD_NAME,
			TopologyControl.SEGMENT_FIELD_NAME, TopologyControl.DIRECTION_FIELD_NAME,
			TopologyControl.LAST_AVERAGE_SPEED_FIELD_NAME).toList(), declarer.schemaBuffer.get(0).toList());
		Assert.assertEquals(new Boolean(false), declarer.directBuffer.get(0));
		
		Assert.assertEquals(TimestampMerger.FLUSH_STREAM_ID, declarer.streamIdBuffer.get(1));
		Assert.assertEquals(new Fields("ts").toList(), declarer.schemaBuffer.get(1).toList());
		Assert.assertEquals(new Boolean(false), declarer.directBuffer.get(1));
	}
	
}
