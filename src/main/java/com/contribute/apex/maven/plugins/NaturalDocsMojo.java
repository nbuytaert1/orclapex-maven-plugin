package com.contribute.apex.maven.plugins;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Run Natural Docs to generate your project's technical documentation.
 * http://www.naturaldocs.org/
 */
@Mojo(name = "run-natural-docs",
defaultPhase = LifecyclePhase.COMPILE)
public class NaturalDocsMojo extends AbstractMojo {

    /**
     * The path to the folder containing the Natural Docs executable.
     */
    @Parameter(property = "run-natural-docs.naturalDocsHome",
    required = true)
    private File naturalDocsHome;
    /**
     * Natural Docs will build the documentation from the files in these
     * directories and all its subdirectories. It is possible to specify
     * multiple directories.
     */
    @Parameter(property = "run-natural-docs.inputSourceDirectories",
    required = true)
    private List<File> inputSourceDirectories;
    /**
     * The output format. The supported formats are HTML and FramedHTML.
     */
    @Parameter(property = "run-natural-docs.outputFormat",
    required = true)
    private String outputFormat;
    /**
     * The folder in which Natural Docs will generate the technical
     * documentation.
     */
    @Parameter(property = "run-natural-docs.outputDirectory",
    required = true)
    private File outputDirectory;
    /**
     * Natural Docs needs a place to store the project's specific configuration
     * and data files.
     */
    @Parameter(property = "run-natural-docs.projectDirectory",
    required = true)
    private File projectDirectory;
    /**
     * Excludes a subdirectory from being scanned. You can specify it multiple
     * times to exclude multiple subdirectories.
     */
    @Parameter(property = "run-natural-docs.excludedSubdirectories")
    private List<File> excludedSubdirectories;
    /**
     * Rebuilds everything from scratch. All source files will be rescanned and
     * all output files will be rebuilt.
     */
    @Parameter(property = "run-natural-docs.rebuild")
    private boolean rebuild;
    /**
     * Rebuilds all output files from scratch.
     */
    @Parameter(property = "run-natural-docs.rebuildOutput")
    private boolean rebuildOutput;
    /**
     * Tells Natural Docs to only include what you explicitly document in the
     * output, and not to find undocumented classes, functions, and variables.
     */
    @Parameter(property = "run-natural-docs.documentedOnly")
    private boolean documentedOnly;
    /**
     * Tells Natural Docs to only use the file name for its menu and page
     * titles.
     */
    @Parameter(property = "run-natural-docs.onlyFileTitles")
    private boolean onlyFileTitles;
    /**
     * Tells Natural Docs to not automatically create group topics if you don't
     * add them yourself.
     */
    @Parameter(property = "run-natural-docs.noAutoGroup")
    private boolean noAutoGroup;
    /**
     * Suppresses all non-error output.
     */
    @Parameter(property = "run-natural-docs.quiet")
    private boolean quiet;
    // unsupported optional parameters:
    //   --images / --style / --tab-length / --highlight

    /**
     * The method called by Maven when the 'run-natural-docs' goal gets
     * executed.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProcessBuilder processBuilder;
        Process process;
        List<String> commandLineArguments = new ArrayList<String>();
        String output;

        String perlExecutable = "perl";
        String NaturalDocsExecutable = "NaturalDocs";
        String commandToExecute = "";

        validateParameters();

        if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
            NaturalDocsExecutable += ".bat";
        }

        commandLineArguments.add(perlExecutable);
        commandLineArguments.add(NaturalDocsExecutable);

        // required parameters
        for (int i = 0; i < inputSourceDirectories.size(); i++) {
            commandLineArguments.add("-i");
            commandLineArguments.add(inputSourceDirectories.get(i).getPath());
        }
        commandLineArguments.add("-o");
        commandLineArguments.add(outputFormat);
        commandLineArguments.add(outputDirectory.getPath());
        commandLineArguments.add("-p");
        commandLineArguments.add(projectDirectory.getPath());

        // optional parameters
        if (excludedSubdirectories != null) {
            for (int i = 0; i < excludedSubdirectories.size(); i++) {
                commandLineArguments.add("-xi");
                commandLineArguments.add(excludedSubdirectories.get(i).getPath());
            }
        }
        if (rebuild) {
            commandLineArguments.add("-r");
        }
        if (rebuildOutput) {
            commandLineArguments.add("-ro");
        }
        if (documentedOnly) {
            commandLineArguments.add("-do");
        }
        if (onlyFileTitles) {
            commandLineArguments.add("-oft");
        }
        if (noAutoGroup) {
            commandLineArguments.add("-nag");
        }
        if (quiet) {
            commandLineArguments.add("-q");
        }

        for (int i = 0; i < commandLineArguments.size(); i++) {
            commandToExecute += commandLineArguments.get(i).toString() + " ";
        }
        getLog().debug("Executing Natural Docs: " + commandToExecute);

        processBuilder = new ProcessBuilder(commandLineArguments);
        processBuilder.directory(naturalDocsHome);
        processBuilder.redirectErrorStream(true);

        try {
            process = processBuilder.start();

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((output = stdInput.readLine()) != null) {
                getLog().info(output);
            }
            stdInput.close();

            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((output = stdError.readLine()) != null) {
                getLog().info(output);
            }
            stdError.close();
        } catch (IOException ex) {
            throw new MojoExecutionException("An unexpected error occurred while executing Natural Docs", ex);
        }
    }

    /**
     * Validate the entered configuration parameters.
     */
    private void validateParameters() throws MojoExecutionException {
        if (!naturalDocsHome.isDirectory()) {
            throw new MojoExecutionException("The specified naturalDocsHome is not a folder: " + naturalDocsHome.getAbsolutePath());
        }
        for (int i = 0; i < inputSourceDirectories.size(); i++) {
            if (!inputSourceDirectories.get(i).isDirectory()) {
                throw new MojoExecutionException("The specified inputSourceDirectory is not a folder: " + inputSourceDirectories.get(i).getAbsolutePath());
            }
        }
        if (!(outputFormat.toLowerCase().equals("html") || outputFormat.toLowerCase().equals("framedhtml"))) {
            throw new MojoExecutionException("Unknown output format: " + outputFormat + ". Valid formats are HTML and FramedHTML.");
        }
        if (!outputDirectory.isDirectory()) {
            throw new MojoExecutionException("The specified outputDirectory is not a folder: " + outputDirectory.getAbsolutePath());
        }
        if (!projectDirectory.isDirectory()) {
            throw new MojoExecutionException("The specified projectDirectory is not a folder: " + projectDirectory.getAbsolutePath());
        }
        for (int i = 0; i < excludedSubdirectories.size(); i++) {
            if (!excludedSubdirectories.get(i).isDirectory()) {
                throw new MojoExecutionException("The specified excludedSubdirectory is not a folder: " + excludedSubdirectories.get(i).getAbsolutePath());
            }
        }
    }
}