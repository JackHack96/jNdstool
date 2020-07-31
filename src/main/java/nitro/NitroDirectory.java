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
 * Copyright 2016 JackHack96
 */
package nitro;

import io.BinaryReader;
import io.BinaryWriter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * This class represents a folder of a Nitro file system
 */
class NitroDirectory implements Comparable<NitroDirectory> {
    private final String name; // Directory name
    private final int id; // Directory ID
    private final NitroDirectory parent; // Parent directory
    private final List<NitroFile> fileList; // The list of directory's files
    private final List<NitroDirectory> directoryList; // The list of subdirectories

    //The following variables are used internally by loadDir method
    private static int currentDirID;
    private static int firstFileID;
    private static int currentOffset;

    public NitroDirectory(String name, int id, NitroDirectory parent) {
        this.name = name;
        this.id = id;
        this.parent = parent;
        this.fileList = new ArrayList<>();
        this.directoryList = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public NitroDirectory getParent() {
        return parent;
    }

    public List<NitroFile> getFileList() {
        Collections.sort(fileList);
        return fileList;
    }

    public List<NitroDirectory> getDirectoryList() {
        Collections.sort(directoryList);
        return directoryList;
    }

    @Override
    public String toString() {
        return "NitroDirectory{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}';
    }

    @Override
    public int compareTo(NitroDirectory nitroDirectory) {
        return (name.compareTo(nitroDirectory.name));
    }

    /**
     * Recursively load the FNT structure
     *
     * @param parent The current parent directory
     * @param stream FNT source stream
     * @param origin Initial stream offset
     * @throws IOException If a file is corrupted or something is wrong
     */
    public static void loadDir(NitroDirectory parent, BinaryReader stream, long origin, Map<Integer, Integer> startOffset, Map<Integer, Integer> endOffset) throws IOException {
        long position = stream.getPosition();
        stream.seek(origin + (8 * (parent.id & 0xfff))); // Go the the FNT main table entry

        int subTableOffset = stream.readInt();
        int firstFileID = stream.readShort();

        stream.seek(origin + subTableOffset); // Go to the FNT sub table entry

        int header; // The header tells us if the entry is a file or a parent, and then it tells us the name size

        while (((header = stream.readByte()) & 0x7f) != 0) { // Until we found a 0x00 or a 0xff header, we can go further
            String name = stream.readString(header & 0x7f, StandardCharsets.US_ASCII); // This could be a parent name or a file name
            if (header > 0x7f) { // This is a directory
                int newID = stream.readShort(); // This will be the next parent ID
                NitroDirectory newDirectory = new NitroDirectory(name, newID, parent);
                parent.directoryList.add(newDirectory);
                loadDir(newDirectory, stream, origin, startOffset, endOffset);
            } else { // This is a file
                parent.fileList.add(new NitroFile(name, firstFileID, startOffset.get(firstFileID), endOffset.get(firstFileID) - startOffset.get(firstFileID), parent));
                firstFileID++;
            }
        }
        stream.seek(position);
    }

    /**
     * Recursively construct the NitroDirectory structure
     *
     * @param currentPath   The current host file system path
     * @param parent        The current parent directory
     * @param firstFileID   First file ID of the current folder
     * @param currentOffset Current offset from the ROM origin
     */
    public static void loadDir(File currentPath, NitroDirectory parent, int currentDirID, int firstFileID, int currentOffset) {
        NitroDirectory.currentDirID = currentDirID;
        NitroDirectory.firstFileID = firstFileID;
        NitroDirectory.currentOffset = currentOffset;
        loadDir(currentPath, parent);
    }

    /**
     * Recursively create the directory tree of the ROM
     *
     * @param currentDir The path to use for creating the tree
     * @param rootDir    The current root directory
     * @throws IOException If a file is corrupted or something is wrong
     */
    public static void createDirectoryTree(Path currentDir, NitroDirectory rootDir) throws IOException {
        for (NitroDirectory d : rootDir.directoryList) {
            if (Files.notExists(currentDir.resolve(d.getName()))) {
                Files.createDirectory(currentDir.resolve(d.name));
            }
            if (d.directoryList.size() > 0)
                createDirectoryTree(currentDir.resolve(d.name), d);
        }
    }

    /**
     * Recursively extract the files from the ROM
     *
     * @param rom        BinaryReader stream of the .nds ROM
     * @param currentDir Current path
     * @param rootDir    The current root directory
     * @throws IOException If a file is corrupted or something is wrong
     */
    public static void createFileTree(BinaryReader rom, Path currentDir, NitroDirectory rootDir) throws IOException {
        for (NitroDirectory d : rootDir.directoryList) {
            for (NitroFile f : d.fileList) {
                if (Files.notExists(currentDir.resolve(d.name).resolve(f.getName()))) {
                    BinaryWriter tmp = new BinaryWriter(currentDir.resolve(d.name).resolve(f.getName()));
                    rom.seek(f.getOffset());
                    tmp.writeBytes(rom.readBuffer(f.getSize()));
                    tmp.close();
                }
            }
            if (d.directoryList.size() > 0)
                createFileTree(rom, currentDir.resolve(d.name), d);
        }
    }

    /**
     * Recursively repack the files in the ROM
     *
     * @param rom        BinaryWriter stream of the .nds ROM
     * @param currentDir Current path
     * @param rootDir    The current root directory
     * @throws IOException If a file is corrupted or something is wrong
     */
    public static void repackFileTree(BinaryWriter rom, Path currentDir, NitroDirectory rootDir) throws IOException {
        for (NitroDirectory d : rootDir.directoryList)
            repackFileTree(rom, currentDir.resolve(d.getName()), d);
        for (NitroFile f : rootDir.fileList) {
            if (Files.exists(currentDir.resolve(f.getName()))) {
                BinaryReader tmp = new BinaryReader(currentDir.resolve(f.getName()));
                if (f.getOffset() != rom.getPosition()) {
                    System.out.println("WARNING! " + f + " real offset differs from assumed one! Assumed: "
                            + f.getOffset() + " Real: " + rom.getPosition());
                    f.setOffset(rom.getPosition());
                }
                rom.writeBytes(tmp.readAll());
                tmp.close();
            } else
                throw new IOException(f.getName() + " file does not exist");
        }
    }


    /**
     * Recursively construct the NitroDirectory structure
     *
     * @param currentPath The current host file system path
     * @param parent      The current parent directory
     */
    private static void loadDir(File currentPath, NitroDirectory parent) {
        File[] dirList = currentPath.listFiles(File::isDirectory);
        File[] fileList = currentPath.listFiles(File::isFile);

        // it's important to sort the file lists alphabetically
        if (dirList != null) {
            Arrays.sort(dirList);
            for (File dir : dirList) {
                currentDirID++;
                NitroDirectory newDirectory = new NitroDirectory(dir.getName(), currentDirID, parent);
                parent.directoryList.add(newDirectory);
                if (Objects.requireNonNull(dir.listFiles()).length > 0)
                    loadDir(dir, newDirectory);
            }
        }
        if (fileList != null) {
            Arrays.sort(fileList);
            for (File file : fileList) {
                parent.fileList.add(new NitroFile(file.getName(), firstFileID, currentOffset, (int) file.length(), parent));
                firstFileID++;
                currentOffset += (int) file.length();
            }
        }
    }
}
