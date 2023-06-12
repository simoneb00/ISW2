package retrievers;

import exceptions.ExecutionException;
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
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CommitUtils;
import weka.ComputeMetrics;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;

public class CommitRetriever {

    private static final Logger logger = LoggerFactory.getLogger(CommitRetriever.class);

    private CommitRetriever() {
    }

    private static Repository repository;

    public static List<Class> retrieveCommits(String projName, List<Ticket> allTickets, int numVersions) throws IOException, JSONException, GitAPIException, ExecutionException {

        List<Class> allClasses = new ArrayList<>();
        List<RevCommit> commits = new ArrayList<>();

        List<Release> releases = GetReleaseInfo.getReleaseInfo(projName, false, numVersions, false);

        logger.info("retrieving the commits for {} ", projName);

        File file = new File(projName.toLowerCase());
        if (file.exists() && file.isDirectory())
            repository = new FileRepository(projName.toLowerCase() + "/.git/");
        else {
            logger.info("Cloning repository...");
            repository = Git.cloneRepository().setURI("https://github.com/apache/" + projName.toLowerCase() + ".git").call().getRepository();
        }

        try (Git git = new Git(repository)) {

            /*
             *   retrieving all the commits
             *   ASSUMPTION: we're discarding the last half of releases, in order to have a smaller number of commits to handle
             */

            LocalDateTime lastRelease = releases.get(releases.size() - 1).getDate();

            String date = lastRelease.getYear() + "-" + lastRelease.getMonthValue() + "-" + lastRelease.getDayOfMonth() + ":23.59";
            Date lastReleaseDate = new SimpleDateFormat("yyyy-MM-dd:hh.mm").parse(date);


            List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            for (Ref branch : branches) {
                Iterable<RevCommit> branchCommits = git.log().add(repository.resolve(branch.getName())).call();
                for (RevCommit commit : branchCommits) {
                    Date commitDate = commit.getAuthorIdent().getWhen();
                    if ((!commits.contains(commit)) && (commitDate.before(lastReleaseDate) || commitDate.equals(lastReleaseDate))) {
                        commits.add(commit);
                    }

                }
            }

            logger.info("Number of total commits: {}", commits.size());

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

            logger.info("Last commits initialized.");
            logger.info("getting the classes");

            for (Release release : releases) {
                if (!release.getAssociatedCommits().isEmpty()) {
                    allClasses.addAll(getClassesFromReleaseCommit(release));
                }
            }

            logger.info("Retrieved classes: {}", allClasses.size());

            int count;

            for (Release release : releases) {
                count = 0;
                for (Class c : allClasses) {
                    if (c.getRelease().getId() == release.getId())
                        count++;
                }

                logger.info("Classes for release {}: {}", release.getId(), count);
            }

            retrieveCommitsForClasses(commits, allClasses);

            logger.info("commits associated to classes: {}", commits.size());

            labelBuggyClasses(allTickets, commits, allClasses, releases);

            count = 0;

            for (Class cls : allClasses) {
                if (cls.isBuggy()) {
                    count++;
                }
            }

            logger.info("All classes: {}", allClasses.size());
            logger.info("Buggy classes: {}", count);

            ComputeMetrics cm = new ComputeMetrics();
            cm.computeMetrics(allClasses, projName);


        } catch (GitAPIException e) {
            e.printStackTrace();
        } catch (ParseException | ExecutionException e) {
            throw new ExecutionException(e);
        }

        return allClasses;
    }

    private static void labelClasses(String className, Ticket ticket, List<Class> allClasses) {

        if (ticket.getFixVersion() == null)
            return;
        for (Class cls : allClasses) {
            if (cls.getName().equals(className) && cls.getRelease().getId() >= ticket.getInjectedVersion().getId() && cls.getRelease().getId() < ticket.getFixVersion().getId()) {
                cls.setBuggy(true);
            }

        }

    }

    private static void labelBuggyClasses(List<Ticket> tickets, List<RevCommit> commits, List<Class> allClasses, List<Release> releases) throws ExecutionException {


        List<Ticket> ticketsWithAV = TicketRetriever.getTicketsWithAV(tickets);   // these are all tickets with fv != iv, so the tickets for which it is possible to detect buggy classes
        logger.info("Tickets with AV: {} - {}", ticketsWithAV.size(), releases.size());

        // we need to retrieve all the commits associated to all the tickets with AV
        for (Ticket ticket : ticketsWithAV) {

            List<RevCommit> commitsAssociatedToTicket = CommitUtils.filterCommitsAssociatedToTicket(ticket, commits);
            logger.info("Ticket {} has {} associated commits", ticket.getKey(), commitsAssociatedToTicket.size());

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
    public static List<String> getModifiedClasses(RevCommit commit) throws ExecutionException {

        List<String> modifiedClasses = new ArrayList<>();

        try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {  // we're not interested in the output
            ObjectReader reader = repository.newObjectReader();

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            ObjectId tree = commit.getTree();
            treeParser.reset(reader, tree);

            CanonicalTreeParser parentTreeParser = new CanonicalTreeParser();
            ObjectId parentTree = commit.getParent(0).getTree();
            parentTreeParser.reset(reader, parentTree);

            diffFormatter.setRepository(repository);

            List<DiffEntry> diffEntries = diffFormatter.scan(parentTree, tree);

            for (DiffEntry entry : diffEntries) {
                if (entry.getNewPath().contains(".java") && !entry.getNewPath().contains("/test/"))
                    modifiedClasses.add(entry.getNewPath());
            }

        } catch (ArrayIndexOutOfBoundsException ignored) {
            // this commit has no parents
        } catch (IOException e) {
            throw new ExecutionException(e);
        }

        return modifiedClasses;
    }

    /*
     *  This method, given all classes and all commits, retrieves, for each class c, its associated commits,
     *  so the list of all those commits that modify, add, remove, delete or rename c.
     */
    private static void retrieveCommitsForClasses(List<RevCommit> commits, List<Class> allClasses) throws ExecutionException {
        for (RevCommit commit : commits) {
            List<String> modifiedClasses = getModifiedClasses(commit);
            for (String modifiedClass : modifiedClasses) {
                for (Class cls : allClasses) {
                    addCommitToClass(commit, cls, modifiedClass);
                }
            }
        }
    }

    private static void addCommitToClass(RevCommit commit, Class cls, String modifiedClass) {
        if ((!cls.getAssociatedCommits().contains(commit)) && (modifiedClass.equals(cls.getName())) && (getReleaseFromCommit(commit).getId() == cls.getRelease().getId())) {
            cls.getAssociatedCommits().add(commit);
        }
    }


    private static List<Class> getClassesFromReleaseCommit(Release release) throws IOException {

        List<Class> classes = new ArrayList<>();

        Map<String, String> classesDescription = getClassesFromCommit(release.getLastCommit());

        for (Entry<String, String> entry : classesDescription.entrySet()) {
            classes.add(new Class(entry.getKey(), entry.getValue(), release));
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

    private static Map<String, String> getClassesFromCommit(RevCommit commit) throws IOException {

        Map<String, String> classDescription = new HashMap<>();

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

    private static Release getReleaseFromCommit(RevCommit commit) {
        LocalDateTime commitDate = commit.getAuthorIdent().getWhen().toInstant().atZone(commit.getAuthorIdent().getZoneId()).toLocalDateTime();
        return TicketRetriever.getRelease(commitDate);
    }
}

