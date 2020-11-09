# jNdstool
This tool is the Java version of the popular `ndstool` by Rafael Vuijk, Dave Murphy and Alexei Karpenko
of the devkitPro team (https://github.com/devkitPro/ndstool).
It is based upon Martin Korth's DS specifications, which you can find in the original HTML version at 
http://problemkaputt.de/gbatek.htm#ndsreference, or in a Markdown version at 
https://mgba-emu.github.io/gbatek/#ndsreference.

This tool was developed as a library rather than a standalone tool, that's why the provided command line
utility is very basic, allowing for full extraction or full build to/from a directory (in contrast,
`ndstool` provides a rich set of command for single file extraction or insertion).

### Usage
For **extracting** a ROM, suppose `foo.nds` to the `foo_extracted` directory, launch the JAR by passing the
following arguments:
```shell script
java -jar jNdstool-1.0.jar -x foo.nds -d foo_extracted
```
For **building** a ROM `foo.nds` from the `foo_extracted` directory, launch the JAR by passing the following
arguments:
```shell script
java -jar jNdstool-1.0.jar -c foo.nds -d foo_extracted
``` 
