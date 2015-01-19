use warnings;
use strict;
$| = 1;

if (not -d "data")
{
	print "Please run from the symap directory\n";
    exit(0);
}

my @seqdirs = <data/pseudo_pseudo/*_to_*>;
my @fpcdirs = <data/fpc_pseudo/*_to_*>;

if (0 == (scalar(@seqdirs)+scalar(@fpcdirs)))
{
	print "No directories found to fix.\n";
    print "Is this the right symap directory?\n";
    exit(0);
}	

foreach my $dir (@seqdirs, @fpcdirs)
{
	my $doneFile = "$dir/all.done";
    if (not -f $doneFile)
    {
    	open F, ">$doneFile" or die "unable to create $doneFile\n";
        close F;
        print "created $doneFile\n";
    }
}
