# ECU-4130

## Assignment 01
### Assignment Description
...
### Source Files
Source Files: Ngrams.java

Data Files: document.txt

To prevent legalities, the corpus data used for this assignment is not available. It has, instead, been substituted with a generic file called `document.txt` to showcase the program’s capacities.
### Compilation, Testing, and Known Issues
```
Compile: javac Ngrams.java
Testing: java Ngrams <in-file> <out-word> <out-char> [options]
Example: java Ngrams ./document.txt ./out/word.txt ./out/char.txt -single

-d[#] will toggle debug statements. Do not use option 2
-s[#] will modify sorting behavior
-n[#] will modify up to what n-gram is processed
-mkcsv will produce CSV files for all frequency counts
-single can be used to process a single TXT file instead of a list
```
Notes:
- Word frequencies will be saved to the file that you specify using `out-word`, and gram frequencies will be saved to the file you specify using `out-char`. If the directories up to these files don’t exist, then they will be created.
- An input file should be specified for `in-file`, and this file may either be a single document TXT, or a list of files (using relative paths) to TXT files for processing.
- If a single file is being processed (that is, you use a single document), append the tag `-single` to the testing command.
