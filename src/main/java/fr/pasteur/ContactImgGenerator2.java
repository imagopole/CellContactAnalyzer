package fr.pasteur;

import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.binary.Thresholder;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.morphology.StructuringElements;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.algorithm.stats.Histogram;
import net.imglib2.algorithm.stats.HistogramBinMapper;
import net.imglib2.algorithm.stats.RealBinMapper;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ContactImgGenerator2< T extends RealType< T > & NativeType< T >> implements Algorithm, MultiThreaded, Benchmark
{
	private static final String BASE_ERROR_MSG = "[ContactImgGenerator2] ";

	private final RandomAccessibleInterval< T > img1;

	private final RandomAccessibleInterval< T > img2;

	private final int contactSize;

	private String errorMessage;

	private int numThreads;

	private final IterableInterval< BitType > out;

	private long processingTime;

	private final double sigma;

	public ContactImgGenerator2( final RandomAccessibleInterval< T > img1, final RandomAccessibleInterval< T > img2, final IterableInterval< BitType > out, final int contactSize, final double sigma )
	{
		this.img1 = img1;
		this.img2 = img2;
		this.out = out;
		this.contactSize = contactSize;
		this.sigma = sigma;
		setNumThreads();
	}

	@Override
	public boolean checkInput()
	{
		for ( int d = 0; d < img1.numDimensions(); d++ )
		{
			if ( img1.dimension( d ) != img2.dimension( d ) )
			{
				errorMessage = BASE_ERROR_MSG + "Source images do not have the same dimensions (for dimension "
						+ d + ", img1 = " + img1.dimension( d ) + " and img2 = " + img2.dimension( d ) + ".";
				return false;
			}
		}
		if ( contactSize < 1 )
		{
			errorMessage = BASE_ERROR_MSG + "The contact size must be greater than 0 (was " + contactSize + ").";
			return false;
		}
		return true;
	}

	@Override
	public boolean process()
	{
		final long start = System.currentTimeMillis();
		final double[] sigmas = Util.getArrayFromValue( sigma, img1.numDimensions() );

		final ImgFactory< T > factory = Util.getArrayOrCellImgFactory( img1, Util.getTypeFromInterval( img1 ) );

		final List< Shape > strel = StructuringElements.disk( contactSize, img1.numDimensions() );

		final Img< T > target1 = factory.create( img1, Util.getTypeFromInterval( img1 ) );
		try
		{
			Gauss3.gauss( sigmas, Views.extendMirrorDouble( img1 ), target1, numThreads );
		}
		catch ( final IncompatibleTypeException e )
		{
			errorMessage = BASE_ERROR_MSG + e.getMessage();
			e.printStackTrace();
			return false;
		}
		final T threshold1 = otsuTreshold( target1 );
		final Img< BitType > thresholded1 = Thresholder.threshold( target1, threshold1, true, numThreads );
		Dilation.dilateInPlace( thresholded1, thresholded1, strel, numThreads );

		System.out.println( "Treshold 1 = " + threshold1 );// DEBUG
		ImageJFunctions.show( target1, "1 - filtered" );
		ImageJFunctions.show( thresholded1, "1 - thresholded" );

		final Img< T > target2 = factory.create( img1, Util.getTypeFromInterval( img1 ) );
		try
		{
			Gauss3.gauss( sigmas, Views.extendMirrorDouble( img2 ), target2, numThreads );
		}
		catch ( final IncompatibleTypeException e )
		{
			errorMessage = BASE_ERROR_MSG + e.getMessage();
			e.printStackTrace();
			return false;
			}
		
		final T threshold2 = otsuTreshold( target2 );
		final Img< BitType > thresholded2 = Thresholder.threshold( target2, threshold2, true, numThreads );
		Dilation.dilateInPlace( thresholded2, thresholded2, strel, numThreads );
		
		System.out.println( "Treshold 2 = " + threshold2 );// DEBUG
		ImageJFunctions.show( target2, "2 - filtered" );
		ImageJFunctions.show( thresholded2, "2 - thresholded" );

		final Cursor< BitType > oc = out.localizingCursor();
		final RandomAccess< BitType > ra1 = thresholded1.randomAccess( out );
		final RandomAccess< BitType > ra2 = thresholded2.randomAccess( out );
		while ( oc.hasNext() )
		{
			oc.fwd();
			ra1.setPosition( oc );
			ra2.setPosition( oc );


			oc.get().set( ra2.get() );
			oc.get().and( ra1.get() );
		}

		final long end = System.currentTimeMillis();
		this.processingTime = end - start;
		return true;
	}

	private static final < T extends RealType< T >> T otsuTreshold( final Img< T > src )
	{

		final T min = Util.getTypeFromInterval( src ).createVariable();
		final T max = Util.getTypeFromInterval( src ).createVariable();
		ComputeMinMax.computeMinMax( src, min, max );

		final HistogramBinMapper< T > mapper = new RealBinMapper< T >( min, max, 256 );
		final Histogram< T > histo = new Histogram< T >( mapper, src );
		histo.process();

		double sum = 0;
		for ( int i = 0; i < histo.getNumBins(); i++ )
		{
			sum += histo.getBinCenter( i ).getRealDouble() * histo.getBin( i );
		}

		final long total = src.size();
		long wB = 0;
		long wF = 0;
		double sumB = 0;
		
		double varMax = 0;
		T threshold = Util.getTypeFromInterval( src ).createVariable();
		threshold.setZero();

		for ( int i = 0; i < histo.getNumBins(); i++ )
		{
			wB += histo.getBin( i );
			wF = total - wB;

			if ( wF == 0 )
			{
				break;
			}
			
			sumB += histo.getBinCenter( i ).getRealDouble() * histo.getBin( i );

			final double mB = sumB / wB;
			final double mF = ( sum - sumB ) / wB;

			final double varBetween = ( double ) wB * ( double ) wF * ( mB - mF ) * ( mB - mF );

			if ( varBetween > varMax )
			{
				varMax = varBetween;
				threshold = histo.getBinCenter( i );
			}
		}

		return threshold;
	}

	@Override
	public String getErrorMessage()
	{
		return errorMessage;
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

	@Override
	public long getProcessingTime()
	{
		return processingTime;
	}

}
