package org.mtransit.parser.ca_toronto_ttc_light_rail;

import static org.mtransit.parser.Constants.EMPTY;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;

import java.util.regex.Pattern;

public class TorontoTTCLightRailAgencyTools extends DefaultAgencyTools {

	public static void main(@NotNull String[] args) {
		new TorontoTTCLightRailAgencyTools().start(args);
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

	@Override
	public @NotNull String cleanDirectionHeadsign(@Nullable GRoute gRoute, int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = STARTS_WITH_DASH_.matcher(directionHeadSign).replaceAll(EMPTY); // keep East/West/North/South
		directionHeadSign = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), directionHeadSign);
		directionHeadSign = CleanUtils.keepTo(directionHeadSign); // LRT 5 & 6
		return super.cleanDirectionHeadsign(gRoute, directionId, fromStopName, directionHeadSign);
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
		tripHeadsign = CleanUtils.toLowerCaseUpperCaseWords(getFirstLanguageNN(), tripHeadsign);
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
}

