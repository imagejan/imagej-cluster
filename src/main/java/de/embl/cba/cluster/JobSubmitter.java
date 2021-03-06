package de.embl.cba.cluster;

import de.embl.cba.cluster.job.JobScript;
import de.embl.cba.cluster.job.SimpleLinuxJobScript;
import de.embl.cba.cluster.job.SlurmJobScript;
import de.embl.cba.cluster.ssh.SSHConnector;
import de.embl.cba.cluster.ssh.SSHConnectorConfig;

import java.util.ArrayList;
import java.util.Map;

import static de.embl.cba.cluster.job.SlurmJobScript.DO_NOT_ECHO;

public class JobSubmitter
{
    public static final String EXECUTION_SYSTEM_EMBL_SLURM = "EMBL Slurm Cluster";
    public static final String EXECUTION_SYSTEM_MAC_OS_LOCALHOST = "MacOS localhost";

    public static final String RUN_IJ_MACRO_OPTIONS = " --mem=MEMORY_MB --ij2 --allow-multiple --headless -eval";
    public static final String RUN_IJ_COMMAND_OPTIONS = " --mem=MEMORY_MB --ij2 --allow-multiple --headless --run";

    public static final String IMAGEJ_EXECTUABLE_ALMF_CLUSTER_XVFB = "xvfb-run -a -e XVFB_ERR_PATH /g/almf/software/Fiji.app/ImageJ-linux64 --allow-multiple --mem=MEMORY_MB --run";

    public static final String IMAGEJ_EXECTUABLE_ALMF_CLUSTER_HEADLESS = "/g/almf/software/Fiji.app/ImageJ-linux64" + RUN_IJ_COMMAND_OPTIONS;
    public static final String IMAGEJ_EXECTUABLE_ALMF_CLUSTER_HEADLESS_MACRO = "/g/almf/software/Fiji.app/ImageJ-linux64 " + RUN_IJ_MACRO_OPTIONS;

    public static final String IMAGEJ_EXECUTABLE_CBA_CLUSTER_XVFB = "xvfb-run -a /g/cba/software/Fiji.app/ImageJ-linux64 --mem=MEMORY_MB --allow-multiple --run";
    public static final String IMAGEJ_EXECUTABLE_MAC_OS = "/Applications/Fiji.app/Contents/MacOS/ImageJ-macosx --mem=MEMORY_MB --allow-multiple --run";

    private String executionSystem;
    private String remoteImageJExectuable;

    private String username;
    private String password;
    private String remoteJobDirectory;

    private ArrayList< String > commands;
    private SSHExecutorService sshExecutorService;

    public JobSubmitter( String executionSystem,
                         String remoteJobDirectory,
                         String remoteImageJExectuable,
                         String username,
                         String password )
    {
        this.executionSystem = executionSystem;
        this.remoteImageJExectuable = remoteImageJExectuable;
        this.username = username;
        this.password = password;
        this.remoteJobDirectory = remoteJobDirectory;

        commands = new ArrayList<>();
    }

    public void clearCommands()
    {
        commands.clear();
    }

    public void addIJCommandWithParameters( String command, Map<String, Object> parameters )
    {
        String commandAndParameters = Commands.createCommandAndParameterString( command, parameters );
        String ijBinaryAndCommandAndParameters = prependIJBinary( commandAndParameters );
        commands.add( ijBinaryAndCommandAndParameters );
    }

    public void addIJMacroExecution( String macroString )
    {
        String quotedMacroString = " \'" + macroString + "\'";
        String ijExecutableAndMacroString = prependIJBinary( quotedMacroString );
        System.out.println( ijExecutableAndMacroString );
        commands.add( ijExecutableAndMacroString );
    }

    public void addLinuxCommand( String command )
    {
        commands.add( command );
    }

    /**
     *
     *
     *
     * @param jobSettings
     * @return
     */
    public JobFuture submitJob( JobSettings jobSettings )
    {
        ArrayList< String > finalCommands = new ArrayList<>();

        if ( executionSystem.equals( EXECUTION_SYSTEM_EMBL_SLURM ) )
        {
            finalCommands.add( "echo $SLURM_JOB_ID" + DO_NOT_ECHO);
            finalCommands.add( "hostname" );
            finalCommands.add( "lscpu" );
            finalCommands.add( "free -m" );
            finalCommands.add( "START_TIME=$SECONDS"  + DO_NOT_ECHO );

            // finalCommands.add( "module load Java" );
            // finalCommands.add( "module load X11" ); // only necessary when the job cannot run headless

            finalCommands.add( "mkdir -p ~/.imagej" );
            finalCommands.add( "sleep 1s" );

            finalCommands.add( "cp /g/almf/software/Fiji.app/IJ_Prefs.txt ~/.imagej/" );
            finalCommands.add( "sleep 1s" );
        }

        for ( String command : commands )
        {
            String finalCommand = command.replace( "MEMORY_MB", "" + jobSettings.memoryPerJobInMegaByte + "M" );
            finalCommands.add( finalCommand );
        }

        if ( executionSystem.equals( EXECUTION_SYSTEM_EMBL_SLURM ) )
        {
            finalCommands.add( "ELAPSED_TIME_IN_SECONDS=$(($SECONDS - $START_TIME))" + DO_NOT_ECHO );
            finalCommands.add( "echo \"Time spent in seconds $ELAPSED_TIME_IN_SECONDS\"" + DO_NOT_ECHO );
        }

        JobScript jobScript = createJobScript( finalCommands, jobSettings );

        JobFuture future = submitJobScript( jobScript );

        return future;

    }

    private JobFuture submitJobScript( JobScript jobScript )
    {
        SSHExecutorService executorService = getSSHExecutorService();

        return executorService.submit( jobScript );
    }

    private SSHExecutorService getSSHExecutorService()
    {
        if ( sshExecutorService == null )
        {
            String hostname = "";
            String jobSubmissionType = "";

            if ( executionSystem.equals( EXECUTION_SYSTEM_EMBL_SLURM ) )
            {
                hostname = SSHConnectorConfig.EMBL_SLURM_HOST;
                jobSubmissionType = SSHExecutorService.SLURM_JOB;

            } else if ( executionSystem.equals( EXECUTION_SYSTEM_MAC_OS_LOCALHOST ) )
            {
                hostname = SSHConnectorConfig.LOCALHOST;
                jobSubmissionType = SSHExecutorService.LINUX_JOB;
            }

            SSHConnectorConfig sshConnectorConfig = new SSHConnectorConfig( username, password, hostname );
            SSHConnector sshConnector = new SSHConnector( sshConnectorConfig );

            sshExecutorService = new SSHExecutorService(
                    sshConnector,
                    remoteJobDirectory,
                    jobSubmissionType );
        }

        return sshExecutorService;
    }

    private JobScript createJobScript( ArrayList< String > completeCommands, JobSettings jobSettings  )
    {
        if ( executionSystem.equals( EXECUTION_SYSTEM_EMBL_SLURM ) )
        {
            JobScript jobScript = new SlurmJobScript( completeCommands, jobSettings );
            return jobScript;
        }
        else if ( executionSystem.equals( EXECUTION_SYSTEM_MAC_OS_LOCALHOST ) )
        {
            JobScript jobScript = new SimpleLinuxJobScript( completeCommands );
            return jobScript;
        }
        else
        {
            return null;
        }
    }

    private String prependIJBinary( String command )
    {
        return "time " + remoteImageJExectuable + " " + command ;
    }
}
