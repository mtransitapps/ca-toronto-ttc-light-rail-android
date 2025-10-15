package org.mtransit.parser.ca_toronto_ttc_light_rail;

import static org.mtransit.parser.Constants.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.commons.TorontoTTCCommons;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GRouteType;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirection;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

// https://open.toronto.ca/dataset/ttc-routes-and-schedules/ # ALL (including SUBWAY)
// https://open.toronto.ca/dataset/surface-routes-and-schedules-for-bustime/ BUS & STREETCAR
// OLD: https://opendata.toronto.ca/toronto.transit.commission/ttc-routes-and-schedules/SurfaceGTFS.zip
// OLD: http://opendata.toronto.ca/TTC/routes/OpenData_TTC_Schedules.zip
// OLD: http://opendata.toronto.ca/toronto.transit.commission/ttc-routes-and-schedules/OpenData_TTC_Schedules.zip
public class TorontoTTCLightRailAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new TorontoTTCLightRailAgencyTools().start(args);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		return LANG_EN;
	}

	private static final Pattern NOT_IN_SERVICE_ = Pattern.compile("(Not In Service)", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (NOT_IN_SERVICE_.matcher(gTrip.getTripHeadsignOrDefault()).matches()) {
			return EXCLUDE;
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public boolean excludeStopTime(@NotNull GStopTime gStopTime) {
		if (NOT_IN_SERVICE_.matcher(gStopTime.getStopHeadsignOrDefault()).matches()) {
			return EXCLUDE;
		}
		return super.excludeStopTime(gStopTime);
	}

	@NotNull
	@Override
	public String getAgencyName() {
		return "TTC";
	}

	@Override
	public @NotNull Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_LIGHT_RAIL;
	}

	@NotNull
	@Override
	public Integer getAgencyExtendedRouteType() {
		return GRouteType.EX_TRAM_SERVICE.getId();
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return true;
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return true;
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return true;
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		routeLongName = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), routeLongName);
		return CleanUtils.cleanLabel(getFirstLanguageNN(), routeLongName);
	}

	@Override
	public @NotNull String getAgencyColor() {
		return TorontoTTCCommons.TTC_RED;
	}

	@Override
	public boolean defaultAgencyColorEnabled() {
		return true;
	}

	@Nullable
	@Override
	public String fixColor(@Nullable String color) {
		final String fixedColor = TorontoTTCCommons.fixColor(color);
		if (fixedColor != null) {
			return fixedColor;
		}
		return super.fixColor(color);
	}

	@NotNull
	@Override
	public List<Integer> getDirectionTypes() {
		return Collections.singletonList(
				MDirection.HEADSIGN_TYPE_DIRECTION
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final String L_ = "L ";

	private static final Pattern DIRECTION_ONLY = Pattern.compile("(^(east|west|north|south)$)", Pattern.CASE_INSENSITIVE);

	@Nullable
	@Override
	public String selectDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2) {
		if (StringUtils.equals(headSign1, headSign2)) {
			return null; // canNOT select
		}
		final boolean startsWith1 = headSign1 != null && headSign1.startsWith(L_);
		final boolean startsWith2 = headSign2 != null && headSign2.startsWith(L_);
		if (startsWith1) {
			if (!startsWith2) {
				return headSign2;
			}
		} else {
			if (startsWith2) {
				return headSign1;
			}
		}
		final boolean match1 = headSign1 != null && DIRECTION_ONLY.matcher(headSign1).find();
		final boolean match2 = headSign2 != null && DIRECTION_ONLY.matcher(headSign2).find();
		if (match1) {
			if (!match2) {
				return headSign1;
			}
		} else if (match2) {
			return headSign2;
		}
		return null;
	}

	private static final Pattern STARTS_WITH_DASH_ = Pattern.compile("((?<=[A-Z]{4,5}) - .*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = STARTS_WITH_DASH_.matcher(directionHeadSign).replaceAll(EMPTY); // keep East/West/North/South
		directionHeadSign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, directionHeadSign);
		return CleanUtils.cleanLabel(getFirstLanguageNN(), directionHeadSign);
	}

	private static final Pattern KEEP_LETTER_AND_TOWARDS_ = Pattern.compile("(^" +
			"(([a-z]+) - )?" + // EAST/WEST/NORTH/SOUTH -
			"(\\d+(/\\d+)?)?" + // 000(/000?)
			"([a-z] )?" + // A (from 000A) <- KEEP 'A'
			"((.*)" + // before to/towards
			"\\s*(towards|to))? " +
			"(.*)" + // after to/towards <- KEEP
			")", Pattern.CASE_INSENSITIVE);
	private static final String KEEP_LETTER_AND_TOWARDS_REPLACEMENT = "$6$10";

	private static final Pattern ENDS_EXTRA_FARE_REQUIRED = Pattern.compile("(( -)? extra fare required .*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern REPLACEMENT_BUS_ = CleanUtils.cleanWords("replacement bus");

	private static final Pattern SHORT_TURN_ = CleanUtils.cleanWords("short turn");
	private static final Pattern BLUE_NIGHT_ = CleanUtils.cleanWords("blue night");

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = KEEP_LETTER_AND_TOWARDS_.matcher(tripHeadsign).replaceAll(KEEP_LETTER_AND_TOWARDS_REPLACEMENT);
		tripHeadsign = ENDS_EXTRA_FARE_REQUIRED.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = REPLACEMENT_BUS_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = SHORT_TURN_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = BLUE_NIGHT_.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = CleanUtils.removeVia(tripHeadsign);
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, tripHeadsign);
		tripHeadsign = CleanUtils.fixMcXCase(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AT.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(getFirstLanguageNN(), tripHeadsign);
	}

	private static Pattern makeRSN_RLN_(@NotNull String rln) {
		return Pattern.compile(
				"(" +
						"(\\d+(/\\d+)?)" + // 000(/000?)
						"([a-z] )?" + // A (from 000A)
						"(\\s*(" + rln + ")\\s*)?" +
						")",
				Pattern.CASE_INSENSITIVE);
	}

	private static final String RSN_RLN_REPLACEMENT = "$4";

	@Override
	public @NotNull String cleanStopHeadSign(@NotNull GRoute gRoute, @NotNull GTrip gTrip, @NotNull GStopTime gStopTime, @NotNull String stopHeadsign) {
		stopHeadsign = makeRSN_RLN_(gRoute.getRouteLongNameOrDefault())
				.matcher(stopHeadsign).replaceAll(RSN_RLN_REPLACEMENT);
		return super.cleanStopHeadSign(gRoute, gTrip, gStopTime, stopHeadsign);
	}

	private static final Pattern SIDE = Pattern.compile("((^|\\W)(side)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String SIDE_REPLACEMENT = "$2" + "$4";

	private static final Pattern ENDS_WITH_TOWARDS_ = Pattern.compile("( towards .*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = ENDS_WITH_TOWARDS_.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = SIDE.matcher(gStopName).replaceAll(SIDE_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.fixMcXCase(gStopName);
		gStopName = CleanUtils.cleanStreetTypes(gStopName);
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(getFirstLanguageNN(), gStopName);
	}

	@Override
	public boolean excludeStop(@NotNull GStop gStop) {
		//noinspection DiscouragedApi
		if (gStop.getStopId().equals(gStop.getStopCode())) {
			return EXCLUDE; // 2025-10-15: merged GTFS > multiple stops with same ID (different code)
		}
		return super.excludeStop(gStop);
	}
}
