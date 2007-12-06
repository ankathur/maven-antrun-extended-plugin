package org.jvnet.maven.plugin.antrun;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Graph of dependencies among Maven artifacts.
 * @author Kohsuke Kawaguchi
 */
public final class DependencyGraph {
    private final MavenComponentBag bag = MavenComponentBag.get();

    private final Node root;
    /**
     * All {@link Node}s keyed by "groupId:artifactId:classifier"
     */
    private final Map<String,Node> nodes = new HashMap<String, Node>();

    /**
     * Creates a full dependency graph with the given artifact at the top.
     */
    public DependencyGraph(Artifact root) throws ProjectBuildingException {
        this.root = toNode(root);
    }

    public DependencyGraph(MavenProject root) throws ProjectBuildingException {
        this.root = toNode(root);
    }

    /**
     * Gets the root Node.
     */
    public Node getRoot() {
        return root;
    }

    /**
     * Gets the associated {@link Node}. If none exists, it will be created.
     */
    private Node toNode(Artifact a) throws ProjectBuildingException {
        String id = a.getGroupId()+':'+a.getArtifactId()+':'+a.getClassifier();

        Node n = nodes.get(id);
        if(n==null) {
            n = new Node(a);
            nodes.put(id, n);
        }
        return n;
    }

    /**
     * Gets the associated {@link Node}. If none exists, it will be created.
     */
    private Node toNode(MavenProject p) throws ProjectBuildingException {
        String id = p.getGroupId()+':'+p.getArtifactId()+":null";

        Node n = nodes.get(id);
        if(n==null) {
            n = new Node(p);
            nodes.put(id, n);
        }
        return n;
    }

    /**
     * Node, which represents an artifact.
     */
    public final class Node {
        private final MavenProject pom;
        private /*final*/ File artifactFile;

        private final List<Edge> forward = new ArrayList<Edge>();
        private final List<Edge> backward = new ArrayList<Edge>();

        private Node(Artifact artifact) throws ProjectBuildingException {
            if(artifact.getScope().equals("system"))
                // system scoped artifacts don't have POM, so the attempt to load it will fail.
                pom = null;
            else {
                pom = bag.mavenProjectBuilder.buildFromRepository(artifact,
                        bag.project.getRemoteArtifactRepositories(),
                        bag.localRepository);
                loadDependencies();
            }
            checkArtifact(artifact);
        }

        private void checkArtifact(Artifact artifact) {
            artifactFile = artifact.getFile();
            if(artifactFile==null)
                throw new IllegalStateException("Artifact is not resolved yet: "+artifact);
        }

        private Node(MavenProject pom) throws ProjectBuildingException {
            this.pom = pom;
            checkArtifact(pom.getArtifact());
            loadDependencies();
        }

        private void loadDependencies() throws ProjectBuildingException {
            for( Dependency d : (List<Dependency>)pom.getDependencies() ) {
                Artifact a = bag.factory.createArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion(), d.getScope(), d.getType());
                new Edge(this,toNode(a),d.getScope(),d.isOptional());
            }
        }

        /**
         * Gets the parsed POM for this artifact.
         *
         * @return null
         *      if POM is not available for this module.
         *      That can happen for example for system-scoped artifacts.
         */
        public MavenProject getProject() throws ProjectBuildingException {
            return pom;
        }

        /**
         * Gets the artifact file, like a jar.
         *
         * @return
         *      never null, always resolved.
         */
        public File getArtifactFile() {
            return artifactFile;
        }

        /**
         * Gets the forward dependency edges (modules that this module depends on.)
         */
        public List<Edge> getForward() {
            return forward;
        }

        /**
         * Gets the backward dependency edges (modules that depend on this module.)
         */
        public List<Edge> getBackward() {
            return backward;
        }
    }

    public final class Edge {
        /**
         * The module that depends on another.
         */
        public final Node src;
        /**
         * The module that is being dependent by another.
         */
        public final Node dst;

        /**
         * Dependency scope. Stuff like "compile", "runtime", etc.
         */
        public final String scope;

        /**
         * True if this dependency is optional.
         */
        public final boolean optional;

        public Edge(Node src, Node dst, String scope, boolean optional) {
            this.src = src;
            this.dst = dst;
            this.scope = scope;
            this.optional = optional;
            src.forward.add(this);
            dst.backward.add(this);
        }
    }
}
