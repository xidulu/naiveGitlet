package gitlet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.io.File;
import java.io.Serializable;

/** A repository.
 *  @author XiWang */

public class Repo implements Serializable {

    /** Initialize an empty repo in current directory. */
    public Repo() {
        curDir = new File(".");
        objectFolder = Utils.join(curDir, ".gitlet");
        _remoteDir = new TreeMap<String, File>();
        _commitToMessage = new TreeMap<String, String>();
        commits = new ArrayList<String>();
        headCache = new Commit();
        _branches = new TreeMap<String, String>();
        head = headCache.getHash();
        _currentBranch = "master";
        newCommit();
        _stagingArea = new Index(headCache);
    }

    /** Return the path of current working directory. */
    public File getWorkingDir() {
        return curDir;
    }

    /** Turn current stage area to commit with message LOG.
     *  And clear the Area;
     *  Store the newest commit hash;
     */
    public void newCommit(String log) {
        if (_stagingArea.getTree().equals(headCache.getTree())) {
            throw new GitletException("No changes added to the commit.");
        }
        headCache = new Commit(_stagingArea, log, headCache.getHash());
        byte[] fileContent = Utils.serialize(headCache);
        head = headCache.getHash();
        commits.add(head);
        _commitToMessage.put(head, log);
        File outputDir = Utils.join(objectFolder, head);
        Utils.writeContents(outputDir, fileContent);
        _stagingArea = new Index(headCache);
        _branches.put(_currentBranch, head);
    }

    /** Add a new merge commit with message LOG, and another parent
     *  hash COPARENT. */
    private void mergeCommit(String log, String coparent) {
        if (_stagingArea.getTree().equals(headCache.getTree())) {
            throw new GitletException("No changes added to the commit.");
        }
        headCache = new Commit(_stagingArea, log, headCache.getHash());
        headCache.setCoParent(_branches.get(coparent));
        byte[] fileContent = Utils.serialize(headCache);
        head = headCache.getHash();
        commits.add(head);
        _commitToMessage.put(head, log);
        File outputDir = Utils.join(objectFolder, head);
        Utils.writeContents(outputDir, fileContent);
        _stagingArea = new Index(headCache);
        _branches.put(_currentBranch, head);
    }

    /** Store the initial hash. */
    public void newCommit() {
        byte[] fileContent = Utils.serialize(headCache);
        String hash = headCache.getHash();
        File outputDir = Utils.join(objectFolder, hash);
        Utils.writeContents(outputDir, fileContent);
        commits.add(hash);
        _commitToMessage.put(hash, "initial commit");
        _branches.put(_currentBranch, head);
    }

    /** Add a new file FILE to index and local disk. */
    public void addFile(String file) {
        File fileDir = Utils.join(curDir, file);
        byte[] fileContent = Utils.readContents(fileDir);
        String hash = readFileHash(file);
        File outputDir = Utils.join(objectFolder, hash);
        Utils.writeContents(outputDir, fileContent);
        _stagingArea.addFile(file, hash);
    }

    /** Extract a file FILE from blobs HASH, store it with name. */
    private void extractFile(String file, String hash) {
        File blobDir = Utils.join(objectFolder, hash);
        File writeDir = Utils.join(curDir, file);
        Utils.writeContents(writeDir, Utils.readContents(blobDir));
    }

    /** Remove FILE from index. */
    public void removeFile(String file) {
        if (_stagingArea.tracked(file).equals("")
                && headCache.tracked(file).equals("")) {
            throw new GitletException("No reason to remove the file.");
        }
        boolean hard = _stagingArea.rmFile(file);
        if (hard) {
            removeHard(file);
        }
    }

    /** Remove FILE from folder. */
    private void removeHard(String file) {
        Utils.restrictedDelete(Utils.join(curDir, file));
    }

    /** Show logs. */
    public void printLog() {
        String first = head;
        while (!first.equals("")) {
            Commit t = getCommit(first);
            System.out.println(t.toString());
            first = t.getParent();
        }
    }

    /** Show global log. */
    public void printGlobalLog() {
        for (String c : commits) {
            System.out.println(getCommit(c.toString()));
        }
    }

    /** Get modification but not staged files, return a set. */
    private TreeSet<String> getModifiedNotStaged() {
        TreeSet<String> result = new TreeSet<String>();
        for (HashMap.Entry<String, String> e
                : _stagingArea.getTree().entrySet()) {
            String file = e.getKey();
            String hash = e.getValue();
            if (!hash.equals(readFileHash(file))) {
                result.add(file);
            }
        }
        return result;
    }


