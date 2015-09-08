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
import net.imglib2.exception.IncompatibleTypeException;
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

	public ContactImgGenerator( final RandomAccessibleInterval< T > img1, final RandomAccessibleInterval< T > img2, final IterableInterval< T > out, final int contactSize, final double sigma )
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
		final double threshold1 = ContactImgGenerator2.otsuTreshold( target1 ).getRealDouble();

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
		final double threshold2 = ContactImgGenerator2.otsuTreshold( target2 ).getRealDouble();

		final Cursor< T > oc = out.localizingCursor();
		final RandomAccess< T > ra1 = target1.randomAccess( out );
		final RandomAccess< T > ra2 = target2.randomAccess( out );
		while ( oc.hasNext() )
		{
			oc.fwd();
			ra1.setPosition( oc );
			ra2.setPosition( oc );

			final double t1 = Math.max( 0., ra1.get().getRealDouble() - threshold1 );
			final double t2 = Math.max( 0., ra2.get().getRealDouble() - threshold2 );

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

}
