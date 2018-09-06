/*
 * Assignment 01
 * Filename: Ngrams.java
 * Version: 1.1
 * Author:  Matthew Morgan
 * Description:
 * This program reads from a set of text files (referred to as the Gutenberg
 * Corpus) and keeps track of the number of occurrences of single characters,
 * unigrams, bigrams, and trigrams, printing the results into files at the end.
 *
 * Accepted Arguments:
 * -d[#]    Toggles printing of debug statements during execution
 *          0: No debugging (default); 1: Simple debug statements;
 *          2: ALL debugging statements
 * -s[#]    Changes the sorting behavior of the program
 *          0: Default behavior; 1: N-gram, descending;
 *          2: Frequency, ascending; 3: Frequency, descending
 * -n[#]    Changes up to what n-gram is processed (default 3)
 * -mkcsv   Makes program generates CSV files for the grams
 * -single  Treats the input parameter as a single file of input for processing
 *
 * ~~~ VERSION HISTORY ~~~
 * Version 1.0 (13 January 2018)
 * Version 1.1 (3 April 2018)
 * - Fixed an error where not specifying a relative path would confuse the
 *   program when trying to parse what directory to store output files in
 */

// Import statements
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Collections;
import java.util.ListIterator;
import java.lang.Comparable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
//import java.time.LocalTime;

/******************************************************************************/
/* Gram is a class that represents a single n-gram, or a letter. */
class Gram implements Comparable<Gram> {
    private int frequency;  // The frequency of the gram at current time
    private String word;    // The gram itself, as a String (example: "the")

    public Gram(String wd) { frequency=1; word=wd; }
    public Gram(char c) { frequency=0; word=Character.toString(c); }

    // Accessor methods
    public String getWord() { return word; }
    public int getFreq() { return frequency; }

    /** Increments the current frequency of the gram */
    public void incFreq() { frequency++; }

    @Override
    public int compareTo(Gram g) {
        if (Ngrams.ENFORCE_SORT_NUM)
            return Integer.compare(this.frequency, g.getFreq());
        else
            return this.word.compareTo(g.getWord());
    }
}
/******************************************************************************/

public class Ngrams {
    // DEBUG_ENABLED specifies whether the program is running with tracing on.
    // DEBUG_SUPPRESS_NEWGRAM suppresses trace statements for 'new' grams.
    // ENFORCE_SORT_NUM and ENFORCE_SORT_REV manipulate the sorting
    // behavior of the program.
    // EXPORT_MAKE_CSV toggles whether to write CSV files for the grams.
    // SINGLE_INPUT_MODE toggles if the input specified is a single file or a
    // list of corpus entries.
    // GRAM_COUNT specifies up to what n-gram is being processed in the corpus.
    public static boolean DEBUG_ENABLED = false,
        DEBUG_SUPPRESS_NEWGRAM = false,
        ENFORCE_SORT_NUM = false,
        ENFORCE_SORT_REV = false,
        EXPORT_MAKE_CSV = false,
        SINGLE_INPUT_MODE = false;
    public static byte GRAM_COUNT = 3;

    // GRAM_DEFAULT is the default number of grams that will be processed.
    // DIR_CSV is the output directory where CSV files will be printed.
    public static final byte GRAM_DEFAULT = 3;
    public static final String DIR_CSV = "./out-csv/";

