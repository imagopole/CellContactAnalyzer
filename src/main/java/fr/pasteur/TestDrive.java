package fr.pasteur;

import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class TestDrive
{

	public static < T extends RealType< T > & NativeType< T >> void main( final String[] args )
	{
		ImageJ.main( args );
		final String c1 = "/Users/tinevez/Projects/AMikhailova/Data/150823 SiT+SAg_1-1.tif";
		final String c2 = "/Users/tinevez/Projects/AMikhailova/Data/150823 SiT+SAg_1-2.tif";
		final int d = 2;
		final int contactSize = 3;

		final ImagePlus imp1 = new ImagePlus( c1 );
		imp1.show();
		final ImagePlus imp2 = new ImagePlus( c2 );
		imp2.show();

		final Img< T > img1 = ImageJFunctions.wrap( imp1 );
		final Img< T > img2 = ImageJFunctions.wrap( imp2 );

		final PlanarImgFactory< T > factory = new PlanarImgFactory< T >();
		final Img< T > out = factory.create( img1, Util.getTypeFromInterval( img1 ) );

//		final PlanarImgFactory< BitType > factory = new PlanarImgFactory< BitType >();
//		final Img< BitType > out = factory.create( img1, new BitType() );

		final int nFrames = imp1.getNFrames();
		long dt = 0;
		final double sigma = 1.;
		for ( int t = 0; t < nFrames; t++ )
		{
			final ContactImgGenerator< T > algo = new ContactImgGenerator< T >(
					Views.hyperSlice( img1, d, t ),
					Views.hyperSlice( img2, d, t ),
					Views.hyperSlice( out, d, t ), contactSize, sigma );

//			final ContactImgGenerator2< T > algo = new ContactImgGenerator2< T >( Views.hyperSlice( img1, d, t ), Views.hyperSlice( img2, d, t ), Views.hyperSlice( out, d, t ), contactSize, sigma );
			if ( !algo.checkInput() || !algo.process() )
			{
				System.err.println( algo.getErrorMessage() );
				return;
			}

			dt += algo.getProcessingTime();

			if ( t > 50 )
				break;
		}
		System.out.println( "Completed in " + dt / 1e3 + " s." );

		ImageJFunctions.show( out, "Contact" );

	}

}
