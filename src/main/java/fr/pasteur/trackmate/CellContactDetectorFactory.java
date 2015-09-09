package fr.pasteur.trackmate;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.io.IOUtils.readDoubleAttribute;
import static fiji.plugin.trackmate.io.IOUtils.readIntegerAttribute;
import static fiji.plugin.trackmate.io.IOUtils.writeAttribute;
import static fiji.plugin.trackmate.util.TMUtils.checkMapKeys;
import static fiji.plugin.trackmate.util.TMUtils.checkParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import net.imagej.ImgPlus;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

import org.jdom2.Element;
import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.detection.SpotDetectorFactory;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.util.TMUtils;

@SuppressWarnings( "deprecation" )
@Plugin( type = SpotDetectorFactory.class )
public class CellContactDetectorFactory< T extends RealType< T > & NativeType< T >> implements SpotDetectorFactory< T >
{

	private static final String PLUGIN_VERSION = "0.0.1";

	static final String INFO_TEXT = "<html>"
			+ "This detector detects cell/cell contacts for cells of two different "
			+ "species imaged in two different channels. "
			+ "<p> "
			+ "Version " + PLUGIN_VERSION
			+ "</html>";

	private static final String KEY = "CELL_CONTACT_DETECTOR";

	static final String NAME = "Cell/cell contact detector";

	/**
	 * The key identifying the parameter setting the fisrt target channel for
	 * contact detection in a multi-channel image. Channels are here 1-numbered,
	 * meaning that "1" is the first available channel (and all images have at
	 * least this channel). Expected valkues are {@link Integer}s greater than
	 * 1.
	 */
	public static final String KEY_CHANNEL_1 = "CHANNEL_1";

	/**
	 * The key identifying the parameter setting the fisrt target channel for
	 * contact detection in a multi-channel image. Channels are here 1-numbered,
	 * meaning that "1" is the first available channel (and all images have at
	 * least this channel). Expected valkues are {@link Integer}s greater than
	 * 1.
	 */
	public static final String KEY_CHANNEL_2 = "CHANNEL_2";

	public static final String KEY_CONTACT_SIZE = "CONTACT_SIZE";

	public static final String KEY_SIGMA_FILTER = "SIGMA_FILTER";

	public static final String KEY_THRESHOLD_1 = "THRESHOLD_C1";

	public static final String KEY_THRESHOLD_2 = "THRESHOLD_C2";

	private ImgPlus< T > img;

	private Map< String, Object > settings;

	private String errorMessage;

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null;
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public CellContactDetector< T > getDetector( final Interval interval, final int frame )
	{
		// In ImgLib2, dimensions are 0-based.
		final int channel1 = ( Integer ) settings.get( KEY_CHANNEL_1 ) - 1;
		final int channel2 = ( Integer ) settings.get( KEY_CHANNEL_2 ) - 1;
		final int contactSize = ( Integer ) settings.get( KEY_CONTACT_SIZE );
		final double sigma = ( Double ) settings.get( KEY_SIGMA_FILTER );
		final double[] calibration = TMUtils.getSpatialCalibration( img );

		RandomAccessibleInterval< T > im1;
		RandomAccessibleInterval< T > im2;

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

		int timeDim = TMUtils.findTAxisIndex( img );
		if ( timeDim >= 0 )
		{
			if ( cDim >= 0 && timeDim > cDim )
			{
				timeDim--;
			}
			im1 = Views.hyperSlice( im1, timeDim, frame );
			im2 = Views.hyperSlice( im2, timeDim, frame );
		}

		// In case we have a 1D image.
		if ( img.dimension( 0 ) < 2 )
		{ // Single column image, will be rotated internally.
			calibration[ 0 ] = calibration[ 1 ]; // It gets NaN otherwise
			calibration[ 1 ] = 1;
			im1 = Views.hyperSlice( im1, 0, 0 );
			im2 = Views.hyperSlice( im2, 0, 0 );
		}
		if ( img.dimension( 1 ) < 2 )
		{ // Single line image
			im1 = Views.hyperSlice( im1, 1, 0 );
			im2 = Views.hyperSlice( im2, 1, 0 );
		}

		final double threshold = ( Double ) settings.get( KEY_THRESHOLD );
		final double threshold_C1 = ( Double ) settings.get( KEY_THRESHOLD_1 );
		final double threshold_C2 = ( Double ) settings.get( KEY_THRESHOLD_2 );
		final CellContactDetector< T > detector = new CellContactDetector< T >( im1, im2, threshold_C1, threshold_C2, contactSize, sigma, threshold, calibration );
		detector.setNumThreads( 1 );
		return detector;
	}

