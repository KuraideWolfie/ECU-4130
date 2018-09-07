/*
 * File:    Query.java
 * Version: 2.2
 * Author:  Matthew Morgan
 * Description:
 * Query displays, and operates, the query interface for processing queries on
 * a corpus and the data contained therein.
 * 
 * ~~~ CHANGE HISTORY ~~~
 * Version 1.0 (9 February 2018)
 * Version 2.1 (28 March 2018)
 * - Conformance to data storage changes (Model and hashtables versus
 *   TokenDictionaries with Indexes in Tokens)
 * - "!vector" handles title and document content vectors
 * Version 2.2 (2 May 2018)
 * - "!title" handles the displaying of document titles to the screen
 * - "!system" allows tuning of system parameters for query operation
 *
 * IT LOOKS LIKE VECTOR ISN'T PRINTING DOCUMENT RANGE???
 * 
 * ~~~ BUCKETLIST ~~~
 * - !doc command lets the user query from the text corpus, or shows an error
 *   if the document can't be found. It should wrap text at 64 characters
 *   - This requires reconstructing the positional index from assn 2 and
 *     saving the corpus' text, pre-processed but not tokenized, in a file
 * - Usage information should show a bit more information about commands to the
 *   user so inferred parameters are made available/ more clear
 * - Support CPL manipulation for query vectorization
 * - Query history (5 entries?)
 * - Complex querying
 *   - ASSN 2: expansive querying, such as "tomato !and youngest daughter /4"
 *   - ASSN 3: Utilization of ands, ors, and nots to specialize results
 *     - This would complicate similarity computation, such as requiring that
 *       terms not being looked for lowering the similarity
 *     - Positional intersection queries could have a special command?
 * - Make an info command that supports the 'title' command and also a special
 *   for 'authors' (which would require an index reformat to include authors)
 */

// Import statements
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collections;

public class Query {
    // CMD_EXIT is a special command that will terminate the query interface
    // CMD_HELP is a special command that will display helpful info to the user
    // CMD_VECTOR is a special command for printing vector information
    // CMD_USAGE is a special command for displaying usage info for commands
    // CMD_TITLE is a special command for printing document titles
    // CMD_TUNE is a special command for tuning the system
    // CMD_TERM is a special command for getting statistics about a term
    private static final String CMD_EXIT = "!exit", CMD_HELP = "!help",
        CMD_VECTOR = "!vector", CMD_USAGE = "!usage", CMD_TITLE = "!title",
        CMD_TUNE = "!system", CMD_TERM = "!term";

    // rawQuery is the raw string given by the user during querying
    // dic is a reference to the dictionary of tokens this query will use
    // vsm is a reference to the vector space model this query will use
    // resCntTitle is the number of documents fetched, by title, that
    //   will be considered in results for document similarity'
    // resCntDoc is the number of documents fetched, by content, that
    //   will be shown as results
    private String rawQuery = "";
    private Model title, documents;
    private ArrayList<String> titles;
    private int resCntTitle = 25, resCntDoc = 10;

    /**
     * Instantiates a query object
     * @param t The VSM for titles
     * @param d The VSM for document content
     * @param ts List of titles, as strings
     */
    public Query(Model t, Model d, ArrayList<String> ts) {
        title = t;
        documents = d;
        titles = ts;
    }

    /** Starts the querying interface, and accesses all other functions of the
     *  class for query processing */
    public void query() {
        // Initialize a scanner for reading user queries and print intro message
        Scanner kbd = new Scanner(FileHandle.customScanner());
        for(int i=0; i<64; i++) { System.out.print("-"+(i==63?"\n":"")); }
        System.out.printf("Type '%s' for help, or '%s' to exit\n",
            CMD_HELP, CMD_EXIT);
        
        // Continue getting queries and processing them until the exit command
        // has been entered
        do {
            queryParse();
            System.out.print("\nQuery > ");
        }
        while(!(rawQuery=kbd.nextLine().trim().toLowerCase()).equals(CMD_EXIT));

        kbd.close();
    }

