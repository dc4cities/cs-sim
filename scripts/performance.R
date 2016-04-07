#!/usr/bin/env Rscript
library(ggplot2)
library(reshape2)
library(plyr)

# Parse command line arguments.
args <- commandArgs(trailingOnly = TRUE)

# Read easc file to compute usage per timeslot
easc <- read.table(paste(args[1],"/eascs.csv",sep=""), sep=";", quote="\"", header=T)

#create a column to have unique na  mes of activities concatenated to the easc name
easc$activity <- paste(easc$easc, easc$activity, sep=":")

# Correctly parse date and time to R format
easc$time <- strptime(easc$time, "%Y-%m-%dT%H:%M:%S.000")

ggplot(easc, aes(x=time,y=performance)) +
  geom_area(aes(colour=activity, fill=activity), position='stack') +
  theme_bw() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1)) +
  theme(legend.position="top", axis.title.x = element_blank())


ggsave(paste(args[1],"/performance.pdf",sep=""), width=6, height=3)

# Generate also a png file, more web friendly to show report on jenkings.
ggsave(paste(args[1],"/performance.png",sep=""), width=6, height=3)

