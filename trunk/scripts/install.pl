#!/usr/bin/env perl
#
# Soderlund Lab, 2010
#
# Purpose: Install SyMAP web-related files
# Usage: perl scripts/install.pl
# reads symap.config

use warnings;
use strict;

use Cwd;
use symap;

$| = 1; # enable autoflush output

sub usage {
  print "\nUsage: install.pl [-h | -r] <configuration file>\n";
  print " Execute from top level directory, i.e. perl scripts/install.pl\n";
  print " It reads file symap.config in the top level directory\n";
  print " -r  remove previous installation\n";
  print " -h  this message\n";
  print " <configuration file> alterative file for symap.config\n\n";
  exit;
}

my $this_path = getcwd();
my $params_file = "";
my $remove = 0;

if (@ARGV) {
  my $arg = shift if (@ARGV);
  if ($arg eq "-h" or $arg eq "-help" or $arg eq "?") { usage(); }
  elsif ($arg eq "-r") {$remove = 1;}
  else {
      $params_file = $arg;
      if (not -f "$this_path/$params_file") {
        print "\nError: file $params_file does not exist\n";
        usage();
	    }
  }
}

if ($params_file eq "") {
  if (-f "$this_path/symap.config") {
     $params_file = "symap.config";
  }
  elsif (-f "$this_path/params") {
      $params_file = "params";
  }
  else {
 	  print "Error: symap.config does not exist in the top-level directory\n";
    usage();
  }
}

#
# Load params file
#
print "Reading file $params_file\n";
my %params;
$params{logfile} = "";
read_parameters($params_file,\%params);

my $dbname = $params{db_name};

$dbname = "symap" if ($dbname eq "");
my $dbserver = $params{db_server};
if ($dbserver =~ /localhost/i)
{
	print "Error: Replace server name 'localhost' with full IP address.\n";
    exit;
}
if ($dbserver eq "")
{
	print "Error:Must enter db_server parameters (provide full IP address of webserver).\n";
	exit;
}
my $htd = $params{html_path};
my $cgd = $params{cgi_path};
my $www = $params{html_url};

if ($htd eq "" or $cgd eq "" or $www eq "") 
{ 
    print "Error: A path parameter field (html_path, cgi_path, html_url) is blank\n";
    print "       Please edit the $params_file file.\n";
    exit;
}

my $useSigned = 0;
if (not $www =~ /$dbserver/)
{
	print "The web server host does not appear to be the same as datatabase host.\n";
    print "(Web:$www , Database:$dbserver)\n";
    print "For this to work, the signed applet must be used.\n";
    print "Web users will see a security popup which they will need to approve.\n";
    if (!yesno("Continue"))
    {
    	exit;
    }
    $useSigned = 1;
}
#
# Remove existing web installation
#

if ($remove) {
  print "Removing previous installation\n";
  execute("rm -rf $htd");
  execute("rm -rf $cgd");
}
#
# Copy over web files
#

my @files;
execute("mkdir $htd") if (not -e $htd);
if (not -e $htd) {
    print "Error: could not make $htd\n";
    exit;
}
my $jarName = ($useSigned ? "symapSigned.jar" : "symap.jar");
execute("cp -fr html/* $htd");
execute("cp -fr java/jar/$jarName $htd/symap.jar");
execute("cp -fr java/jar/plugin.jar $htd");
execute("cp -fr java/jar/applet-launcher.jar $htd");
execute("cp -fr java/jar/gluegen-rt.jar $htd");
execute("cp -fr java/jar/jogl.jar $htd");
execute("cp -fr java/jar/j3dcore.jar $htd");
execute("cp -fr java/jar/j3dutils.jar $htd");
execute("cp -fr java/jar/vecmath.jar $htd");
execute("chmod 777 $htd/png");
execute("chmod 777 $htd/jpeg");

execute("mkdir $cgd") if (not -e $cgd);
if (not -e $cgd) {
    print "Error: could not make $cgd\n";
    exit;
}
execute("cp -fr cgi/* $cgd");
execute("cp -f $this_path/symap.pm $cgd/symap.pm");

#
# Set site-specific parameters in params files
#

my $jdbstr = "jdbc:mysql://".$params{db_server}."/".$params{db_name};

if (defined $params{site_logo} and $params{site_logo} =~ /\S/ and 
    not $params{site_logo} =~ /^html/ and not $params{site_logo} =~ /^\//)
{
    $params{site_logo} = $params{html_url}."/".$params{site_logo};
}

my $dbstr = "dbi:mysql:$dbname:$dbserver";

fix_file("$cgd/params","DB_NAME",$params{db_name});
fix_file("$cgd/params","PERL_DB_STR",$dbstr);
fix_file("$cgd/params","DB_CLIENT_USER",$params{db_clientuser});
fix_file("$cgd/params","DB_CLIENT_PASSWD",$params{db_clientpasswd});
fix_file("$cgd/params","JAVA_DB_STR",$jdbstr);
fix_file("$cgd/params","SYMAP_HTML_URL",$params{html_url});
fix_file("$cgd/params","SYMAP_CGI_URL",$params{cgi_url});
fix_file("$htd/projects.html","SYMAP_CGI_URL",$params{cgi_url});
fix_file("$cgd/params","SYMAP_HTML_DIR",$params{html_path});
fix_file("$cgd/params","SYMAP_DIR",$this_path);
fix_file("$cgd/params","SITE_LOGO",$params{site_logo});
fix_file("$cgd/params","SITE_URL",$params{site_url});
fix_file("$cgd/params","SITE_SEARCH",$params{site_search});
fix_file("$cgd/params","SITE_CONTACT",$params{site_contact});
fix_file("$cgd/params","SITE_EMAIL",$params{site_email});

print "\nSyMAP web installation complete.\n";
print "To view, go to $www/projects.html\n\n";
exit;

##############################################################################

sub fix_file
{
    my $path = shift;
    my $key = shift;
    my $value = shift;

    $value = "" if (not defined $value);

    undef $/;
    open F, $path or die "Failed to open $path\n";
    my $contents = <F>;
    close F;
    $/ = "\n";
    
    if ($contents =~ /$key/)
    {
        $contents =~ s/$key/$value/g;
    }
    else
    {
        die "keyword $key not found in $path\n";
    }

    open F, ">$path" or die "Failed to open $path\n";
    print F $contents;
    close F;
}

##############################################################################
