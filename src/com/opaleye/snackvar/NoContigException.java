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

/**
 * Title : NoContigException
 * An exception thrown when there is no overlap between forward trace and reverse trace 
 * @author Young-gon Kim
 *2018.8
 */
public class NoContigException extends Exception {
	public NoContigException() {
		super("Alignment Failed : There is no overlap between forward trace and reverse trace!\n\n"
				+ "* Common causes\n"
				+ "1) Wrong match of forward - reverse traces\n"
				+ "2) Reversed assignment of forward-reverse traces\n"
				+ "    Check for the errors in file naming (R <-> F)\n"
				+ "3) Wrong reference file");
	}
}
