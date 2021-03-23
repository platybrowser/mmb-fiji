package de.embl.cba.mobie2.view;

import bdv.util.BdvHandle;
import bdv.util.BdvOptions;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import de.embl.cba.bdv.utils.BdvUtils;
import de.embl.cba.mobie.n5.source.LabelSource;
import de.embl.cba.mobie2.*;
import de.embl.cba.mobie2.color.MoBIEColoringModel;
import de.embl.cba.mobie2.color.LabelConverter;
import de.embl.cba.mobie2.select.SelectionModelAndLabelAdapter;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.ColoringListener;
import de.embl.cba.tables.imagesegment.ImageSegment;
import de.embl.cba.tables.imagesegment.LabelFrameAndImage;
import de.embl.cba.tables.select.SelectionListener;
import de.embl.cba.tables.select.SelectionModel;
import mpicbg.spim.data.SpimData;
import net.imglib2.RealPoint;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;
import sc.fiji.bdvpg.bdv.BdvCreator;
import sc.fiji.bdvpg.bdv.BdvHandleHelper;
import sc.fiji.bdvpg.bdv.navigate.ViewerTransformChanger;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.sourceandconverter.display.ColorChanger;

import java.util.*;
import java.util.stream.Collectors;

public class ImageViewer< T extends ImageSegment > implements ColoringListener, SelectionListener< T >
{
	private final MoBIE2 moBIE2;
	private final SourceAndConverterBdvDisplayService displayService;
	private final BdvHandle bdvHandle;
	private final boolean is2D;
	private ArrayList< SourceAndConverter > labelSources;
	private List< SelectionModelAndLabelAdapter< T > > selectionModels;

	public ImageViewer( MoBIE2 moBIE2, boolean is2D )
	{
		this.moBIE2 = moBIE2;
		this.is2D = is2D;
		displayService = SourceAndConverterServices.getSourceAndConverterDisplayService();
		bdvHandle = createBdv();
		installBehaviours( bdvHandle );
		labelSources = new ArrayList<>();
		selectionModels = new ArrayList<>();
	}

	private void installBehaviours( BdvHandle bdvHandle )
	{
		Behaviours behaviours = new Behaviours( new InputTriggerConfig() );
		behaviours.install( bdvHandle.getBdvHandle().getTriggerbindings(), "MoBIE" );
		addSelectionBehaviour( behaviours );
	}

	private void addSelectionBehaviour( Behaviours behaviours )
	{
		behaviours.behaviour(
				( ClickBehaviour ) ( x, y ) ->
						new Thread( () -> toggleSelectionAtMousePosition() ).start(),
				"-toggle-select", "ctrl button1" ); ;
	}

	private synchronized void toggleSelectionAtMousePosition()
	{
		// TODO: Replace by function
		final RealPoint globalMouseCoordinates = BdvUtils.getGlobalMouseCoordinates( bdvHandle );
		List< SourceAndConverter< ? > > sources = bdvHandle.getViewerPanel().state().getSources();
		final int timepoint = bdvHandle.getViewerPanel().state().getCurrentTimepoint();

		Set< SourceAndConverter< ? > > sourcesAtMousePosition = sources.stream()
				.filter( source -> SourceAndConverterHelper.isPositionWithinSourceInterval( source, globalMouseCoordinates, timepoint, is2D ) )
				.collect( Collectors.toSet() );

		for ( SourceAndConverter< ? > sourceAndConverter : sourcesAtMousePosition )
		{
			if ( labelSources.contains( sourceAndConverter ) )
			{
				Source< ? > source = sourceAndConverter.getSpimSource();
				if ( source instanceof LabelSource )
					source = ( ( LabelSource ) source ).getWrappedSource();

				final Double labelIndex = BdvUtils.getPixelValue( source, globalMouseCoordinates, timepoint );

				if ( labelIndex == 0 ) return;

				final LabelFrameAndImage labelFrameAndImage = new LabelFrameAndImage( labelIndex, timepoint, source.getName() );

				for ( SelectionModelAndLabelAdapter< T > modelAndLabelAdapter : selectionModels )
				{
					final SelectionModel< T > selectionModel = modelAndLabelAdapter.getSelectionModel();
					final HashMap< LabelFrameAndImage, T > adapter = modelAndLabelAdapter.getAdapter();

					if ( adapter.containsKey( labelFrameAndImage ) )
					{
						final T segment = adapter.get( labelFrameAndImage );
						selectionModel.toggle( segment );
						if ( selectionModel.isSelected( segment ) )
						{
							selectionModel.focus( segment );
						}
					}
				}
			}
		}
	}



