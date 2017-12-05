/*
 * Copyright (c) 2017.  Richard Scott McNew.
 *
 * This file is part of Liquid Fortress Packet Analyzer.
 *
 * Liquid Fortress Packet Analyzer is free software: you can redistribute
 * it and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Liquid Fortress Packet Analyzer is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Liquid Fortress Packet Analyzer.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.liquidfortress.packetanalyzer.statistics;

import java.util.HashSet;

/**
 * UdpSources
 * <p/>
 * HashSet used to track unique UDP sources
 */
public class UdpSources {

    // UDP sources are stored in "IP Address:port" format
    private static HashSet<String> sources = new HashSet<>();

    public static int size() {
        return sources.size();
    }

    public static boolean contains(Object o) {
        return sources.contains(o);
    }

    public static boolean add(String s) {
        return sources.add(s);
    }
}
