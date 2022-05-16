package gitlet;

import java.io.File;
import java.io.Serializable;

/**
 * Class to store contents of files.
 *
 * @author Samhith Kakarla
 */
public class Blob implements Serializable {
    /**
     * stores string versions of contents.
     */
    private String contents;
    /**
     * name of file.
     */
    private String name;

    /* initializes blob. */
    public Blob(String fileName) {
        name = fileName;
        File file = new File(Commands.CWD, fileName);
        if (!file.exists()) {
            contents = null;
        } else {
            contents = Utils.readContentsAsString(file);
        }
    }

    /**
     * gets contents.
     * @return contents
     */
    public String getContents() {
        return contents;
    }

    /**
     * stores string versions of contents.
     *
     * @param a
     */
    public void setContets(String a) {
        this.contents = a;
    }

    /**
     * gets name.
     * @return name
     */
    public String getName() {
        return name;
    }

}
