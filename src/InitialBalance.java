/*
 * This is a MotiveWave version of the Initial Balance Wave Map indicator as used by Trader Drysdale
 * as part of the VWAP Wave system.
 * Find out more about the VWAP Wave system here:
 * https://traderdrysdale.com/
 * https://www.tradingview.com/script/YT6IrC8d-Initial-Balance-Wave-Map/
 * https://youtu.be/4PEsb7O5xsA?si=TBeQW5Vt85B_u5E0
 * https://www.instagram.com/traderdrysdale/?hl=en
 */

import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@StudyHeader(
        namespace="gambcl.motivewave",
        id="INITIAL_BALANCE",
        name="Initial Balance Wave Map",
        desc="Displays the specified Initial Balance region and levels.",
        menu="Custom",
        overlay=true,
        studyOverlay=false,
        requiresBarUpdates=true,
        underlayByDefault=true)
public class InitialBalance extends Study
{
    class InitialBalanceRegion extends Figure
    {
        final private Instrument _instrument;
        final private long _startTime;
        final private long _endTime;
        private Double _high = null;
        private Double _low = null;
        private boolean _showRange;

        private boolean _isConfirmed;

        private long _rangeEndTime;

        public InitialBalanceRegion(Instrument instrument, long startTime, long endTime)
        {
            _instrument = instrument;
            _startTime = startTime;
            _endTime = endTime;
            _rangeEndTime = endTime;
            _showRange = false;
            _isConfirmed = false;
        }

        public long getStartTime()
        {
            return _startTime;
        }

        public long getEndTime()
        {
            return _endTime;
        }

        public Double getHigh()
        {
            return _high;
        }

        public void setHigh(Double high)
        {
            _high = high;
        }

        public Double getMid()
        {
            if (_high != null && _low != null)
                return _instrument.round((_high + _low) / 2.0);
            return null;
        }

        public Double getLow()
        {
            return _low;
        }

        public void setLow(Double low)
        {
            _low = low;
        }

        public boolean isShowRange()
        {
            return _showRange;
        }

        public void setShowRange(boolean showRange)
        {
            _showRange = showRange;
        }

        public boolean isUpdated()
        {
            return (_high != null && _low != null);
        }

        public boolean isConfirmed()
        {
            return _isConfirmed;
        }

        public void setIsConfirmed(boolean isConfirmed)
        {
            if (!isUpdated())
                return;

            _isConfirmed = isConfirmed;

            // Set PopUp message, now that IB values are confirmed.
            if (isUpdated())
                setPopupMessage("IB High: " + format(_high) + "\nIB Mid: " + format(getMid()) + "\nIB Low: " + format(_low) + "\nIB 𝚫: " + format(_high - _low));
        }

        public long getRangeEndTime()
        {
            return _rangeEndTime;
        }

        public void setRangeEndTime(long rangeEndTime)
        {
            _rangeEndTime = rangeEndTime;
        }

        public boolean isTimeInside(long time)
        {
            return (_startTime <= time) && (time < _endTime);
        }

        @Override
        public boolean isVisible(DrawContext ctx)
        {
            var bounds = ctx.getBounds();
            int leftX = ctx.translateTime(_startTime);
            int rangeEndX = ctx.translateTime(_rangeEndTime);

            if (leftX > bounds.getMaxX())
                return false;
            if (rangeEndX < 0)
                return false;

            return true;
        }