    /** Print the status. */
    public void printStatus() {
        System.out.print("=== Branches ===\n");
        for (String s : _branches.keySet()) {
            if (s.equals(_currentBranch)) {
                System.out.printf("*%s\n", s);
            } else {
                System.out.printf("%s\n", s);
            }
        }
        System.out.println();

        System.out.print("=== Staged Files ===\n");
        for (String s : getStaged()) {
            System.out.printf("%s\n", s);
        }
        System.out.println();

        System.out.print("=== Removed Files ===\n");
        for (String s : getRemovedFiles()) {
            System.out.printf("%s\n", s);
        }
        System.out.println();

        TreeSet<String> allFilesNameSet
            = new TreeSet<>(Utils.plainFilenamesIn(curDir));
        System.out.print("=== Modifications Not Staged For Commit ===\n");
        for (String s : getModifiedNotStaged()) {
            if (allFilesNameSet.contains(s)) {
                System.out.printf("%s (modified)\n", s);
            } else {
                System.out.printf("%s (deleted)\n", s);
            }
        }
        System.out.println();

        System.out.print("=== Untracked Files ===\n");
        allFilesNameSet.removeAll(_stagingArea.getKeys());
        for (String s : allFilesNameSet) {
            System.out.printf("%s\n", s);
        }
    }

    /** Read and return a commit with hash HASH from the history. */
    public Commit getCommit(String hash) {
        if (!commits.contains(hash)) {
            throw new GitletException("No commit with that id exists.");
        }
        File readDir = Utils.join(objectFolder, hash);
        return Utils.readObject(readDir, Commit.class);
    }

    /** Find commits with specific information */

    /** Checkout to branch NAME. */
    public void checkout2branch(String name) {
        if (name.equals(_currentBranch)) {
            throw
                new GitletException("No need to checkout the current branch.");
        }
        if (!_branches.keySet().contains(name)) {
            throw new GitletException("No such branch exists.");
        }
        _currentBranch = name;
        revertWorkingFolder(_branches.get(name));
    }

    /** Reset a FILE from head. */
    public void revertFile(String file) {
        String blobHash = headCache.tracked(file);
        if (blobHash.equals("")) {
            throw new GitletException("File does not exist in that commit.");
        }
        extractFile(file, blobHash);
    }

    /** Reset a file from past commit COMMIT, MODIFIED FILE NOT STAGED. */
    public void revertFile(String commit, String file) {
        String originHash = headCache.tracked(file);
        String blobHash = getCommit(commit).tracked(file);
        if (blobHash.equals("")) {
            throw new GitletException("File does not exist in that commit.");
        }
        extractFile(file, blobHash);
    }

    /** Simply reset to a commit(dangerous) COMMIT. */
    public void dangerousReset(String commit) {
        HashMap<String, String> oldTree = getCommit(commit).getTree();
        Set<String> oldKey = oldTree.keySet();
        Set<String> currentKey = _stagingArea.getTree().keySet();
        for (String file : currentKey) {
            removeHard(file);
        }
        for (String file : oldKey) {
            extractFile(file, oldTree.get(file));
        }
        headCache = getCommit(commit);
        head = commit;
        _branches.put(_currentBranch, head);
        _stagingArea = new Index(headCache);
    }

    /** Reset whole working folder to past commit COMMIT. */
    public void revertWorkingFolder(String commit) {
        if (!commits.contains(commit)) {
            throw new GitletException("No commit with that id exists.");
        }
        HashMap<String, String> oldTree = getCommit(commit).getTree();
        Set<String> oldKey = oldTree.keySet();
        Set<String> currentKey = _stagingArea.getTree().keySet();
        for (String file : oldKey) {
            if (_stagingArea.tracked(file).equals("")) {
                File fileDir = Utils.join(curDir, file);
                if (fileDir.exists()) {
                    throw new GitletException(
                            "There is an untracked file in the way; "
                            + "delete it or add it first.");
                }
            }
        }
        for (String file : currentKey) {
            removeHard(file);
        }
        for (String file : oldKey) {
            extractFile(file, oldTree.get(file));
        }
        headCache = getCommit(commit);
        head = headCache.getHash();
        _branches.put(_currentBranch, head);
        _stagingArea = new Index(headCache);
    }

