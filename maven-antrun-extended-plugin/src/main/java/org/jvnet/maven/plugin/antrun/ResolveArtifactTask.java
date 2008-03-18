package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Copy;

import java.io.File;

/**
 * Ant task that resolves an artifact through Maven.
 *
 * @author Paul Sterk
 * @author Kohsuke Kawaguchi
 */
public class ResolveArtifactTask extends Task {
    // properties of artifact.
    private String groupId,artifactId,version,type,classifier;

    private String property;
    private File tofile,todir;

    public void setProperty(String property) {
        this.property = property;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    /**
     * The file name to copy the artifact to.
     * Optional. The name is 
     */
    public void setTofile(File target) {
        this.tofile = target;
    }

    /**
     * The directory name to copy the artifact to.
     * Optional.
     */
    public void setTodir(File target) {
        this.todir = target;
    }

    public void execute() throws BuildException {
        log("Starting execute", Project.MSG_DEBUG);
        try {
            MavenComponentBag w = MavenComponentBag.get();
            Artifact a = w.createArtifactWithClassifier(groupId, artifactId, version, type, classifier);
            w.resolveArtifact(a);
            // Property attribute is optional. Check for null value
            if (property != null) {                
                getProject().setProperty(property, a.getFile().getAbsolutePath());
            }

            if(tofile!=null) {
                log("Copying "+a.getFile()+" to "+tofile);
                Copy cp = new Copy();
                cp.setProject(getProject());
                cp.setFile(a.getFile());
                cp.setTofile(tofile);
                cp.execute();
            }

            if(todir!=null) {
                log("Copying "+a.getFile()+" to "+todir);
                Copy cp = new Copy();
                cp.setProject(getProject());
                cp.setFile(a.getFile());
                cp.setTodir(todir);
                cp.execute();
            }
        } catch (Throwable ex) {
            log("Problem resolving artifact: "+ex.getMessage(), Project.MSG_ERR);
            throw new BuildException(ex);
        }
        log("Exiting execute", Project.MSG_DEBUG);
    }
}