        @Override
        public boolean contains(double x, double y, DrawContext ctx)
        {
            if (isUpdated())
            {
                var settings = getSettings();
                var ibHighLine = settings.getPath(IB_HIGH_LINE);
                var ibMidLine = settings.getPath(IB_MID_LINE);
                var ibLowLine = settings.getPath(IB_LOW_LINE);
                int leftX = ctx.translateTime(_startTime);
                int rangeEndX = ctx.translateTime(_rangeEndTime);

                /* NOTE: Detecting mouse over IB region prevents mousing over bars.
                int rightX = ctx.translateTime(_endTime);
                var rangeFill = settings.getColorInfo(RANGE_FILL);
                var showDeveloping = settings.getBoolean(SHOW_DEVELOPING_INITIAL_BALANCE);
                // Detect mouse over the IB region (if shown).
                if (_isConfirmed || showDeveloping)
                {
                    // Draw region.
                    if (rangeFill != null && rangeFill.isEnabled())
                    {
                        var topY = ctx.translateValue(_high);
                        var bottomY = ctx.translateValue(_low);
                        if (x >= leftX && x <= rightX && y >= topY && y <= bottomY)
                            return ;
                    }
                }*/

                // Detect mouse over the lines (if shown)
                if (_isConfirmed && x >= leftX && x <= rangeEndX)
                {
                    if (ibHighLine != null && ibHighLine.isEnabled())
                    {
                        var lineY = ctx.translateValue(_high);
                        if (Math.abs(y - lineY) <= (2 * ctx.getTickHeight()))
                            return true;
                    }
                    if (ibMidLine != null && ibMidLine.isEnabled())
                    {
                        var lineY = ctx.translateValue(getMid());
                        if (Math.abs(y - lineY) <= (2 * ctx.getTickHeight()))
                            return true;
                    }
                    if (ibLowLine != null && ibLowLine.isEnabled())
                    {
                        var lineY = ctx.translateValue(_low);
                        if (Math.abs(y - lineY) <= (2 * ctx.getTickHeight()))
                            return true;
                    }
                }
            }
            return false;
        }

        public boolean updateRange(double low, double high)
        {
            boolean updated = false;

            if (low < _low)
            {
                _low = low;
                updated = true;
            }

            if (high > _high)
            {
                _high = high;
                updated = true;
            }

            return updated;
        }

        @Override
        public void draw(Graphics2D gc, DrawContext ctx)
        {
            var bounds = ctx.getBounds();
            int leftX = ctx.translateTime(_startTime);

            if (leftX >= (int)bounds.getMaxX())
                return; // Not visible

            int rightX = ctx.translateTime(_endTime);

            int rangeEndX = ctx.translateTime(_rangeEndTime);
            if (rangeEndX < 0)
                return; // Not visible

            var settings = getSettings();
            var timeframeFill = settings.getColorInfo(TIMEFRAME_FILL);
            var rangeFill = settings.getColorInfo(RANGE_FILL);
            var showDeveloping = settings.getBoolean(SHOW_DEVELOPING_INITIAL_BALANCE);
            var ibHighLine = settings.getPath(IB_HIGH_LINE);
            var ibMidLine = settings.getPath(IB_MID_LINE);
            var ibLowLine = settings.getPath(IB_LOW_LINE);
            var numExtLevels = settings.getInteger(NUM_EXTENSION_LEVELS);
            var ibHighExtLine = settings.getPath(IB_HIGH_EXT_LINE);
            var ibMidExtLine = settings.getPath(IB_MID_EXT_LINE);
            var ibLowExtLine = settings.getPath(IB_LOW_EXT_LINE);
            var font = settings.getFont(LBL_FONT);
            var align = settings.getString(LBL_ALIGN);

            if (timeframeFill != null && timeframeFill.isEnabled())
            {
                // Draw timeframe.
                var fill = timeframeFill.getColor();
                gc.setColor(fill);
                gc.fillRect(leftX, bounds.y,  rightX - leftX, bounds.height);
            }

            if (_low != null && _high != null)
            {
                // If enabled, allow region to draw while IB is developing.
                if (_isConfirmed || showDeveloping)
                {
                    // Draw region.
                    if (rangeFill != null && rangeFill.isEnabled())
                    {
                        int currentTimeX = ctx.translateTime(ctx.getDataContext().getCurrentTime());
                        var topY = ctx.translateValue(_high);
                        var bottomY = ctx.translateValue(_low);
                        var fill = rangeFill.getColor();
                        gc.setColor(fill);
                        gc.fillRect(leftX, topY,  Math.min(rightX, currentTimeX) - leftX, bottomY - topY);
                    }
                }

                // Only draw lines once IB is confirmed.
                if (_isConfirmed)
                {
                    // Draw IBH/IBL lines.
                    if (ibHighLine != null && ibHighLine.isEnabled())
                    {
                        drawLine(gc, ctx, ibHighLine, font, align, _high, "IB High: ");
                    }
                    if (ibMidLine != null && ibMidLine.isEnabled())
                    {
                        drawLine(gc, ctx, ibMidLine, font, align, round((_low + _high) / 2.0), "IB Mid: ");
                    }
                    if (ibLowLine != null && ibLowLine.isEnabled())
                    {
                        drawLine(gc, ctx, ibLowLine, font, align, _low, "IB Low: ");
                    }

                    // Draw IBH/IBL lines.
                    if (numExtLevels > 0)
                    {
                        double ibPriceRange = _high - _low;
                        double ibHalfPriceRange = ibPriceRange / 2.0;
                        for (int i = 0; i < numExtLevels; i++)
                        {
                            // Draw high extension line
                            if (ibHighExtLine != null && ibHighExtLine.isEnabled())
                            {
                                drawLine(gc, ctx, ibHighExtLine, font, align, _high + (i + 1) * ibPriceRange, "IB High + " + (i+1) + "xIB𝚫: ");

                                // Draw high extension mid line
                                if (ibMidExtLine != null && ibMidExtLine.isEnabled())
                                {
                                    drawLine(gc, ctx, ibMidExtLine, font, align, _high + (i * ibPriceRange) + ibHalfPriceRange, "IB High + " + (i>0?i:"") + "½xIB𝚫: ");
                                }
                            }

                            // Draw low extension line
                            if (ibLowExtLine != null && ibLowExtLine.isEnabled())
                            {
                                drawLine(gc, ctx, ibLowExtLine, font, align, _low - (i + 1) * ibPriceRange, "IB Low - " + (i+1) + "xIB𝚫: ");

                                // Draw low extension mid line
                                if (ibMidExtLine != null && ibMidExtLine.isEnabled())
                                {
                                    drawLine(gc, ctx, ibMidExtLine, font, align,  _low - (i * ibPriceRange) - ibHalfPriceRange, "IB Low - " + (i>0?i:"") + "½xIB𝚫: ");
                                }
                            }
                        }
                    }
                }
            }
        }

