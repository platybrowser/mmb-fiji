package de.embl.cba.platynereis.platybrowser;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.io.File;


@Plugin(type = Command.class, menuPath = "Plugins>EMBL>Explore>Platynereis Atlas" )
public class PlatyBrowserCommand implements Command
{
	@Parameter ( label = "Platynereis Atlas Data Folder", style = "directory")
	public File dataFolder;

	@Override
	public void run()
	{
		new PlatyBrowser( dataFolder );
	}
}