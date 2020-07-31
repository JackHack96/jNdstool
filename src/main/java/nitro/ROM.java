/*
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
 * Copyright 2016-2017 JackHack96
 */
package nitro;

import io.BinaryReader;
import io.BinaryWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This is the main class, which handles the entire ROM
 */
public class ROM {

    /**
     * Load an existing folder containing ROM's files
     *
     * @param path The path where the ROM files are stored
     * @throws IOException If something goes wrong
     */
    public ROM(Path path) throws IOException {
        // General check of the files
        if (Files.notExists(path.resolve("data")))
            throw new IOException("data subfolder not found! Please check the given directory!");
        if (Files.notExists(path.resolve("overlay")))
            throw new IOException("overlay subfolder not found! Please check the given directory!");
        if (Files.notExists(path.resolve("arm9")))
            throw new IOException("arm9 file not found! Please check the given directory!");
        if (Files.notExists(path.resolve("arm9ovltable")))
            throw new IOException("arm9 overlay table file not found! Please check the given directory!");
        if (Files.notExists(path.resolve("arm7")))
            throw new IOException("arm7 file not found! Please check the given directory!");
        if (Files.notExists(path.resolve("arm7ovltable")))
            throw new IOException("arm7 overlay table not found! Please check the given directory!");
        if (Files.notExists(path.resolve("header")))
            throw new IOException("header file not found! Please check the given directory!");
        if (Files.notExists(path.resolve("banner")))
            throw new IOException("banner file not found! Please check the given directory!");

        // Now we load the header
        //BinaryReader reader = new BinaryReader(path.resolve("header"));

        // The ROM's header, which contains all the informations for the other things
        //NitroHeader header = NitroHeader.readHeader(reader);

        //reader.close();
    }