        protected void drawLine(Graphics2D gc, DrawContext ctx, PathInfo path, FontInfo font, String align, double value, String prefix)
        {
            if (!path.isEnabled() || value == Float.MAX_VALUE || value == Float.MIN_VALUE)
                return;

            var y = ctx.translateValue(value);

            if (font != null && font.isEnabled())
            {
                int x = ctx.translateTime(_startTime);
                int rx = ctx.translateTime(_rangeEndTime);
                var gb = ctx.getBounds();
                if (x < gb.x)
                    x = gb.x;
                int x2 = Math.min(rx, (int) gb.getMaxX());

                // Draw label + line
                var color = path.getColor();
                var fnt = font.getFont();
                String valFmt = ctx.format(value);

                String lbl = prefix + valFmt;
                if (path.isShowTag())
                {
                    if (path.getTagFont() != null)
                        fnt = path.getTagFont();
                    lbl = path.getTag();
                    if (lbl == null)
                        lbl = "";
                    if (path.isShowTagValue())
                        lbl += " " + valFmt;
                    if (path.getTagTextColor() != null)
                        color = path.getTagTextColor();
                }

                gc.setFont(fnt);
                gc.setColor(color);
                gc.setStroke(ctx.isSelected() ? path.getSelectedStroke() : path.getStroke());
                var fm = gc.getFontMetrics();
                int w = fm.stringWidth(lbl);
                switch(align)
                {
                    case RIGHT:
                        gc.drawLine(x, y, x2-w-5, y);
                        gc.drawString(lbl, x2 - w, y+fm.getAscent()/2);
                        break;
                    case LEFT:
                        if (x2 - x < w + 5)
                            gc.drawLine(x, y, x2, y);
                        else
                        {
                            gc.drawLine(x+w+5, y, x2, y);
                            gc.drawString(lbl, x, y+fm.getAscent()/2);
                        }
                        break;
                    case MIDDLE:
                        int cx = (x + x2)/2;
                        if (cx < x + 5)
                            gc.drawLine(x, y, x2, y);
                        else
                        {
                            gc.drawLine(x, y, cx-w/2 - 2, y);
                            gc.drawString(lbl, cx-w/2, y+fm.getAscent()/2);
                            gc.drawLine(cx+w/2+2, y, x2, y);
                        }
                        break;
                    default:
                }
            }
            else
            {
                // Draw line
                int leftX = ctx.translateTime(_startTime);
                int rangeEndX = ctx.translateTime(_rangeEndTime);
                gc.setColor(path.getColor());
                gc.setStroke(ctx.isSelected() ? path.getSelectedStroke() : path.getStroke());
                gc.drawLine(leftX, y, rangeEndX, y);
            }
        }
    }

