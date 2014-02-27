package com.contribute;

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
     * The database connection string used in the SQL*Plus login argument (e.g.
     * localhost:1521/orcl.company.com).
     *
     * @parameter expression="${import.connectionString}"
     * @required
     */
    private String connectionString;
    /**
     * The database username used to login in SQL*Plus.
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
     * The command to start the SQL*Plus executable. The default value is
     * 'sqlplus'.
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
     * The relative path to the folder containing the application export
     * file(s).
     *
     * @parameter expression="${import.appExportLocation}"
     * @required
     */
    private String appExportLocation;
    /**
     * The APEX workspace in which you want to import the application. Omit this
     * parameter to import the application in the original workspace.
     *
     * @parameter expression="${import.workspaceName}"
     * @required
     */
    private String workspaceName;
    /**
     * The target APEX application ID. Omit this parameter to import the
     * application with the original application ID.
     *
     * @parameter expression="${import.appId}"
     */
    private String appId;
    /**
     * Set the application alias.
     *
     * @parameter expression="${import.appAlias}"
     */
    private String appAlias;
    /**
     * Set the application name.
     *
     * @parameter expression="${import.appName}"
     */
    private String appName;
    /**
     * Set the application parsing schema.
     *
     * @parameter expression="${import.appParsingSchema}"
     */
    private String appParsingSchema;
    /**
     * Set the image prefix of the application.
     *
     * @parameter expression="${import.appImagePrefix}"
     */
    private String appImagePrefix;
    /**
     * Set the proxy server attributes of the application to be imported.
     *
     * @parameter expression="${import.appProxy}"
     */
    private String appProxy;
    /**
     * The offset value for the application import.
     *
     * @parameter expression="${import.appOffset}"
     */
    private String appOffset;
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
                throw new MojoExecutionException("SQL*Plus process returned exit value 1");
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
        script = "declare\n"
                + "  l_workspace_id apex_workspaces.workspace_id%type;\n"
                + "begin\n";
        if (workspaceName != null) {
            script = script + ""
                    + "  select workspace_id\n"
                    + "  into l_workspace_id\n"
                    + "  from apex_workspaces\n"
                    + "  where upper(workspace) = upper('" + workspaceName + "');\n\n"
                    + "  apex_application_install.set_workspace_id(l_workspace_id);\n";
        }
        if (appId != null) {
            script = script + ""
                    + "  apex_application_install.set_application_id(" + appId + ");\n";
        }
        if (appAlias != null) {
            script = script + ""
                    + "  apex_application_install.set_application_alias('" + appAlias + "');\n";
        }
        if (appName != null) {
            script = script + ""
                    + "  apex_application_install.set_application_name('" + appName + "');\n";
        }
        if (appParsingSchema != null) {
            script = script + ""
                    + "  apex_application_install.set_schema('" + appParsingSchema + "');\n";
        }
        if (appImagePrefix != null) {
            script = script + ""
                    + "  apex_application_install.set_image_prefix('" + appImagePrefix + "');\n";
        }
        if (appProxy != null) {
            script = script + ""
                    + "  apex_application_install.set_proxy('" + appProxy + "');\n";
        }
        if (appOffset != null) {
            script = script + ""
                    + "  apex_application_install.set_offset(" + appOffset + ");\n";
        } else {
            script = script + ""
                    + "  apex_application_install.generate_offset;\n";
        }
        script = script + ""
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
        for (File appExportFile : appExportFiles) {
            bufferedWriter.write("@" + appExportFile.getAbsolutePath());
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