    /**
     * Extract the entire ROM in the host file system
     *
     * @param romPath The path of the .nds file
     * @param dirPath The path where to extract files
     * @throws IOException If something goes wrong
     */
    public static void extractROM(Path romPath, Path dirPath) throws IOException {
        if (Files.notExists(dirPath))
            Files.createDirectory(dirPath);
        if (!Files.isWritable(dirPath)) // If we can't read or write, we don't own the directory
            throw new IOException("Can't write in the directory! Check permissions!");

        BinaryReader rom = new BinaryReader(romPath);
        NitroDirectory root = new NitroDirectory("root", 0xf000, null);
        NitroHeader header = NitroHeader.readHeader(rom);
        Map<Integer, Integer> startOffset = new HashMap<>(); // The ROM's files start offset
        Map<Integer, Integer> endOffset = new HashMap<>(); // The ROM's files end offsets

        rom.seek(header.getFatOffset());

        for (int i = 0; i < header.getFatSize() / 8; i++) {
            startOffset.put(i, rom.readInt());
            endOffset.put(i, rom.readInt());
        }

        // Load the directory structure
        rom.seek(header.getFntOffset());
        NitroDirectory.loadDir(root, rom, rom.getPosition(), startOffset, endOffset);

        // Let's create the directory tree
        if (Files.notExists(dirPath.resolve("data")))
            Files.createDirectory(dirPath.resolve("data"));
        NitroDirectory.createDirectoryTree(dirPath.resolve("data"), root);

        // And now we fill everything with files
        NitroDirectory.createFileTree(rom, dirPath.resolve("data"), root);

        // We also have to extract the header, the ARM binary files and the overlays
        BinaryWriter w; // The writer for the various files

        // The overlays
        if (Files.notExists(dirPath.resolve("overlay")))
            Files.createDirectory(dirPath.resolve("overlay"));
        for (int i = 0; i < header.getArm9OverlaySize() / 0x20; i++) {
            if (Files.notExists(dirPath.resolve("overlay").resolve(String.format("overlay_%04d.bin", i)))) {
                w = new BinaryWriter(dirPath.resolve("overlay").resolve(String.format("overlay_%04d.bin", i)));
                rom.seek(startOffset.get(i));
                w.writeBytes(rom.readBuffer(endOffset.get(i) - startOffset.get(i)));
                w.close();
            }
        }
        int arm7OvSize = (header.getArm7OverlaySize() / 0x20);
        for (int i = 0; i < header.getArm7OverlaySize() / 0x20; i++) {
            if (Files.notExists(dirPath.resolve("overlay").resolve(String.format("overlay_%04d.bin", i + arm7OvSize)))) {
                w = new BinaryWriter(dirPath.resolve("overlay").resolve(String.format("overlay_%04d.bin", i + arm7OvSize)));
                rom.seek(startOffset.get(i + arm7OvSize));
                w.writeBytes(rom.readBuffer(endOffset.get(i + arm7OvSize) - startOffset.get(i + arm7OvSize)));
                w.close();
            }
        }

        // The header and the two arms
        if (Files.notExists(dirPath.resolve("header.bin"))) {
            rom.seek(0);
            w = new BinaryWriter(dirPath.resolve("header.bin"));
            w.writeBytes(rom.readBuffer(header.getHeaderSize()));
            w.close();
        }

        if (Files.notExists(dirPath.resolve("arm9.bin"))) {
            rom.seek(header.getArm9RomOffset());
            w = new BinaryWriter(dirPath.resolve("arm9.bin"));
            w.writeBytes(rom.readBuffer(header.getArm9Size()));
            w.close();
        }

        if (Files.notExists(dirPath.resolve("arm9ovltable.bin"))) {
            rom.seek(header.getArm9OverlayOffset());
            w = new BinaryWriter(dirPath.resolve("arm9ovltable.bin"));
            w.writeBytes(rom.readBuffer(header.getArm9OverlaySize()));
            w.close();
        }

        if (Files.notExists(dirPath.resolve("arm7.bin"))) {
            rom.seek(header.getArm7RomOffset());
            w = new BinaryWriter(dirPath.resolve("arm7.bin"));
            w.writeBytes(rom.readBuffer(header.getArm7Size()));
            w.close();
        }

        if (Files.notExists(dirPath.resolve("arm7ovltable.bin"))) {
            rom.seek(header.getArm7OverlayOffset());
            w = new BinaryWriter(dirPath.resolve("arm7ovltable.bin"));
            w.writeBytes(rom.readBuffer(header.getArm7OverlaySize()));
            w.close();
        }

        if (Files.notExists(dirPath.resolve("banner.bin"))) {
            rom.seek(header.getIconOffset());
            w = new BinaryWriter(dirPath.resolve("banner.bin"));
            w.writeBytes(rom.readBuffer(0x840));
            w.close();
        }

        rom.close();
    }

