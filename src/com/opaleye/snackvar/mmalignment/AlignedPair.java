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

package com.opaleye.snackvar.mmalignment;

public class AlignedPair {
	private String alignedString1 = null;
	private String alignedString2 = null;
	private int start1 = 0;
	private int start2 = 0;
	
	public AlignedPair(String alignedString1, String alignedString2) {
		super();
		this.alignedString1 = alignedString1;
		this.alignedString2 = alignedString2;
	}
	
	public String getAlignedString1() {
		return alignedString1;
	}
	public String getAlignedString2() {
		return alignedString2;
	}
	
	public AlignedPair addLeft(AlignedPair ap) {
		String as1 = ap.getAlignedString1() + this.getAlignedString1();
		String as2 = ap.getAlignedString2() + this.getAlignedString2();
		AlignedPair ret = new AlignedPair(as1, as2);
		return ret;
	}
	
	public AlignedPair addRight(AlignedPair ap) {
		String as1 = this.getAlignedString1() + ap.getAlignedString1();
		String as2 = this.getAlignedString2() + ap.getAlignedString2();
		AlignedPair ret = new AlignedPair(as1, as2);
		return ret;
	}
	
	public void printStrings() {
		System.out.println(alignedString1);
		System.out.println(alignedString2);
	}

	public int getStart1() {
		return start1;
	}

	public void setStart1(int start1) {
		this.start1 = start1;
	}

	public int getStart2() {
		return start2;
	}

	public void setStart2(int start2) {
		this.start2 = start2;
	}

}
