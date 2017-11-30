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

package com.liquidfortress.packetanalyzer.util;

import java.net.Inet4Address;
import java.util.Comparator;

/**
 * Inet4AddressComparator
 * <p/>
 * Comparator for Inet4Address class
 */
public class Inet4AddressComparator implements Comparator<Inet4Address> {
    @Override
    public int compare(Inet4Address ip1, Inet4Address ip2) {
        return ip1.getHostAddress().compareTo(ip2.getHostAddress());
    }
}