    final static String TIMEFRAME = "timeframe";
    final static String TIMEFRAME_FILL = "timeframeFill";
    final static String RANGE_FILL = "rangeFill";
    final static String SHOW_DEVELOPING_INITIAL_BALANCE = "showDevelopingInitialBalance";
    final static String IB_HIGH_LINE = "ibHighLine";
    final static String IB_MID_LINE = "ibMidLine";
    final static String IB_LOW_LINE = "ibLowLine";
    final static String NUM_EXTENSION_LEVELS = "numExtensionLevels";
    final static String IB_HIGH_EXT_LINE = "ibHighExtLine";
    final static String IB_MID_EXT_LINE = "ibMidExtLine";
    final static String IB_LOW_EXT_LINE = "ibLowExtLine";
    final static String LBL_FONT = "lblFont";
    final static String LBL_ALIGN = "lblAlign";
    final static String IB_HIGH_INDICATOR = "ibHighIndicator";
    final static String IB_MID_INDICATOR = "ibMidIndicator";
    final static String IB_LOW_INDICATOR = "ibLowIndicator";
    final static String MINUTE_BARS = "minuteBars";
    final static String LEFT="L", RIGHT="R", MIDDLE="M";
    final static int MAX_EXTENSION_LEVELS = 3;

    private final ArrayList<InitialBalanceRegion> _regions = new ArrayList<>();

    enum Values
    {
        IB_HIGH,
        IB_MID,
        IB_LOW
    }

