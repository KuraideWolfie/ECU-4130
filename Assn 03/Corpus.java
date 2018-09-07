/*
 * File:    Corpus.java
 * Version: 2.1.1
 * Author:  Matthew Morgan
 * Description:
 * Corpus is the primary driver for parsing corpus documents and generating the
 * tiered index used for vector space model generation. It contains functions
 * to save and load this data, and calls upon the Query class to execute
 * queries on the generated models. Generation is handled in two phases:
 * - Tiered Index Gen: Corpus documents are recursively located if a directory
 *   is defined, and after they are, they are all processed according to a set
 *   of regulations in a preprocessing function. This process leads to having
 *   a tiered index of data for document titles and content
 * - Vector Space Model Generation: Generation of a VSM using the tiered index
 *   generated in the prior phase, where the dictionary of terms
 *   in each document are computed to have specific weights based on their
 *   frequencies. The VSM is generated both for document titles AND content
 *
 * ~~~ CHANGE HISTORY ~~~
 * Version 1.0 (26 January 2018)
 * Version 2.0 (22 February 2018)
 * Version 2.1 (28 March 2018)
 * Version 2.1.1 (3 April 2018)
 * - Fixed a bug where specifying an input file instead of input directory for
 *   a corpus wouldn't load entries properly
 * 
 * ~~~ BUCKETLIST ~~~
 * - Allow only a single parameter for execution - <in-corpa> - which will make
 *   all data generated remain in RAM only during the run-time of the program.
 *   This could be mixed with another flag - -tmp - if the 1 parameter is given
 *   but the user wants to enter the filenames at run-time.
 */

