/*
 * Assignment 02
 * File:    Query.java
 * Date:    9 February 2018
 * Author:  Matthew Morgan
 * Description:
 * Query contains all functions (exempt those also used in Corpus index generation) to process
 * term-existence and phrase queries.
 */

// Import statements
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Collections;

public class Query {
    // PATH_FILE is the index of the indexFile array that contains only the filename
    // PATH_FULL is the index of the indexFile array that contains the full path name
    // QUERY_HISTORY_SIZE is the number of history entries made available during query processing
    // SEEK_WORD_PRINT is the number of words a single seek will print, inclusive of the word at
    // the position being seeked itself
    // QUERY_RESULT_LINECOUNT is the number of results to be printed on a single line for a query
    private static final byte PATH_FILE = 0, PATH_FULL = 1,
        QUERY_HISTORY_SIZE = 10, SEEK_WORD_PRINT = 8, QUERY_RESULT_LINECOUNT = 10;

    // fileIndex is an array of 2-entry arrays that stores file and path names for corpus entries
    // filQuery is the path of the file through which queries will automatically be read
    // queryHistory is an arraylist that contains the history of queries made by the user
    // prevQueryResult stores a token reference to the results of the most recent query
    // QUERY_REDIRECT is a boolean toggle for redirecting query input to a text file
    private static String[][] fileIndex;
    public static String filQuery = null;
    private static ArrayList<String> queryHistory = new ArrayList<>(QUERY_HISTORY_SIZE);
    private static Token prevQueryResult = null;
    public static boolean QUERY_REDIRECT = false;

