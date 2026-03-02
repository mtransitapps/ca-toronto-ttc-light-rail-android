package org.mtransit.parser.ca_toronto_ttc_light_rail;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.DefaultAgencyTools;

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
}

