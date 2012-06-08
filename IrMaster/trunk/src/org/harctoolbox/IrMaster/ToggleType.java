/*
Copyright (C) 2009-2012 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.IrMaster;

/**
 * Type of toggle in an IR signal.
 */

public enum ToggleType {

    /**
     * Generate the toggle code with toggle = 0.
     */
    toggle_0,
    /**
     * Generate the toggle code with toggle = 1.
     */
    toggle_1,

    /**
     * Don't care
     */
    dont_care;

    /**
     * Do not generate toggle codes
     */
    //no_toggle,
    /**
     * Generate toggle codes
     */
    //do_toggle;

    public static ToggleType flip(ToggleType t) {
        return t == toggle_0 ? toggle_1 : toggle_0;
    }

    public static int toInt(ToggleType t) {
        return t == dont_care ? -1 : t.ordinal();// == toggle_1 ? 1 : 0;
    }

    public static ToggleType parse(String t) {
        return t.equals("0") ? ToggleType.toggle_0 : t.equals("1") ? ToggleType.toggle_1 : ToggleType.dont_care;
    }

    public static String toString(ToggleType toggle) {
        return toggle == ToggleType.toggle_0 ? "0"
                : toggle == ToggleType.toggle_1 ? "1" : "-";
    }
}