    /**
     * Build the entire ROM from the given directory
     *
     * @param dirPath The path of the directory containing the files
     * @param romPath The path of the .nds file
     * @throws IOException If something goes wrong
     */
    public static void buildROM(Path dirPath, Path romPath) throws IOException {
        // General check of the files
        if (Files.notExists(dirPath.resolve("data")))
            throw new IOException("data subfolder not found! Please check the given directory!");
        if (Files.notExists(dirPath.resolve("overlay")))
            throw new IOException("overlay subfolder not found! Please check the given directory!");
        if (Files.notExists(dirPath.resolve("arm9.bin")))
            throw new IOException("arm9 file not found! Please check the given directory!");
        if (Files.notExists(dirPath.resolve("arm9ovltable.bin")))
            throw new IOException("arm9 overlay table file not found! Please check the given directory!");
        if (Files.notExists(dirPath.resolve("arm7.bin")))
            throw new IOException("arm7 file not found! Please check the given directory!");
        if (Files.notExists(dirPath.resolve("arm7ovltable.bin")))
            throw new IOException("arm7 overlay table not found! Please check the given directory!");
        if (Files.notExists(dirPath.resolve("header.bin")))
            throw new IOException("header file not found! Please check the given directory!");
        if (Files.notExists(dirPath.resolve("banner.bin")))
            throw new IOException("banner file not found! Please check the given directory!");

        BinaryWriter rom = new BinaryWriter(romPath); // The stream for the .nds file
        BinaryReader reader = new BinaryReader(dirPath.resolve("header.bin")); // The reader for the various files

        NitroHeader header = NitroHeader.readHeader(reader);
        NitroDirectory root = new NitroDirectory("root", 0xf000, null);

        NitroHeader.writeHeader(header, rom); // The header first

        // The ARM9
        reader = new BinaryReader(dirPath.resolve("arm9.bin"));
        header.setArm9RomOffset(rom.getPosition());
        rom.writeBytes(reader.readAll());
        header.setArm9Size(rom.getPosition() - header.getArm9RomOffset());

        // The ARM9 overlay table
        reader = new BinaryReader(dirPath.resolve("arm9ovltable.bin"));
        header.setArm9OverlayOffset(rom.getPosition());
        rom.writeBytes(reader.readAll());
        header.setArm9OverlaySize(rom.getPosition() - header.getArm9OverlayOffset());

        // This will be needed for the FAT
        List<Integer> overlayStartOffsets = new ArrayList<>();
        List<Integer> overlayEndOffsets = new ArrayList<>();

        File[] overlays = dirPath.resolve("overlay").toFile().listFiles();
        assert overlays != null;
        Arrays.sort(overlays);

        // The ARM9 overlays
        for (int i = 0; i < header.getArm9OverlaySize() / 0x20; i++) {
            reader = new BinaryReader(overlays[i].toPath());
            overlayStartOffsets.add(rom.getPosition());
            rom.writeBytes(reader.readAll());
            overlayEndOffsets.add(rom.getPosition() + reader.getSize());
        }

        // The ARM7
        reader = new BinaryReader(dirPath.resolve("arm7.bin"));
        header.setArm7RomOffset(rom.getPosition());
        rom.writeBytes(reader.readAll());
        header.setArm7Size(rom.getPosition() - header.getArm7RomOffset());

        // The ARM7 overlay table
        reader = new BinaryReader(dirPath.resolve("arm7ovltable.bin"));
        header.setArm7OverlayOffset(rom.getPosition());
        rom.writeBytes(reader.readAll());
        header.setArm7OverlaySize(rom.getPosition() - header.getArm7OverlayOffset());

        // The ARM7 overlays
        for (int i = header.getArm9OverlaySize() / 0x20; i < header.getArm9OverlaySize() / 0x20 + header.getArm7OverlaySize() / 0x20; i++) {
            reader = new BinaryReader(overlays[i].toPath());
            overlayStartOffsets.add(rom.getPosition());
            rom.writeBytes(reader.readAll());
            overlayEndOffsets.add(rom.getPosition() + reader.getSize());
        }

        // Recursively load directories and files, getting the root nitro directory
        NitroDirectory.loadDir(dirPath.resolve("data").toFile(),
                root,
                0xf000,
                Objects.requireNonNull(dirPath.resolve("overlay").toFile().listFiles()).length,
                rom.getPosition()
                        + FNT.calculateFNTSize(dirPath.resolve("data").toFile())
                        + FAT.calculateFATSize(dirPath.resolve("data").toFile())
                        + overlays.length * 8
                        + 0x840);

        // The File Name Table
        header.setFntOffset(rom.getPosition());
        FNT.writeFNT(rom, root);
        header.setFntSize(rom.getPosition() - header.getFntOffset());

        // The File Allocation Table
        header.setFatOffset(rom.getPosition());
        FAT.writeFAT(rom, root, overlayStartOffsets, overlayEndOffsets);
        header.setFatSize(rom.getPosition() - header.getFatOffset());

        // The banner
        header.setIconOffset(rom.getPosition());
        reader = new BinaryReader(dirPath.resolve("banner.bin"));
        rom.writeBytes(reader.readAll());

        // The actual files
        NitroDirectory.repackFileTree(rom, dirPath.resolve("data"), root);

        // Write updated header
        rom.seek(0);
        NitroHeader.writeHeader(header, rom);

        rom.close();
        reader.close();
    }
}