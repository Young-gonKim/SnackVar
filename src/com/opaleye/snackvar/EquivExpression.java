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

package com.opaleye.snackvar;

public class EquivExpression implements Comparable<EquivExpression> {
	private int gIndex1, gIndex2;
	private String HGVS;
	
	public EquivExpression(int gIndex1, int gIndex2, String hGVS) {
		super();
		this.gIndex1 = gIndex1;
		this.gIndex2 = gIndex2;
		HGVS = hGVS;
	}

	//equiv expression 끼리의 비교이므로 gIndex1, gIndex2사이 간격 같음. gIndex1끼리만 비교하면 됨.
	public int compareTo(EquivExpression e) {
		int g1 = this.gIndex1;
		int g2 = e.getgIndex1();
		return g2 - g1;
	}

	public int getgIndex1() {
		return gIndex1;
	}

	public void setgIndex1(int gIndex1) {
		this.gIndex1 = gIndex1;
	}

	public int getgIndex2() {
		return gIndex2;
	}

	public void setgIndex2(int gIndex2) {
		this.gIndex2 = gIndex2;
	}

	public String getHGVS() {
		return HGVS;
	}

	public void setHGVS(String hGVS) {
		HGVS = hGVS;
	}
}
