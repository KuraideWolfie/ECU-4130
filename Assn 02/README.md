# ECU-4130

## Assignment 02
### Assignment Description
This programming assignment was the first attempt at indexing and providing search results for a given set of corpus data, where the following techniques were used: tokenization, inverted indices, and positional intersection. Single term queries and phrase queries were to be executable.
### Source Files
Source Files: Corpus.java, Query.java, Stemmer.java, and Token.java

Data Files: query.txt, index.zip

Thanks to the efforts of the folks at the following website, the Porter Stemmer algorithm used for this assignment is available for download for compilation and testing. To see other versions of the algorithm, inclusive of the one used for this assignment, please visit https://tartarus.org/martin/PorterStemmer/.

While the corpus data for this assignment is not available, the generated index data is made available through the file `index.zip`. To use the data of this file, extract the folder inside to the same directory as the one you compile the program in. _(Because this folder will thus exist, you can specify anything as the <infile-corpus> parameter of the testing command, and no errors will occur.)_
### Compilation, Testing, and Known Issues
```
Compile: javac Corpus.java Query.java Token.java Stemmer.java
Testing: java Corpus <infile-corpus> [options]

-indNew specifies to overwrite the current index (if one exists)
-query[f] redirects query retrieval to the file ‘f’
```
Issues:
- The `infile-corpus` parameter is mandatory even if an index exists. If you don’t specify a corpus but specify a query file using the `-query[f]` tag, then the tag will be ignored. (For example, `java Corpus -query[./corpus/query.txt]` will ignore you specified a query file entirely.)

Notes:
- The querying interface accepts a couple of special commands: `~help`, `~seek`, and `~exit` for displaying help during querying, printing a few words from a document at a given position, and exiting the querying interface. See the program for more information.
- The `infile-corpus` parameter is expected to be a TXT file that lists relative paths to the documents of a ‘corpus.’ A subset of data from the Gutenberg corpus was used in the assignment, but any series of documents may be used provided the list file is written correctly. Furthermore, _the corpus directory, and all files in the corpus, MUST be in the same directory as the compiled classes._
- The querying interface only supports term and phrase queries. Sample queries are `armadillo`, `document /5 ink`, and `~seek austen-emma.txt 53` (special command).
- The program generates a folder called `index` when it’s finished processing, containing a series of ‘TOK’ files and an ‘IND’ file. These files are generated so that, when the program operates again, corpus processing can be skipped.