    /** Parses the query made by the user, detecting special commands to
     *  appropriately handle the query. A regular query on the documents is
     *  only considered if no special commands are found. */
    private void queryParse() {
        if (!rawQuery.equals("")) {
            // Special command detection
            if (rawQuery.equals(CMD_HELP)) { help(); }
            else if (rawQuery.contains(CMD_VECTOR)) { vector(); }
            else if (rawQuery.contains(CMD_USAGE)) { usage(); }
            else if (rawQuery.contains(CMD_TITLE)) { title(); }
            else if (rawQuery.contains(CMD_TUNE)) { system(); }
            else {
                // No special commands were detected
                queryExecute();
            }
        }
    }

    /** Executes the user's query by vectorizing the query, computing the
     *  cosine similarity against all document titles, then document content,
     *  and displays the results to the screen. */
    private void queryExecute() {
        Hashtable<String,Double> query = queryGen(rawQuery.split(" "));
        Hashtable<Double,ArrayList<Integer>> res = similarity(title,query,null);

        // Recompute the similarity of the top 25 documents based on the results
        // of the title similarity comparisons
        res = similarity(documents, query, getTopDocuments(resCntTitle, res));

        System.out.printf("  %5s : %s\n", "Doc", "Title");
        for(int id : getTopDocuments(resCntDoc, res))
            System.out.printf("  %5d : %s\n", id, titles.get(id));
    }

    /** Generates a vector based on the user query
     *  @param terms An array containing the tokens, unstemmed, to vectorize
     *  @return A vector that can be used for cosine similarity */
    private Hashtable<String,Double> queryGen(String[] terms) {
        Hashtable<String,Double> query = new Hashtable<>();

        // Add the terms to the query vector, using direct term frequency for
        // weighting, and normalize it
        for(String word : terms) {
            if (!word.equals("")) {
                word = Token.stemToken(word);
                if (query.containsKey(word))
                    query.put(word, query.get(word)+1.0);
                else
                    query.put(word, 1.0);
            }
        }

        // Normalize and return the query
        Model.normalize(query);
        return query;
    }

    /**
     *  Computes cosine similarity results for a given model and query
     *  @param model The model to compare the query vector against
     *  @param query The user query
     *  @param ids The IDs to compute the similarity for
     *  @return A hashtable of results, where keys are similarities, and the
     *    values are a list of document IDs with that similarity
     */
    private Hashtable<Double,ArrayList<Integer>> similarity(Model model,
        Hashtable<String,Double> query, ArrayList<Integer> ids) {
        // result is the final table of similarities and document IDs
        // sim is for temporary storage of similarities
        Hashtable<Double,ArrayList<Integer>> result = new Hashtable<>();
        double sim;

        if (ids == null) {
            // Since no document ids were specified, we compare against the
            // entire vsm
            for(int i=0; i<model.size(); i++) {
                sim = Model.cosineSim(model.getDoc(i), query);
                if (!result.containsKey(sim)) { result.put(sim,new ArrayList<>()); }
                result.get(sim).add(i);
            }
        }
        else {
            // Since a list of ids was specified, we only compare against those
            // documents in the model
            for(int i=0; i<ids.size(); i++) {
                sim = Model.cosineSim(model.getDoc(ids.get(i)), query);
                if (!result.containsKey(sim)) { result.put(sim,new ArrayList<>()); }
                result.get(sim).add(ids.get(i));
            }
        }

        return result;
    }