	private BdvHandle createBdv()
	{
		BdvCreator creator = new BdvCreator( BdvOptions.options(), true, 1 );
		creator.run();
		return creator.get();
	}

	public List< SourceAndConverter< ? > > show( ImageDisplay imageDisplays )
	{
		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : imageDisplays.sources )
		{
			final ImageSource source = moBIE2.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE2.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			new ColorChanger( sourceAndConverter, ColorUtils.getARGBType(  imageDisplays.color ) ).run();

			sourceAndConverters.add( sourceAndConverter );

			displayService.show( bdvHandle, sourceAndConverter );

			if ( imageDisplays.contrastLimits != null )
			{
				displayService.getConverterSetup( sourceAndConverter ).setDisplayRange( imageDisplays.contrastLimits[ 0 ], imageDisplays.contrastLimits[ 1 ] );
			}
			else
			{
				// TODO: auto adjust contrast? may be expensive...
			}
		}

		return sourceAndConverters;
	}

	public List< SourceAndConverter< ? > > show( SegmentationDisplay segmentationDisplay, MoBIEColoringModel coloringModel, SelectionModelAndLabelAdapter selectionModelAndAdapter )
	{
		final ArrayList< SourceAndConverter< ? > > sourceAndConverters = new ArrayList<>();

		for ( String sourceName : segmentationDisplay.sources )
		{
			final SegmentationSource source = ( SegmentationSource ) moBIE2.getSource( sourceName );
			final SpimData spimData = BdvUtils.openSpimData( moBIE2.getAbsoluteImageLocation( source ) );
			final SourceAndConverter sourceAndConverter = SourceAndConverterHelper.createSourceAndConverters( spimData ).get( 0 );

			LabelConverter labelConverter = new LabelConverter(
					selectionModelAndAdapter.getAdapter(),
					sourceName,
					coloringModel );

			SourceAndConverter labelSourceAndConverter = asLabelSourceAndConverter( sourceAndConverter, labelConverter );

			sourceAndConverters.add( labelSourceAndConverter );

			displayService.show( bdvHandle, labelSourceAndConverter );

			// TODO: think about the alpha!
		}

		labelSources.addAll( sourceAndConverters );
		selectionModels.add( selectionModelAndAdapter );

		return sourceAndConverters;
	}

	private SourceAndConverter asLabelSourceAndConverter( SourceAndConverter sourceAndConverter, LabelConverter labelConverter )
	{
		LabelSource volatileLabelSource = new LabelSource( sourceAndConverter.asVolatile().getSpimSource() );
		SourceAndConverter volatileSourceAndConverter = new SourceAndConverter( volatileLabelSource, labelConverter );
		LabelSource labelSource = new LabelSource( sourceAndConverter.getSpimSource() );
		return new SourceAndConverter( labelSource, labelConverter, volatileSourceAndConverter );
	}

	@Override
	public synchronized void coloringChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void selectionChanged()
	{
		bdvHandle.getViewerPanel().requestRepaint();
	}

	@Override
	public synchronized void focusEvent( T selection )
	{
		final double[] position = new double[ 3 ];
		selection.localize( position );

		new ViewerTransformChanger(
				bdvHandle,
				BdvHandleHelper.getViewerTransformWithNewCenter( bdvHandle, position ),
				false,
				500 ).run();
	}

	public BdvHandle getBdvHandle()
	{
		return bdvHandle;
	}
}