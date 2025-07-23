import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.MarkerDescriptor;
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
                removeFigure(this);
                if (_marker != null)
                    removeFigure(_marker);
            }
            else
            {
                addFigure(this);
                if (_marker != null)
                    addFigure(_marker);
            }
            _active = active;
        }

        @Override
        public void draw(Graphics2D gc, DrawContext ctx)
        {
            var settings = ctx.getSettings();
            var bullishColor = settings.getColorInfo(SHOW_BULLISH);
            var bearishColor = settings.getColorInfo(SHOW_BEARISH);
            var bounds = ctx.getBounds();
            var leftX = ctx.translateTime(_startTime);
            var rightX = (int) (_filled ? ctx.translateTime(_filledTime) : bounds.getMaxX());

            if (_direction == Direction.Bullish && bullishColor != null)
            {
                var y = ctx.translateValue(_high);
                gc.setColor(bullishColor.getColor());
                gc.setStroke(new BasicStroke(4));
                gc.drawLine(leftX, y, rightX, y);
            }
            else if (_direction == Direction.Bearish && bearishColor != null)
            {
                var y = ctx.translateValue(_low);
                gc.setColor(bearishColor.getColor());
                gc.setStroke(new BasicStroke(4));
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
        grpInputs.addRow(new ColorDescriptor(SHOW_BULLISH, "Show Bullish", defaults.getGreen(), true, true));
        grpInputs.addRow(new ColorDescriptor(SHOW_BEARISH, "Show Bearish", defaults.getRed(), true, true));

        var grpMarkers = tabGeneral.addGroup("Markers");
        grpMarkers.addRow(new MarkerDescriptor(BULLISH_MARKER, "Bullish", Enums.MarkerType.TRIANGLE, Enums.Size.MEDIUM, defaults.getGreen(), defaults.getLineColor(), true, true));
        grpMarkers.addRow(new MarkerDescriptor(BEARISH_MARKER, "Bearish", Enums.MarkerType.TRIANGLE, Enums.Size.MEDIUM, defaults.getRed(), defaults.getLineColor(), true, true));

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
        var bullishColor = settings.getColorInfo(SHOW_BULLISH);
        var bearishColor = settings.getColorInfo(SHOW_BEARISH);
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

        if (bullishColor != null &&
                bullishColor.isEnabled() &&
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
        else if (bearishColor != null &&
                bearishColor.isEnabled() &&
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