    /** Create a new branch NAME. */
    public void createBranch(String name) {
        if (_branches.containsKey(name)) {
            throw
                new GitletException("A branch with that name already exists.");
        }
        _branches.put(name, head);
    }

    /** Remove a branch NAME. */
    public void removeBranch(String name) {
        if (name.equals(_currentBranch)) {
            throw new GitletException("Cannot remove the current branch.");
        }
        if (!_branches.containsKey(name)) {
            throw
                new GitletException("A branch with that name does not exist.");
        }
        _branches.remove(name);
    }

    /** Read FILE's content, return its hash. */
    private String readFileHash(String file) {
        File fileDir = Utils.join(curDir, file);
        if (!fileDir.exists()) {
            return "";
        }
        return Utils.sha1(Utils.readContents(fileDir));
    }

    /** Get staged files, return a set. */
    public TreeSet<String> getStaged() {
        TreeSet<String> result = new TreeSet<String>();
        for (HashMap.Entry<String, String> e
                : _stagingArea.getTree().entrySet()) {
            String file = e.getKey();
            String hash = e.getValue();
            if (!hash.equals(headCache.tracked(file))) {
                result.add(file);
            }
        }
        return result;
    }

    /** Get removed files, return a set. */
    public TreeSet<String> getRemovedFiles() {
        TreeSet<String> result = new TreeSet<String>();
        for (HashMap.Entry<String, String> e
                : headCache.getTree().entrySet()) {
            String file = e.getKey();
            if (_stagingArea.tracked(file).equals("")) {
                result.add(file);
            }
        }
        return result;
    }

    /** Iterate through givenBranch.
     *  GIVENBRANCH.
     *  CURRENTBRANCH.
     *  SPLITPOINTCOMMIT.
     *  TOBECHECKEDOUT.
     *  CONFLICTFILE.
     */
    private void suoDuanMergeFangFa(Commit givenBranch,
                                    Commit currentBranch,
                                    Commit splitPointCommit,
                                    Set<String> toBeCheckedOut,
                                    Set<String> conflictFile) {
        for (String file : givenBranch.getKeys()) {
            String givenFileHash = givenBranch.tracked(file);
            String currentFileHash = currentBranch.tracked(file);
            if (!givenFileHash.equals(currentFileHash)) {
                String splitPointHash = splitPointCommit.tracked(file);
                if ((!currentFileHash.equals(splitPointHash))
                        && (!givenFileHash.equals(splitPointHash))) {
                    conflictFile.add(file);
                    continue;
                }
                if (currentFileHash.equals(splitPointCommit.tracked(file))) {
                    if ((!readFileHash(file).equals(""))
                            && (currentFileHash.equals(""))) {
                        throw
                            new GitletException(
                                    "There is an untracked file in the way; "
                                    + "delete it or add it first.");
                    }
                    toBeCheckedOut.add(file);
                }
            }
        }
    }

    /** Iterate through givenBranch.
     *  GIVENBRANCH.
     *  CURRENTBRANCH.
     *  SPLITPOINTCOMMIT.
     *  TOBEDELETED.
     *  CONFLICTFILE.
     */
    private void suoDuanMergeFangFa2(Commit givenBranch,
                                     Commit currentBranch,
                                     Commit splitPointCommit,
                                     Set<String> toBeDeleted,
                                     Set<String> conflictFile) {
        for (String file : currentBranch.getKeys()) {
            String givenFileHash = givenBranch.tracked(file);
            String currentFileHash = currentBranch.tracked(file);
            if (givenFileHash.equals("")) {
                if ((!splitPointCommit.tracked(file).equals("")
                            &&
                            (!currentFileHash.equals(
                            splitPointCommit.tracked(file))))) {
                    conflictFile.add(file);
                    continue;
                }
                if (currentFileHash.equals(splitPointCommit.tracked(file))) {
                    if (!_stagingArea.tracked(file).equals("")) {
                        toBeDeleted.add(file);
                    } else {
                        if (!readFileHash(file).equals("")) {
                            throw new GitletException(
                                    "There is an untracked file in the way;"
                                    + " delete it or add it first.");
                        }
                    }
                }
            }
        }
    }

