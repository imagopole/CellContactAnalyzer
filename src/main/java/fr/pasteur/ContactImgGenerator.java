package fr.pasteur;

import java.util.List;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Algorithm;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.morphology.Dilation;
import net.imglib2.algorithm.morphology.StructuringElements;
import net.imglib2.algorithm.neighborhood.Shape;
import net.imglib2.algorithm.stats.ComputeMinMax;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.histogram.Real1dBinMapper;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ContactImgGenerator< T extends RealType< T > & NativeType< T >> implements Algorithm, MultiThreaded, Benchmark
{
	private static final String BASE_ERROR_MSG = "[ContactImgGenerator] ";

	private final RandomAccessibleInterval< T > img1;

	private final RandomAccessibleInterval< T > img2;

	private final int contactSize;

	private String errorMessage;

	private int numThreads;

	private final IterableInterval< T > out;

	private long processingTime;

	private final double sigma;

	private final double threshold_C1;

	private final double threshold_C2;

	public ContactImgGenerator( final RandomAccessibleInterval< T > img1, final RandomAccessibleInterval< T > img2, final IterableInterval< T > out,
			final double threshold_C1, final double threshold_C2, final int contactSize, final double sigma )
	{
		this.img1 = img1;
		this.img2 = img2;
		this.out = out;
		this.threshold_C1 = threshold_C1;
		this.threshold_C2 = threshold_C2;
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
		if (sigma <= 0) 
		{
			errorMessage = BASE_ERROR_MSG + "The gaussian filter sigma is lower than or equal to 0 (σ = " + sigma + ").";
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
		Dilation.dilateInPlace( target1, target1, strel, numThreads );

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
		Dilation.dilateInPlace( target2, target2, strel, numThreads );

		final Cursor< T > oc = out.localizingCursor();
		final RandomAccess< T > ra1 = target1.randomAccess( out );
		final RandomAccess< T > ra2 = target2.randomAccess( out );
		while ( oc.hasNext() )
		{
			oc.fwd();
			ra1.setPosition( oc );
			ra2.setPosition( oc );

			final double t1 = Math.max( 0., ra1.get().getRealDouble() - threshold_C1 );
			final double t2 = Math.max( 0., ra2.get().getRealDouble() - threshold_C2 );

			oc.get().setReal( ( t1 * t2 ) / ( t1 + t2 ) );
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

	public static final < T extends RealType< T >> T otsuTreshold( final RandomAccessibleInterval<T> src )
	{

		final T min = Util.getTypeFromInterval( src ).createVariable();
		final T max = Util.getTypeFromInterval( src ).createVariable();
		ComputeMinMax.computeMinMax( src, min, max );

		final Real1dBinMapper< T > mapper = new Real1dBinMapper< T >( min.getRealDouble(), max.getRealDouble(), 256l, false );
		final Histogram1d< T > hist = new Histogram1d< T >( mapper );
		hist.countData( Views.iterable( src ) );

		final long[] histogram = hist.toLongArray();
		// Otsu's threshold algorithm
		// C++ code by Jordan Bevik <Jordan.Bevic@qtiworld.com>
		// ported to ImageJ plugin by G.Landini
		int k, kStar; // k = the current threshold; kStar = optimal threshold
		final int L = histogram.length; // The total intensity of the image
		long N1, N; // N1 = # points with intensity <=k; N = total number of points
		long Sk; // The total intensity for all histogram points <=k
		long S;
		double BCV, BCVmax; // The current Between Class Variance and maximum BCV
		double num, denom; // temporary bookkeeping

		// Initialize values:
		S = 0;
		N = 0;
		for ( k = 0; k < L; k++ )
		{
			S += k * histogram[ k ]; // Total histogram intensity
			N += histogram[ k ]; // Total number of data points
		}

		Sk = 0;
		N1 = histogram[ 0 ]; // The entry for zero intensity
		BCV = 0;
		BCVmax = 0;
		kStar = 0;

		// Look at each possible threshold value,
		// calculate the between-class variance, and decide if it's a max
		for ( k = 1; k < L - 1; k++ )
		{ // No need to check endpoints k = 0 or k = L-1
			Sk += k * histogram[ k ];
			N1 += histogram[ k ];

			// The float casting here is to avoid compiler warning about loss of
			// precision and will prevent overflow in the case of large saturated images
			denom = ( double ) ( N1 ) * ( N - N1 ); // Maximum value of denom is (N^2)/4 = approx. 3E10

			if ( denom != 0 )
			{
				// Float here is to avoid loss of precision when dividing
				num = ( ( double ) N1 / N ) * S - Sk; // Maximum value of num =
				// 255*N = approx 8E7
				BCV = ( num * num ) / denom;
			}
			else
				BCV = 0;

			if ( BCV >= BCVmax )
			{ // Assign the best threshold found so far
				BCVmax = BCV;
				kStar = k;
			}
		}
		// kStar += 1; // Use QTI convention that intensity -> 1 if intensity >= k
		// (the algorithm was developed for I-> 1 if I <= k.)
		final T val = hist.firstDataValue().createVariable();
		hist.getCenterValue( kStar, val );
		return val;
	}

}
