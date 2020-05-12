SnackVar Ver 2.1.1
======================
SnackVar is a free software for Sanger sequencing analysis in clinical environment.<br>
It supports automatic detection of variants including SNVs and indel variants (homozygous and heterozygous).<br>
Detected variants are provided in the form of HGVS (Human Genome Variation Society) nomenclature.

# 1. How to Run
1. You need to have [Java](https://www.java.com) installed.(Version 8 or higher)
2. Download SnackVar_V2.1.1.zip
3. Execute Ganseq_Run.jar 
(If not run by double click, right click-> open with -> JAVA)

# 2. How to Use
Using ganseq is quite straightforward.<br>
**(Refer to Ganseq_HowToUse.avi)**
<img src="screenshots/screenshot.jpg">
1. Open reference file (Genbank file)<br>
2. Open forward and/or reverse trace file<br>
3. Trim the low-quality ends of trace file<br>
<img src="screenshots/trimming1.jpg">
<img src="screenshots/trimming2.jpg">
4. Click 'Run' Button<br>
5. Verify suggested variants<br>

**Heterozygous Indel Variant**
<img src="screenshots/screenshot_indel.jpg">

**How to download reference file**
<img src="screenshots/genbank1.jpg">
<img src="screenshots/genbank2.jpg">

# 3. Utilized Libraries
1. BioJAVA Legacy  (https://github.com/biojava/biojava-legacy)
2. jAligner (https://github.com/ahmedmoustafa/JAligner)

