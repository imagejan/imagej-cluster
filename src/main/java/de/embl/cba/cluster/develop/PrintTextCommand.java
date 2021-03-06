package de.embl.cba.cluster.develop;

import de.embl.cba.cluster.Commands;
import ij.CompositeImage;
import net.imagej.legacy.IJ1Helper;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import net.imagej.legacy.LegacyService;

import java.awt.*;
import java.awt.image.IndexColorModel;

@Plugin(type = Command.class, menuPath = "Plugins>Sandbox>Print text")
public class PrintTextCommand implements Command
{
    public static final String PLUGIN_NAME = "Print text";

    @Parameter
    private LegacyService legacyService;

    @Parameter
    public LogService logService;

    @Parameter( label = "Some text" )
    public String inputText;
    public static final String INPUT_TEXT = "inputText";

    @Parameter( label = "Quit after running" )
    public boolean quitAfterRun = false;
    public static final String QUIT_AFTER_RUN = "quitAfterRun";

    public void run()
    {
        logService.info( "# PrintTextCommand" );
        logService.info( "Text: " + inputText );

        final IJ1Helper helper = legacyService.getIJ1Helper();
        logService.info( "isRMIEnabled: " + helper.isRMIEnabled() );

        if ( quitAfterRun ) Commands.quitImageJ( logService );

        return;
    }

}