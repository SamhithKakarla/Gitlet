package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class Commit implements Serializable {
    /**
     * parent commit.
     */
    private String _parent;
    /**
     * parent commit if merge.
     */
    private String _mergeParent;
    /**
     * message.
     */
    private String _message;
    /**
     * timestamp.
     */
    private String _timestamp;
    /**
     * blobs.
     */
    private HashMap<String, String> _filenameToSha;

    /**
     * initializes commits.
     * @param message
     * @param timestamp
     * @param filenames
     *
     */
    public Commit(String message, String timestamp, HashMap<String,
            String> filenames) {
        _message = message;
        _timestamp = timestamp;
        _filenameToSha = new HashMap<>(filenames);
        _parent = "";
        _mergeParent = "";
    }

    public String logMessage() {
        return "===\n"
                + "commit " + Utils.sha1(this.toString()) + "\n"
                + "Date: " + _timestamp + "\n"
                + _message;
    }

    public String getParent() {
        return _parent;
    }

    public void setParent(String parent) {
        _parent = parent;
    }

    public String getMergeParent() {
        return _mergeParent;
    }

    public void setMergeParent(String mergeParent) {
        _mergeParent = mergeParent;
    }

    public String getMessage() {
        return _message;
    }

    public void setMessage(String message) {
        _message = message;
    }

    public String getTimestamp() {
        return _timestamp;
    }

    public void setTimestamp(String timestamp) {
        _timestamp = timestamp;
    }

    public HashMap<String, String> getFilenameToSha() {
        return _filenameToSha;
    }

    public void setFilenameToSha(HashMap<String, String> filenameToSha) {
        _filenameToSha = filenameToSha;
    }

    public String toString() {
        return _message + _timestamp + _parent;
    }


}
