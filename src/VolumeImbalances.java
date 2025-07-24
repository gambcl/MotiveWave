import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.MarkerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.Marker;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.*;
import java.util.ArrayList;

@StudyHeader(
        namespace="gambcl.motivewave",
        id="VOLUME_IMBALANCES",
        name="Volume Imbalances",
        desc="Displays volume imbalances, identified by candle gaps.",
        menu="Custom",
        overlay=true,
        studyOverlay=false,
        requiresBarUpdates=true)
public class VolumeImbalances extends Study
{
    enum Direction
    {
        Bullish,
        Bearish
    }

    class VolumeImbalance extends Figure
    {
        private long _startTime;
        private long _startBarIdx;
        private Direction _direction;
        private double _high;
        private double _low;
        private boolean _filled;
        private Long _filledTime;
        private Long _filledBarIdx;
        private Marker _marker;
        private boolean _active;

        VolumeImbalance(long startTime, long startBarIdx, Direction direction, double high, double low)
        {
            _startTime = startTime;
            _startBarIdx = startBarIdx;
            _direction = direction;
            _high = high;
            _low = low;
            _filled = false;
            _filledTime = null;
            _filledBarIdx = null;
            _active = false;
        }

        public long getStartTime()
        {
            return _startTime;
        }

        public long getStartBarIdx()
        {
            return _startBarIdx;
        }

        public Direction getDirection()
        {
            return _direction;
        }

        public double getHigh()
        {
            return _high;
        }

        public double getLow()
        {
            return _low;
        }

        public boolean isFilled()
        {
            return _filled;
        }

        public void setFilled(long filledTime, long filledBarIdx)
        {
            _filled = true;
            _filledTime = filledTime;
            _filledBarIdx = filledBarIdx;
        }

        public Marker getMarker()
        {
            return _marker;
        }

        public void setMarker(Marker marker)
        {
            _marker = marker;
        }

        public boolean isActive()
        {
            return _active;
        }

        public void setActive(boolean active)
        {
            if (_active == active)
                return;

            if (!active)
            {
                setPopupMessage(null);
                removeFigure(this);
                if (_marker != null)
                    removeFigure(_marker);
            }
            else
            {
                setPopupMessage(_direction == Direction.Bullish ? "Bullish Volume Imbalance " + format(_high) : "Bearish Volume Imbalance " + format(_low));
                addFigure(this);
                if (_marker != null)
                    addFigure(_marker);
            }
            _active = active;
        }

        @Override
        public boolean contains(double x, double y, DrawContext ctx)
        {
            if (!isActive())
                return false;

            // Check x coordinate.
            var bounds = ctx.getBounds();
            var leftX = ctx.translateTime(_startTime);
            var rightX = _filledTime != null ? ctx.translateTime(_filledTime) : bounds.getMaxX();
            if (x < leftX || x > rightX)
                return false;

            // Check y coordinate.
            var settings = getSettings();
            var pathBullish = settings.getPath(SHOW_BULLISH);
            var pathBearish = settings.getPath(SHOW_BEARISH);
            if ((_direction == Direction.Bullish && pathBullish != null && pathBullish.isEnabled()) ||
                    (_direction == Direction.Bearish && pathBearish != null && pathBearish.isEnabled()))
            {
                var topY = ctx.translateValue(_high);
                var bottomY = ctx.translateValue(_low);
                if (y >= topY && y <= bottomY)
                    return true;
            }
            return false;
        }

        @Override
        public void draw(Graphics2D gc, DrawContext ctx)
        {
            var settings = ctx.getSettings();
            var bullishPath = settings.getPath(SHOW_BULLISH);
            var bearishPath = settings.getPath(SHOW_BEARISH);
            var bounds = ctx.getBounds();
            var leftX = ctx.translateTime(_startTime);
            var rightX = (int) (_filled ? ctx.translateTime(_filledTime) : bounds.getMaxX());

            if (_direction == Direction.Bullish && bullishPath != null)
            {
                var y = ctx.translateValue(_high);
                gc.setColor(bullishPath.getColor());
                gc.setStroke(ctx.isSelected() ? bullishPath.getSelectedStroke() : bullishPath.getStroke());
                gc.drawLine(leftX, y, rightX, y);
            }
            else if (_direction == Direction.Bearish && bearishPath != null)
            {
                var y = ctx.translateValue(_low);
                gc.setColor(bearishPath.getColor());
                gc.setStroke(ctx.isSelected() ? bearishPath.getSelectedStroke() : bearishPath.getStroke());
                gc.drawLine(leftX, y, rightX, y);
            }
        }
    }

