<details><summary> 👈 <code><i> Click arrows to expand/collapse details on this page </i></code></summary></details>

# APKEditor
### Powerful android apk resources editor
This tool uses [ARSCLib](https://github.com/REAndroid/ARSCLib) to edit any apk resources and has six main features

<details><summary><code>java -jar APKEditor.jar <b>-h</b></code></summary>

```ShellSession
$ java -jar APKEditor.jar -h
APKEditor - x.x.x
https://github.com/REAndroid/APKEditor
Android binary resource files editor
Commands:
  d | decode      Decodes android resources binary to readable json/xml/raw.
  b | build       Builds android binary from json/xml/raw.
  m | merge       Merges split apk files from directory or compressed apk files
                  like XAPK, APKM, APKS ...
  x | refactor    Refactors obfuscated resource names
  p | protect     Protects/Obfuscates apk resource files. Using unique
                  obfuscation techniques.
  info            Prints information of apk.
Other options:
  -h | -help      Displays this help and exit
  -v | -version   Displays version information and exit

To get help about each command run with:
<command> -h

```
</details>

#### 1- Decompile
* Decompiles resources of apk to human readable json string.
* Decompiles resources of apk to XML source code (for un-obfuscated apk only). Use  ``` -t xml ```
<details> <summary><code>java -jar APKEditor.jar <b>d</b> -i path/to/your-file.apk</code></summary>

```ShellSession
$ java -jar APKEditor.jar d -i test.apk -o test_json
00.000 I: [DECOMPILE] Using: APKEditor version 1.4.3, ARSCLib version 1.3.6
             -t = xml      
      -load-dex = 3        
       -dex-lib = jf       
             -i = test.apk 
             -o = test_json
 _________________________ 
00.006 I: [DECOMPILE] Loading ...
00.101 I: [DECOMPILE] Decompiling to xml ...
00.264 I: [DECOMPILE] Initializing android framework ...
00.264 I: [DECOMPILE] Loading android framework for version: 34
00.304 I: [DECOMPILE] Initialized framework: android-34 (14)
00.320 I: [DECOMPILE] [SANITIZE]: Sanitizing paths ...
00.341 I: [DECOMPILE] Validating resource names ...
00.396 I: [DECOMPILE] All resource names are valid
00.396 I: [DECOMPILE] Decode: archive-info.json
00.398 I: [DECOMPILE] Decode: uncompressed-files.json
00.412 I: [DECOMPILE] Decoding: AndroidManifest.xml
00.450 I: [DECOMPILE] public.xml: com.test.name -> package_1
00.508 I: [DECOMPILE] Res files: resources
02.483 I: [DECOMPILE] Baksmali: classes.dex
05.041 I: [DECOMPILE] Baksmali: classes2.dex
12.643 I: [DECOMPILE] Extracting root files ...
12.766 I: [DECOMPILE] Dumping signatures ...
12.766 I: [DECOMPILE] Saved to: test_json
```
<details> <summary><code>more info</code></summary>

```ShellSession
Decodes android resources binary to readable json/xml/raw.
Usage:
  d [Options, flags]
Options:
  -framework            Path of framework file (can be multiple).
  -framework-version    Preferred framework version number
  -i                    Input path.
  -load-dex             Number of dex files to load at a time.
                        If the apk dex files count greater than this value,
                        then the decoder loads one dex at a time.
                          *Applies only when -dex-lib set to internal.
                          *Default = 3
                          *See<Notes> below.
  -o                    Output path. Optional, if not provided then a new file
                        will be generated on same directory as input
  -remove-annotation    [Baksmali] Type name of annotation to remove from dex
                          e.g: -remove-annotation Lkotlin/Metadata;
                          *Can be multiple.
  -res-dir              Sets resource files root dir name. e.g. for obfuscation
                        to move files from 'res/*' to 'r/*' or vice versa.
  -sig                  Signatures directory path.
  -t                    Decode types:
                        [xml, json, raw, sig]
  -dex-lib              Dex library to use:
                         1) internal : Use internal library, supports dex
                        versions up to 042.
                         2) jf : Use library by JesusFreke/smali, supports dex
                        versions 035 and below.
                          *Default = internal
                          *See <Notes> below.
                        [internal, jf]
  -comment-level        [Baksmali] Sets the comment level.
                         1) off : No comment will be printed.
                         2) basic : Prints basic and resource id comments.
                         3) detail : Including <basic>, Prints class & method
                        relation comments.
                         4) detail2 : Including <detail>, Prints encoded hex
                        strings with literal as comment.
                         5) full : Prints all comments.
                         **Default = detail
                        [off, basic, detail, detail2, full]
Flags:
  -dex                  Copy raw dex files / skip smali.
  -dex-markers          Dumps dex markers (applies only when smali mode).
  -dex-profile          Decodes dex profile binary files under assets/dexopt to
                        readable json files.
  -f                    Force delete output path.
  -h | -help | --help   Displays this help and exit.
  -keep-res-path        Keeps original res/* file paths:
                          *Applies only when decoding to xml
                          *All res/* files will be placed on dir <res-files>
                          *The relative paths will be linked to values/*xml
  -no-cache             Do not create dex .cache files.
  -no-dex-debug         Drops all debug info from smali/dex.
  -smali-registers      [Baksmali] Use ".registers" directive instead of the
                        default ".locals".
  -split-json           Splits resources.arsc into multiple parts as per type
                        entries (use this for large files)
  -vrd                  Validate resources dir name
                        (eg. if a drawable resource file path is 'res/abc.png'
                        then it will be moved to 'res/drawable/abc.png)'
Examples:
  1)  [Basic]
  java -jar APKEditor.jar d -i path/input.apk
  2)  [Specify output]
  java -jar APKEditor.jar d -i path/input.apk -o path/output.apk
  3)  [Specify decode type]
  java -jar APKEditor.jar d -t xml -i path/input.apk
  4)  [Specify framework file(s)]
  java -jar APKEditor.jar d -i path/input.apk -framework framework-res.apk
  -framework platforms/android-32/android.jar
  5)  [Decode apk signature block]
  java -jar APKEditor.jar d -t sig -i path/input.apk -sig path/signatures_dir
Notes:
  1)  [internal] Dex builder:
  * Fully supports dex files up to 042.
  * Highest dex file compression.
  * Builds with similar dex-section order as r8/dx.
  * Convenient dex markers editing, see file smali/classes/dex-file.json
  * Additional helpful smali comments: e.g class/method hierarchy.
  * Supports whitespaces on class simple name as introduced on dex 040+
  2)  [-load-dex] To print correct class/method hierarchy, it is necessary to
  load all dex files at once. This may result high memory consumption and
  could fail with "OutOfMemoryError" thus you are required to limit the
  number of dex files to load at a time. You can overcome this problem with
  -Xmx memory arg e.g java -Xmx8g -jar APKEditor.jar ...
```
</details>
</details>

#### 2- Build
Builds back to apk from decompiled json/XML files
<details> <summary><code>java -jar APKEditor.jar <b>b</b> -i path/to/decompiled-directory</code></summary>

```ShellSession
$ java -jar APKEditor.jar b -i test_json -o test_edited.apk
00.000 I: [BUILD] Using: APKEditor version 1.4.3, ARSCLib version 1.3.6
            -t = xml            
      -dex-lib = jf             
            -i = test_json      
            -o = test_edited.apk
 ______________________________ 
00.005 I: [BUILD] Scanning XML directory ...
00.024 I: [BUILD] Scanning: test_json
00.168 I: [BUILD] Initializing android framework ...
00.168 I: [BUILD] Loading android framework for version: 34
00.254 I: [BUILD] Initialized framework: android-34 (14)
00.254 I: [BUILD] Set main package id from manifest: @mipmap/ic_launcher
00.256 I: [BUILD] Main package id initialized: id = 0x7f, from: @mipmap/ic_launcher
00.256 I: [BUILD] Encoding attrs ...
00.307 I: [BUILD] Encoding values ...
00.560 I: [BUILD] Scan: package_1/res
00.661 I: [BUILD] Scanned 5718 files: package_1/res
00.852 I: [BUILD] Add manifest: AndroidManifest.xml
00.852 I: [BUILD] Building dex ...
00.960 I: [BUILD] (1/2) Cached: classes.dex
00.973 I: [BUILD] (2/2) Cached: classes2.dex
01.033 I: [BUILD] Scanning root directory ...
01.036 I: [BUILD] Restoring original file paths ...
01.078 I: [BUILD] Loading signatures ...
01.082 I: [BUILD] Sorting files ...
01.104 I: [BUILD] Refreshing resource table ...
01.163 I: [BUILD] TableBlock: packages = 1, size = 3067996 bytes
01.164 I: [BUILD] Applying: extractNativeLibs=false
01.164 I: [BUILD] Writing apk...
01.185 I: [BUILD] Buffering compress changed files ...
06.083 I: [BUILD] Writing files: 6637
06.201 I: [BUILD] Writing signature block ...
06.241 I: [BUILD] Saved to: test_edited.apk
```
<details> <summary><code>more info</code></summary>

```ShellSession
Builds android binary from json/xml/raw.
Options:
  -framework            Path of framework file (can be multiple).
  -framework-version    Preferred framework version number
  -i                    Input path.
  -o                    Output path. Optional, if not provided then a new file
                        will be generated on same directory as input
  -res-dir              Sets resource files root dir name. e.g. for obfuscation
                        to move files from 'res/*' to 'r/*' or vice versa.
  -sig                  Signatures directory path.
  -t                    Build types, By default build types determined by quick
                        scanning of input directory files. Values are:
                        [xml, json, raw, sig]
  -extractNativeLibs    Sets extractNativeLibs attribute on manifest and
                        applies compression of library files (*.so).
                          *Default = manifest
                          1) manifest: read and apply from manifest.
                          2) none: remove attribute from manifest and store
                        libraries compressed.
                          3) true: set manifest attribute 'true' and store
                        libraries compressed.
                          4) false: set manifest attribute 'false' and store
                        libraries un-compressed with 4096 alignment.
                        [manifest, none, false, true]
  -dex-lib              Dex library to use:
                         1) internal : Use internal library, supports dex
                        versions up to 042.
                         2) jf : Use library by JesusFreke/smali, supports dex
                        versions 035 and below.
                          *Default = jf
                          **WARN: The default value will be replaced by
                        "internal" on coming versions.
                          *See <Notes> below.
                        [internal, jf]
Flags:
  -f                    Force delete output path.
  -h | -help | --help   Displays this help and exit.
  -no-cache             Ignore built cached .dex files and re-build smali files.
  -vrd                  Validate resources dir name
                        (eg. if a drawable resource file path is 'res/abc.png'
                        then it will be moved to 'res/drawable/abc.png)'
Examples:
  1)  [Basic]
    java -jar APKEditor.jar b -i path/input_directory
  2)  [Specify output]
    java -jar APKEditor.jar b -i path/input_directory -o path/output.apk
  3)  [Restore signatures]
    java -jar APKEditor.jar b -t sig -i path/input.apk -sig
  path/signatures_dir
  4)  [Specify framework]
    java -jar APKEditor.jar b -i path/input_directory -framework
  framework-res.apk -framework platforms/android-32/android.jar
```
</details>
</details>

#### 3- Merge
Merges multiple splitted apk files (directory, xapk, apkm, apks ...) to standalone apk
<details> <summary><code>java -jar APKEditor.jar <b>m</b> -i path/to/input</code></summary>

 ```ShellSession
$ java -jar APKEditor.jar m -i apk_files
00.000 I: [MERGE] Using: APKEditor version 1.4.3, ARSCLib version 1.3.6
      -i = apk_files           
      -o = apk_files_merged.apk
 _____________________________ 
00.009 I: [MERGE] Searching apk files ...
00.011 I: [MERGE] Found apk files: 3
00.214 I: [MERGE] Found modules: 3                                              
00.329 I: [MERGE] Merging: base
00.331 I: [MERGE] Added [base] classes.dex -> classes.dex
00.331 I: [MERGE] Added [base] classes2.dex -> classes2.dex
01.289 I: [MERGE] Merging: config.arm64_v8a                                     
01.293 I: [MERGE] Merging: config.xxhdpi                                        
01.634 I: [MERGE] Sanitizing manifest ...                                       
01.637 I: [MERGE] Removed-attribute : splitTypes
01.637 I: [MERGE] Removed-attribute : requiredSplitTypes
01.640 I: [MERGE] Attributes on <application> removed: 0x01010591 (isSplitRequired)
01.641 I: [MERGE] Removed-element : <meta-data> name="com.android.vending.splits.required"
01.641 I: [MERGE] Removed-element : <meta-data> name="com.android.stamp.source"
01.641 I: [MERGE] Removed-element : <meta-data> name="com.android.stamp.type"
01.645 I: [MERGE] Removed-table-entry : res/xml/splits0.xml
01.646 I: [MERGE] Removed-element : <meta-data> name="com.android.vending.splits"
01.646 I: [MERGE] Removed-element : <meta-data> name="com.android.vending.derived.apk.id"
01.779 I: [MERGE] Applying: extractNativeLibs=false
01.779 I: [MERGE] Writing apk ...
01.806 I: [MERGE] Buffering compress changed files ...
04.766 I: [MERGE] Writing files: 6637
04.981 I: [MERGE] Writing signature block ...                                   
05.035 I: [MERGE] Saved to: apk_files_merged.apk
```  
![apkmerger](/.github/apkmerger.png)

<details> <summary><code>more info</code></summary>

```ShellSession
Merges split apk files from directory or compressed apk files like XAPK,
APKM, APKS ...
Options:
  -i                    Input path.
  -o                    Output path. Optional, if not provided then a new file
                        will be generated on same directory as input
  -res-dir              Sets resource files root dir name. e.g. for obfuscation
                        to move files from 'res/*' to 'r/*' or vice versa.
  -extractNativeLibs    Sets extractNativeLibs attribute on manifest and
                        applies compression of library files (*.so).
                          *Default = manifest
                          1) manifest: read and apply from manifest.
                          2) none: remove attribute from manifest and store
                        libraries compressed.
                          3) true: set manifest attribute 'true' and store
                        libraries compressed.
                          4) false: set manifest attribute 'false' and store
                        libraries un-compressed with 4096 alignment.
                        [manifest, none, false, true]
Flags:
  -clean-meta           Cleans META-INF directory along with signature block.
  -f                    Force delete output path.
  -h | -help | --help   Displays this help and exit.
  -validate-modules     Validates for same versionNumber of base.apk with split
                        apk files.
  -vrd                  Validate resources dir name
                        (eg. if a drawable resource file path is 'res/abc.png'
                        then it will be moved to 'res/drawable/abc.png)'
Examples:
  1)  [Basic]
    java -jar APKEditor.jar m -i path/input -o path/output.apk
```
</details>
</details>

#### 4- Refactor
Refactors obfuscated resource entry names
<details> <summary><code>java -jar APKEditor.jar <b>x</b> -i path/to/input.apk</code></summary>

 ```ShellSession
$ java -jar APKEditor.jar x -i input.apk
00.000 I: [REFACTOR] Using: APKEditor version 1.4.3, ARSCLib version 1.3.6
      -i = input.apk           
      -o = input_refactored.apk
 _____________________________ 
00.006 I: [REFACTOR] Loading apk: input.apk
00.105 I: [REFACTOR] Auto refactoring ...
00.105 I: [REFACTOR] Validating resource names ...
00.279 I: [REFACTOR] Initializing android framework ...
00.279 I: [REFACTOR] Loading android framework for version: 34
00.320 I: [REFACTOR] Initialized framework: android-34 (14)
00.364 I: [REFACTOR] All resource names are valid
00.364 I: [REFACTOR] Validating file paths ...
00.455 I: [REFACTOR] Auto renamed entries
00.644 I: [REFACTOR] Removed unused table strings
Table size changed = 3067996, 3282324
00.644 I: [REFACTOR] Writing apk ...
00.663 I: [REFACTOR] Buffering compress changed files ...
01.587 I: [REFACTOR] Writing files: 6637
01.729 I: [REFACTOR] Writing signature block ...
01.783 I: [REFACTOR] Saved to: input_refactored.apk
```  
<details> <summary><code>more info</code></summary>

```ShellSession
Refactors obfuscated resource names
Options:
  -i                    Input path.
  -o                    Output path. Optional, if not provided then a new file
                        will be generated on same directory as input
  -public-xml           Path of resource ids xml file (public.xml)
                        Loads names and applies to resources from 'public.xml'
                        file
Flags:
  -clean-meta           Cleans META-INF directory along with signature block.
  -f                    Force delete output path.
  -fix-types            Corrects resource type names based on usages and values
  -h | -help | --help   Displays this help and exit.
Examples:
  1)  [Basic]
    java -jar APKEditor.jar x -i path/input.apk -o path/output.apk
```
</details>
</details>

#### 5- Protect
Protects apk resources against almost all known decompile/modify tools.
<details> <summary><code>java -jar APKEditor.jar <b>p</b> -i path/to/input.apk</code></summary>

 ```ShellSession
00.000 I: [PROTECT] Using: APKEditor version 1.4.3, ARSCLib version 1.3.6
      -keep-type = font              
              -i = test.apk          
              -o = test_protected.apk
 ___________________________________ 
00.178 I: [PROTECT] DirectoryConfuser: Confusing ...
00.519 I: [PROTECT] FileNameConfuser: Confusing ...
00.602 I: [PROTECT] TableConfuser: Confusing ...
00.758 I: [PROTECT] TableConfuser: Type names ...
00.831 I: [PROTECT] Writing apk ...
00.858 I: [PROTECT] Buffering compress changed files ...
01.794 I: [PROTECT] Writing files: 6637
01.931 I: [PROTECT] Writing signature block ...
01.987 I: [PROTECT] Saved to: test_protected.apk
```  
<details> <summary><code>more info</code></summary>

```ShellSession
Protects/Obfuscates apk resource files. Using unique obfuscation techniques.
Options:
  -i                    Input path.
  -keep-type            Keep specific resource type names (e.g drawable), By
                        default keeps only <font> resource type.
                         *Can be multiple
  -o                    Output path. Optional, if not provided then a new file
                        will be generated on same directory as input
Flags:
  -confuse-zip          Confuse zip structure. When this turned on:
                         * Apps might crash if accessing apk files directly e.g
                        Class.getResourceAsStream().
                         * Some apk scanners might flag it as "Malformed zip"
  -dic-dir-names        Path to a text file containing a list of directory
                        names separated by new line.
  -dic-file-names       Path to a text file containing a list of file names
                        separated by new line.
  -f                    Force delete output path.
  -h | -help | --help   Displays this help and exit.
  -skip-manifest        Do not protect manifest.
Examples:
  1)  [Basic]
    java -jar APKEditor.jar p -i path/input.apk -o path/output.apk
```
</details>
</details>

#### 6- Info  (⭐NEW⭐)
Prints/dumps from basic up to detailed information of apk.
<details> <summary><code>java -jar APKEditor.jar <b>info</b> -v -resources -i input.apk </code></summary>

 ```ShellSession
Package name=com.mypackage id=0x7f
  type string id=1 entryCount=1
    resource 0x7f010000 string/app_name
      () My Application
      (-de) Meine Bewerbung
      (-ru-rRU) Мое заявление
  type mipmap id=2 entryCount=1
    resource 0x7f020000 mipmap/ic_launcher_round
      () res/mipmap/ic_launcher_round.png
  type drawable id=3 entryCount=1
    resource 0x7f030000 drawable/ic_launcher
      () #006400

```  
<details> <summary><code>more info</code></summary>

```ShellSession
Prints information of apk.
Options:
  -filter-type          Prints only the specified resource type names
                          *This applies only when flag '-resources' used.
                          *Can be multiple.
  -framework            Path of framework file (can be multiple).
  -framework-version    Preferred framework version number
  -i                    Input path.
  -o                    Output path. Optional, if not provided then a new file
                        will be generated on same directory as input
  -res                  Prints resource entries specified by either of:
                          1) Hex or decimal resource id.
                          2) Full resource name e.g @string/app_name.
                         *Can be multiple.
  -xmlstrings           Print the strings of the given compiled xml assets.
                         *Can be multiple
  -xmltree              Prints the compiled xmls in the given assets.
                         *Can be multiple
  -t                    Print types/formats:
                        [text, json, xml]
Flags:
  -activities           Prints main activity class name. If verbose mode,
                        prints all declared activities including
                        <activity-alias>.
  -app-class            Application class name.
  -app-icon             App icon path/value. If verbose mode, prints all
                        configurations.
  -app-name             App name. If verbose mode, prints all configurations.
  -app-round-icon       App round icon path/value. If verbose mode, prints all
                        configurations.
  -configurations       Print the configurations in the APK.
  -dex                  Prints dex information.
  -f                    Force delete output path.
  -h | -help | --help   Displays this help and exit.
  -languages            Print the languages in the APK.
  -list-files           List files inside apk.
  -list-xml-files       List compiled xml files inside apk.
  -locales              Print the locales in the APK.
  -min-sdk-version      Minimum SDK version.
  -package              Package name(application id) from manifest and if
                        verbose mode, prints resource table packages.
  -permissions          Permissions.
  -resources            Prints all resources
  -signatures           Prints signature information.
  -signatures-base64    Prints signature information with base64 certificates.
  -strings              Print the contents of the resource table string pool in
                        the APK.
  -target-sdk-version   Target SDK version.
  -v                    Verbose mode.
  -version-code         App version code.
  -version-name         App version name.
Examples:
  1)  [Basic]
    java -jar APKEditor.jar info -i file.apk
  2)  [Specify output and type]
    java -jar APKEditor.jar info -i path/input.apk -t json -v -o
  info_file.json
  3)  [Print only specific type]
    java -jar APKEditor.jar info -i path/input.apk -resources -filter-type
  drawable
```
</details>
</details>


---

***Build executable jar***
<details> <summary> <code> ./gradlew fatJar </code> </summary>

 ```ShellSession
 
# NB: Due to my lazyness , the dependency ARSCLib.jar is pre-built and placed under APKEditor/libs/ARSCLib.jar or you can build yourself and replace it.
git clone https://github.com/REAndroid/APKEditor
cd APKEditor
./gradlew fatJar
# Executable jar will be placed ./build/libs/APKEditor-x.x.x.jar

 ```
 </details>

***Downloads***
* [Latest release with pre-built executable jar](https://github.com/REAndroid/APKEditor/releases/latest)

***Contribute***
* Everyone is so welcome in this project, if you have some code improvements please make a pull request
* Please share your ideas / thoughts in [discussions](https://github.com/REAndroid/APKEditor/discussions)
* Please create issue you faced while using this tool along with your apk



<details> <summary><i><b>Contact</b></i></summary> 

* Telegram: [@kikfox](https://t.me/kikfox)
* Email: [thekikfox@gmail.com](mailto:thekikfox@gmail.com)

</details>