    @Override
    public void initialize(Defaults defaults)
    {
        List<NVP> aligns = new ArrayList();
        aligns.add(new NVP("Left", LEFT));
        aligns.add(new NVP("Middle", MIDDLE));
        aligns.add(new NVP("Right", RIGHT));

        var sd = createSD();
        var tabGeneral = sd.addTab("General");
        var tabLines = sd.addTab("Lines");

        var grpInputs = tabGeneral.addGroup("Inputs");
        grpInputs.addRow(new TimeFrameDescriptor(TIMEFRAME, "Timeframe", (int)((9*Util.MILLIS_IN_HOUR) + (Util.MILLIS_IN_HOUR / 2)), (int)((10*Util.MILLIS_IN_HOUR) + (Util.MILLIS_IN_HOUR / 2)), true, false));
        grpInputs.addRow(new BooleanDescriptor(SHOW_DEVELOPING_INITIAL_BALANCE, "Show Developing Initial Balance", true));

        var grpRegions = tabGeneral.addGroup("Regions");
        grpRegions.addRow(new ColorDescriptor(TIMEFRAME_FILL, "Highlight Timeframe", Util.getAlphaFill(defaults.getPurple()), true, true));
        grpRegions.addRow(new ColorDescriptor(RANGE_FILL, "Highlight Range", Util.getAlphaFill(defaults.getYellow()), true, true));

        var grpLines = tabLines.addGroup("Lines");
        var pathIBHigh = new PathDescriptor(IB_HIGH_LINE, "IB High", defaults.getGreen(), 2.0f, null, true, false, true);
        pathIBHigh.setSupportsAdvancedPanel(false);
        grpLines.addRow(pathIBHigh);
        var pathIBMid = new PathDescriptor(IB_MID_LINE, "IB Mid", defaults.getOrange(), 1.0f,  new float[] {4, 4}, true, false, true);
        pathIBMid.setSupportsAdvancedPanel(false);
        grpLines.addRow(pathIBMid);
        var pathIBLow = new PathDescriptor(IB_LOW_LINE, "IB Low", defaults.getRed(), 2.0f, null, true, false, true);
        pathIBLow.setSupportsAdvancedPanel(false);
        grpLines.addRow(pathIBLow);

        var grpExtLines = tabLines.addGroup("Extension Lines");
        grpExtLines.addRow(new IntegerDescriptor(NUM_EXTENSION_LEVELS, "Show Extension Levels", 2, 0, MAX_EXTENSION_LEVELS, 1));
        var pathIBHighExt = new PathDescriptor(IB_HIGH_EXT_LINE, "IB High Extension", defaults.getGreen(), 2.0f, new float[] {9, 4}, true, false, true);
        pathIBHighExt.setSupportsAdvancedPanel(false);
        grpExtLines.addRow(pathIBHighExt);
        var pathIBMidExt = new PathDescriptor(IB_MID_EXT_LINE, "IB Mid Extension", defaults.getOrange(), 1.0f,  new float[] {4, 4}, true, false, true);
        pathIBMidExt.setSupportsAdvancedPanel(false);
        grpExtLines.addRow(pathIBMidExt);
        var pathIBLowExt = new PathDescriptor(IB_LOW_EXT_LINE, "IB Low Extension", defaults.getRed(), 2.0f, new float[] {9, 4}, true, false, true);
        pathIBLowExt.setSupportsAdvancedPanel(false);
        grpExtLines.addRow(pathIBLowExt);

        // Only enable the extension settings if NUM_EXTENSION_LEVELS is > 0.
        sd.addDependency(new ValueDependency(NUM_EXTENSION_LEVELS,
                                                Arrays.asList(new String[]{IB_HIGH_EXT_LINE, IB_MID_EXT_LINE, IB_LOW_EXT_LINE}),
                                                IntStream.rangeClosed(1, MAX_EXTENSION_LEVELS)
                                                    .boxed()
                                                    .collect(Collectors.toList())));

        var grpLabels = tabLines.addGroup("Labels");
        grpLabels.addRow(new FontDescriptor(LBL_FONT, "Font", defaults.getFont(), Color.black, false, true, true));
        grpLabels.addRow(new DiscreteDescriptor(LBL_ALIGN, "Align", RIGHT, aligns));

        var grpIndicators = tabGeneral.addGroup("Indicators");
        grpIndicators.addRow(new IndicatorDescriptor(IB_HIGH_INDICATOR, "IB High", defaults.getGreen(), Color.white, false, false, true));
        grpIndicators.addRow(new IndicatorDescriptor(IB_MID_INDICATOR, "IB Mid", defaults.getOrange(), Color.white, false, false, true));
        grpIndicators.addRow(new IndicatorDescriptor(IB_LOW_INDICATOR, "IB Low", defaults.getRed(), Color.white, false, false, true));

        // Required invisible input to declare other bar sizes.
        sd.addInvisibleSetting(new BarSizeDescriptor(MINUTE_BARS, "MINUTE", BarSize.getBarSize(Enums.BarSizeType.LINEAR, Enums.IntervalType.MINUTE, 1)));

        sd.addQuickSettings(TIMEFRAME, TIMEFRAME_FILL, RANGE_FILL, SHOW_DEVELOPING_INITIAL_BALANCE, NUM_EXTENSION_LEVELS);

        var rd = createRD();
        rd.setLabelSettings(TIMEFRAME);
        rd.setLabelPrefix("Initial Balance");

        rd.exportValue(new ValueDescriptor(Values.IB_HIGH, "IB High", new String[] {TIMEFRAME}));
        rd.exportValue(new ValueDescriptor(Values.IB_MID, "IB Mid", new String[] {TIMEFRAME}));
        rd.exportValue(new ValueDescriptor(Values.IB_LOW, "IB Low", new String[] {TIMEFRAME}));

        rd.declareIndicator(Values.IB_HIGH, IB_HIGH_INDICATOR);
        rd.declareIndicator(Values.IB_MID, IB_MID_INDICATOR);
        rd.declareIndicator(Values.IB_LOW, IB_LOW_INDICATOR);
    }