// Import statements
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class Corpus {
    // FILE_ENTRY is the filename of a corpus entry list when auto-generated
    private static final String FILE_ENTRY = "corpus.dat";

    // datTitle and datDoc are token dictionaries for index generation
    // vsmTitle and vsmDoc are vector space models for titles and doc content
    // docTitles is an arraylist of document titles
    // corpus, index, and vsm[] are file paths for storing/loading data
	// corIsDir specifies whether the corpus is a directory or not
    public static Hashtable<String,Token> datTitle = new Hashtable<>(), 
        datDoc = new Hashtable<>();
    public static ArrayList<String> docTitles = new ArrayList<>();
    public static Model vsmTitle = new Model(), vsmDoc = new Model();
    private static String corpus = "", index = "", vsm[];
	private static boolean corIsDir = false;

    public static void main(String[] args) {
        vsm = new String[]{"", ""};
        argumentCheck(args);

        // Phase skip flags
        boolean skipCorpus = corpus.equals(""), skipIndex = index.equals("");

        if (!skipCorpus) {
            // The corpus phase isn't being skipped. Generate an input file if
            // necessary, generate the tiered index, and save it. If a corpus
			// file was specified, replace all '\' with '/' to prevent errors
			corIsDir = (new File(corpus)).isDirectory();
			if (corpus.contains("\\")) { corpus = corpus.replaceAll("\\", "/"); }
            if (corIsDir) { corpusGenerateInput(); }
			
            System.out.println("Generating Tiered Index...");
            corpusProcess();
            System.out.println("Saving Tiered Index to Disk...");
            indexSave();
        }
        else if (!skipIndex) {
            // The corpus phase is being skipped, but the vsm generation phase
            // isn't - read in the tiered index
            System.out.println("Reading Tiered Index... Please Wait...");
            indexLoad();
        }

        if (!skipIndex) {
            // Generate the VSM using the available tiered index, and save it
            System.out.println("Generating Vector Space Model...");
            corpusVectorSpace();
            System.out.println("Saving Vector Space Model to Disk...");
            vsmTitle.save(vsm[0]);
            vsmDoc.save(vsm[1]);
            vsmTitleSave();
        }
        else {
            // Both the corpus and vsm-generation phases have been skipped;
            // just read in the vector space model
            System.out.println("Reading Vector Space Model... Please Wait...");
            vsmTitle.load(vsm[0]);
            vsmDoc.load(vsm[1]);
            vsmTitleLoad();
        }

        Query query = new Query(vsmTitle, vsmDoc, docTitles);
        query.query();
    }

    /**
     * Checks the command line for invalid arguments, toggling flags as valid
     * ones are parsed out. If an invalid argument is found, the program will
     * be terminated
     * @param args The set of arguments passed to the program
     */
    public static void argumentCheck(String[] args) {
        switch(args.length) {
            // Only one argument specified - help
            case 1:
                if (!args[0].equals("-help"))
                    System.err.println("SYS: Unrecognized parameter!");
                printUsage();
                System.exit(1);
                break;
            
            // VSM parameters were specified
            case 2: vsm[0] = args[0]; vsm[1] = args[1]; break;
            // VSM and index parameters specified
            case 3: index = args[0]; vsm[0] = args[1]; vsm[1] = args[2]; break;
            // All parameters specified
            case 4: corpus = args[0]; index = args[1];
                vsm[0] = args[2]; vsm[1] = args[3]; break;
            // Unexpected parameter set - terminate and print usage
            default:
                System.err.println("SYS: Unrecognized set of parameters!");
                printUsage();
                System.exit(1);
        }
    }

    /** Prints the proper usage information for this program */
    public static void printUsage() {
        System.err.println(
            "Usage: java Corpus <in-corpa> <out-ind> <out-vsm-head> <out-vsm>\n"+
            "       java Corpus <in-ind> <out-vsm-head> <out-vsm>\n"+
            "       java Corpus <in-vsm-head> <in-vsm>\n"+
            "       java Corpus -help\n"+
            "Parameters:\n"+
            "  -help     | Shows this help information\n"+
            "  in-corpa  | Directory to generate a corpus-entry list for, or\n"+
            "     a text file containing a corpus-entry list\n"+
            "  out-vsm-head | Output file for a vsm for doc titles\n"+
            "  out-vsm      | Output file for a vsm for doc content\n"+
            "  out-ind      | Output file for the generated tier index\n"+
            "  in-vsm-head  | File storing a prior vsm for doc titles\n"+
            "  in-vsm       | File storing a prior vsm for doc content\n"+
            "  in-ind       | File storing a prior saved tier index");
    }

    /** Generates an input file, 'files.txt,' in the parent directory of a
     *  corpus specified by the user as a CLI argument. */
    public static void corpusGenerateInput() {
        // dir is a reference to the directory specified by the user
        // files is a list of files that comprise the corpus
        // w is a BufferedWriter used to save the generated data to a file
        File dir = new File(corpus);
        ArrayList<String> files = new ArrayList<>();
        BufferedWriter w;

        System.out.printf("Generating Corpus Entry File...\n");

        // The location specified isn't a directory or doesn't exist error
        if (!dir.isDirectory() || !dir.exists()) {
            System.err.printf("ERR: Location '%s' is invalid!\n", corpus);
            System.exit(1);
        }

        // Delete the default file if it already exists, and then recurse the
        // directory to generate file listing
        (new File(corpus+FILE_ENTRY)).delete();
        recurseDirectory(files, corpus);

        // Write all of the locations of the files located to a text file in
        // the root of the corpa directory (also write the number of files)
        try {
            // Write nothing to a file if the corpus specified has no entries
            if (files.size()==0)
                throw new IOException("     The corpus given is empty");

            w = new BufferedWriter(new FileWriter(corpus+FILE_ENTRY));
            w.write(files.size()+"\n");
            for(String file : files)
                w.write(file+"\n");
            w.close();
        }
        catch(IOException e) {
            System.err.println("SYS: Error during input file generation!");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Success message that relays the number of files counted
        System.out.printf("  Input file successfully generated! %d files were "+
            "detected in the corpus...\n  The location of the file is: '%s'\n",
            files.size(), corpus+FILE_ENTRY);
    }

    /**
     * Recursively digs through a folder to create a list of all files in the
     * given parent directory
     * @param L The list to store the filenames in
     * @param directory The directory being recursed to generate filenames
     */
    public static void recurseDirectory(ArrayList<String> L, String directory) {
        // For every path in the directory, if the path is a directory, recurse
        // down to locate more files; otherwise, add the pathname to the list
        // of files, L
        for(File f : (new File(directory)).listFiles())
            if (f.isDirectory())
                recurseDirectory(L, directory+f.getName()+"/");
            else
                L.add(directory.replace(corpus,"./")+f.getName());
    }

    /** Processes the corpus, generating token data by reading all of the files
     *  and making appropriate calls to the dictionary and processing functions
     *  for line data.
     * 
     * The format of a corpus entry is specified as follows:
     * -------------------------------------------------------------------------
     * .T
     * Title of the document, terminated by a period.
     * .A
     * Authors, terminated by a period. (May be blank)
     * .B
     * Bibliography, or publication, terminated by a period. (May be blank)
     * .W
     * The content of the work itself
     * -----------------------------------------------------------------------*/
    public static void corpusProcess() {
        try {
            // cor is a pointer to the entry-file list
            // entry is a pointer to the current entry being read/processed
            // fileCount is the number of entries there are in the corpus
            // percentile is the number of files that comprise "10%" of the data
            // complete flags every tenth percentile
            BufferedReader entry,
                cor = new BufferedReader(new FileReader(
					corpus + (corIsDir ? FILE_ENTRY : "")
				));
            int fileCount = Integer.parseInt(cor.readLine()),
                percentile = (int)Math.ceil(1.0*fileCount/10);
            byte complete = 1;
			
			// Strip the corpus entry path to just the directory
			corpus = corpus.substring(0, corpus.lastIndexOf("/")+1);

            // While there are entries to be processed, process them
            for(int i=0; i<fileCount; i++) {
                // Line is temporary storage for lines read from the file
                // title is the title of the document
                // docLoc is the current position after a line is processed
                String line, title="";
                int docLoc = 1;

                // Percentile message that shows approximately every 10% of
                // the files being read
                if (i % percentile == 0 && complete <= 10) {
                    System.out.printf("%8d of %6d files read. Please wait...\n",
                        i, fileCount);
                    complete++;
                }

                entry=new BufferedReader(new FileReader(corpus+cor.readLine()));

                // Read in the document's data
                while((line = entry.readLine()) != null) {
                    switch(line) {
                        case ".T":
                            title = readSegment(entry, ".A");
                            processLine(true, title, i, 0);
                            docTitles.add(title.trim().replace(".",""));
                            break;
                        case ".A":
                        case ".B":
                            break;
                        case ".W":
                            while((line = entry.readLine()) != null)
                                docLoc = processLine(false, line, i, docLoc);
                            break;
                    }
                }

                entry.close();
            }

            cor.close();
        }
        catch(IOException e) {
            System.err.println("ERR: Error during corpus processing!");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Reads lines in a file (typically a corpus entry) until a certain stopping
     * point has been reached - that is, a line with the exact contents of the
     * stop term provided
     * @param r The reader being used to read the file
     * @param stop The specific string a line should be to stop reading
     * @return Concatenation of all the lines read from where the function
     *   began reading until when the stop point was reached
     */
    public static String readSegment(BufferedReader r, String stop) {
        String res = "", line;

        // Initially mark the stream, and then read in lines and mark the start
        // of each line. When the specific line to stop at is reached, the
        // stream is reset, and the final set of data read returned
        try {
            r.mark(0);
            while(!(line = r.readLine()).equals(stop)) {
                res += " " + line;
                r.mark(0);
            }
            r.reset();
        }
        catch(IOException e) {
            System.err.println("ERR: Error during file reading!");
            e.printStackTrace();
            System.exit(1);
        }

        return res;
    }

    /** Saves the generated tiered index to the disk */
    public static void indexSave() {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(index));

            // Document titles
            w.write(docTitles.size()+"\n");
            for(int i=0; i<docTitles.size(); i++)
                w.write(docTitles.get(i)+"\n");

            // Tokens in the titles
            for(Token t : Collections.list(datTitle.elements()))
                w.write(t.toString()+"\n");

            w.write("-\n");

            // Tokens in the document content
            for(Token t : Collections.list(datDoc.elements()))
                w.write(t.toString()+"\n");

            w.close();
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /** Loads a previously-generated tiered index from the disk */
    public static void indexLoad() {
        try {
            // data is used for temporarily storing lines from the file
            // tmp is used for temporarily referencing Tokens before putting
            // them into the hashtables
            BufferedReader r = new BufferedReader(new FileReader(index));
            String data;
            Token tmp;

            // Document titles
            for(int i=Integer.parseInt(r.readLine()); i>0; i--)
                docTitles.add(r.readLine());

            // Title tokens end when a hyphen is read as a line
            while(!(data = r.readLine()).equals("-")) {
                tmp = new Token("word");
                tmp.read(data);
                datTitle.put(tmp.stem, tmp);
            }

            // Document content tokens span until the end of the file
            while((data = r.readLine()) != null) {
                tmp = new Token("word");
                tmp.read(data);
                datDoc.put(tmp.stem, tmp);
            }

            r.close();
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
    
    /** Generates the vector space model for the current set of token data */
    public static void corpusVectorSpace() {
        // tokens is a list of tokens in one of the index tiers
        // table is the current hashtable of tokens being worked with
        // cModel is the current model being worked with
        // weight is the IDF weight of the token
        Hashtable<String,Token> table;
        ArrayList<String> tokens;
        Model cModel;
        double weight;

        // Add document vectors to the models
        for(int i=0; i<docTitles.size(); i++) {
            vsmTitle.addDoc(i); vsmDoc.addDoc(i);
        }

        for(int i=0; i<2; i++) {
            table = (i==0 ? datTitle : datDoc);
            cModel = (i==0 ? vsmTitle : vsmDoc);
            tokens = Collections.list(table.keys());

            // For every token, compute the IDF weight, and then assign that
            // token component's weight to the IDF weight multiplied by the TF
            // weight of that token for each document
            for(String t : tokens) {
                Token tok = table.get(t);
                weight = tok.weightIdf(docTitles.size());

                for(int tDoc : Collections.list(tok.postings.keys()))
                    cModel.setDocComponent(tDoc, t, weight*tok.weightTf(tDoc));
            }

            cModel.normalize();
        }
    }

    /** Saves all the titles in the vector space model in the VSM file */
    public static void vsmTitleSave() {
        try {
            BufferedWriter w = new BufferedWriter(new FileWriter(vsm[0], true));

            // Write all the titles to the file
            for(int i=0; i<docTitles.size(); i++)
                w.write(docTitles.get(i)+"\n");

            w.close();
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /** Loads all the titles in a prior-saved vector space model */
    public static void vsmTitleLoad() {
        try {
            String tmp;
            BufferedReader r = new BufferedReader(new FileReader(vsm[0]));

            // Skip down to where the titles are stored
            for(int i=Integer.parseInt(r.readLine()); i>0; i--)
                r.readLine();

            // Read in all the titles
            while((tmp = r.readLine()) != null)
                docTitles.add(tmp);

            r.close();
        }
        catch(IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Processes a line of text, updating the appropriate index tier
     * @param isTitle Is the line being processed the document's title
     * @param line The line of text being processed
     * @param doc The ID of the document
     * @param loc The current location, in the document, if applicable
     * @return The updated value for the current location in the document
     */
    public static int processLine(boolean isTitle,String line,int doc,int loc) {
        // Preprocess the line and select the tier of the index working with
        line = preProcessLine(line);
        Hashtable<String,Token> ind = (isTitle ? datTitle : datDoc);

        // Loop through each word in the line, not including blanks generated
        // by multi-space gaps between words. This generates the postings lists
        for(String word : line.split(" ")) {
            if (!word.equals("")) {
                // Add the token to the index, if it doesn't already exist,
                // and add the current location to the postings list
                Token t = new Token(word);
                if (ind.putIfAbsent(t.stem, t) != null) {
                    t = ind.get(t.stem);
                    t.variants.add(word);
                }

                if (!t.postings.containsKey(doc))
                    t.postings.putIfAbsent(doc, new ArrayList<Integer>());
                t.postings.get(doc).add(loc);

                loc++;
            }
        }

        // Return the updated location in the document
        return loc;
    }

    /**
     * Preprocesses a line of text by removing any characters detected as
     * illegal. The following rules are applied to the string, in order of
     * sequence applied:
     * <ol>
     * <li>The line is lowercased prior to processing characters
     * <li>If c is a number or case-insensitive letter, it's kept
     * <li>If c is a hyphen and the characters before and after satisfy rule 2,
     * it's kept; otherwise, if a hyphen is to the right, a space is
     * substituted
     * <li>If c is a comma and the characters before and after are both numbers,
     * it's kept
     * <li>If c is NOT an apostrophe, a space is appended in its place
     * </ol><br />
     * @param line The line of text to be preprocessed using the rules above
     * @return The line after preprocessing has been completed
     */
    public static String preProcessLine(String line) {
        line = line.toLowerCase();
        String stripped = ""; // String generated by preprocessing

        // Iterate through ALL characters to apply the rules for preprocessing
        for(int i=0; i<line.length(); i++) {
            char c = line.charAt(i); // Character to be tested

            if (isLetorNum(c) || c==' ')
                stripped += Character.toString(c);
            else if (c=='-') {
                // Attempt retrieval of characters before and after the current
                // to test for the 2nd rule; if an exception occurs, ignore it
                try {
                    char p = line.charAt(i-1), n = line.charAt(i+1);
                    if (isLetorNum(p) && isLetorNum(n))
                        stripped += '-';
                    else if (n=='-')
                        stripped += ' ';
                }
                catch(Exception e) { /* Do nothing */ }
            }
            else if (c==',') {
                // Attempt retrieval of the characters before and after
                // to test for numerical value; if exception occurs, ignore it
                try {
                    char p = line.charAt(i-1), n = line.charAt(i+1);
                    if (Character.isDigit(p) && Character.isDigit(n))
                        stripped += ',';
                }
                catch(Exception e) { /* Do nothing */ }
            }
            else if (!(c=='\''))
                stripped += ' ';
        }

        return stripped;
    }

    /**
     * Checks where the given character is a lowercase/uppercase letter, or if
     * that character is a digit
     * @param c The character to check
     * @return True if the character is a letter or number, or false if not
     */
    public static boolean isLetorNum(char c) {
        return (c>='0' && c<='9') || (c>='a' && c<='z') || (c>='A' && c<='Z');
    }
}