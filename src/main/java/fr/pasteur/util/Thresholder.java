package fr.pasteur.util;

import java.util.Vector;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.Converter;
import net.imglib2.img.Img;
import net.imglib2.multithreading.Chunk;
import net.imglib2.multithreading.SimpleMultiThreading;
import net.imglib2.type.BooleanType;
import net.imglib2.type.Type;
import net.imglib2.type.logic.BitType;

/**
 * Collection of static utilities meant to generate {@link BitType} images from
 * {@link Comparable} images.
 *
 * @author Jean-Yves Tinevez
 */
@SuppressWarnings( "deprecation" )
public class Thresholder
{

	/**
	 * Writes in a boolean {@link Img} by simple thresholding of the values of a
	 * source image.
	 *
	 * @param source
	 *            the image to threshold.
	 * @param target
	 *            the boolean image to write to. Must have the same dimensions
	 *            than the <code>source</code>.
	 * @param threshold
	 *            the threshold.
	 * @param above
	 *            if <code>true</code>, the target value will be true for source
	 *            values strictly above the threshold, <code>false</code>
	 *            otherwise.
	 * @param numThreads
	 *            the number of threads to use for processing.
	 */
	public static final < T extends Type< T > & Comparable< T >, R extends BooleanType< R >> void threshold( final RandomAccessibleInterval< T > source, final IterableInterval< R > target, final T threshold, final boolean above, final int numThreads )
	{
		final Converter< T, R > converter;
		if ( above )
		{
			converter = new Converter< T, R >()
			{
				@Override
				public void convert( final T input, final R output )
				{
					output.set( input.compareTo( threshold ) > 0 );
				}
			};
		}
		else
		{
			converter = new Converter< T, R >()
			{
				@Override
				public void convert( final T input, final R output )
				{
					output.set( input.compareTo( threshold ) < 0 );
				}
			};
		}

		final Vector< Chunk > chunks = SimpleMultiThreading.divideIntoChunks( target.size(), numThreads );
		final Thread[] threads = SimpleMultiThreading.newThreads( numThreads );
		for ( int i = 0; i < threads.length; i++ )
		{
			final Chunk chunk = chunks.get( i );
			threads[ i ] = new Thread( "Thresholder thread " + i )
			{
				@Override
				public void run()
				{
					final Cursor< R > cursorTarget = target.cursor();
					cursorTarget.jumpFwd( chunk.getStartPosition() );
					final RandomAccess< T > ra = source.randomAccess( target );
					for ( long steps = 0; steps < chunk.getLoopSize(); steps++ )
					{
						cursorTarget.fwd();
						ra.setPosition( cursorTarget );
						converter.convert( ra.get(), cursorTarget.get() );
					}
				}
			};
		}

		SimpleMultiThreading.startAndJoin( threads );
	}

}