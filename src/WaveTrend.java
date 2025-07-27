import com.motivewave.platform.sdk.common.*;
import com.motivewave.platform.sdk.common.desc.*;
import com.motivewave.platform.sdk.draw.Marker;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.*;

@StudyHeader(
        namespace="gambcl.motivewave",
        id="WAVE_TREND",
        name="Wave Trend",
        desc="Displays wave trend oscillator.",
        menu="Custom",
        overlay=false,
        studyOverlay=false)
public class WaveTrend extends Study
{
    enum Values {FAST_WAVE, SLOW_WAVE, WAVE_DELTA, ESA, TMP, CI}

    final static String FAST_WAVE_PATH = "fastWavePath";
    final static String SLOW_WAVE_PATH = "slowWavePath";
    final static String WAVE_DELTA_PATH = "waveDeltaPath";

    final static String FAST_WAVE_SHADE = "fastWaveShade";
    final static String SLOW_WAVE_SHADE = "slowWaveShade";
    final static String WAVE_DELTA_SHADE = "waveDeltaShade";

    final static String FAST_WAVE_INDICATOR = "fastWaveIndicator";
    final static String SLOW_WAVE_INDICATOR = "slowWaveIndicator";
    final static String WAVE_DELTA_INDICATOR = "waveDeltaIndicator";

    final static String BULLISH_CROSSOVER_MARKER = "bullishCrossoverMarker";
    final static String BEARISH_CROSSOVER_MARKER = "bearishCrossoverMarker";

    final static String OVERBOUGHT_GUIDE = "overboughtGuide";
    final static String OVERSOLD_GUIDE = "oversoldGuide";

    final static int CHLEN = 9;
    final static int AVG = 12;
    final static int MALEN = 3;

    @Override
    public void initialize(Defaults defaults)
    {
        var sd = createSD();
        var tabGeneral = sd.addTab("General");

        //Color fastColor = new Color(144, 202, 249, 178);
        //Color slowColor = new Color(13, 71, 161, 178);
        Color deltaColor = new Color(255, 235, 59, 191);

        var grpPaths = tabGeneral.addGroup("Lines");
        //var fastWavePath = new PathDescriptor(FAST_WAVE_PATH, "Fast Wave", fastColor, 1, null, true, false, true);
        var fastWavePath = new PathDescriptor(FAST_WAVE_PATH, "Fast Wave", defaults.getGreen(), 1, null, true, false, true);
        fastWavePath.setSupportsAdvancedPanel(false);
        grpPaths.addRow(fastWavePath);

        //var slowWavePath = new PathDescriptor(SLOW_WAVE_PATH, "Slow Wave", slowColor, 1, null, true, false, true);
        var slowWavePath = new PathDescriptor(SLOW_WAVE_PATH, "Slow Wave", defaults.getRed(), 1, null, true, false, true);
        slowWavePath.setSupportsAdvancedPanel(false);
        grpPaths.addRow(slowWavePath);

        var waveDeltaPath = new PathDescriptor(WAVE_DELTA_PATH, "Wave Delta", deltaColor, 1, null, true, false, true);
        waveDeltaPath.setSupportsAdvancedPanel(false);
        grpPaths.addRow(waveDeltaPath);

        var grpShades = tabGeneral.addGroup("Fills");
        //grpShades.addRow(new ShadeDescriptor(FAST_WAVE_SHADE, "Fast Wave", FAST_WAVE_PATH, 0, Enums.ShadeType.BOTH, fastColor, true, true));
        //grpShades.addRow(new ShadeDescriptor(SLOW_WAVE_SHADE, "Slow Wave", SLOW_WAVE_PATH, 0, Enums.ShadeType.BOTH, slowColor, true, true));
        grpShades.addRow(new ShadeDescriptor(WAVE_DELTA_SHADE, "Wave Delta", WAVE_DELTA_PATH, 0, Enums.ShadeType.BOTH, deltaColor, true, true));

        var grpIndicators = tabGeneral.addGroup("Indicators");
        grpIndicators.addRow(new IndicatorDescriptor(FAST_WAVE_INDICATOR, "Fast Wave", fastWavePath.getColor(), defaults.getTextColor(), false, true, true));
        grpIndicators.addRow(new IndicatorDescriptor(SLOW_WAVE_INDICATOR, "Slow Wave", slowWavePath.getColor(), defaults.getTextColor(), false, true, true));
        grpIndicators.addRow(new IndicatorDescriptor(WAVE_DELTA_INDICATOR, "Wave Delta", waveDeltaPath.getColor(), defaults.getTextColor(), false, true, true));

        var grpMarkers = tabGeneral.addGroup("Markers");
        grpMarkers.addRow(new MarkerDescriptor(BULLISH_CROSSOVER_MARKER, "Bullish Crossover", Enums.MarkerType.CIRCLE, Enums.Size.SMALL, defaults.getGreen(), defaults.getGreen(), true, true));
        grpMarkers.addRow(new MarkerDescriptor(BEARISH_CROSSOVER_MARKER, "Bearish Crossover", Enums.MarkerType.CIRCLE, Enums.Size.SMALL, defaults.getRed(), defaults.getRed(), true, true));

        var grpGuides = tabGeneral.addGroup("Guides");
        var guideOverbought = new GuideDescriptor(OVERBOUGHT_GUIDE, "Overbought", 60, -100, 100, 1, true);
        //guideOverbought.setLineColor(defaults.getLineColor());
        grpGuides.addRow(guideOverbought);
        var guideOversold = new GuideDescriptor(OVERSOLD_GUIDE, "Oversold", -60, -100, 100, 1, true);
        //guideOversold.setLineColor(defaults.getLineColor());
        grpGuides.addRow(guideOversold);

        var rd = createRD();
        rd.setLabelPrefix("Wave Trend");
        rd.addHorizontalLine(new LineInfo(0, defaults.getLineColor(), 1, null, true));
        rd.setFixedTopValue(100);
        rd.setFixedBottomValue(-100);
        rd.exportValue(new ValueDescriptor(Values.FAST_WAVE, "Fast Wave", new String[] {}));
        rd.exportValue(new ValueDescriptor(Values.SLOW_WAVE, "Slow Wave", new String[] {}));
        rd.exportValue(new ValueDescriptor(Values.WAVE_DELTA, "Wave Delta", new String[] {}));
        rd.declarePath(Values.WAVE_DELTA, WAVE_DELTA_PATH);
        rd.declarePath(Values.FAST_WAVE, FAST_WAVE_PATH);
        rd.declarePath(Values.SLOW_WAVE, SLOW_WAVE_PATH);
        rd.declareIndicator(Values.FAST_WAVE, FAST_WAVE_INDICATOR);
        rd.declareIndicator(Values.SLOW_WAVE, SLOW_WAVE_INDICATOR);
        rd.declareIndicator(Values.WAVE_DELTA, WAVE_DELTA_INDICATOR);
    }

