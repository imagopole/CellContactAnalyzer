package fr.pasteur.trackmate;

import fiji.plugin.trackmate.TrackMatePlugIn_;
import ij.CompositeImage;
import ij.ImageJ;
import ij.ImagePlus;


public class CellContactTest
{
	public static void main( final String[] args )
	{
		ImageJ.main( args );
		final String file = "/Users/tinevez/Projects/AMikhailova/Data/150823 SiT+SAg_1.tif";
		final CompositeImage imp = new CompositeImage( new ImagePlus( file ) );
		imp.setOpenAsHyperStack( true );
		imp.show();

		new TrackMatePlugIn_().run( null );
	}
}
