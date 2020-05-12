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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.biojava.bio.seq.Feature;
import org.biojava.bio.symbol.Location;
import org.biojavax.SimpleNamespace;
import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequenceIterator;

import com.opaleye.snackvar.tools.SymbolTools;

/**
 * Title : ReferenceFile
 * Reference file manager class
 * The method readGenBankFromFile is derived from BioJAVA Legacy Cookbook (https://biojava.org/wiki/BioJava:Cookbook:SeqIO:ReadGESBiojavax)
 * @author Young-gon Kim
 *2018.5
 */
public class Reference {
	public static final int FASTA=0;	
	public static final int GenBank=1;
	
	//0: FASTA, 1:Genbank         
	private int refType = Reference.GenBank;
	private String refString;
	private Vector<Integer> cDnaStart, cDnaEnd;
	private Vector<TranscriptVariant> tvList = null;
	private String refName;

	/**
	 * Constructor
	 * @param refFile : reference file
	 * @param type : Genbank vs FASTA
	 */
	public Reference(File refFile, int type) throws Exception{
		refType = type;
		refName = refFile.getName();
		if(type==Reference.FASTA) {
			readFastaFromFile (refFile);
		}
		else if (type==Reference.GenBank) {
			readGenBankFromFile (refFile);
		}
	}

	/**
	 * Reads Reference Genbank file which contains information regarding sequence, exons and coding sequence regions
	 * Derived from BioJAVA Legacy Cookbook (https://biojava.org/wiki/BioJava:Cookbook:SeqIO:ReadGESBiojavax)
	 * @param file : Input Genbank file
	 */
	public void readGenBankFromFile(File file) throws Exception{
		BufferedReader br = null;
		SimpleNamespace ns = null;

		try{
			br = new BufferedReader(new FileReader(file));
			ns = new SimpleNamespace("biojava");
			tvList = new Vector<TranscriptVariant>();
			// You can use any of the convenience methods found in the BioJava 1.6 API
			//RichSequenceIterator rsi = RichSequence.IOTools.readFastaDNA(br,ns);
			RichSequenceIterator rsi = RichSequence.IOTools.readGenbankDNA(br, ns);

			// Since a single file can contain more than a sequence, you need to iterate over
			// rsi to get the information.
			String geneName = "";
			while(rsi.hasNext()){
				RichSequence rs = rsi.nextRichSequence();
				//System.out.println(rs.seqString());

				String desc = rs.getDescription();
				int i1=0;
				for(i1=0;i1<desc.length();i1++) {
					char tempChar = desc.charAt(i1);
					if(tempChar=='(') break;
				}

				for(int j=i1+1;j<desc.length();j++) {
					char tempChar = desc.charAt(j);
					if(tempChar==')') break;
					else geneName += tempChar;
				}

				System.out.println("==========================");
				System.out.println("gene Name : " + geneName);
				System.out.println("==========================");


				refString=rs.seqString().toUpperCase();
				refString = refString.replaceAll("\n", "");

				Set<Feature> featureSet = rs.getFeatureSet();
				Iterator<Feature> iter = featureSet.iterator();

				while(iter.hasNext()) {
					Feature feature = (Feature)iter.next();

					if(feature.getType().contains("CDS")) {
						if(!geneName.equals(feature.getAnnotation().getProperty("gene"))) continue;
						Vector<Integer> tempCDnaStart = new Vector<Integer>();
						Vector<Integer> tempCDnaEnd = new Vector<Integer>();
						int transcriptSize = 0;
						//System.out.println("type : " + feature.getType());
						//System.out.println("note : " + feature.getAnnotation().getProperty("note"));
						Iterator<Location> cdsIter = feature.getLocation().blockIterator();

						while(cdsIter.hasNext()) {
							Location cds = (Location)cdsIter.next();
							tempCDnaStart.add((new Integer(cds.getMin())));
							tempCDnaEnd.add((new Integer(cds.getMax())));
							transcriptSize += (cds.getMax() - cds.getMin() +1);
							//System.out.println("cds : (" + cds.getMin() + ", " + cds.getMax()+")");
						}
						TranscriptVariant tv = new TranscriptVariant (tempCDnaStart, tempCDnaEnd, transcriptSize, (String)feature.getAnnotation().getProperty("note"));
						tvList.add(tv);

					}

				}
			}
		}
		catch(Exception ex){
			ex.printStackTrace();
			throw new Exception ("Error in reading GenBank File");
		}
	}

	/**
	 * Reads FASTA file as a reference. FASTA format is not used as reference anymore.
	 * @param file : Reference FASTA file
	 */
	public void readFastaFromFile (File file) throws Exception {


		cDnaStart = new Vector<Integer>();
		cDnaEnd = new Vector<Integer>();

		StringBuffer buffer = new StringBuffer();
		String ret = "";

		boolean skipFirstLine = false;
		boolean codingRegionFound = false;
		try (FileReader fileReader = new FileReader(file)){
			int ch;
			int counter = 1;
			String ATGC = "ATGC", atgc = "atgc";
			while ((ch = fileReader.read()) != -1) {
				char readChar = (char)ch;
				if(readChar == '>') skipFirstLine = true;
				if(skipFirstLine) {
					if(readChar == '\n') skipFirstLine = false;
					else continue;
				}
				String readCharString = new Character(readChar).toString();

				if(ATGC.contains(readCharString)) {
					if(!codingRegionFound) {
						codingRegionFound = true;
						cDnaStart.add(counter);
					}
					buffer.append(readChar);
					counter++;
				}
				else if(atgc.contains(readCharString)) {
					if(codingRegionFound) {
						codingRegionFound = false;
						cDnaEnd.add(counter-1);
					}
					buffer.append(readChar);
					counter++;
				}
			}
			// 끝까지 대문자로 끝날때 handling
			if(codingRegionFound) {
				codingRegionFound = false;
				cDnaEnd.add(counter-1);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			throw new Exception("Error in reading FASTA File");
		}

		if(cDnaStart.size()==0) {
			throw new Exception("No coding region(Upper case letters) found in the reference file");
		}

		ret = buffer.toString();
		refString = ret;
	}

	/**
	 * Getters and Setters of member variables
	 */
	public int getRefType() {
		return refType;
	}
	public void setRefType(int refType) {
		this.refType = refType;
	}
	public String getRefString() {
		return refString;
	}
	public void setRefString(String refString) {
		this.refString = refString;
	}
	public Vector<Integer> getcDnaStart() {
		return cDnaStart;
	}
	public void setcDnaStart(Vector<Integer> cDnaStart) {
		this.cDnaStart = cDnaStart;
	}
	public Vector<Integer> getcDnaEnd() {
		return cDnaEnd;
	}
	public void setcDnaEnd(Vector<Integer> cDnaEnd) {
		this.cDnaEnd = cDnaEnd;
	}

	public Vector<TranscriptVariant> getTvList() {
		return tvList;
	}

	public void setTvList(Vector<TranscriptVariant> tvList) {
		this.tvList = tvList;
	}

	public String getRefName() {
		return refName;
	}

}
