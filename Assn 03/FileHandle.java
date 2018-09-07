/*
 * File:    FileHandle.java
 * Version: 2.0
 * Author:  Matthew Morgan
 * Description:
 * Files contains a series of functions regarding file io and detection.
 * 
 * ~~~ CHANGE HISTORY ~~~
 * Version 1.0 (22 February 2018)
 * Version 1.1 (29 March 2018)
 * - Appended recursive function for file listing
 * Version 2.0 (20 May 2018)
 * 
 * ~~~ BUCKETLIST ~~~
 * - Revise confirmOverwrite(...)
 * - Make recurseDirectory() capable of filtering filename extensions
 */

// Import statements
import java.util.ArrayList;
import java.util.Scanner;
import java.io.IOException;
import java.io.File;
import java.io.FilterInputStream;

public class FileHandle {
    /**
     * Prompts the user to confirm whether they wish to overwrite the given
     * directory or file
     * @param file The file to confirm overwrite for
     * @return True if the user confirms overwrite, or false otherwise
     */
    public static boolean confirmOverwrite(File file) {
        // Scanner for input, the user's response, and whether that
        // response is valid or not
        Scanner kbd = new Scanner(customScanner());
        String response;
        boolean validRes = true;

        System.out.printf("WARNING: A %s already exists with the given name! "+
            "Do you wish to overwrite it?\n",
            (file.isDirectory() ? "directory" : "file"));
        System.out.printf("  Type answer (y/n) > ");

        // While the user's response is detected as invalid, get input
        // from the user
        do {
            response = kbd.next().toLowerCase();
            validRes = (response.contains("y") || response.contains("n"))
                && response.length()==1;
            if (!validRes)
                System.out.printf("  Invalid response!\n  Type answer (y/n) > ");
        }
        while(!validRes);

        // Return if the response was 'y'
        kbd.close();
        return response.contains("y");
    }

    /** Creates a FilterInputStream that overrides the closing function,
     *  making it possible to open a scanner and close it without closing
     *  System.in */
    public static FilterInputStream customScanner() {
        return new FilterInputStream(System.in) {
            @Override
            public void close() throws IOException {}
        };
    }

    /**
     * Recursively digs through a folder to create a list of all files in the
     * given parent directory
     * @param directory The directory being recursed to generate filenames
     * @return A list of strings representing all the filenames
     */
    public static ArrayList<String> recurseDirectory(String directory) {
        ArrayList<String> L = new ArrayList<>();
        File dir = new File(directory);

        if (dir.isDirectory()) {
            // For every path in the directory, if the path is a directory, recurse
            // down to locate more files; otherwise, add the pathname to the list
            // of files, L
            for(File f : dir.listFiles())
                if (f.isDirectory())
                    L.addAll(recurseDirectory(directory+f.getName()+"/"));
                else
                    L.add(directory+f.getName());

        }
        else {
            // ERROR: A directory wasn't specified
            System.err.printf("ERR: '%s' is not a directory!\n", directory);
            System.exit(1);
        }
        
        return L;
    }
}