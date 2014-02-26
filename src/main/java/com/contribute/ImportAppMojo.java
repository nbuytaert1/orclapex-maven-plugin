package com.contribute;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Import an APEX application.
 *
 * @goal import
 *
 * @phase compile
 */
public class ImportAppMojo extends AbstractMojo {

    /**
     * The database connection string used in the SQL*Plus login argument.
     *
     * @parameter expression="${import.connectionString}"
     * @required
     */
    private String connectionString;
    /**
     * The database username.
     *
     * @parameter expression="${import.username}"
     * @required
     */
    private String username;
    /**
     * The database user's password.
     *
     * @parameter expression="${import.password}"
     * @required
     */
    private String password;
    /**
     * The command to start the SQL*Plus executable.
     *
     * @parameter expression="${import.sqlplusCmd}" default-value="sqlplus"
     */
    private String sqlplusCmd;
    /**
     * The ORACLE_HOME system environment variable.
     *
     * @parameter expression="${import.oracleHome}"
     */
    private String oracleHome;
    /**
     * The TNS_ADMIN environment variable to specify the location of the
     * tnsnames.ora file.
     *
     * @parameter expression="${import.tnsAdmin}"
     */
    private String tnsAdmin;
    /**
     * Environment variable to specify the path used to search for libraries on
     * UNIX and Linux systems.
     *
     * @parameter expression="${import.libraryPath}"
     */
    private String libraryPath;
    /**
     * The relative path to the folder containing the application export files.
     *
     * @parameter expression="${import.appExportLocation}"
     * @required
     */
    private String appExportLocation;
    /**
     * The target APEX workspace.
     *
     * @parameter expression="${import.workspaceName}"
     * @required
     */
    private String workspaceName;
    /**
     * The target APEX application ID.
     *
     * @parameter expression="${import.appId}"
     * @required
     */
    private String appId;
    private final String sqlFileExtension = ".sql";

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            File scriptsToRunTmpFile = createScriptsToRunTmpFile();
            String buffer;

            getLog().debug("Executing SQL*Plus: " + sqlplusCmd + " -L " + getSqlPlusLoginArgument() + " @" + scriptsToRunTmpFile.getAbsolutePath());

            ProcessBuilder processBuilder = new ProcessBuilder(sqlplusCmd, "-L", getSqlPlusLoginArgument(), "@" + scriptsToRunTmpFile.getAbsolutePath());
            Map environmentVariables = processBuilder.environment();

            // http://docs.oracle.com/cd/B28359_01/server.111/b31189/ch2.htm
            if (oracleHome != null) {
                environmentVariables.put("ORACLE_HOME", oracleHome);
            }
            if (tnsAdmin != null) {
                environmentVariables.put("TNS_ADMIN", tnsAdmin);
            }
            if (libraryPath != null) {
                environmentVariables.put("LD_LIBRARY_PATH", libraryPath);
                environmentVariables.put("DYLD_LIBRARY_PATH", libraryPath);
                environmentVariables.put("LIBPATH", libraryPath);
                environmentVariables.put("SHLIB_PATH", libraryPath);
            }

            getLog().debug("Printing all environment variables:");
            for (Object key : environmentVariables.keySet()) {
                getLog().debug("  " + key.toString() + "=" + environmentVariables.get(key));
            }

            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((buffer = stdInput.readLine()) != null) {
                System.out.println(buffer);
            }
            stdInput.close();

            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((buffer = stdError.readLine()) != null) {
                System.out.println(buffer);
            }
            stdError.close();

            getLog().debug("Process exit value: " + process.exitValue());
            if (process.exitValue() == 1) {
                throw new MojoExecutionException("SQL*Plus process exited with code 1");
            }
        } catch (IOException ex) {
            throw new MojoExecutionException("An error occurred while executing SQL*Plus", ex);
        }
    }

    public String getSqlPlusLoginArgument() {
        return username + "/" + password + "@" + "\"" + connectionString + "\"";
    }

    public File createSetApexEnvTmpFile() throws IOException {
        File setApexEnvTmpFile;
        String script;

        setApexEnvTmpFile = File.createTempFile("setApexEnv", sqlFileExtension);
        setApexEnvTmpFile.deleteOnExit();

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(setApexEnvTmpFile));

        // http://docs.oracle.com/cd/E37097_01/doc/doc.42/e35127/apex_app_inst.htm#AEAPI530
        // TODO: support more import parameters
        script = "declare\n"
                + "  l_workspace_id apex_workspaces.workspace_id%type;\n"
                + "begin\n"
                + "  select workspace_id\n"
                + "  into l_workspace_id\n"
                + "  from apex_workspaces\n"
                + "  where upper(workspace) = upper('" + workspaceName + "');\n"
                + "\n"
                + "  apex_application_install.set_workspace_id(l_workspace_id);\n"
                + "  apex_application_install.set_application_id(" + appId + ");\n"
                + "  apex_application_install.generate_offset;\n"
                + "end;\n"
                + "/";
        bufferedWriter.write(script);
        bufferedWriter.close();

        return setApexEnvTmpFile;
    }

    public File createScriptsToRunTmpFile() throws IOException {
        File scriptsToRunTmpFile;
        File setApexEnvTmpFile = createSetApexEnvTmpFile();
        File[] appExportFiles = getAppExportFiles();

        scriptsToRunTmpFile = File.createTempFile("scriptsToRun", sqlFileExtension);
        scriptsToRunTmpFile.deleteOnExit();

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(scriptsToRunTmpFile));
        bufferedWriter.write("@" + setApexEnvTmpFile.getAbsolutePath());
        bufferedWriter.newLine();
        for (int i = 0; i < appExportFiles.length; i++) {
            bufferedWriter.write("@" + appExportFiles[i].getAbsolutePath());
            bufferedWriter.newLine();
        }
        bufferedWriter.write("exit;");
        bufferedWriter.close();

        return scriptsToRunTmpFile;
    }

    public File[] getAppExportFiles() {
        // TODO: validate whether folder exists and is not empty.
        File file = new File(appExportLocation);
        File[] fileList = file.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (file.getName().toLowerCase().endsWith(sqlFileExtension)) {
                    return true;
                }
                return false;
            }
        });

        return fileList;
    }
}