	@Override
	public ConfigurationPanel getDetectorConfigurationPanel( final Settings settings, final Model model )
	{
		return new CellContactConfigurationPanel( settings.imp, model );
	}

	@Override
	public boolean setTarget( final ImgPlus< T > img, final Map< String, Object > settings )
	{
		this.img = img;
		this.settings = settings;
		return checkSettings( settings );
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public boolean marshall( final Map< String, Object > settings, final Element element )
	{
		final StringBuilder errorHolder = new StringBuilder();
		final List< String > intKeys = Arrays.asList( new String[] { KEY_CHANNEL_1, KEY_CHANNEL_2, KEY_CONTACT_SIZE, KEY_TARGET_CHANNEL } );
		boolean ok = true;
		for ( final String key : intKeys )
		{
			ok = ok && writeAttribute( settings, element, key, Integer.class, errorHolder );
		}

		final String[] doubleKeys = new String[] { KEY_SIGMA_FILTER, KEY_THRESHOLD, KEY_THRESHOLD_1, KEY_THRESHOLD_2 };
		for ( final String key : doubleKeys )
		{
			ok = ok && writeAttribute( settings, element, key, Double.class, errorHolder );

		}

		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

	@Override
	public boolean unmarshall( final Element element, final Map< String, Object > settings )
	{
		settings.clear();
		final StringBuilder errorHolder = new StringBuilder();
		boolean ok = true;
		ok = ok & readIntegerAttribute( element, settings, KEY_CHANNEL_1, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_CHANNEL_2, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_TARGET_CHANNEL, errorHolder );
		ok = ok & readIntegerAttribute( element, settings, KEY_CONTACT_SIZE, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_SIGMA_FILTER, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_THRESHOLD, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_THRESHOLD_1, errorHolder );
		ok = ok & readDoubleAttribute( element, settings, KEY_THRESHOLD_2, errorHolder );
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
			return false;
		}
		return checkSettings( settings );
	}

	@Override
	public Map< String, Object > getDefaultSettings()
	{
		final Map< String, Object > settings = new HashMap< String, Object >();
		settings.put( KEY_CHANNEL_1, Integer.valueOf( 2 ) );
		settings.put( KEY_CHANNEL_2, Integer.valueOf( 3 ) );
		settings.put( KEY_CONTACT_SIZE, Integer.valueOf( 3 ) );
		settings.put( KEY_SIGMA_FILTER, Double.valueOf( 1. ) );
		settings.put( KEY_THRESHOLD, Double.valueOf( 0.01 ) );
		settings.put( KEY_THRESHOLD_1, Double.valueOf( 200. ) );
		settings.put( KEY_THRESHOLD_2, Double.valueOf( 200. ) );
		settings.put( KEY_TARGET_CHANNEL, Integer.valueOf( 1 ) ); // dummy
		return settings;
	}

	@Override
	public boolean checkSettings( final Map< String, Object > settings )
	{
		boolean ok = true;
		final StringBuilder errorHolder = new StringBuilder();
		ok = ok & checkParameter( settings, KEY_CHANNEL_1, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CHANNEL_2, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_TARGET_CHANNEL, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_CONTACT_SIZE, Integer.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_SIGMA_FILTER, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_THRESHOLD, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_THRESHOLD_1, Double.class, errorHolder );
		ok = ok & checkParameter( settings, KEY_THRESHOLD_2, Double.class, errorHolder );
		final List< String > mandatoryKeys = new ArrayList< String >();
		mandatoryKeys.add( KEY_CHANNEL_1 );
		mandatoryKeys.add( KEY_CHANNEL_2 );
		mandatoryKeys.add( KEY_TARGET_CHANNEL );
		mandatoryKeys.add( KEY_CONTACT_SIZE );
		mandatoryKeys.add( KEY_SIGMA_FILTER );
		mandatoryKeys.add( KEY_THRESHOLD );
		mandatoryKeys.add( KEY_THRESHOLD_1 );
		mandatoryKeys.add( KEY_THRESHOLD_2 );
		ok = ok & checkMapKeys( settings, mandatoryKeys, null, errorHolder );
		
		if (ok)
		{
			final Integer channel1 = ( Integer ) settings.get( KEY_CHANNEL_1 );
			final Integer channel2 = ( Integer ) settings.get( KEY_CHANNEL_2 );
			if ( channel1 != null && channel2 != null && channel1 == channel2 )
			{
				errorHolder.append( "Target channels should not be equal.\n" );
				ok = false;
			}
		}
		
		if ( !ok )
		{
			errorMessage = errorHolder.toString();
		}
		return ok;
	}

}
