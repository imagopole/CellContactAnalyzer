package fr.pasteur;

import static fiji.plugin.trackmate.gui.TrackMateWizard.BIG_FONT;
import static fiji.plugin.trackmate.gui.TrackMateWizard.FONT;
import fiji.plugin.trackmate.Model;
import fr.pasteur.trackmate.CellContactDetectorFactory;
import ij.ImagePlus;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Map;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class CCCTGui extends JFrame
{
	private static final long serialVersionUID = 1L;

	private final JPanel contentPane;

	private final CCCTConfigPanel configPanel;

	private final JProgressBar progressBar;

	/**
	 * Create the frame.
	 */
	@SuppressWarnings( { "rawtypes", "unchecked" } )
	public CCCTGui( final ImagePlus imp, final CCCTProcessor ccctProcessor )
	{
		setSize( 300, 550 );
		contentPane = new JPanel();
		contentPane.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
		contentPane.setLayout( new BorderLayout( 0, 0 ) );
		setContentPane( contentPane );

		final JTabbedPane tabbedPane = new JTabbedPane( JTabbedPane.TOP );
		contentPane.add( tabbedPane, BorderLayout.CENTER );

		// #1.
		final Model model = new Model();
		configPanel = new CCCTConfigPanel( imp, model );
		configPanel.setSettings( new CellContactDetectorFactory().getDefaultSettings() );
		tabbedPane.addTab( "Configure", configPanel );

		// #2.
		final JPanel panelAnalyze = new JPanel();
		tabbedPane.addTab( "Analyze", null, panelAnalyze, null );

		final JCheckBox chckbxContactImage = new JCheckBox( "Show contact image." );
		chckbxContactImage.setFont( FONT );
		chckbxContactImage.setSelected( true );

		final JCheckBox chckbxGenerateContactsMask = new JCheckBox( "Generate contact mask." );
		chckbxGenerateContactsMask.setFont( FONT );

		final JCheckBox chckbxGenerateLabelImage = new JCheckBox( "Generate label image." );
		chckbxGenerateLabelImage.setFont( FONT );

		final JCheckBox chckbxTrackLabels = new JCheckBox( "Track labels." );
		chckbxTrackLabels.setFont( FONT );

		chckbxGenerateLabelImage.addItemListener( new ItemListener()
		{
			@Override
			public void itemStateChanged( final ItemEvent e )
			{
				chckbxTrackLabels.setEnabled( chckbxGenerateLabelImage.isSelected() );
			}
		} );
		chckbxGenerateLabelImage.setSelected( false );

		final JButton btnGo = new JButton( "Go!" );
		btnGo.setFont( BIG_FONT );
		btnGo.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( final ActionEvent e )
			{
				btnGo.setEnabled( false );
				new Thread( "CCCT process thread" )
				{
					@Override
					public void run()
					{
						try
						{
							ccctProcessor.process(
									chckbxContactImage.isSelected(),
									chckbxGenerateContactsMask.isSelected(),
									chckbxGenerateLabelImage.isSelected(),
									chckbxTrackLabels.isSelected()
									);
						}
						finally
						{
							btnGo.setEnabled( true );
						}

					};
				}.start();
			}
		} );

		progressBar = new JProgressBar( 0, 100 );

		final GroupLayout gl_panelAnalyze = new GroupLayout( panelAnalyze );
		gl_panelAnalyze.setHorizontalGroup(
				gl_panelAnalyze.createParallelGroup( Alignment.LEADING )
						.addGroup( gl_panelAnalyze.createSequentialGroup()
								.addContainerGap()
								.addGroup( gl_panelAnalyze.createParallelGroup( Alignment.LEADING )
										.addComponent( progressBar, GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE )
										.addComponent( chckbxContactImage, GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE )
										.addComponent( chckbxTrackLabels, GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE )
										.addComponent( chckbxGenerateLabelImage, GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE )
										.addComponent( chckbxGenerateContactsMask, GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE )
										.addComponent( btnGo, Alignment.TRAILING ) )
								.addContainerGap() )
				);
		gl_panelAnalyze.setVerticalGroup(
				gl_panelAnalyze.createParallelGroup( Alignment.LEADING )
						.addGroup( gl_panelAnalyze.createSequentialGroup()
								.addContainerGap()
								.addComponent( chckbxContactImage )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( chckbxGenerateContactsMask )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( chckbxGenerateLabelImage )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( chckbxTrackLabels )
								.addPreferredGap( ComponentPlacement.RELATED, 278, Short.MAX_VALUE )
								.addComponent( btnGo, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE )
								.addPreferredGap( ComponentPlacement.RELATED )
								.addComponent( progressBar, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE )
								.addContainerGap() )
				);
		panelAnalyze.setLayout( gl_panelAnalyze );
	}

	public Map< String, Object > getSettings()
	{
		return configPanel.getSettings();
	}

	public void setProgress( final double d )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				progressBar.setValue( ( int ) ( 100 * d ) );
			}
		} );
	}
}
