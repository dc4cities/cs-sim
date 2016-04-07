#!/usr/bin/env perl -w
if (scalar(@ARGV) != 2) {
	print("Usage set_timestamp.pl origin D");
	print("Pick the timestamp of 'origin' and the data from file 'D'. Print the result on stdout");
	exit 1
}
open (TS, $ARGV[0]) or die("unable to read the timestamps");
open (D, $ARGV[1]) or die("unable to read the data");
while (my $l = <TS>) {
	my @buf = split(/;/, $l);
	my $l2 = <D>;
	if (!$l2) {
		last;
	}	
	print($buf[0], ";");
	my @arr = split(/;/, $l2);
	shift(@arr);
	print join(";", @arr);	
}
close TS;
close D;