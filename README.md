SnackVar Ver 2.1.1
======================
SnackVar is a free software for Sanger sequencing analysis in clinical environment.<br>
It supports automatic detection of variants including SNVs and indel variants (homozygous and heterozygous).<br>
Detected variants are provided in the form of HGVS (Human Genome Variation Society) nomenclature.

# 1. How to Run
1. You need to have [Java](https://www.java.com) installed.(Version 8 or higher)
2. Download SnackVar_V2.1.1.zip and unzip. (Download : Release tab above)
3. Run <br>
Windows : Run.bat or SnackVAR.jar (If not run by double click, right click-> open with -> JAVA)<br>
Linux, Mac : java -jar SnackVAR.jar

# 2. How to Use
Using SnackVar is quite straightforward.<br>

[SnackVar_Demo.mp4](SnackVar_Demo.mp4)


<img src="fig/fig_ref_selection.png">
1. Choose reference sequence(RefSeq or gene name)<br>
2. Open forward and/or reverse trace file<br>
3. Trim the low-quality ends of trace file<br>
<img src="fig/fig_trimming.png">
4. Click 'Run' Button<br>
5. Results are shown as below<br>
<img src="fig/fig_result1.png">

**Heterozygous Indel Variant (by clicking "Hetero Indel View")**
<img src="fig/fig_hetero_indel_view.png">


# 3. Utilized Libraries
BioJAVA Legacy  (https://github.com/biojava/biojava-legacy)

