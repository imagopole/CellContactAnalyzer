package fr.pasteur;

import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CHANNEL_1;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CHANNEL_2;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CONTACT_SIZE;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_SIGMA_FILTER;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_THRESHOLD_1;
import fiji.plugin.trackmate.gui.GuiUtils;
import fiji.plugin.trackmate.util.TMUtils;
import ij.CompositeImage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

@SuppressWarnings( "deprecation" )
public class CCCT_< T extends RealType< T > & NativeType< T >> implements PlugIn, CCCTProcessor, MultiThreaded
{

	public static final String PLUGIN_VERSION = "0.0.2-SNAPSHOT";

	public static final String PLUGIN_NAME = "CCCT";

	ImagePlus imp;

	private CCCTGui gui;

	private int numThreads;

	public CCCT_()
	{
		setNumThreads();
	}

	@Override
	public void run( final String arg )
	{
		if ( null != arg && arg.length() > 0 )
		{
			imp = new CompositeImage( new ImagePlus( arg ) );
		}
		else
		{
			imp = WindowManager.getCurrentImage();
		}
		if ( null == imp )
		{
			IJ.error( PLUGIN_NAME + " v" + PLUGIN_VERSION, "Please open a multi-channel image first." );
			return;
		}
		if ( !imp.isVisible() )
		{
			imp.show();
		}

		gui = new CCCTGui( imp, this );
		GuiUtils.positionWindow( gui, imp.getWindow() );
		gui.setVisible( true );
	}

	@Override
	public void process( final boolean showContactImage, final boolean contactMask, final boolean contactLabels, final boolean trackLabels )
	{
		@SuppressWarnings( "unchecked" )
		final ImgPlus< T > img = TMUtils.rawWraps( imp );


		final Map< String, Object > settings = gui.getSettings();

		// In ImgLib2, dimensions are 0-based.
		final int channel1 = ( Integer ) settings.get( KEY_CHANNEL_1 ) - 1;
		final int channel2 = ( Integer ) settings.get( KEY_CHANNEL_2 ) - 1;
		final int contactSize = ( Integer ) settings.get( KEY_CONTACT_SIZE );
		final double sigma = ( Double ) settings.get( KEY_SIGMA_FILTER );
		final double thresholdC1 = ( Double ) settings.get( KEY_THRESHOLD_1 );
		final double thresholdC2 = ( Double ) settings.get( KEY_THRESHOLD_1 );

		final RandomAccessibleInterval< T > im1;
		final RandomAccessibleInterval< T > im2;

		final int cDim = TMUtils.findCAxisIndex( img );
		if ( cDim < 0 )
		{
			im1 = img;
			im2 = img;
		}
		else
		{
			im1 = Views.hyperSlice( img, cDim, channel1 );
			im2 = Views.hyperSlice( img, cDim, channel2 );
		}

		final PlanarImgFactory< T > factory = new PlanarImgFactory< T >();
		final Img< T > out = factory.create( im1, Util.getTypeFromInterval( img ) );

		int timeDim = TMUtils.findTAxisIndex( img );
		if ( timeDim >= 0 )
		{
			if ( cDim >= 0 && timeDim > cDim )
			{
				timeDim--;
			}
		}
		final int td = timeDim;

		final int nFrames = imp.getNFrames();
		final AtomicInteger ai = new AtomicInteger( 0 );
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			threads[ i ] = new Thread( "CCCT Thread" )
			{
				@Override
				public void run()
				{
					for ( int frame = ai.getAndIncrement(); frame < nFrames; frame = ai.getAndIncrement() )
					{
						final ContactImgGenerator< T > algo = new ContactImgGenerator< T >(
								Views.hyperSlice( im1, td, frame ),
								Views.hyperSlice( im2, td, frame ),
								Views.hyperSlice( out, out.numDimensions() - 1, frame ),
								thresholdC1, thresholdC2, contactSize, sigma );

						if ( !algo.checkInput() || !algo.process() )
						{
							System.err.println( algo.getErrorMessage() );
							return;
						}

						gui.setProgress( ( 1.0 + ai.get() ) / nFrames );
					}
				}
			};
		}
		SimpleMultiThreading.startAndJoin( threads );

		if ( showContactImage )
		{
			final ImagePlus contacts = ImageJFunctions.wrap( out, "Contacts" );
			contacts.setCalibration( imp.getCalibration() );
			contacts.setDimensions( 1, imp.getNSlices(), imp.getNFrames() );
			contacts.show();
		}
	}

	public static < T extends RealType< T > & NativeType< T >> void main( final String[] args )
	{
		ImageJ.main( args );
		final File file = new File( "/Users/tinevez/Projects/AMikhailova/Data/150823 SiT+SAg_1.tif" );
		new CCCT_< T >().run( file.getAbsolutePath() );
	}

	@Override
	public void setNumThreads()
	{
		this.numThreads = Runtime.getRuntime().availableProcessors();
	}

	@Override
	public void setNumThreads( final int numThreads )
	{
		this.numThreads = numThreads;
	}

	@Override
	public int getNumThreads()
	{
		return numThreads;
	}
}