    /** Accumulates the document IDs of those most similar to a query by looking
     *  at a table generated as a result of similarity computations
     *  @param k The number of top documents to fetch
     *  @param table The hashtable containing similarity computation results
     *  @return A list of document IDs of the top-k similar documents */
    private ArrayList<Integer> getTopDocuments(int k,
        Hashtable<Double,ArrayList<Integer>> table) {
        // sim is used for tracking what key/list entry is being looked at
        // result is the resulting list of document IDs to be looked at
        // keys is a list of the similarities in the table in descending order
        // tmp is a temporary storage variable for lists of IDs in the table
        int sim[] = {0, 0};
        ArrayList<Integer> result = new ArrayList<>(), tmp;
        ArrayList<Double> keys = Collections.list(table.keys());
        Collections.sort(keys, Collections.reverseOrder());

        // While the number of requested top documents hasn't been found, we
        // seek the next top document, or stop if there are no further documents
        while(k > 0 && sim[0] < keys.size()) {
            tmp = table.get(keys.get(sim[0]));

            if (tmp.size() == sim[1]) {
                sim[0]++; sim[1] = 0; continue;
            }

            result.add(tmp.get(sim[1]));

            sim[1]++;
            k--;
        }

        return result;
    }

    /** Prints helpful usage information about a specified command
     *  @param cmd The command to provide usage information for */
    private void cmdUsage(String cmd) {
        // usage is a string set to the proper text based on the command the
        // text is being printed for
        String usage = "";
        switch("!"+cmd) {
            case CMD_HELP:   usage = "!help"; break;
            case CMD_EXIT:   usage = "!exit"; break;
            case CMD_USAGE:  usage = "!usage <cmd>"; break;
            case CMD_TITLE:  usage = "!title <id> [id]"; break;
            case CMD_TUNE:   usage = "!system [restitle] [resdoc]"; break;
            case CMD_VECTOR:
                usage = "!vector [<D|T> <id> [cpl]] [Q <query>]";
                break;
        }

        // Either print the usage text, or show that the command is unrecognized
        if (usage.equals("")) {
            System.out.printf("Unknown command '%s'!\n", cmd);
            return;
        }
        else
            System.out.printf("Usage: %s\n", usage);
    }

    /** Displays system parameters, or allows the modification of them. */
    private void system() {
        String[] cmd = rawQuery.split(" ");
        int tmp;

        try {
            // Attempt to execute the query, erroring if:
            // - The length of the command is greater than three
            switch(cmd.length) {
                // Display system parameters
                case 1:
                    System.out.println("Current System Parameters:");
                    System.out.println("  Result Generation:");
                    System.out.printf("    Phase 1|T Top-K: %d\n", resCntTitle);
                    System.out.printf("    Phase 2|D Top-K: %s\n", resCntDoc);
                    break;
                // Document count parameter is being modified
                case 3:
                    tmp = Integer.parseInt(cmd[2]);
                    resCntDoc = (tmp > 0 ? tmp : resCntDoc);
                // Title count parameter is being modified
                case 2:
                    tmp = Integer.parseInt(cmd[1]);
                    resCntTitle = (tmp > 0 ? tmp : resCntTitle);
                    if (resCntDoc > resCntTitle) { resCntDoc = resCntTitle; }
                    break;
                default: throw new Exception("");
            }
        }
        catch(Exception e) { usageInvalid("system"); }
    }

    /** Displays helpful information, such as accepted, special commands. */
    private void help() {
        System.out.printf(
          "\n----------------------------------------------------------------"+
          "\nQuery Interface Version 2.1, 28 March 2018, Matthew Morgan"+
          "\n\nQuerying:"+
          "\n    Type any number of terms, separated by spaces, for a query."+
          "\n    Press <enter> to submit the query for processing."+
          "\n\nSpecial Commands: (Prefix with !)"+
          "\n    help   : Displays this information"+
          "\n    exit   : Exits the querying interface"+
          "\n    usage  : Print usage information for any commands here"+
          "\n    vector : Print an available list of document vectors, print"+
          "\n        a vector's components, or see a query vectorized"+
          "\n    title  : Print document titles to the screen"+
          "\n    system : Tune the system's parameters or view them"+
          "\n----------------------------------------------------------------"+
          "\n"
        );
    }

