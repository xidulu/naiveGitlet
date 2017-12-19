package gitlet;
import java.io.File;

/**
 * Pure file, with no name, no path.
 * @author XiWang */

public class Blob {
    /**
     * Change a file FILE to blob, and return its hash.
     */
    static String file2blob(File file) {
        byte[] fileContent = Utils.readContents(file);
        String sha = Utils.sha1(fileContent);
        String fileDirectory = "./objects/" + sha;
        Utils.writeContents(new File(fileDirectory), fileContent);
        return sha;
    }

}
