#!/usr/bin/env Rscript
require("ggplot2")
require("plyr")

args <- commandArgs(trailingOnly = TRUE)

dta <- read.table(paste(args[1],"/optionConsolidator.csv",sep=""), sep=";", quote="\"", header=T)

firsts = ddply(dta, .(slot), function(z) {
    z[z$duration == min(z$duration), ][1, ]
})

dta <- merge(dta, firsts, by="slot");
names(dta) <- gsub("\\.", "", names(dta));
dta$delta = dta$durationx - dta$durationy;
dta$improvement = (dta$profitx - dta$profity) / 10000;# / dta$profitx * 100;
p <- ggplot(dta,aes(x=delta, y=improvement)) + geom_point(shape=3, size=1)  + ylab("improvement (euros)") + xlab("delay (ms)") + theme(legend.position="top") + geom_jitter();
ggsave(paste(args[1],"/cons-quality.pdf",sep=""), height=3, width=3)
ggsave(paste(args[1],"/cons-quality.png",sep=""), height=3, width=3)
