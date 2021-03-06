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
package de.hub.cs.dbis.aeolus.sinks;

import backtype.storm.tuple.Tuple;





/**
 * @author Matthias J. Sax
 */
class TestFileOutputBolt extends AbstractFileOutputBolt {
	private final static long serialVersionUID = -956984089329568377L;
	
	@Override
	protected String tupleToString(Tuple t) {
		return t.toString();
	}
	
}
