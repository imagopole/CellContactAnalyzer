package fr.pasteur.trackmate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.labeling.AllConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.labeling.NativeImgLabeling;
import net.imglib2.roi.IterableRegionOfInterest;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.SpotDetector;
import fr.pasteur.ContactImgGenerator;
import fr.pasteur.util.Thresholder;

@SuppressWarnings( "deprecation" )
public class CellContactDetector< T extends RealType< T > & NativeType< T >> implements SpotDetector< T >, MultiThreaded, Benchmark
{

	private static final String BASE_ERROR_MSG = "[CellContactDetector] ";

	private final RandomAccessibleInterval< T > im1;

	private final RandomAccessibleInterval< T > im2;

	private final int contactSensitivity;

	private final double sigma;

	private List< Spot > spots;

	private String errorMessage;

	private long processingTime;

	private int numThreads;

	private final double threshold;

	private final double[] calibration;

	private final double threshold_C1;

	private final double threshold_C2;

	public CellContactDetector( final RandomAccessibleInterval< T > im1, final RandomAccessibleInterval< T > im2, final double threshold_C1, final double threshold_C2, final int contactSensitivity, final double sigma, final double threshold, final double[] calibration )
	{
		this.im1 = im1;
		this.im2 = im2;
		this.threshold_C1 = threshold_C1;
		this.threshold_C2 = threshold_C2;
		this.contactSensitivity = contactSensitivity;
		this.sigma = sigma;
		this.threshold = threshold;
		this.calibration = calibration;
	}

	@Override
	public List< Spot > getResult()
	{
		return spots;
	}

	@Override
	public boolean checkInput()
	{
		if ( null == im1 )
		{
			errorMessage = BASE_ERROR_MSG + "Image 1 is null.";
			return false;
		}
		if ( null == im2 )
		{
			errorMessage = BASE_ERROR_MSG + "Image 2 is null.";
			return false;
		}
		if ( im1.numDimensions() > 3 )
		{
			errorMessage = BASE_ERROR_MSG + "Image 1 must be 1D, 2D or 3D, got " + im1.numDimensions() + "D.";
			return false;
		}
		if ( im2.numDimensions() > 3 )
		{
			errorMessage = BASE_ERROR_MSG + "Image 2 must be 1D, 2D or 3D, got " + im1.numDimensions() + "D.";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();

		final ImgFactory< T > factory = Util.getArrayOrCellImgFactory( im1, Util.getTypeFromInterval( im1 ) );
		final Img< T > out = factory.create( im1, Util.getTypeFromInterval( im1 ) );
		
		final ContactImgGenerator< T > generator = new ContactImgGenerator< T >( im1, im2, out, threshold_C1, threshold_C2, contactSensitivity, sigma );
		generator.setNumThreads( numThreads );
		if ( !generator.checkInput() || !generator.process() )
		{
			errorMessage = BASE_ERROR_MSG + generator.getErrorMessage();
			return false;
		}
		
		
		/*
		 * Generate binary mask.
		 */

		final ImgFactory< BitType > maskFactory = Util.getArrayOrCellImgFactory( im1, new BitType() );
		final Img< BitType > mask = maskFactory.create( im1, new BitType() );
		final T valTreshold = out.firstElement().createVariable();
		valTreshold.setZero();
		Thresholder.threshold( out, mask, valTreshold, true, numThreads );

		/*
		 * Find connected components.
		 */

		final Iterator< Integer > names = AllConnectedComponents.getIntegerNames( 0 );
		final Img< UnsignedIntType > lbl = new PlanarImgFactory< UnsignedIntType >().create( mask, new UnsignedIntType() );
		final NativeImgLabeling< Integer, UnsignedIntType > labeling = new NativeImgLabeling< Integer, UnsignedIntType >( lbl );
		AllConnectedComponents.labelAllConnectedComponents( labeling, mask, names );

		/*
		 * Loop over connected components and generate spots.
		 */

		final Collection< Integer > labels = labeling.getLabels();
		spots = new ArrayList< Spot >( labels.size() );
		for ( final Integer label : labels )
		{
			final long size = labeling.getArea( label );
			if ( size < threshold )
			{
				continue;
			}


			final IterableRegionOfInterest roi = labeling.getIterableRegionOfInterest( label );
			final Cursor< T > cursor = roi.getIterableIntervalOverROI( out ).cursor();
			final double[] lpos = new double[ im1.numDimensions() ];
			final double[] pos = new double[] { 0., 0., 0. };
			double x = 0.;
			double y = 0.;
			double z = 0.;
			double quality = 0.;
			while ( cursor.hasNext() )
			{
				cursor.fwd();
				cursor.localize( lpos );
				System.arraycopy( lpos, 0, pos, 0, lpos.length );
				x += pos[ 0 ] * calibration[ 0 ];
				y += pos[ 1 ] * calibration[ 1 ];
				z += pos[ 2 ] * calibration[ 2 ];
				quality += cursor.get().getRealDouble();
			}
			// Spot position is the mean position of all pixels from the region.
			x /= size;
			y /= size;
			z /= size;

			double volume = size;
			for ( int d = 0; d < im1.numDimensions(); d++ )
			{
				volume *= calibration[ d ];
			}
			final double radius;
			switch ( im1.numDimensions() )
			{
			case 1:
				radius = volume / 2;
				break;
			case 2:
			default:
				radius = Math.sqrt( volume / Math.PI );
				break;
			case 3:
				radius = Math.pow( 3. * volume / 4. / Math.PI, 1. / 3. );
			}

			final Spot spot = new Spot( x, y, z, radius, quality );
			spots.add( spot );
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return true;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public long getProcessingTime()
	{
		return processingTime;
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