    final static String SHOW_BULLISH = "showBullish";
    final static String SHOW_BEARISH = "showBearish";
    final static String BULLISH_MARKER = "bullishMarker";
    final static String BEARISH_MARKER = "bearishMarker";
    final static int MIN_TICKS = 1;
    final ArrayList<VolumeImbalance> _unfilled = new ArrayList<>();
    final ArrayList<VolumeImbalance> _filled = new ArrayList<>();
    long _prevStartTime = 0;
    double _prevHigh = Double.MIN_VALUE;
    double _prevLow = Double.MAX_VALUE;

    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var tabGeneral = sd.addTab("General");

        var grpInputs = tabGeneral.addGroup("Inputs");
        var pathBullish = new PathDescriptor(SHOW_BULLISH, "Show Bullish", Util.awtColor(0, 255, 255, 255), 4.0f, null, true, false, true);
        pathBullish.setSupportsAdvancedPanel(false);
        grpInputs.addRow(pathBullish);
        var pathBearish = new PathDescriptor(SHOW_BEARISH, "Show Bearish", Util.awtColor(238, 130, 238, 255), 4.0f, null, true, false, true);
        pathBearish.setSupportsAdvancedPanel(false);
        grpInputs.addRow(pathBearish);

        var grpMarkers = tabGeneral.addGroup("Markers");
        grpMarkers.addRow(new MarkerDescriptor(BULLISH_MARKER, "Bullish", Enums.MarkerType.TRIANGLE, Enums.Size.MEDIUM, Util.awtColor(0, 255, 255, 255), defaults.getLineColor(), true, true));
        grpMarkers.addRow(new MarkerDescriptor(BEARISH_MARKER, "Bearish", Enums.MarkerType.TRIANGLE, Enums.Size.MEDIUM, Util.awtColor(238, 130, 238, 255), defaults.getLineColor(), true, true));

        sd.addQuickSettings(SHOW_BULLISH, SHOW_BEARISH);

