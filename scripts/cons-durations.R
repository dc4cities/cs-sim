#!/usr/bin/env Rscript
require("ggplot2")
require("plyr")
args <- commandArgs(trailingOnly = TRUE)

df <- read.table(paste(args[1],"/optionConsolidator.csv",sep=""), sep=";", quote="\"", header=T)

firsts = ddply(df, .(slot), function(z) {
    z[z$duration == min(z$duration), ][1, ]
})
firsts$type="first";
lasts = ddply(df, .(slot), function(z) {
    z[z$duration == max(z$duration), ][1, ]
})
lasts$type="last";
dta <- rbind(firsts,lasts);
names(dta)[names(dta)=="type"] <- "solution"
p <- ggplot(dta, aes(duration)) + stat_ecdf(aes(colour=solution))
p <- p  + theme_bw() + xlab("duration (ms)") + ylab("ratio of problems") + theme(legend.position="top");
ggsave(paste(args[1],"/cons-durations.pdf",sep=""), height=3, width=3);
ggsave(paste(args[1],"/cons-durations.png",sep=""), height=3, width=3);