package de.embl.cba.mobie2.display;

import de.embl.cba.mobie2.color.ColoringModelWrapper;
import de.embl.cba.mobie2.view.ScatterPlotViewer;
import de.embl.cba.mobie2.view.TableViewer;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRowImageSegment;

import java.util.List;

public class SegmentationDisplay extends SourceDisplay
{
	private final double alpha;
	private final String lut;

	// TODO: rework according to ImageDisplay
	public transient SelectionModel< TableRowImageSegment > selectionModel;
	public transient ColoringModelWrapper< TableRowImageSegment > coloringModel;
	public transient TableViewer< TableRowImageSegment > tableViewer;
	public transient ScatterPlotViewer< TableRowImageSegment > scatterPlotViewer;
	public transient List< TableRowImageSegment > segments;

	public SegmentationDisplay( String name, List< String > sources, double alpha, String lut )
	{
		super( name, sources );
		this.alpha = alpha;
		this.lut = lut;
	}

	public double getAlpha()
	{
		return alpha;
	}

	public String getLut()
	{
		return lut;
	}
}