    @Override
    protected void calculate(int i, DataContext ctx)
    {
        /*
        wavetrend(_src, _chlen=9, _avg=12, _malen=3) =>
        _esa = ta.ema(_src, _chlen)
        _de = ta.ema(math.abs(_src - _esa), _chlen)
        _ci = (_src - _esa) / (0.015 * _de)
        _tci = ta.ema(_ci, _avg)
        _wt1 = _tci
        _wt2 = ta.sma(_wt1, _malen)
        [_wt1, _wt2]
         */
        if (i < (CHLEN + CHLEN + AVG + MALEN))
            return;

        var series = ctx.getDataSeries();
        Double esa = series.ema(i, CHLEN, Enums.BarInput.TP);
        series.setDouble(i, Values.ESA, esa);
        var tp = series.getTypicalPrice(i);
        if (tp == null)
            return;
        series.setDouble(i, Values.TMP, Math.abs(tp - esa));
        var de = series.ema(i, CHLEN, Values.TMP);
        if (de == null)
            return;
        var ci = (tp - esa) / (0.015 * de);
        series.setDouble(i, Values.CI, ci);
        var wt1 = series.ema(i, AVG, Values.CI);
        if (wt1 == null)
            return;
        series.setDouble(i, Values.FAST_WAVE, wt1);
        var wt2 = series.sma(i, MALEN, Values.FAST_WAVE);
        if (wt2 == null)
            return;
        series.setDouble(i, Values.SLOW_WAVE, wt2);
        series.setDouble(i, Values.WAVE_DELTA, wt1 - wt2);

        var settings = getSettings();
        var bullishMarker = settings.getMarker(BULLISH_CROSSOVER_MARKER);
        var bearishMarker = settings.getMarker(BEARISH_CROSSOVER_MARKER);
        var coord = new Coordinate(series.getStartTime(i), series.getDouble(i, Values.SLOW_WAVE));
        if (bullishMarker != null && bullishMarker.isEnabled() && crossedAbove(series, i, Values.FAST_WAVE, Values.SLOW_WAVE))
        {
            addFigure(new Marker(coord, Enums.Position.CENTER, bullishMarker, "Bullish Crossover"));
        }
        else if (bearishMarker != null && bearishMarker.isEnabled() && crossedBelow(series, i, Values.FAST_WAVE, Values.SLOW_WAVE))
        {
            addFigure(new Marker(coord, Enums.Position.CENTER, bearishMarker, "Bearish Crossover"));
        }

        series.setComplete(i);
    }
}
