package de.embl.cba.cluster;

import de.embl.cba.cluster.logger.Logger;

import java.io.IOException;

public abstract class SlurmJobStatus
{
    public static final String COMPLETED = "COMPLETED";

    public static final String CANCELLED = "CANCELLED";
    public static final String RUNNING = "RUNNING";
    public static final String PENDING = "PENDING";


    public static void monitorJobStatusAndShowOutAndErrWhenDone( JobFuture future ) throws IOException
    {
        for ( ; ; )
        {
            String status = logJobStatus( future );

            if ( status.equals( COMPLETED ) )
            {
                logJobError( future );
                logJobOutput( future );
                break;
            }
        }
    }


    private static void logJobOutput( JobFuture future ) throws IOException
    {
        Logger.log( future.getOutput() );
    }

    private static void logJobError( JobFuture future ) throws IOException
    {
        Logger.log( future.getError() );
    }

    private static String logJobStatus( JobFuture future )
    {
        String status = future.getStatus();
        Logger.log( "Status of job " + future.jobID + " is " + status + "\n" );
        return status;
    }
}
