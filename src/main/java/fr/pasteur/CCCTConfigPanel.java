package fr.pasteur;

import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CHANNEL_1;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CHANNEL_2;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CONTACT_SENSITIVITY;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_SIGMA_FILTER;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_THRESHOLD_1;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_THRESHOLD_2;
import ij.IJ;
import ij.ImagePlus;

import java.util.Map;

import net.imagej.ImgPlus;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.util.TMUtils;
import fr.pasteur.trackmate.CellContactConfigurationPanel;

@SuppressWarnings( "deprecation" )
class CCCTConfigPanel extends CellContactConfigurationPanel
{

	private final ImagePlus imp;

	public CCCTConfigPanel( final ImagePlus imp, final Model model )
	{
		super( imp, model );
		this.imp = imp;
		this.lblThreshold.setVisible( false );
		this.jtfThreshold.setVisible( false );
	}

	@Override
	protected void preview()
	{
		btnPreview.setEnabled( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@SuppressWarnings( { "rawtypes", "unchecked" } )
			@Override
			public void run()
			{
				try
				{
					final Map< String, Object > settings = getSettings();

					// In ImgLib2, dimensions are 0-based.
					final int channel1 = ( Integer ) settings.get( KEY_CHANNEL_1 ) - 1;
					final int channel2 = ( Integer ) settings.get( KEY_CHANNEL_2 ) - 1;
					final int contactSensitivity = ( Integer ) settings.get( KEY_CONTACT_SENSITIVITY );
					final double sigma = ( Double ) settings.get( KEY_SIGMA_FILTER );

					RandomAccessibleInterval im1;
					RandomAccessibleInterval im2;

					final ImgPlus img = TMUtils.rawWraps( imp );
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
					final int frame = imp.getFrame() - 1;
					if ( timeDim >= 0 )
					{
						if ( cDim >= 0 && timeDim > cDim )
						{
							timeDim--;
						}
						im1 = Views.hyperSlice( im1, timeDim, frame );
						im2 = Views.hyperSlice( im2, timeDim, frame );
					}

					final double threshold_C1 = ( Double ) settings.get( KEY_THRESHOLD_1 );
					final double threshold_C2 = ( Double ) settings.get( KEY_THRESHOLD_2 );

					final ImgFactory factory = new PlanarImgFactory();
					final Img out = factory.create( im1, Util.getTypeFromInterval( im1 ) );

					final ContactImgGenerator generator = new ContactImgGenerator( im1, im2, out,
							threshold_C1, threshold_C2, contactSensitivity, sigma );

					if ( !generator.checkInput() || !generator.process() )
					{
						IJ.error( CCCT_.PLUGIN_NAME + " v" + CCCT_.PLUGIN_VERSION, generator.getErrorMessage() );
						return;
					}

					final ImagePlus wrap = ImageJFunctions.wrap( out, CCCT_.PLUGIN_NAME + " preview frame " + ( frame + 1 ) );
					wrap.setCalibration( imp.getCalibration() );
					wrap.getProcessor().resetMinAndMax();
					wrap.show();
				}
				finally
				{
					btnPreview.setEnabled( true );
				}

			};
		}.start();
	}

	private static final long serialVersionUID = 1L;

}