        var rd = createRD();
        rd.setLabelPrefix("Volume Imbalances");
    }

    @Override
    protected void calculateValues(DataContext ctx)
    {
        clearFigures();
        _unfilled.clear();
        _filled.clear();

        var series = ctx.getDataSeries();
        for (int currIdx = 0; currIdx < series.size(); currIdx++)
        {
            if (currIdx == 0)
                continue;

            detectFilledVolumeImbalances(ctx, currIdx, true, true);
            detectUnfilledVolumeImbalances(ctx, currIdx);
        }
    }

    @Override
    public void onBarUpdate(DataContext ctx)
    {
        var series = ctx.getDataSeries();
        int currIdx = series.size() - 1;
        boolean newBar = series.getStartTime(currIdx) != _prevStartTime;
        if (newBar)
        {
            _prevHigh = Double.MIN_VALUE;
            _prevLow = Double.MAX_VALUE;
        }
        boolean checkHigh = series.getHigh(currIdx) > _prevHigh;
        boolean checkLow = series.getLow(currIdx) < _prevLow;

        // Check for filling existing VolumeImbalance (on new bar, or when bar range has increased).
        if (newBar || checkHigh || checkLow)
        {
            detectFilledVolumeImbalances(ctx, currIdx, checkHigh, checkLow);
        }

        // Detect new VolumeImbalance.
        detectUnfilledVolumeImbalances(ctx, currIdx);

        _prevStartTime = series.getStartTime(currIdx);
        _prevHigh = series.getHigh(currIdx);
        _prevLow = series.getLow(currIdx);
    }

    private boolean isBullish(DataSeries series, int idx)
    {
        return series.getClose(idx) >= series.getOpen(idx);
    }

    private boolean isBearish(DataSeries series, int idx)
    {
        return !isBullish(series, idx);
    }

    private void detectFilledVolumeImbalances(DataContext ctx, int currIdx, boolean checkHigh, boolean checkLow)
    {
        var series = ctx.getDataSeries();
        ArrayList<VolumeImbalance> newlyFilled = new ArrayList<>();
        for (VolumeImbalance volumeImbalance : _unfilled)
        {
            if (currIdx <= volumeImbalance.getStartBarIdx() || volumeImbalance.isFilled() || !volumeImbalance.isActive())
                continue;

            if ((checkHigh && (series.getHigh(currIdx) >= volumeImbalance.getLow() && series.getLow(currIdx) < volumeImbalance.getLow())) ||
                    (checkLow && (series.getLow(currIdx) <= volumeImbalance.getHigh() && series.getHigh(currIdx) > volumeImbalance.getHigh())))
            {
                // Volume imbalance filled.
                volumeImbalance.setFilled(series.getStartTime(currIdx), currIdx);
                _filled.add(volumeImbalance);
                newlyFilled.add(volumeImbalance);
                // TODO: Signal volume imbalance filled at currIdx
                debug("VolumeImbalance filled at " + Util.formatYYYYMMMDDHHSSMMM(series.getStartTime(currIdx), ctx.getTimeZone()));
            }
        }
        _unfilled.removeAll(newlyFilled);
    }

    private void detectUnfilledVolumeImbalances(DataContext ctx, int currIdx)
    {
        var series = ctx.getDataSeries();
        var settings = getSettings();
        var bullishPath = settings.getPath(SHOW_BULLISH);
        var bearishPath = settings.getPath(SHOW_BEARISH);
        var bullishMarker = settings.getMarker(BULLISH_MARKER);
        var bearishMarker = settings.getMarker(BEARISH_MARKER);
        int prevIdx = currIdx - 1;
        var prevClose = series.getClose(prevIdx);
        var currOpen = series.getOpen(currIdx);
        var imbalanceTicks = (currOpen - prevClose) / series.getInstrument().getTickSize();
        var absImbalanceTicks = Math.abs(imbalanceTicks);
        VolumeImbalance volumeImbalance = null;
        boolean active = false;

        // Check to see if a VolumeImbalance has already been created for this bar.
        if (!_unfilled.isEmpty() && _unfilled.getLast().getStartBarIdx() == currIdx)
            volumeImbalance = _unfilled.getLast();

        if (bullishPath != null &&
                bullishPath.isEnabled() &&
                isBullish(series, prevIdx) &&
                isBullish(series, currIdx) &&
                (series.getOpen(currIdx) > series.getClose(prevIdx)) &&
                absImbalanceTicks >= MIN_TICKS)
        {
            // Bullish imbalance
            active = true;
            if (volumeImbalance == null)
            {
                volumeImbalance = new VolumeImbalance(series.getStartTime(currIdx),
                        currIdx,
                        Direction.Bullish,
                        currOpen,
                        prevClose);
                if (bullishMarker != null && bullishMarker.isEnabled())
                {
                    volumeImbalance.setMarker(new Marker(new Coordinate(series.getStartTime(currIdx), series.getLow(currIdx)), Enums.Position.BOTTOM, bullishMarker, "Bullish Volume Imbalance"));
                }
                _unfilled.add(volumeImbalance);
                // TODO: Signal new volume imbalance at currIdx
                debug("New bullish VolumeImbalance detected at " + Util.formatYYYYMMMDDHHSSMMM(series.getStartTime(currIdx), ctx.getTimeZone()));
            }
        }
        else if (bearishPath != null &&
                bearishPath.isEnabled() &&
                isBearish(series, prevIdx) &&
                isBearish(series, currIdx) &&
                (series.getOpen(currIdx) < series.getClose(prevIdx)) &&
                absImbalanceTicks >= MIN_TICKS)
        {
            // Bearish imbalance
            active = true;
            if (volumeImbalance == null)
            {
                volumeImbalance = new VolumeImbalance(series.getStartTime(currIdx),
                        currIdx,
                        Direction.Bearish,
                        prevClose,
                        currOpen);
                if (bearishMarker != null && bearishMarker.isEnabled())
                {
                    volumeImbalance.setMarker(new Marker(new Coordinate(series.getStartTime(currIdx), series.getHigh(currIdx)), Enums.Position.TOP, bearishMarker, "Bearish Volume Imbalance"));
                }
                _unfilled.add(volumeImbalance);
                // TODO: Signal new volume imbalance at currIdx
                debug("New bearish VolumeImbalance detected at " + Util.formatYYYYMMMDDHHSSMMM(series.getStartTime(currIdx), ctx.getTimeZone()));
            }
        }

        if (volumeImbalance != null)
            volumeImbalance.setActive(active);
    }
}
