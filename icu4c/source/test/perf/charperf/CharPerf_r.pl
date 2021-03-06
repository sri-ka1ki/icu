#!/usr/bin/perl
#  ********************************************************************
#  * Copyright (C) 2016 and later: Unicode, Inc. and others.
#  * License & terms of use: http://www.unicode.org/copyright.html
#  ********************************************************************
#  ********************************************************************
#  * COPYRIGHT:
#  * Copyright (c) 2002-2013, International Business Machines
#  * Corporation and others. All Rights Reserved.
#  ********************************************************************

#use strict;

require "../perldriver/Common.pl";

use lib '../perldriver';

use PerfFramework;

my $options = {
    "title"=>"Character property performance regression: ICU (".$ICUPreviousVersion." and ".$ICULatestVersion.")",
    "headers"=>"ICU".$ICUPreviousVersion." ICU".$ICULatestVersion,
    "operationIs"=>"code point",
    "timePerOperationIs"=>"Time per code point",
    "passes"=>"10",
    "time"=>"5",
    #"outputType"=>"HTML",
    "dataDir"=>"Not Using Data Files",
    "outputDir"=>"../results"
};

# programs

my $p1; # Previous
my $p2; # Latest
if ($OnWindows) {
    $p1 = "cd ".$ICUPrevious."/bin && ".$ICUPathPrevious."/charperf/$WindowsPlatform/Release/charperf.exe";
    $p2 = "cd ".$ICULatest."/bin && ".$ICUPathLatest."/charperf/$WindowsPlatform/Release/charperf.exe";
} else {
    $p1 = "LD_LIBRARY_PATH=".$ICUPrevious."/source/lib:".$ICUPrevious."/source/tools/ctestfw ".$ICUPathPrevious."/charperf/charperf";
    $p2 = "LD_LIBRARY_PATH=".$ICULatest."/source/lib:".$ICULatest."/source/tools/ctestfw ".$ICUPathLatest."/charperf/charperf";
}

my $dataFiles = "";

my $tests = { 
    "isAlpha",        ["$p1 TestIsAlpha"        , "$p2 TestIsAlpha"        ],
    "isUpper",        ["$p1 TestIsUpper"        , "$p2 TestIsUpper"        ],
    "isLower",        ["$p1 TestIsLower"        , "$p2 TestIsLower"        ],
    "isDigit",        ["$p1 TestIsDigit"        , "$p2 TestIsDigit"        ],
    "isSpace",        ["$p1 TestIsSpace"        , "$p2 TestIsSpace"        ],
    "isAlphaNumeric", ["$p1 TestIsAlphaNumeric" , "$p2 TestIsAlphaNumeric" ],
    "isPrint",        ["$p1 TestIsPrint"        , "$p2 TestIsPrint"        ],
    "isControl",      ["$p1 TestIsControl"      , "$p2 TestIsControl"      ],
    "toLower",        ["$p1 TestToLower"        , "$p2 TestToLower"        ],
    "toUpper",        ["$p1 TestToUpper"        , "$p2 TestToUpper"        ],
    "isWhiteSpace",   ["$p1 TestIsWhiteSpace"   , "$p2 TestIsWhiteSpace"   ],
};

runTests($options, $tests, $dataFiles);


