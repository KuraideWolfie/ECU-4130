# ECU-4130

## Assignment 03
### Assignment Description
This assignment build on the prior, generating the appropriate indices to generate a vector space model for corpus data. This data could then be queried using an interface similar to the one from the second assignment; however, instead of performing phrase queries, queries were instead limited to things similar to that you would use for, example, Google.

To avoid legalities, the corpus data used with this assignment is not available; however, files have been provided that contain data generated from the corpus files for the assignment.

Thanks to the efforts of the folks at the following website, the Porter Stemmer algorithm used for this assignment is available for download for compilation and testing. To see other versions of the algorithm, inclusive of the one used for this assignment, please visit https://tartarus.org/martin/PorterStemmer/.
### Source Files
Source Files: Corpus.java, Query.java, Stemmer.java, FileHandle.java, Token.java, Model.java, and Duet.java

Data Files: index, vsmData, vsmTitle, and Assn03.jar
### Compilation, Testing, and Known Issues
```
Compile:
javac -d out “./Corpus.java” “./Query.java” “./Stemmer.java” “./FileHandle.java” “./Token.java” “./Model.java” “./Duet.java”
jar cfe ./Assn03.jar Corpus -C out .

Testing:
java -jar Assn03.jar <in-corpa> <out-ind> <out-vsm-head> <out-vsm>
java -jar Assn03.jar <in-ind> <out-vsm-head> <out-vsm>
java -jar Assn03.jar <in-vsm-head> <in-vsm>

Use the first command if you’re processing a corpus for the first time. You must specify output filenames for the index, and vector space models for document titles/content.
Use the second command if you have an index already generated. You will have to specify output filenames for the vector space model files.
Use the third command if you have vector space model files, and want to skip all of processing. (Note that not having an index loaded may limit some features of the program.)
```
Issues:
- The ‘vector’ command doesn’t print document range – that is, the range of IDs it will accept based on the corpus data provided.

Notes:
- The `in-corpa` parameter for testing can be one of two options – a file listing corpus entries, or a directory containing all TXT files for a corpus. All entries in the corpus must follow a particular specification as written in the file `Corpus.java` file.
- The files `vsmData` and `vsmTitle` that are included on this repository are pre-generated vector space model files. You can run the provided jar file using this command, presuming that the files are in the same directory: `java -jar Assn03.jar ./vsmTitle ./vsmData`. The file `index` is a pre-generated index, and can be used with the second testing option, where the corpus is ignored.
- The query interface for this assignment supports some special commands. You can access these during execution by using a special help command as shown in the program. Usage data for the special commands is provided during commandline execution using one of those commands (or if the syntax is invalid).