    /** Check whether BRANCH satisfies basic merge conditions. */
    private void checkMergeCondition(String branch) {
        if (!_branches.containsKey(branch)) {
            throw
                new GitletException("A branch with that name does not exist.");
        }
        if (branch.equals(_currentBranch)) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
        if ((!getRemovedFiles().isEmpty()) || (!getStaged().isEmpty())) {
            throw new GitletException("You have uncommitted changes.");
        }
    }

    /** Merge another branch with current branch BRANCH. */
    public void merge(String branch) {
        checkMergeCondition(branch);
        String splitPoint = getSplitPoint(branch);
        Boolean meetConflict = false;
        Commit splitPointCommit = getCommit(splitPoint);
        Commit givenBranch = getCommit(_branches.get(branch));
        Commit currentBranch = getCommit(_branches.get(_currentBranch));
        TreeSet<String> toBeDeleted = new TreeSet<String>();
        TreeSet<String> toBeCheckedOut = new TreeSet<String>();
        TreeSet<String> conflictFile = new TreeSet<String>();
        if (splitPoint.equals(_branches.get(_currentBranch))) {
            throw new GitletException("Current branch fast-forwarded.");
        }
        if (splitPoint.equals(_branches.get(branch))) {
            head = _branches.get(branch);
            _branches.put(_currentBranch, head);
            headCache = getCommit(head);
            throw new GitletException(
                    "Given branch is an ancestor of the current branch.");
        }
        suoDuanMergeFangFa(givenBranch, currentBranch, splitPointCommit,
                           toBeCheckedOut, conflictFile);
        suoDuanMergeFangFa2(givenBranch, currentBranch, splitPointCommit,
                            toBeDeleted, conflictFile);
        for (String file : toBeCheckedOut) {
            revertFile(givenBranch.getHash(), file); addFile(file);
        }
        for (String file : toBeDeleted) {
            removeFile(file);
        }
        if (!conflictFile.isEmpty()) {
            System.out.println("Encountered a merge conflict.");
        }
        for (String file : conflictFile) {
            String first, second;
            String givenFileHash = givenBranch.tracked(file);
            String currentFileHash = currentBranch.tracked(file);
            if (!currentFileHash.equals("")) {
                first = Utils.readContentsAsString(
                        Utils.join(objectFolder, currentFileHash));
            } else {
                first = "";
            }
            if (!givenFileHash.equals("")) {
                second = Utils.readContentsAsString(
                        Utils.join(objectFolder, givenFileHash));
            } else {
                second = "";
            }
            String third =
                "<<<<<<< HEAD\n" + first + "=======\n" + second + ">>>>>>>\n";
            Utils.writeContents(Utils.join(curDir, file), third);
            addFile(file);
        }
        mergeCommit(String.format("Merged %s into %s.",
                    branch, _currentBranch), branch);
    }

    /** Return split point of BRANCH with current branch. */
    private String getSplitPoint(String branch) {
        TreeSet<String> pathGivenBranch = new TreeSet<String>();
        String first = _branches.get(branch);
        while (!first.equals("")) {
            Commit t = getCommit(first);
            pathGivenBranch.add(first);
            first = t.getParent();
        }
        first = _branches.get(_currentBranch);
        while (!first.equals("")) {
            Commit t = getCommit(first);
            if (pathGivenBranch.contains(first)) {
                return first;
            }
            first = t.getParent();
        }
        return "NOT FOUND";

    }

