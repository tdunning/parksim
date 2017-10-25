### Run this from the project main directory after running tests:
### $ R
### > source("src/test/R/plot-scans.r")

x = read.csv("scans.csv")
plot(y~x, x[x$j >= 0,], xlim=c(1470, 1530), ylim=c(1470, 1530), cex=0.2)
for (i in 0:7) {
    lines(y~x, rbind(x[x$i==i&x$j>=0,],x[x$i==i&x$j>=0,]))
}
points(y~x, x[x$j < 0,], pch=21, bg='red', col=NA)
th = seq(0, 2*pi, length.out=1000)
lines(cos(th)*20+1500, sin(th)*20+1500)