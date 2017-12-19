package gitlet;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * A copy of newest snapshot.
 *  @author XiWang */

public class Index implements Serializable {
    /** Construct index from commit COMMIT. */
    public Index(Commit commit) {
        _tree = new HashMap<String, String>(commit.getTree());
        _snapshot = new HashMap<String, String>(commit.getTree());
    }

    /** Return file tree. */
    public HashMap<String, String> getTree() {
        return _tree;
    }

    /** Add a file FILENAME with hash HASH. */
    public void addFile(String filename, String hash) {
        _tree.put(filename, hash);
    }

    /** Remove a file FILENAME, return true if needs remove from hardisk.
     *  If is not in last commit, remove from index tree.
     *  If is in last commit and is in this index tree,
     *  remove the file from disk and tree.
     *  If is in last commit, but not in this index,
     *  do nothing.
     */
    public boolean rmFile(String filename) {
        String lastCommitRecord = _snapshot.get(filename);
        String currentRecord = _tree.get(filename);
        if (lastCommitRecord == null) {
            _tree.remove(filename);
            return false;
        } else {
            if (_tree.containsKey(filename)) {
                _tree.remove(filename);
                return true;
            } else {
                return false;
            }
        }
    }

    /** Check whether a file FILE is tracked,
     *  return its hash if tracked or empty.
     */
    public String tracked(String file) {
        if (_tree.containsKey(file)) {
            return _tree.get(file);
        } else {
            return "";
        }
    }

    /** Return a key set. */
    public Set<String> getKeys() {
        return _tree.keySet();
    }

    /** The file tree in this index. */
    private HashMap<String, String> _tree;

    /** File tree from last commit. */
    private HashMap<String, String> _snapshot;

}