    public static void main(String[] args) {
        argumentCheck(args);

        // Show debug statements about execution
        if (DEBUG_ENABLED) {
            if (GRAM_COUNT != GRAM_DEFAULT)
                System.out.printf("DEBUG: Processing to %d-grams!\n", GRAM_COUNT);
            if (SINGLE_INPUT_MODE)
                System.out.println("DEBUG: Processing in single-input mode!");
        }

        // cntChars is used to store character frequencies.
        // cntGrams is used to store references to hashtables for n-grams.
        Gram cntChars[] = new Gram[26];
        ArrayList<Hashtable<String,Gram>> cntGrams = new ArrayList<>(GRAM_COUNT);

        // Initialize the character frequency array and list to default values
        for(int i=0; i<26; i++) { cntChars[i] = new Gram((char)(i+'a')); }
        for(int i=0; i<GRAM_COUNT; i++) { cntGrams.add(new Hashtable<>()); }

        if (!SINGLE_INPUT_MODE) {
            // Read in the list of corpus entries
            if (DEBUG_ENABLED) { System.out.println("DEBUG: Reading in file list."); }
            ArrayList<String> fileList = readFileList(args[0]);

            // Read each corpus entry
            if (DEBUG_ENABLED) { System.out.println("DEBUG: Reading corpus data."); }
            for(String corpusEntry : fileList)
                readCorpusEntry(corpusEntry, cntChars, cntGrams);
        }
        else
            readCorpusEntry(args[0], cntChars, cntGrams);

        // Create directories for output files if they don't exist
		File dirChar, dirWord;
		
		if (args[1].contains("\\")) { args[1] = args[1].replaceAll("\\", "/"); }
		if (args[2].contains("\\")) { args[2] = args[2].replaceAll("\\", "/"); }
		
		if (args[2].contains("/")) {
			dirChar = new File(args[2].substring(0, args[2].lastIndexOf("/")));
			if (!dirChar.exists()) { dirChar.mkdir(); }
		}
		
		if (args[1].contains("/")) {
			dirWord = new File(args[1].substring(0, args[1].lastIndexOf("/")));
			if (!dirWord.exists()) { dirWord.mkdir(); }
		}
		
        if (EXPORT_MAKE_CSV) { createCSVDir(); }

        // Write output files into their directories
        if (DEBUG_ENABLED) {
            System.out.println("DEBUG: Writing output files.");
            System.out.println("  Char Frequencies: '"+args[2]+"'");
        }
        writeFreqChar(args[2], cntChars);
        if (DEBUG_ENABLED) { System.out.println("  Gram Frequencies: "+"'"+args[1]+"'"); }
        writeFreqGram(args[1], cntGrams);
    }

    /**
     * Runs a check on the parameters given to the program to validate them.
     * @param args The arguments supplied to the program
     */
    public static void argumentCheck(String[] args) {
        // Current error code status (set to have no flags)
        boolean errorFound = false;

        // Specific argument checking
        try {
            if (args.length < 3) { throw new Exception("Number of arguments is insufficient!"); }
            else
                for (int i = 3; i < args.length; i++) {
                    String arg = args[i];
                    // Generation of CSV files in separate directory
                    if (arg.equals("-mkcsv"))
                        EXPORT_MAKE_CSV = true;
                    else if (arg.equals("-single"))
                        SINGLE_INPUT_MODE = true;
                    else {
                        byte n = (arg.length()<3 ? 0 : Byte.parseByte(arg.substring(3, arg.length()-1)));
                        // Debug statement modifier
                        if (arg.contains("-d")) {
                            if (n < 0 || n > 2) { throw new Exception("Invalid debug statement value!"); }
                            else {
                                DEBUG_ENABLED = (n > 0);
                                DEBUG_SUPPRESS_NEWGRAM = (n == 1);
                            }
                        }
                        // Sorting behavior modifier
                        else if (arg.contains("-s"))
                            if (n < 0 || n > 3) { throw new Exception("Invalid sorting value!"); }
                            else {
                                ENFORCE_SORT_NUM = (n >= 2);
                                ENFORCE_SORT_REV = (n % 2 == 1);
                            }
                            // N-gram processing override
                        else if (arg.contains("-n"))
                            if (n < 1) { throw new Exception("Invalid number of grams!"); }
                            else GRAM_COUNT = n;
                        else
                            throw new Exception("Invalid argument detected!");
                    }
                }
        }
        catch(Exception e) {
            errorFound = true;
            System.err.println(e.getMessage());
        }

        // Return that no errors were found
        if (!errorFound) { return; }

        // Print correct usage statements, then exit program
        System.err.println("Usage:    java Ngrams <corpus-list-file> <outfile-words> <outfile-chrs> [-d[#]]"+
            " [-s[#]] [-n[#]] [-mkcsv] [-single]");
        System.err.println("Options:");
        System.err.println("    -d[#]   Toggles debug statements (Default: 0)\n"+
            "           0: No statements; 1: Minimal; 2: ALL");
        System.err.println("    -s[#]   Changes sorting behavior of the program (Default: 0)\n"+
            "           0/1: N-gram, asc/des; 2/3: Frequency, asc/des");
        System.err.println("    -n[#]   Changes up to what n-gram is processed (Default: 3)");
        System.err.println("    -mkcsv  Creates CSV files for all frequency counts");
        System.err.println("    -single Specifies that given input is a single file, not corpus list");
        System.exit(1);
    }

