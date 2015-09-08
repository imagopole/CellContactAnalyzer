package fr.pasteur.trackmate;

import java.util.List;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.Benchmark;
import net.imglib2.algorithm.MultiThreaded;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.detection.LogDetector;
import fiji.plugin.trackmate.detection.SpotDetector;
import fr.pasteur.ContactImgGenerator;

public class CellContactDetector< T extends RealType< T > & NativeType< T >> implements SpotDetector< T >, MultiThreaded, Benchmark
{

	private static final String BASE_ERROR_MSG = "[CellContactDetector] ";

	private final RandomAccessibleInterval< T > im1;

	private final RandomAccessibleInterval< T > im2;

	private final int contactSize;

	private final double sigma;

	private List< Spot > spots;

	private String errorMessage;

	private long processingTime;

	private int numThreads;

	private final double threshold;

	private final double[] calibration;

	public CellContactDetector( final RandomAccessibleInterval< T > im1, final RandomAccessibleInterval< T > im2, final int contactSize, final double sigma, final double threshold, final double[] calibration )
	{
		this.im1 = im1;
		this.im2 = im2;
		this.contactSize = contactSize;
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
		
		final ContactImgGenerator< T > generator = new ContactImgGenerator< T >( im1, im2, out, contactSize, sigma );
		generator.setNumThreads( numThreads );
		if ( !generator.checkInput() || !generator.process() )
		{
			errorMessage = BASE_ERROR_MSG + generator.getErrorMessage();
			return false;
		}
		
		final double radius = 2.0 * contactSize;
		final LogDetector< T > detector = new LogDetector< T >( out, out, calibration, radius, threshold, false, false );
		detector.setNumThreads( numThreads );
		if ( !detector.checkInput() || !detector.process() )
		{
			errorMessage = BASE_ERROR_MSG + detector.getErrorMessage();
			return false;
		}

		spots = detector.getResult();

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
