/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opaleye.snackvar.reference;

import java.util.Vector;

/**
 * Title : TranscriptVariant
 * @author Young-gon Kim
 * 2018.10.
 */
public class TranscriptVariant {
	private Vector<Integer> cDnaStart, cDnaEnd;
	private int size = 0;
	private String description = null;
	
	/**
	 * Constructor
	 * @param cDnaStart
	 * @param cDnaEnd
	 * @param size
	 * @param description
	 */
	public TranscriptVariant(Vector<Integer> cDnaStart, Vector<Integer> cDnaEnd, int size, String description) {
		super();
		this.cDnaStart = cDnaStart;
		this.cDnaEnd = cDnaEnd;
		this.size = size;
		this.description = description;
		//System.out.println("size : " + size);
		//System.out.println("description : " + description);
	}
	/**
	 * getter functions
	 */
	public Vector<Integer> getcDnaStart() {
		return cDnaStart;
	}
	public Vector<Integer> getcDnaEnd() {
		return cDnaEnd;
	}
	public int getSize() {
		return size;
	}
	public String getDescription() {
		return description;
	}
}