    /**
     * Reads a list of filenames from an input file at the path given.
     * @param path The path to the input file which contains filenames
     * @return An ArrayList containing the list of filenames read in
     */
    public static ArrayList<String> readFileList(String path) {
        // Stores the list of file-names found in the first input file
        ArrayList<String> list = new ArrayList<>();

        // Attempt to open the first input file, where corpus entries are stored,
        // and parse each entry into the ArrayList for further reading.
        try {
            if (DEBUG_ENABLED) { System.out.println("DEBUG: Corpus file list:"); }

            // The BufferedReader is used for reading from the file, and the String
            // is used for temporary storage of each line read from the file.
            BufferedReader file = new BufferedReader(new FileReader(path));
            String newFile;

            // Loop the reading of lines and storage into the list until an empty
            // line is found in the input file. Print each filename if debugging.
            // Close the file after reading is finished.
            while ((newFile = file.readLine()) != null)
                if (!newFile.equals("")) {
                    if (DEBUG_ENABLED) { System.out.printf("  %s\n", newFile); }
                    list.add(newFile);
                }
            file.close();
        }
        catch(IOException e) {
            System.err.println("Error during file handling on file '"+path+"'!");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Return the final, generated list of corpus entries
        return list;
    }

    /**
     * Reads an entire, single corpus entry from the given input file
     * @param path The path to the file that contains a corpus entry
     * @param cntChr Array of character frequencies
     * @param cntGram List of hashtables containing n-gram frequency information
     */
    public static void readCorpusEntry(String path, Gram[] cntChr,
                                       ArrayList<Hashtable<String,Gram>> cntGram) {
        // Attempt to open the corpus entry and read lines, one at a time, to
        // process and count character and n-gram frequencies
        try {
            // BufferedReader that will be used to read from the file
            // A string to temporarily store the line being read
            // A pattern for matching words using regular expression
            // ArrayList meant to store the words of the n-grams (exempt unigrams)
            BufferedReader file = new BufferedReader(new FileReader(path));
            String line;
            Pattern wdPattern = Pattern.compile("[a-zA-Z]+");
            String[] strGrams = new String[GRAM_COUNT];

            // Initialize each n-gram to default value (an empty string)
            for(int i=1; i<GRAM_COUNT; i++)
                strGrams[i] = "";

            if (DEBUG_ENABLED) { System.out.println("  Entry: '"+path+"'"); }

            // While there are still lines in the document being read, process
            // these lines and update character and n-gram frequencies. Close the
            // file after finishing reading.
            while((line = file.readLine()) != null) {
                processLine(line, cntChr, cntGram, wdPattern, strGrams);
            }
            file.close();
        }
        catch(IOException e) {
            System.err.println("Error during file handling on file '"+path+"'!");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Processes a line of text by counting character frequencies, unigram,
     * bigram, and trigram occurrences (or up to n-grams).
     * @param line The line of text to be processed
     * @param cntChr Array containing character frequency information
     * @param cntGram List of hashtables containing n-gram frequency information
     * @param wdPattern Pattern object for usage in matching words
     * @param strGrams Array of strings which help in processing n-grams
     */
    public static void processLine(String line, Gram[] cntChr,
                                   ArrayList<Hashtable<String,Gram>> cntGram, Pattern wdPattern,
                                   String[] strGrams) {
        // wdMatcher is a matcher for detecting words in the line being processed
        Matcher wdMatcher = wdPattern.matcher(line);

        while(wdMatcher.find()) {
            String word = line.substring(wdMatcher.start(),wdMatcher.end()).toLowerCase();

            // Character processing; increment character frequencies (if a letter).
            // A character's index in the array is found by deducting 97 ('a') from
            // it's value as an integer.
            for(int i=0; i<word.length(); i++)
                if (Character.isLetter(word.charAt(i)))
                    cntChr[((int)word.charAt(i))-97].incFreq();

            // Gram processing; increment unigram frequencies.
            incGramFrequency(word, 0, cntGram);

            // Gram processing; increment n-gram frequencies.
            // An n-gram's frequency is only updated if the number of spaces in the
            // String that represents the current n-gram is equivalent to 'n' itself.
            for(int i=1; i<strGrams.length; i++) {
                // newGram is used for temporary storage and updating of the n-gram.
                String newGram = strGrams[i] + (strGrams[i].length()>0?" ":"") + word;

                if ((newGram.length()-newGram.replaceAll(" ", "").length())==i) {
                    incGramFrequency(newGram, i, cntGram);
                    strGrams[i] = newGram.substring(newGram.indexOf(' ')+1);
                }
                else
                    strGrams[i] = newGram;
            }
        }
    }

    /**
     * Increments the frequency of the nth gram in its respective hashtable.
     * @param gram The gram to update the frequency of
     * @param n What type of n-gram 'gram' is (1 for uni, 2 for bi, etc)
     * @param cntGram List of hashtables containing n-gram frequency information
     */
    public static void incGramFrequency(String gram, int n,
                                        ArrayList<Hashtable<String,Gram>> cntGram) {
        // Hashtable that contains all the n-grams where 'gram' belongs.
        Hashtable<String,Gram> gramTable = cntGram.get(n);

        // Increment the frequency if the gram exists in the table already, or
        // set the frequency to 1 if this is the gram's first occurrence.
        Gram g = gramTable.putIfAbsent(gram, new Gram(gram));
        if (g != null)
            g.incFreq();
        else
        if (DEBUG_ENABLED && !DEBUG_SUPPRESS_NEWGRAM)
            System.out.printf("    " + "New %d-Gram: '%s'\n", n+1, gram);
    }

    /**
     * Writes character frequency information to a file
     * @param path The path of the file to write frequency information to
     * @param chrFreq Array containing the character frequencies
     */
    public static void writeFreqChar(String path, Gram[] chrFreq) {
        // Attempt to open the file where all character frequencies will be written
        // and write all of the frequencies to that file before closing it.
        try {
            // PrintWriter for writing down character frequency data to the file.
            PrintWriter chFile = new PrintWriter(new File(path));

            // Write all character frequencies into the file after sorting them
            Arrays.sort(chrFreq,(ENFORCE_SORT_REV ? Collections.reverseOrder() : null));
            for(int i=0; i<26; i++)
                chFile.write(String.format("%8d %s\n", chrFreq[i].getFreq(), chrFreq[i].getWord()));

            chFile.close();
        }
        catch(IOException e) {
            System.err.println("Error during file handling: '"+path+"'!");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Write character frequencies to a CSV file
        generateCSVChar(chrFreq);
    }

    /**
     * Writes frequencies for n-grams into an output file.
     * @param path The path of the file to write the data to
     * @param cntGrams A list of hashtables that store frequency information
     *        for n-grams
     */
    public static void writeFreqGram(String path, ArrayList<Hashtable<String,Gram>> cntGrams) {
        // Attempt to open the file for gram frequencies, write those frequencies to
        // the file (with separators between each n-gram section), and then close
        // the file.
        try {
            // PrintWriter to write gram frequency data to the file
            // ArrayList created from translating a gram Hashtable into a list
            // ListIterator created from the sorted list of n-grams
            PrintWriter grFile = new PrintWriter(new File(path));
            ArrayList<Gram> listNGram;
            ListIterator<Gram> iterator;

            // Loop through each set of grams and write them in their own section.
            // Sections are separated by a set of hyphens.
            for (int i=0; i<cntGrams.size(); i++) {
                if (DEBUG_ENABLED) { System.out.println("    Sorting and printing "+
                    (i+1)+"-grams."); }

                // Setup: Translate the i-th gram hashtable into an ArrayList, sort
                // it, and then generate an iterator from it.
                listNGram = Collections.list(cntGrams.get(i).elements());
                listNGram.sort(ENFORCE_SORT_REV ? Collections.reverseOrder():null);
                iterator = listNGram.listIterator();

                grFile.write((i+1)+"-Grams\n");
                // While there are still grams in the iterator, write the gram's
                // information into the file
                while(iterator.hasNext()) {
                    Gram g = iterator.next();
                    grFile.write(String.format("%8d %s\n",g.getFreq(),g.getWord()));
                }
                grFile.write("--------------------------------------------------\n");

                // Generate a CSV file for the i-th gram list
                generateCSVGram(i+1, listNGram);
            }

            grFile.close();
        }
        catch(IOException e) {
            System.err.println("Exception during file handling for '"+path+"'!");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /** Creates the output directory for CSV files if it doesn't already exist */
    public static void createCSVDir() {
        // The directory to be created
        File dir = new File(DIR_CSV);

        // Attempt to create the CSV output directory if it doesn't exist
        if (!dir.exists()) {
            if (DEBUG_ENABLED)
                System.out.println("DEBUG: Creating CSV output directory.");

            dir.mkdir();
        }
    }

    /**
     * Writes a single CSV file for all character frequencies
     * @param chrFreq Array of character frequencies
     */
    public static void generateCSVChar(Gram[] chrFreq) {
        // Exit function if toggle isn't active
        if (!EXPORT_MAKE_CSV) { return; }

        // Debug statement
        if (DEBUG_ENABLED)
            System.out.println("    Writing CSV file for character frequencies");

        // Attempt to open a CSV file, write character frequencies to it, then close
        try {
            // csvFreq will be used to write data to the CSV file
            PrintWriter csvFreq = new PrintWriter(new File(DIR_CSV+"charfreq.csv"));

            // Write header information to the file
            csvFreq.write("letter,frequency\n");

            // Write character frequencies to the file
            for(int i=0; i<chrFreq.length; i++)
                csvFreq.write(String.format("%s,%d\n", chrFreq[i].getWord(),
                    chrFreq[i].getFreq()));

            csvFreq.close();
        }
        catch(IOException e) {
            System.err.println("Error when writing to character frequency file!");
            System.err.println(e.getMessage());
        }
    }

    /**
     * Writes a single CSV file for the n-th gram specified as a parameter. The
     * file contains the grams, their ranks, frequencies, and constants, 'k,'
     * computed by multiplying rank and frequency
     * @param n The type of gram (1 for uni, 2 for bi, etc)
     * @param list The list of grams to write data for
     */
    public static void generateCSVGram(int n, ArrayList<Gram> list) {
        // Exit function if toggle isn't active
        if (!EXPORT_MAKE_CSV) { return; }

        // path is the location of the output file
        String path = String.format(DIR_CSV+"%d-gram.csv", n);

        // Print debug statement specifying that a CSV file is being written
        if (DEBUG_ENABLED)
            System.out.printf("    Writing CSV file for %d-grams: '%s'\n", n, path);

        // Try to open the file-path containing n-gram information, write all the
        // grams, frequencies, ranks, and constants 'k' to the file, and then
        // close it.
        try {
            // csvGram will be used to write data to the file
            // iterator will be used to iterate through each of the n-grams
            PrintWriter csvGram = new PrintWriter(new File(path));
            ListIterator<Gram> iterator = list.listIterator();

            // Write the header information for the CSV file
            csvGram.write(String.format("%d-gram,frequency,rank\n", n));

            // Iterate through each of the n-grams and write it's information. The
            // amount of information written should be based on the header above
            for(int cRank=1; iterator.hasNext(); cRank++) {
                Gram g = iterator.next();
                csvGram.write(String.format("%s,%d,%d\n", g.getWord(), g.getFreq(),
                    cRank));
            }

            csvGram.close();
        }
        catch(IOException e) {
            // File handling exception arose
            System.err.println("Error during file handle: '"+path+"'!");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}