    /**
     * Allows processing of phrase queries based on a positional index that should be
     * generated before this function's execution. Serves as a basis for all the other
     * querying functions to be called
     */
    public void query() {
        // in is a scanner that accepts input from the appropriate location
        // file is a variable for storing the object that points to input
        // input is used for storing the information passed by the user
        // valid is a boolean toggle for whether a query is valid or not
        Scanner in;
        File file;
        String input;
        boolean valid;
        loadFileIndex();

        try {
            // Select where the scanner gets its input. If input is being redirected but
            // the file doesn't exist for scanning, display an error and exit the function
            if (QUERY_REDIRECT && (file = new File(filQuery)).exists())
                in = new Scanner(file);
            else if (!QUERY_REDIRECT)
                in = new Scanner(System.in);
            else {
                System.err.printf("SYS: The file '%s' doesn't exist!\n", filQuery);
                return;
            }

            while((QUERY_REDIRECT && in.hasNextLine()) || !QUERY_REDIRECT) {
                // Get a query from the user, and reset validity toggle
                System.out.printf("\nEnter a query, or type '~help' for info > ");
                input = in.nextLine().trim();
                if (QUERY_REDIRECT) { System.out.printf("%s\n", input); }
                valid = true;

                // Test the query. If '~help' is entered, help information is shown. If '~seek' is entered, then
                // one of the documents of the corpus is to be attempted to be opened. If '~exit' is entered,
                // then querying terminates. If none of these, the query is run.
                if (input.equals("~help"))
                    cmdHelp();
                else if (input.contains("~seek"))
                    cmdSeek(input);
                else if (input.equals("~exit")) {
                    in.close();
                    return;
                }
                else
                    valid = queryValid(input);
                
                // Print that the query given was invalid if it was detected as so
                if (!valid)
                    System.out.printf("  The query '%s' is invalid.\n", input);
            }

            in.close();
        }
        catch(Exception e) {
            System.err.println("SYS: An error has occured during querying!");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    /** Displays help information to the user about querying */
    private void cmdHelp() {
        System.out.println();
        System.out.println("Query Help Information:");
        System.out.println("  ~seek <doc> <pos> : Prints a few words from the document 'doc,' starting from pos");
        System.out.println("  ~exit             : Ends query processing and stops the program");
        System.out.println("  <term>         : Checks if the specified term exists in the corpus");
        System.out.println("  <t1> /<s> <t2> : Checks if t1 and t2 are within 's' words of each other");
        System.out.println("    's' will be inferred to be 1 if it isn't included in the query");
        System.out.println();
    }

    /**
     * Processes a seek command specified by the user
     * @param input The query that contains the keyword for the seek command
     */
    private void cmdSeek(String input) {
        // valid is a toggle for whether the properties of the command are correct or not
        // param is an array of the parameters passed as the command
        // index is the index in the fileIndex array containing the full pathname
        boolean valid = true;
        String[] param = input.split(" ");
        int index = -1;

        // If the length of the seek command is not 3, the first parameter isn't "~seek"
        // or the file doesn't exist, the seek request is invalid
        if (!(param.length == 3) || !param[0].equals("~seek")) { valid = false; }
        else
            try {
                // Get the index of the desire file's full name
                for(int i=0; i<fileIndex.length && index==-1; i++)
                    if (fileIndex[i][PATH_FILE].equals(param[1])) { index = i; }

                // Open a BufferedReader, fetch the excerpt requested, then close the reader
                BufferedReader file = new BufferedReader(new FileReader(fileIndex[index][PATH_FULL]));
                seekPrint(file, Integer.parseInt(param[2]));
                file.close();
            }
            catch (Exception e) { valid = false; }

        // If the query was found invalid, print error
        if (!valid) { System.out.printf("  Your seek request is invalid!\n"); }
    }

    /**
     * Reads in words to generate an excerpt requested by the user, starting at the position given and spanning
     * the number of words predefined by the constant SEEK_WORD_PRINT above
     * @param f A reader that points to the file to read the excerpt from
     * @param pos The position where the excerpt should start
     */
    private void seekPrint(BufferedReader f, int pos) {
        // line is used to read in information from the reader provided
        // result is the line to be printed AFTER execution
        // cPos is the current position being looked at
        String line, result="";
        int cPos = 1;

        try {
            // While there are still lines available in the file and the position
            // being sought (incremented by the words to be printed) hasn't been
            // reached, read lines from the file
            while((line = f.readLine()) != null && cPos<pos+SEEK_WORD_PRINT) {
                line = Corpus.preProcessLine(line);
                
                // For each word in the line that isn't blank, print if it the position
                // desired has been reached; else, just increment the counter for the
                // current position in the document (return if it's exceeded)
                for(String word : line.split(" "))
                    if (!word.equals("")) {
                        if (cPos<pos+SEEK_WORD_PRINT) {
                            if (cPos>=pos) { result = result+word+" "; }
                            cPos++;
                        }
                        else { break; }
                    }
            }
        }
        catch(IOException e) {
            System.err.println("SYS: An error occurred during seek!");
            System.err.println(e.getMessage());
        }

        System.out.printf("  Result: '...%s...'\n", result.trim());
    }

    /**
     * Tests whether a given query is valid (or not)
     * @param q The query text being tested
     * @return True if the query is valid, or false if not
     */
    private static boolean queryValid(String q) {
        /*
         * Query Rules:
         * - Terms must be at least 1 character - a letter or number
         * - Terms must start with a letter or number
         * - Proximity must be specified between two terms (not before or after)
         * - Proximity must be specified as '/#', where # is any integer
         * 
         * Valid examples:        Invalid examples:
         * - 'term'               - 'apple orange /3'
         * - 'apple /3 orange'    - '/3 apple orange'
         *                        - '_term' or '/'
         */

        // terms is an array containing ALL of the terms for the query
        // result is a reference to a Token object where the final result is stored
        // lnCnt is a counter that tracks the number of positions printed on a line currently
        String[] terms = q.toLowerCase().split(" ");
        Token result = null;
        int lnCnt;

        // For every detected term in the query, check validity
        for(int i=0; i<terms.length; i++) {
            if (terms[i].contains("/")) {
                if (terms[i].length()==1 || i==0 || i==terms.length-1) { return false; }
            }
            else if (terms[i].length()==0 || !Corpus.isLetorNum(terms[i].charAt(0))) { return false; }
        }

        // The terms detected in the query are valid. Process the existence of a token if it was the only
        // term passed, or check the intersection of two tokens' positional indexes if more was passed
        if (terms.length == 1 && (result = queryToken(terms[0])) != null) {
            for(int doc : result.getIndex().keySet())
                System.out.printf("  '%s' has %d match(es)\n", fileIndex[doc][PATH_FILE], result.getDoc(doc).getFreq());
        }
        else if (terms.length > 1 && (result = queryPhrase(terms)) != null) {
            // For every document in the result Token object containing the intersection, print positions
            for(int doc : result.getIndex().keySet()) {
                lnCnt = 0;
                System.out.printf("  Document '%s':\n", fileIndex[doc][PATH_FILE]);

                // For every position in the current document of the intersection, print that position
                for(int pos : result.getDoc(doc).getLocations()) {
                    System.out.printf("%s%s%s",
                        (lnCnt == 0 ? "    " : ""), Corpus.padStringLeft(Integer.toString(pos),'-',7),
                        (lnCnt == QUERY_RESULT_LINECOUNT ? "\n" : " "));
                    if (lnCnt == QUERY_RESULT_LINECOUNT) { lnCnt = 0; } else { lnCnt++; }
                }
                System.out.println();
            }
        }

        if (result == null) { System.out.printf("  No results were found for the query.\n"); }

        return true;
    }

    /**
     * Processes a phrase query (two terms or more, in sequence)
     * @param terms An array of terms to be used during query processing
     * @return The token found by processing the phrase query, or null if no result
     */
    private static Token queryPhrase(String[] terms) {
        Token t1, t2, res;
        t1 = queryToken(terms[0]);
        t2 = queryToken((terms.length==2 ? terms[1] : terms[2]));

        // Return there were no results if either token inquiry resulted in nothing
        if (t1 == null || t2 == null) { return null; }

        res = positionalIntersect(t1, t2, (terms.length==2 ? 1 : Integer.parseInt(terms[1].substring(1))));
        return res;
    }

    /**
     * Generates an intersection between two tokens' positional listings, factoring in
     * the proximity desired. The intersection is stored in a generic Token object
     * @param tokA The first token to perform intersection with
     * @param tokB The second token to perform intersection with
     * @param proximity The number of words, maximum, allowed between tokA and tokB
     * @return A Token object that includes the intersected positional listing as its index
     *   or null if the intersection between the tokens has no result
     */
    private static Token positionalIntersect(Token tokA, Token tokB, int proximity) {
        // result is where the final intersection list will be stored
        // docs[] is a list of documents that are contained in the tokens' positional indexes
        Token result = new Token("Intersector");
        ArrayList<ArrayList<Integer>> docs = new ArrayList<>(2);
        docs.add(Collections.list(tokA.getIndex().keys()));
        docs.add(Collections.list(tokB.getIndex().keys()));

        // Sort both document lists in ascending order
        Collections.sort(docs.get(0));
        Collections.sort(docs.get(1));

        // While both of the document ID lists is NOT empty
        while(!docs.get(0).isEmpty() && !docs.get(1).isEmpty()) {
            // d is temporary storage for the first ID in both lists
            int d[] = { docs.get(0).get(0), docs.get(1).get(0) };

            if (d[0] == d[1]) {
                // post is the list of locations in the document that the tokens occur
                ArrayList<ArrayList<Integer>> post = new ArrayList<>(2);
                post.add(tokA.getDoc(d[0]).getLocations());
                post.add(tokB.getDoc(d[0]).getLocations());

                // Remove the ID from both document ID lists and add the ID to the intersection
                result.addDoc(d[0]);
                docs.get(0).remove(0);
                docs.get(1).remove(0);

                // Intersect the position listings for both tokens by adding positions from tokA that are
                // within the proximity of tokB to the result list, checking if the position exists in
                // the intersection already to prevent duplicates
                for(int i=0; i<post.get(0).size(); i++)
                    for (int k=0; k<post.get(1).size(); k++) {
                        if (Math.abs(post.get(0).get(i) - post.get(1).get(k)) <= proximity) {
                            if (!result.isInDocatLoc(d[0], post.get(0).get(i))) // Disallows duplicates
                                result.addDocLocation(d[0], post.get(0).get(i));
                        }
                        else if (post.get(1).get(k) > post.get(0).get(i))
                            break;
                    }

                // Remove the document if there is no intersection
                if (result.getDoc(d[0]).getLocations().isEmpty()) { result.remDoc(d[0]); }
            }
            else if (d[0] < d[1])
                docs.get(0).remove(0);
            else
                docs.get(1).remove(0);
        }

        // The result returned should be null if no intersections were found (in other words, if the
        // frequency of the generic token made for the intersection has no frequency - 0), or just the
        // token itself if there is at least 1 occurence in the intersection
        return (result.getFreq()==0 ? null : result);
    }

    /**
     * Checks if a token exists in the positional index saved to the disk, and reads in information
     * about that token if it does
     * @param t The token to check the existence of in the corpus
     * @return A reference to the Token object created, or null if the token doesn't exist in-corpus
     */
    private static Token queryToken(String t) {
        // path is a reference to the file where the token should be stored
        File path = new File(Corpus.DIR_INDEX+t.charAt(0)+Corpus.EXT_TOKEN);

        // Only attempt to load token information if the file it should exist in
        // exists for reading!
        if (path.exists()) {
            try {
                // file is a reference to a reader for getting data on the token
                // line is temporary storage for the data being read from the file
                // stem is the stemmed variant of the term
                BufferedReader file = new BufferedReader(new FileReader(path));
                String line, stem = Token.stemToken(t);

                // While there are still lines in the document to be checked, loop reading in of lines
                // If the token is found, then its information is read, the file is closed, and the
                // function returns the object reference
                while((line = file.readLine()) != null)
                    if (line.equals(stem)) {
                        Token token = indexLoadToken(t, file);

                        file.close();
                        return token;
                    }

                file.close();
            }
            catch(IOException e) {
                System.err.println("SYS: Error occurred during query > token existence check!");
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        return null;
    }

    /**
     * Attempts to load a token from the positional index on the disk
     * @param token The token, as a string, to be read in
     * @param file The file where the token's information is being read
     * @return A token object if read was successful, or null if not
     */
    private static Token indexLoadToken(String token, BufferedReader file) {
        // Try to create a token object and load in its postings list from the positional
        // index, returning the token on successful creation
        try {
            // t is the token being generated, doc is the current document whose positional
            // postings is being read in, and data is a temporary storage variable
            Token t = new Token(token);
            int doc;
            String data;

            // Iterate through all the document listings for this token, parsing the positions
            // where the token occurs from the positional index files
            while(((data = file.readLine()) != null) && data.contains("{")) {
                doc = Integer.parseInt(data.substring(0,data.indexOf(" ")));

                t.addDoc(doc);
                for(String w : data.substring(data.indexOf("{")).split(" "))
                    if (!w.equals("{") && !w.equals("}"))
                        t.addDocLocation(doc, Integer.parseInt(w));
            }
            
            // Return the token created
            return t;
        }
        catch(IOException e) {
            System.err.println("SYS: Error during token load!");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        // Return null (something went wrong)
        return null;
    }

    /**
     * Reads in the list of corpus entries from the main index directory, storing the paths to each
     * entry's file in fileIndex
     */
    private static void loadFileIndex() {
        // Try to read in all of the file paths of the corpus entries and
        // store them into the fileIndex array
        try {
            // docs will be used to read in the entries of the corpus from the documents file
            BufferedReader docs = new BufferedReader(new FileReader(Corpus.FIL_DOCS));
            fileIndex = new String[Integer.parseInt(docs.readLine())][2];

            // Read in the full path to the file and then shrink it for a preview shown for query results
            for(int i=0; i<fileIndex.length; i++) {
                fileIndex[i][PATH_FULL] = docs.readLine();
                fileIndex[i][PATH_FILE] = fileIndex[i][PATH_FULL].substring(fileIndex[i][PATH_FULL].lastIndexOf('/')+1);
            }
            
            docs.close();
        }
        catch(IOException e) {
            System.err.println("SYS: Error on file index load!");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}