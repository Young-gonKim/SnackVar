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

public class TwoPeaks {
	private boolean secondPeakExist;
	private char firstBase, secondBase;
	private int firstPeakHeight, secondPeakHeight;
	
	public TwoPeaks(char firstBase, char secondBase, int firstPeakHeight, int secondPeakHeight, boolean secondPeakExist) {
		super();
		this.firstBase = firstBase;
		this.secondBase = secondBase;
		this.firstPeakHeight = firstPeakHeight;
		this.secondPeakHeight = secondPeakHeight;
		this.secondPeakExist = secondPeakExist;
	}
	
	public boolean secondPeakExist() {
		return secondPeakExist;
	}
	public char getFirstBase() {
		return firstBase;
	}
	public char getSecondBase() {
		return secondBase;
	}

	
	

}
