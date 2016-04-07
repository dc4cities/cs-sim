#!/usr/bin/env perl
my $SRC="src/test/java/eu/dc4cities/benchcs/CentralSystemSimulatorTest.java";

sub  trim { my $s = shift; $s =~ s/^\s+|\s+$//g; return $s };

sub printFooter {
print "</tbody>
</table>
</body>
</html>";
}

sub printDaily {
    my ($testName) = @_;
    open F, "./test/test$testName/daily.txt" or return;
    while (my $line = <F>) {
        print "$line<br/>";
    }
    close F;
}

sub printHeader {
my ($date,$id,$number,$url,$job,$jobUrl) = @_;
$previousBuild = $jobUrl . "ws/html/" . ($number -1) . "/index.html";
$nextBuild = $jobUrl . "ws/html/" . ($number +1) . "/index.html";

print "<!DOCTYPE html>
<html>
<head>
<script type=\"text/javascript\" src=\"../js/shCore.js\"></script>
<script type=\"text/javascript\" src=\"../js/shBrushJava.js\"></script>
<script type=\"text/javascript\">
     SyntaxHighlighter.all()
</script>
<link href=\"../css/shCore.css\" rel=\"stylesheet\" type=\"text/css\" />
<link href=\"../css/shThemeDefault.css\" rel=\"stylesheet\" type=\"text/css\" />
<style>.bodytext {
    font-family: Consolas,Monaco,\"Bitstream Vera Sans Mono\",\"Courier New\",Courier,monospace;
    width: 100%;
    border-collapse: collapse;
}

#trial {
    font-family: Consolas,Monaco,\"Bitstream Vera Sans Mono\",\"Courier New\",Courier,monospace;
    width: 100%;
    border: 1px solid;
    border-collapse: collapse;
}

th {
    font-size: 1.1em;
    text-align: center;
    padding-top: 5px;
    padding-bottom: 4px;
    background-color: #9999CC;
    color: #ffffff;
}

td {
    color: #000000;
    border: 1px solid;
    background-color: #FFFFFF;
}

#trial ul li {
    font-family: monospace;
    font-size: 11px;
    text-align: left;
}

ul li {
    font-family: monospace;
    font-size: 16px;
    text-align: left;
}

.headcell {
    text-align : center;
}

img {
    height: 200px;
}

small {
    font-size: 8pt;
}

</style>
</head>

<body>
    <h2 class=\"bodytext\" align=\"left\">
        Project <a href='$jobUrl'>$job</a>
        build <a href='$url'>$number</a>
        $date
    </h2>
    <ul>
        <li><a href=\"$url\">Link to build $number </a></li>
        <li><a href=\"$previousBuild\" >Previous build</a></li>
        <li><a href=\"$nextBuild\" >Next build</a></li>
        <li><a href=\"$url/console\">Log output of build $number</a></li>
        <li><a href=\"./quick-reports.txt\">Quick-reports</a></li>
        <li><a href=\"./quick-diff.txt\">Quick-diff</a></li>
    </ul>
    <table  id=\"trial\">
    <thead>
    <tr>
        <th>Test case</th>
        <th>Renewable percentage in source</th>
        <th>Power usage per energy type</th>
        <th>Power usage per activity</th>
        <th>Wasted pure renewable power</th>
        <th>Performance per activity</th>
        <th>Consolidation duration</th>
        <th>Consolidation improvement</th>
        <th>Numerical results</th>
        <th style=\"min-width: 200px;\">Daily results</th>
        <th>Parameters</th>
    </tr>
    </thead>
    <tbody>
";
}

sub printLine {
    my ($testName,$params) = @_;
    open (my $RENPCT, "<", "./test/test$testName/sourceRenPct.txt");
    {
        local $/;
        $sourceRenPct = <$RENPCT>;
    }
    close($RENPCT);
    open (my $RENPCT, "<", "./test/test$testName/DC4CitiesRenPct.txt");
    {
        local $/;
        $DC4CitiesRenPct = <$RENPCT>;
    }
    close($RENPCT);
print "
<tr>
    <td><h3>$testName</h3></td>
    <td><a href='./test/test$testName/forecast.pdf'><img src='./test/test$testName/forecast.png'></a></td>
    <td><a href='./test/test$testName/power.pdf'><img src='./test/test$testName/power.png'></a></td>
    <td><a href='./test/test$testName/power_per_datacenter.pdf'><img src='./test/test$testName/power_per_datacenter.png'></a></td>
    <td><a href='./test/test$testName/renewable-waste.pdf'><img src='./test/test$testName/renewable-waste.png'></a></td>
    <td><a href='./test/test$testName/performance.pdf'><img src='./test/test$testName/performance.png'></a></td>
    <td><a href='./test/test$testName/cons-durations.pdf'><img src='./test/test$testName/cons-durations.png'></a></td>
    <td><a href='./test/test$testName/cons-quality.pdf'><img src='./test/test$testName/cons-quality.png'></a></td>
    <td> $sourceRenPct </br> $DC4CitiesRenPct </td>
    <td>
    ";
    printDaily($testName);
    print "</td><td><small><pre class=\"brush: java\">$params</pre></small></td>
</tr>
";
}

printHeader(@ARGV);
open JAVA, $SRC or die("Unable to open '" + $SRC + "'");
while (my $line = <JAVA>) {
    if ($line =~ /\@Test/) {
        #Got a unit test, the test name is in the next line
        my $params = "$line";
        $line = <JAVA>;
        $params = $params .$line;
        my ($testName) = $line =~ /test(\w+)\(\)/;
        while ($line = <JAVA>) {
            if ($line =~ /\}\);/) {
                $params = $params .$line;
                last;
            } else {
                $params = $params .$line;
            }
        }
        printLine($testName,$params);
    }
}
close(JAVA);
printFooter();