    /** Prints the titles of either a single document or a range of documents. */
    private void title() {
        // start is the starting title to print
        // end is the end of the range of titles to be printed (-1 if not given)
        String[] cmd = rawQuery.split(" ");
        int start, end = -1;

        try {
            if (cmd.length < 2 || cmd.length > 3) { throw new Exception(""); }

            // Try to parse the query, printing the command is invalid if an
            // error occurs due to:
            // - The number of specified parameters is not between 2 and 3
            // - The ids to be printed are not numeric in valid
            // - The start or ending ID is out of range
            // - The end ID is less than the start ID
            start = Integer.parseInt(cmd[1]);
            if (cmd.length == 3) { end = Integer.parseInt(cmd[2]); }
            if (start < 0 || start >= titles.size() || end >= titles.size())
                throw new Exception("");
            if (end != -1 && (end < start))
                throw new Exception("");

            if (end == -1)
                System.out.printf("Document %d Title: '%s'\n", start, titles.get(start));
            else {
                while(start <= end) {
                    System.out.printf("Document %d Title: '%s'\n", start, titles.get(start));
                    start++;
                }
            }
        }
        catch(Exception e) { usageInvalid("title"); }
    }

    /** Prints the available set of document vector IDs, prints the components
     *  of a document vector, or vectorizes a query and prints the vector. The
     *  number of components printed, per line, can be controlled when printing
     *  document vectors - not queries that are vectorized" */
    private void vector() {
        // doc is the document whose vector should be printed
        // cpl is the number of components to print to the screen
        String[] cmd = rawQuery.split(" ");
        int doc;
        byte cpl = 2;

        // Try to process the query, printing the command is invalid if
        // an error occurs due to:
        //
        // Document/Title:
        // - The number of terms in the query exceeding 4
        // - The document ID entered being invalid
        // - The number of components to print being invalid
        // Query Vectorization:
        // - The number of terms in the query equalling 2
        try {
            cmd[1] = cmd[1].toLowerCase();

            if (!cmd[1].equals("q")) {
                // The user's input is NOT a query to be vectorized
                if (cmd.length > 4) { throw new Exception(""); }

                // If the length is one, print what documents have vectors
                if (cmd.length == 1) {
                    System.out.printf("Valid Vectors: %d-%d\n",
                        0, title.size()-1);
                    return;
                }

                // Parse doc ID and CPL for printing the vector (if given)
                doc = Integer.parseInt(cmd[2]);
                if (cmd.length >= 4) { cpl = Byte.parseByte(cmd[3]); }
                
                // Act based on the type of vector being printed
                switch(cmd[1].toLowerCase()) {
                    case "d":
                        System.out.print("Document ");
                        documents.printVector(doc, cpl);
                        break;
                    case "t":
                        System.out.print("Title ");
                        title.printVector(doc, cpl);
                        break;
                    default:
                        throw new Exception("");
                }
            }
            else {
                // The user's input is a query to be vectorized
                if (cmd.length == 2) { throw new Exception(""); }

                System.out.println("Query Vector");
                cmd[0] = ""; cmd[1] = "";
                Model.printVector(queryGen(cmd));
            }
        }
        catch(Exception e) { usageInvalid("vector"); }
    }

    /** Prints basic usage information regarding the commands in the interface*/
    private void usage() {
        String[] cmd = rawQuery.split(" ");

        // Try to process the query. Possible errors:
        // - There's only one term in the query
        try {
            if (cmd.length != 2)
                throw new Exception("");
            
            cmdUsage(cmd[1]);
        }
        catch(Exception e) { usageInvalid("usage"); }
    }

    /** Prints invalid usage information to the user
      * @param cmd The command to print the usage of */
    private void usageInvalid(String cmd) {
        System.out.print("Invalid usage! ");
        cmdUsage(cmd);
    }
}