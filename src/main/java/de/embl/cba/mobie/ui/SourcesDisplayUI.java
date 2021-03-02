package de.embl.cba.mobie.ui;

import bdv.tools.brightness.ConverterSetup;
import de.embl.cba.bdv.utils.BdvDialogs;
import de.embl.cba.tables.image.SourceAndMetadata;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

@Deprecated
public class SourcesDisplayUI
{
	// TODO: move this stuff to the UserInterfacePanelsProvider
	public static JCheckBox createBigDataViewerVisibilityCheckbox(
			int[] dims,
			SourceAndMetadata< ? > sam,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "S" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( new Dimension( dims[ 0 ], dims[ 1 ] ) );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				if ( sam.metadata().bdvStackSource != null )
					sam.metadata().bdvStackSource.setActive( checkBox.isSelected() );
			}
		} );

		return checkBox;
	}

	public static JCheckBox createVolumeViewVisibilityCheckbox(
			SourcesDisplayManager sourcesDisplayManager,
			int[] dims,
			SourceAndMetadata< ? > sam,
			boolean isVisible )
	{
		JCheckBox checkBox = new JCheckBox( "V" );
		checkBox.setSelected( isVisible );
		checkBox.setPreferredSize( new Dimension( dims[ 0 ], dims[ 1 ] ) );

		checkBox.addActionListener( new ActionListener()
		{
			@Override
			public void actionPerformed( ActionEvent e )
			{
				new Thread( () -> {
					sam.metadata().showImageIn3d = checkBox.isSelected();
					sam.metadata().showSelectedSegmentsIn3d = checkBox.isSelected();
					sourcesDisplayManager.updateSegments3dView( sam, sourcesDisplayManager );
					sourcesDisplayManager.updateSource3dView( sam, sourcesDisplayManager, false );
				}).start();
			}
		} );

		return checkBox;
	}

	public static JButton createBrightnessButton( int[] buttonDimensions,
												  SourceAndMetadata< ? > sam,
												  final double rangeMin,
												  final double rangeMax )
	{
		JButton button = new JButton( "B" );
		button.setPreferredSize( new Dimension(
				buttonDimensions[ 0 ],
				buttonDimensions[ 1 ] ) );

		button.addActionListener( e ->
		{
			final List< ConverterSetup > converterSetups = sam.metadata().bdvStackSource.getConverterSetups();

			BdvDialogs.showBrightnessDialog(
					sam.metadata().displayName,
					converterSetups,
					rangeMin,
					rangeMax );

			// TODO: Can this be done for the content as well?
		} );

		return button;
	}
}
