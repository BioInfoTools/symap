SyMAP takes as input two sequence genomes or a sequence genome and a FPC physical map. It builds the synteny map and provides a versatile Java interface for query and display.

References:
  * C. Soderlund, M. Bomhoff, and W. Nelson (2011) SyMAP v3.4: a turnkey synteny system with application to plant genomes Nucleic Acids Research 39(10):e68 [Link](http://nar.oxfordjournals.org/content/39/10/e68)
  * C. Soderlund, W. Nelson, A. Shoemaker and A. Paterson (2006) SyMAP: A system for discovering and viewing syntenic regions of FPC maps. Genome Research 16:1159-1168. [Link](http://genome.cshlp.org/content/16/9/1159.abstract)

Contents:
  * Introduction
  * Downloads
  * Documentation
  * Plant synteny

## Introduction ##
The SyMAP package builds the synteny map and provides versatile query and display interface.

Build:
SyMAP takes as input a two sequence genomes or a sequence genome and a FPC physical map. It uses MUMmer (Kurtz et al. 2004) to compute the raw hits between the genomes, and uses BLAT (Kent 2002) to compute the raw hits between the FPC clone end sequences and the genome.  The hits are clustered and filtered using the optional gene annotation. The filtered hits are input to the synteny algorithm, which was designed to discover duplicated regions and form larger-scale synteny blocks, where intervening micro-rearrangements are allowed.

Query and Display:
SyMAP provides extensive interactive Java displays at all levels of resolution along with simultaneous displays of multiple aligned pairs. The synteny blocks from multiple chromosomes may be displayed in a high-level dot plot or three-dimensional view, and the user may then drill down to see the details of a region, including the alignments of the hits to the gene annotation.

Highlights:
  * GUI manager to run synteny computations and view results.
  * Multiple display modes (dot plot, circular, side-by-side, closeup, 3D).
  * Draft sequence ordering by synteny.
  * Construction of cross-species gene families.
  * Complete annotation-based queries.
  * All displays accessible through the web.

## Downloads ##

  * [SyMAP](http://www.agcol.arizona.edu/software/symap/v4.2/download) - jars, documentation and demo files

## Documentation ##

  * [SyMAP Tour](http://www.agcol.arizona.edu/software/symap/v4.2/Tour.html) - snapshots of various SyMAP interfaces
  * [System Guide](http://www.agcol.arizona.edu/software/symap/v4.2/SystemGuide.html) - installing and building a SyMAP mysql database
  * [User Guide](http://www.agcol.arizona.edu/software/symap/v4.2/SystemGuide.html) - the query and display interface

## Plant Synteny ##
  * [SyMAPdb](http://www.symapdb.org) - plant synteny for the following (1) Maize, Rice, Brachypodium, Sorghum; (2) Arabidopsis, Medicago, Soy, Poplar, Grape; (3) Apple, Peach, Cucumber