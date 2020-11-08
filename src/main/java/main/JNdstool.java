/*
 *
 * This file is part of jNdstool.
 *
 * jNdstool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jNdstool. If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c)  2020 JackHack96
 *
 */

package main;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.MutuallyExclusiveGroup;
import net.sourceforge.argparse4j.inf.Namespace;
import nitro.ROM;

import java.io.IOException;
import java.nio.file.Paths;

public class JNdstool {
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Test").build().defaultHelp(true).description("Extract or build NDS ROMs");
        MutuallyExclusiveGroup createOrExtract = parser.addMutuallyExclusiveGroup();
        createOrExtract.addArgument("-x", "--extract").help("Exctract the given NDS ROM");
        createOrExtract.addArgument("-c", "--create").help("Create a ROM based on a directory");
        createOrExtract.required(true);
        parser.addArgument("-d", "--directory").help("Directory where to extract the ROM or to create from").required(true);
        Namespace res;
        try {
            res = parser.parseArgs(args);
            if (res.get("extract") != null) {
                ROM.extractROM(Paths.get(res.getString("extract")), Paths.get(res.getString("directory")));
            } else {
                ROM.buildROM(Paths.get(res.getString("create")), Paths.get(res.getString("directory")));
            }
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
