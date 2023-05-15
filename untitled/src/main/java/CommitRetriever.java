import model.Class;
import model.Release;
import model.Ticket;
import org.codehaus.jettison.json.JSONException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.NullOutputStream;
import utils.CSV;
import utils.CommitUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class CommitRetriever {

    private static Repository repository;

    public static List<Class> retrieveCommits(String projName, List<Ticket> allTickets, int numVersions) throws IOException, JSONException, GitAPIException {

        List<Class> allClasses = new ArrayList<>();
        List<RevCommit> commits = new ArrayList<>();

        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, false, numVersions, true);

        System.out.println("------------- retrieving the commits for " + projName + "-----------------");
        System.out.println(releases.size());

        File file = new File(projName.toLowerCase());
        if (file.exists() && file.isDirectory())
            repository = new FileRepository(projName.toLowerCase() + "/.git/");
        else {
            System.out.println("Cloning repository...");
            repository = Git.cloneRepository().setURI("https://github.com/apache/" + projName.toLowerCase() + ".git").call().getRepository();
        }

        try (Git git = new Git(repository)) {

            /*
             *   retrieving all the commits
             *   ASSUMPTION: we're discarding the last half of releases, in order to have a smaller number of commits to handle
             */

            LocalDateTime lastRelease = releases.get(releases.size() - 1).getDate();

            System.out.println("Last release: " + lastRelease);

            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
                    if (!commits.contains(commit)) {
                        if (commitDate.isBefore(lastRelease) || commitDate.isEqual(lastRelease))
                            commits.add(commit);
                    }

                }
            }

            System.out.println("Number of total commits: " + commits.size());

            for (Ticket ticket : allTickets) {
                System.out.println(ticket.key);
            }

            for (RevCommit commit : commits) {
                System.out.println(commit.getShortMessage());
            }

            // initializing last commits for all releases
            for (int i = 0; i < releases.size(); i++) {

                LocalDateTime firstDate;

                if (i == 0) {
                    firstDate = LocalDateTime.of(1970, 1, 1, 0, 0);
                    initReleaseCommits(releases.get(i), firstDate, commits);
                } else {
                    firstDate = releases.get(i - 1).getDate();
                    initReleaseCommits(releases.get(i), firstDate, commits);
                }
            }

            System.out.println("Last commits initialized.");

            for (Release release : releases) {
                if (!release.getAssociatedCommits().isEmpty())
                    System.out.println(release.getId() + ": " + release.getAssociatedCommits().size() + ", last commit: " + release.getLastCommit().getAuthorIdent().getWhen());
            }

            List<List<Class>> classes = new ArrayList<>();

            System.out.println("getting the classes");

            for (Release release : releases) {
                if (!release.getAssociatedCommits().isEmpty()) {
                    classes.add(getClassesFromReleaseCommit(release));
                }
            }


            for (List<Class> classList : classes) {
                allClasses.addAll(classList);
            }

            System.out.println("Classes retrieved");

            retrieveCommitsForClasses(commits, allClasses);

            System.out.println("commits associated to classes: " + commits.size());

            System.out.println("CHECK");
            System.out.println("tickets: " + allTickets.size());
            System.out.println("commits: " + commits.size());
            System.out.println("classes: " + allClasses.size());
            System.out.println("releases: " + releases.size());
            labelBuggyClasses(allTickets, commits, allClasses, releases);

            int count = 0;
            List<Integer> versions = new ArrayList<>();

            for (Class cls : allClasses) {
                if (cls.isBuggy()) {
                    versions.add(cls.getRelease().getId());
                    count++;
                }
            }


            System.out.println("All classes: " + allClasses.size());
            System.out.println("Buggy classes: " + count);
            System.out.println(numVersions + ": Versions of the buggy classes: " + versions);

            ComputeMetrics computeMetrics = new ComputeMetrics();
            computeMetrics.computeMetrics(allClasses, projName);

            // CSV.generateCSV(allClasses, projName, numVersions);


        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        return allClasses;
    }

    private static void labelClasses(String className, Ticket ticket, List<Class> allClasses) {

        if (ticket.fixVersion == null)
            return;
        for (Class cls : allClasses) {
            if (cls.getName().equals(className) && cls.getRelease().getId() >= ticket.injectedVersion.getId() && cls.getRelease().getId() < ticket.fixVersion.getId()) {
                cls.setBuggy(true);
                System.out.println("Class " + cls.getName() + " of release " + cls.getRelease().getId() + "is buggy according to ticket " + ticket.key);
            }

        }

    }



    private static void labelBuggyClasses(List<Ticket> tickets, List<RevCommit> commits, List<Class> allClasses, List<Release> releases) throws JSONException, IOException {

        System.out.println("---------------- LABELING ------------------");
        System.out.println("Printing all commits: ");

        for (RevCommit commit : commits) {
            System.out.println(commit.getShortMessage());
        }

        List<Ticket> ticketsWithAV = TicketRetriever.getTicketsWithAV(tickets, releases);   // these are all tickets with fv != iv, so the tickets for which it is possible to detect buggy classes
        System.out.println("Tickets with AV: " + ticketsWithAV.size() + " - " + releases.size());

        System.out.println("Tickets with fv = null: ");
        int count = 0;
        for (Ticket ticket : ticketsWithAV) {
            if (ticket.fixVersion == null) {
                count++;
                System.out.println(ticket.key);
            } else {
                System.out.println(ticket.fixVersion.getName());
            }
        }

        System.out.println("Release " + releases.size() + ", tickets with fv = null: " + count);

        // we need to retrieve all the commits associated to all the tickets with AV
        for (Ticket ticket : ticketsWithAV) {

            List<RevCommit> commitsAssociatedToTicket = CommitUtils.filterCommitsAssociatedToTicket(ticket, commits);

            // for each commit associated to the ticket, we need the modified classes
            for (RevCommit commit : commitsAssociatedToTicket) {

                List<String> modifiedClassesNames = getModifiedClasses(commit);

                // each one of these classes is buggy if it belongs to a release with id s.t. ticket.IV.id <= class.releaseId < ticket.FV.id
                for (String modifiedClass : modifiedClassesNames) {
                    labelClasses(modifiedClass, ticket, allClasses);
                }

            }
        }

    }

    // this method returns the names of all the classes that have been modified by the commit
    public static List<String> getModifiedClasses(RevCommit commit) {

        List<String> modifiedClasses = new ArrayList<>();

        try (DiffFormatter diffFormatter = new DiffFormatter(NullOutputStream.INSTANCE)) {  // we're not interested in the output
            diffFormatter.setRepository(repository);
            ObjectReader reader = repository.newObjectReader();

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            ObjectId tree = commit.getTree();
            treeParser.reset(reader, tree);

            CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
            ObjectId parentTree = commit.getParent(0).getTree();
            parentTreeParser.reset(reader, parentTree);

            List<DiffEntry> diffEntries = diffFormatter.scan(parentTree, tree);

            for (DiffEntry entry : diffEntries) {
                if (entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/test/"))
                    modifiedClasses.add(entry.getNewPath());
            }

        } catch (ArrayIndexOutOfBoundsException ignored) {

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return modifiedClasses;
    }

    /*
     *  This method, given all classes and all commits, retrieves, for each class c, its associated commits,
     *  so the list of all those commits that modify, add, remove, delete or rename c.
     */
    private static void retrieveCommitsForClasses(List<RevCommit> commits, List<Class> allClasses) throws IOException, JSONException {
        for (RevCommit commit : commits) {
            List<String> modifiedClasses = getModifiedClasses(commit);
            for (String modifiedClass : modifiedClasses) {
                for (Class cls : allClasses) {
                    if (!cls.getAssociatedCommits().contains(commit)) {
                        if (modifiedClass.equals(cls.getName()) && getReleaseFromCommit(commit).getId() == cls.getRelease().getId())
                            cls.getAssociatedCommits().add(commit);
                    }
                }
            }
        }
    }

    private static List<Class> getClassesFromReleaseCommit(Release release) throws IOException {

        List<Class> classes = new ArrayList<>();

        HashMap<String, String> classesDescription = getClassesFromCommit(release.getLastCommit());
        Set<String> names = classesDescription.keySet();
        Collection<String> implementations = classesDescription.values();

        for (int i = 0; i < names.size(); i++) {
            String name = names.toArray()[i].toString();
            String implementation = implementations.toArray()[i].toString();

            Class newClass = new Class(name, implementation, release);

            classes.add(newClass);
        }

        return classes;
    }

    private static void initReleaseCommits(Release release, LocalDateTime firstDateTime, List<RevCommit> commits) {
        LocalDateTime lastDateTime = release.getDate();
        List<RevCommit> associatedCommits = new ArrayList<>();

        for (RevCommit commit : commits) {
            LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();

            if (commitDate.isAfter(firstDateTime) && commitDate.isBefore(lastDateTime) || commitDate.isEqual(lastDateTime))
                associatedCommits.add(commit);
        }

        release.setAssociatedCommits(associatedCommits);
        initializeReleaseLastCommit(release);
    }

    private static void initializeReleaseLastCommit(Release release) {
        if (release.getAssociatedCommits().isEmpty())
            return;

        RevCommit lastCommit = release.getAssociatedCommits().get(0);

        for (RevCommit commit : release.getAssociatedCommits()) {
            if (commit.getAuthorIdent().getWhen().after(lastCommit.getAuthorIdent().getWhen())) {
                lastCommit = commit;
            }
        }

        release.setLastCommit(lastCommit);
    }

    private static HashMap<String, String> getClassesFromCommit(RevCommit commit) throws IOException {

        HashMap<String, String> classDescription = new HashMap<>();
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(true);

        while (treeWalk.next()) {
            if (treeWalk.getPathString().contains(".java") && !treeWalk.getPathString().contains("/test/")) {
                String path = treeWalk.getPathString();
                String content = new String(repository.open(treeWalk.getObjectId(0)).getBytes());
                classDescription.put(path, content);
            }
        }

        treeWalk.close();

        return classDescription;
    }

    private static Release getReleaseFromCommit(RevCommit commit) throws JSONException, IOException {
        LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
        return TicketRetriever.getRelease(commitDate);
    }
}