    /** Find commmit with specific message MESSAGE. */
    public void doFind(String message) {
        Boolean find = false;
        for (HashMap.Entry<String, String> e : _commitToMessage.entrySet()) {
            String c = e.getKey();
            String m = e.getValue();
            if (m.equals(message)) {
                System.out.println(c);
                find = true;
            }
        }
        if (!find) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /** Convert short UID ID back to full length, return the result. */
    public String changeBackShortID(String id) {
        for (String s : commits) {
            if (s.startsWith(id)) {
                return s;
            }
        }
        return "ARE YOU SURE ABOUT THAT!";
    }

    /** Return hash for a branch BRANCH. */
    private String branch2hash(String branch) {
        return _branches.get(branch);
    }


    /**
     * ******************
     * Start remote part.
     * ******************
     */
    /** Fetch the remote branch BRANCH from remote name NAME. */
    public void fetchRemoteBranch(String name, String branch) {
        File remoteDir = _remoteDir.get(name);
        if (!remoteDir.exists()) {
            throw new GitletException("Remote directory not found.");
        }
        File remoteObjectDir = remoteDir;
        Repo remoteRepo =
            Utils.readObject(Utils.join(remoteObjectDir, "REPO"), Repo.class);
        String remoteBranchHead = remoteRepo.branch2hash(branch);
        if (remoteBranchHead == null) {
            throw new GitletException("That remote does not have that branch.");
        }
        String first = remoteRepo.branch2hash(branch);
        while (!first.equals("")) {
            byte[] fileContent =
                Utils.readContents(Utils.join(remoteObjectDir, first));
            File outputDir = Utils.join(objectFolder, first);
            Utils.writeContents(outputDir, fileContent);
            commits.add(first);
            remoteRepo.moveAllBlobs(first, objectFolder);
            first = remoteRepo.getCommit(first).getParent();
        }
        String newBranchName = String.format("%s/%s", name, branch);
        _branches.put(newBranchName, remoteBranchHead);
    }

    /** Pull the remote branch BRANCH from remote name NAME. */
    public void pull(String name, String branch) {
        fetchRemoteBranch(name, branch);
        String newBranchName = String.format("%s/%s", name, branch);
        merge(newBranchName);
    }

    /** Push the current branch into remote BRANCH in NAME. */
    public void push(String name, String branch) {
        File remoteDir = _remoteDir.get(name);
        if (!remoteDir.exists()) {
            throw new GitletException("Remote directory not found.");
        }
        File remoteObjectDir = Utils.join(remoteDir);
        Repo remoteRepo
            = Utils.readObject(Utils.join(remoteObjectDir, "REPO"), Repo.class);
        String remoteBranchHead = remoteRepo.branch2hash(branch);
        String first = _branches.get(_currentBranch);
        TreeSet<String> diffCommits = new TreeSet<String>();
        Boolean b = false;
        while (!first.equals("")) {
            diffCommits.add(first);
            if (first.equals(remoteBranchHead)) {
                b = true;
                break;
            }
            first = getCommit(first).getParent();
        }
        if (!b) {
            throw
                new GitletException(
                        " Please pull down remote changes before pushing.");
        }
        for (String commit : diffCommits) {
            byte[] fileContent =
                Utils.readContents(Utils.join(objectFolder, commit));
            File outputDir = Utils.join(remoteObjectDir, commit);
            Utils.writeContents(outputDir, fileContent);
            moveAllBlobs(commit, remoteObjectDir);
            remoteRepo.commits.add(commit);
        }
        remoteRepo.dangerousReset(_branches.get(_currentBranch));
        Utils.writeObject(Utils.join(remoteObjectDir, "REPO"), remoteRepo);
    }

    /** Move all blobs involved in current repo's COMMIT
     *  to another folder OUTPUTDIR. */
    private void moveAllBlobs(String commit, File outputDir) {
        Commit cache = getCommit(commit);
        File absObjectFolder = Utils.join(absPath, ".gitlet");
        for (String file : cache.getKeys()) {
            String hash = cache.tracked(file);
            byte[] fileContent
                = Utils.readContents(Utils.join(absObjectFolder, hash));
            Utils.writeContents(Utils.join(outputDir, hash), fileContent);
        }
    }

    /** Add a remote with path FILE, name NAME. */
    public void addRemote(String name, File file) {
        if (_remoteDir.containsKey(name)) {
            throw
                new GitletException("A remote with that name already exists.");
        }
        _remoteDir.put(name, file);
    }

    /** Remove remote NAME. */
    public void removeRemote(String name) {
        if (!_remoteDir.containsKey(name)) {
            throw
                new GitletException("A remote with that name does not exist.");
        }
        _remoteDir.remove(name);
    }



    /** Store the current working folder. */
    private File curDir;
    /** Store the folder for objects. */
    private File objectFolder;
    /** Store all the commits in this folder, store the hash. */
    private List<String> commits;
    /** Current Staging Area. */
    private Index _stagingArea;
    /** Newest commit. */
    private String head;
    /** Cache for the head commit. */
    private Commit headCache;
    /** Branches. name to hash. */
    private TreeMap<String, String> _branches;
    /** Current branch name. */
    private String _currentBranch;
    /** Store commit-message pair. */
    private TreeMap<String, String> _commitToMessage;
    /** Store remote dir. */
    private TreeMap<String, File> _remoteDir;
    /** Store absoloute path. */
    private File absPath = new File(System.getProperty("user.dir"));

}