    @Override
    protected void calculateValues(DataContext ctx)
    {
        // Clear old regions.
        clearFigures();
        _regions.clear();

        // Nothing to do if we are not on an intraday chart.
        var series = ctx.getDataSeries();
        if (!series.getBarSize().isIntraday())
            return;

        // Get settings.
        var settings = getSettings();
        var tf = settings.getTimeFrame(TIMEFRAME);
        var highlightTimeframe = settings.getColorInfo(TIMEFRAME_FILL);

        var instr = ctx.getInstrument();
        var now = ctx.getCurrentTime();
        // Use 1-minute bars to build IB regions.
        var minuteSeries = ctx.getDataSeries(BarSize.getBarSize(Enums.BarSizeType.LINEAR, Enums.IntervalType.MINUTE, 1));
        long regionsFirstDay = Util.getMidnight(minuteSeries.getStartTime(0), ctx.getTimeZone());
        long regionsLastDay = Util.getMidnight(now + (15 * Util.MILLIS_IN_DAY), ctx.getTimeZone());

        long day = regionsFirstDay;
        InitialBalanceRegion prevRegion = null;
        while (day <  regionsLastDay)
        {
            long day2 = day;
            if (tf.getStartTime() > tf.getEndTime())
                day2 = Util.getNextDayMidnight(day, ctx.getTimeZone());
            long nextDay = Util.getNextDayMidnight(day, ctx.getTimeZone());

            long regionStartTime = day + tf.getStartTime();
            long regionEndTime = day2 + tf.getEndTime();

            var region = new InitialBalanceRegion(ctx.getInstrument(), regionStartTime, regionEndTime);
            _regions.add(region);
            addFigure(region);

            if (now > regionStartTime)
            {
                int si = minuteSeries.findIndex(regionStartTime);
                int ei = minuteSeries.findIndex(regionEndTime - 100);

                // Make sure this is a valid range
                if (minuteSeries.getStartTime(si) > regionEndTime || minuteSeries.getStartTime(ei) < regionStartTime) {
                    day = nextDay;
                    //debug("calculateValues: Skipping region " + Util.formatYYYYMMMDDHHSSMMM(regionStartTime, ctx.getTimeZone()));
                    continue;
                }

                Double h = minuteSeries.highest(ei, ei-si+1, Enums.BarInput.HIGH);
                Double l = minuteSeries.lowest(ei, ei-si+1, Enums.BarInput.LOW);

                if (h != null && l != null)
                {
                    region.setHigh(h);
                    region.setLow(l);
                }
                if (now >= regionEndTime)
                {
                    region.setIsConfirmed(true);
                    // Now that IB is confirmed, update bar values.
                    updateBarValues(series, region);
                }
            }

            if (prevRegion != null)
            {
                prevRegion.setRangeEndTime(region.getStartTime());
                updateBarValues(series, prevRegion);
            }
            day = nextDay;
            prevRegion = region;
        }

        //dumpRegions("calculateValues");
    }

    @Override
    public void onBarUpdate(DataContext ctx)
    {
        // Find the currently developing IB region.
        var now = ctx.getCurrentTime();
        var region = findLatestInitialBalanceRegion(now);
        if (region != null)
        {
            //debug("Found developing region for " + Util.formatYYYYMMMDDHHSSMMM(now));
            //dumpRegion(region, "onBarUpdate[BEFORE]");
            var series = ctx.getDataSeries();
            if (region.updateRange(series.getLow(), series.getHigh()))
            {
                //dumpRegion(region, "onBarUpdate[AFTER]");
            }
        }
    }

    private InitialBalanceRegion findLatestInitialBalanceRegion(long time)
    {
        for (int i = _regions.size() - 1; i >= 0; i--)
        {
            var region = _regions.get(i);
            if (region.isTimeInside(time))
                return region;
        }
        return null;
    }

    private void updateBarValues(DataSeries series, InitialBalanceRegion region)
    {
        if (series == null || region == null)
            return;
        if (!region.isUpdated())
            return;
        if (!region.isConfirmed())
            return;

        for (int i = series.size() - 1; i >= 0; i--)
        {
            var barStartTime = series.getStartTime(i);
            var barEndTime = series.getEndTime(i);

            if (barStartTime >= region.getStartTime() && barStartTime < region.getRangeEndTime())
            {
                if (series.isComplete(i))
                    break;

                series.setDouble(i, Values.IB_HIGH, region.getHigh());
                series.setDouble(i, Values.IB_MID, region.getMid());
                series.setDouble(i, Values.IB_LOW, region.getLow());
                series.setComplete(i);
            }
            else if (barEndTime <= region.getStartTime())
                break;
        }
    }

    private void dumpRegions(String prefix)
    {
        for (var region : _regions)
        {
            dumpRegion(region, prefix);
        }
    }

    private void dumpRegion(InitialBalanceRegion region, String prefix)
    {
        debug((prefix != null ? prefix + " " : "") + "REGION: " + Util.formatYYYYMMMDDHHSSMMM(region.getStartTime()) + ", updated=" + region.isUpdated() + ", confirmed=" + region.isConfirmed() + ", high=" + (region.isUpdated() ? region.getHigh() : "null") + ", low=" + (region.isUpdated() ? region.getLow() : "null"));
    }
}
