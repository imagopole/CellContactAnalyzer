package fr.pasteur.trackmate;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_THRESHOLD;
import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CHANNEL_1;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CHANNEL_2;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_CONTACT_SIZE;
import static fr.pasteur.trackmate.CellContactDetectorFactory.KEY_SIGMA_FILTER;
import ij.ImagePlus;
import ij.gui.NewImage;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotCollection;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.gui.ConfigurationPanel;
import fiji.plugin.trackmate.util.JLabelLogger;

public class CellContactConfigurationPanel extends ConfigurationPanel
{
	private static final long serialVersionUID = 1L;

	private final ImagePlus imp;

	private final Model model;

	private final JButton btnPreview;

	private final JSlider sliderCh1;

	private final JSlider sliderCh2;

	private final JFormattedTextField jtfContactSize;

	private final JFormattedTextField jtfSigma;

	private final JFormattedTextField jtfThreshold;

	private final Logger localLogger;

	public CellContactConfigurationPanel( final ImagePlus imp, final Model model )
	{
		this.imp = imp == null ? NewImage.createByteImage( "Blank", 50, 50, 3, NewImage.FILL_BLACK ) : imp;
		this.model = model;

		final JLabel lblTitle = new JLabel( "Settings for detector:" );
		lblTitle.setFont( FONT );

		final JLabel lblDetectorName = new JLabel( CellContactDetectorFactory.NAME );
		lblDetectorName.setHorizontalAlignment( SwingConstants.CENTER );
		lblDetectorName.setFont( BIG_FONT );

		final JLabel lblChannel1 = new JLabel( "Channel 1:" );
		lblChannel1.setFont( FONT );

		final JLabel lblInfo = new JLabel();
		lblInfo.setFont( FONT.deriveFont( Font.ITALIC ) );
		lblInfo.setText( CellContactDetectorFactory.INFO_TEXT );

		sliderCh1 = new JSlider( 1, imp.getNChannels() );

		final JLabel jtfCh1 = new JLabel( "1" );
		jtfCh1.setHorizontalAlignment( SwingConstants.CENTER );
		jtfCh1.setFont( FONT );

		sliderCh1.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				jtfCh1.setText( "" + sliderCh1.getValue() );
			}
		} );

		final JLabel lblChannel2 = new JLabel( "Channel 2:" );
		lblChannel2.setFont( FONT );

		sliderCh2 = new JSlider( 1, imp.getNChannels() );

		final JLabel jtfCh2 = new JLabel( "2" );
		jtfCh2.setHorizontalAlignment( SwingConstants.CENTER );
		jtfCh2.setFont( FONT );

		sliderCh2.addChangeListener( new ChangeListener()
		{
			@Override
			public void stateChanged( final ChangeEvent e )
			{
				jtfCh2.setText( "" + sliderCh2.getValue() );
			}
		} );

		final JLabel lblContactSize = new JLabel( "Contact size:" );
		lblContactSize.setFont( FONT );

		jtfContactSize = new JFormattedTextField( Integer.valueOf( 3 ) );
		jtfContactSize.setHorizontalAlignment( SwingConstants.CENTER );
		jtfContactSize.setFont( FONT );

		final JLabel lblFilterSigma = new JLabel( "Filter sigma:" );
		lblFilterSigma.setFont( FONT );

		jtfSigma = new JFormattedTextField( Double.valueOf( 1.0 ) );
		jtfSigma.setHorizontalAlignment( SwingConstants.CENTER );
		jtfSigma.setFont( FONT );

		final JLabel lblThreshold = new JLabel( "Threshold:" );
		lblThreshold.setFont( FONT );

		jtfThreshold = new JFormattedTextField( Double.valueOf( 0. ) );
		jtfThreshold.setHorizontalAlignment( SwingConstants.CENTER );
		jtfThreshold.setFont( FONT );

		btnPreview = new JButton( "Preview" );
		btnPreview.setFont( FONT );
		btnPreview.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent arg0 )
			{
				preview();
			}
		} );

		final JLabelLogger labelLogger = new JLabelLogger();
		localLogger = labelLogger.getLogger();

		final GroupLayout groupLayout = new GroupLayout( this );
		groupLayout.setHorizontalGroup(
				groupLayout.createParallelGroup( Alignment.LEADING )
						.addGroup( groupLayout.createSequentialGroup()
								.addContainerGap()
								.addGroup( groupLayout.createParallelGroup( Alignment.LEADING )
										.addGroup( groupLayout.createSequentialGroup()
												.addComponent( lblChannel1, GroupLayout.PREFERRED_SIZE, 86, GroupLayout.PREFERRED_SIZE )
												.addPreferredGap( ComponentPlacement.RELATED )
												.addComponent( sliderCh1, GroupLayout.DEFAULT_SIZE, 144, Short.MAX_VALUE )
												.addGap( 3 )
												.addComponent( jtfCh1, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE )
												.addContainerGap() )
										.addGroup( groupLayout.createSequentialGroup()
												.addGroup( groupLayout.createParallelGroup( Alignment.TRAILING )
														.addComponent( lblInfo, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE )
														.addComponent( lblDetectorName, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE )
														.addComponent( lblTitle, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 292, Short.MAX_VALUE ) )
												.addGap( 3 ) )
										.addGroup( groupLayout.createSequentialGroup()
												.addComponent( lblChannel2, GroupLayout.PREFERRED_SIZE, 86, GroupLayout.PREFERRED_SIZE )
												.addGap( 6 )
												.addComponent( sliderCh2, GroupLayout.DEFAULT_SIZE, 141, Short.MAX_VALUE )
												.addPreferredGap( ComponentPlacement.RELATED )
												.addComponent( jtfCh2, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE )
												.addContainerGap() )
										.addGroup( groupLayout.createSequentialGroup()
												.addGroup( groupLayout.createParallelGroup( Alignment.TRAILING, false )
														.addComponent( lblThreshold, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
														.addComponent( lblFilterSigma, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
														.addComponent( lblContactSize, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE ) )
												.addPreferredGap( ComponentPlacement.RELATED )
												.addGroup( groupLayout.createParallelGroup( Alignment.LEADING, false )
														.addComponent( jtfSigma )
														.addComponent( jtfContactSize, GroupLayout.DEFAULT_SIZE, 43, Short.MAX_VALUE )
														.addComponent( jtfThreshold, GroupLayout.PREFERRED_SIZE, 68, GroupLayout.PREFERRED_SIZE ) )
												.addGap( 120 ) )
										.addGroup( groupLayout.createSequentialGroup()
												.addComponent( btnPreview )
												.addGap( 18 )
												.addComponent( labelLogger, GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE )
												.addContainerGap() ) ) )
				);
		groupLayout.setVerticalGroup(
				groupLayout.createParallelGroup( Alignment.LEADING )
						.addGroup( groupLayout.createSequentialGroup()
								.addContainerGap()
								.addComponent( lblTitle )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( lblDetectorName, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( lblInfo, GroupLayout.PREFERRED_SIZE, 129, GroupLayout.PREFERRED_SIZE )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addGroup( groupLayout.createParallelGroup( Alignment.LEADING, false )
										.addComponent( sliderCh1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
										.addComponent( lblChannel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
										.addComponent( jtfCh1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE ) )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addGroup( groupLayout.createParallelGroup( Alignment.LEADING, false )
										.addComponent( lblChannel2, GroupLayout.DEFAULT_SIZE, 29, Short.MAX_VALUE )
										.addComponent( sliderCh2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE )
										.addComponent( jtfCh2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE ) )
								.addPreferredGap( ComponentPlacement.UNRELATED )
								.addGroup( groupLayout.createParallelGroup( Alignment.BASELINE )
										.addComponent( lblContactSize )
										.addComponent( jtfContactSize, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ) )
								.addPreferredGap( ComponentPlacement.UNRELATED )
								.addGroup( groupLayout.createParallelGroup( Alignment.BASELINE )
										.addComponent( lblFilterSigma )
										.addComponent( jtfSigma, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ) )
								.addPreferredGap( ComponentPlacement.UNRELATED )
								.addGroup( groupLayout.createParallelGroup( Alignment.BASELINE )
										.addComponent( lblThreshold )
										.addComponent( jtfThreshold, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE ) )
								.addGap( 55 )
								.addGroup( groupLayout.createParallelGroup( Alignment.BASELINE )
										.addComponent( btnPreview )
										.addComponent( labelLogger, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE ) )
								.addContainerGap( 81, Short.MAX_VALUE ) )
				);
		setLayout( groupLayout );
	}

	@Override
	public void setSettings( final Map< String, Object > settings )
	{
		sliderCh1.setValue( Math.min( imp.getNChannels(), ( Integer ) settings.get( KEY_CHANNEL_1 ) ) );
		sliderCh2.setValue( Math.min( imp.getNChannels(), ( Integer ) settings.get( KEY_CHANNEL_2 ) ) );
		jtfContactSize.setValue( settings.get( KEY_CONTACT_SIZE ) );
		jtfSigma.setValue( settings.get( KEY_SIGMA_FILTER ) );
		jtfThreshold.setValue( settings.get( KEY_THRESHOLD ) );
	}

	@Override
	public Map< String, Object > getSettings()
	{
		final HashMap< String, Object > settings = new HashMap< String, Object >( 5 );
		final int channel1 = sliderCh1.getValue();
		final int channel2 = sliderCh2.getValue();
		try
		{
			jtfContactSize.commitEdit();
			jtfSigma.commitEdit();
			jtfThreshold.commitEdit();
		}
		catch ( final ParseException e )
		{
			e.printStackTrace();
		}

		final int contactSize = ( Integer ) jtfContactSize.getValue();
		final double sigma = ( Double ) jtfSigma.getValue();
		final double threshold = ( Double ) jtfThreshold.getValue();

		settings.put( KEY_CHANNEL_1, channel1 );
		settings.put( KEY_CHANNEL_2, channel2 );
		settings.put( KEY_CONTACT_SIZE, contactSize );
		settings.put( KEY_SIGMA_FILTER, sigma );
		settings.put( KEY_THRESHOLD, threshold );
		// Add a dummy target channel
		settings.put( KEY_TARGET_CHANNEL, 1 );
		return settings;
	}

	private void preview()
	{
		btnPreview.setEnabled( false );
		new Thread( "TrackMate preview detection thread" )
		{
			@SuppressWarnings( "rawtypes" )
			@Override
			public void run()
			{
				final Settings settings = new Settings();
				settings.setFrom( imp );
				final int frame = imp.getFrame() - 1;
				settings.tstart = frame;
				settings.tend = frame;

				settings.detectorFactory = new CellContactDetectorFactory();
				settings.detectorSettings = getSettings();

				final TrackMate trackmate = new TrackMate( settings );
				trackmate.getModel().setLogger( localLogger );

				final boolean detectionOk = trackmate.execDetection();
				if ( !detectionOk )
				{
					localLogger.error( trackmate.getErrorMessage() );
					return;
				}
				localLogger.log( "Found " + trackmate.getModel().getSpots().getNSpots( false ) + " spots." );

				// Wrap new spots in a list.
				final SpotCollection newspots = trackmate.getModel().getSpots();
				final Iterator< Spot > it = newspots.iterator( frame, false );
				final ArrayList< Spot > spotsToCopy = new ArrayList< Spot >( newspots.getNSpots( frame, false ) );
				while ( it.hasNext() )
				{
					spotsToCopy.add( it.next() );
				}
				// Pass new spot list to model.
				model.getSpots().put( frame, spotsToCopy );
				// Make them visible
				for ( final Spot spot : spotsToCopy )
				{
					spot.putFeature( SpotCollection.VISIBLITY, SpotCollection.ONE );
				}
				// Generate event for listener to reflect changes.
				model.setSpots( model.getSpots(), true );

				btnPreview.setEnabled( true );

			};
		}.start();
	}
}
