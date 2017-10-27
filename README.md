### Geo-search Example

This repo contains an example of how to implement geographical 
search using the s2-geometry library originally from Google.

### Quick Gratification

If you want to cut directly to the chase, you can look at the Geo class. The important
method there is `scan` which breaks down a circular region centered at a point
into a small number of sequential scans of quasi-rectangular regions. Because
of the way that the geohashing works in S2, each of these regions corresponds
to cells that have sequential cell id's. Since we assume that we are storing
data ordered by cell ID, this strategy gives us pretty efficient searches. 

This `scan` method isn't usable as it stands. For one thing, it assumes that 
the world is a 3km by 3km patch of land next to the MapR headquarters and 
assumes geographic coordinates are expressed in meters east and north from 
that point. Also, it is assumed that data is stored in a TreeSet rather than
in a database. These defects should be really easy to fix.
 
### Basic Ideas

In the S2-geometry library, the entire surface of the world is divided into 
four sided square-ish regions. Depending on the so-called level, these 
regions can be as small as roughly a centimeter on a side or as large as 
a sixth of the entire surface of the earth. At any particular level, regions 
are ordered using a Hilbert curve so that a sequence of four regions will 
cover the region that is one levels up.

There are easy methods in the S2 library for finding a small set of regions
that overlap with an arbitrary latitude/longitude box or a circle centered at 
any given point. You can also define arbitrary polygons and get a list of 
regions that cover the polygon. These covering regions will be as large as
possible to minimize their number, but will be limited in size to limit 
the spillover where the regions cover more area than the original shape.

The ID's for regions of various sizes are cleverly selected so that if 
you take the ID's for consecutive regions of a particular level, you can
know that all of the ID's for all overlapping regions at a lower level
will be between these two limiting ID's.

Here is a picture of how a search works. We want to search for red dots 
that are inside the circle. S2 provides us with a list of the squishy
rectangles, each of which represents a sequential scan of S2 id's.
![searching for points in a circle](/s2-search.png)
  
