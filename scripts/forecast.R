#!/usr/bin/env Rscript
library(ggplot2)
library(reshape2)
library(plyr)

# Parse command line arguments.
args <- commandArgs(trailingOnly = TRUE)

# Read easc file to compute usage per timeslot
easc <- read.table(paste(args[1],"/eascs.csv",sep=""), sep=";", quote="\"", header=T)

# Read forecasts.csv generated by Statistics.java on cs-bench project.
forecasts <- read.table(paste(args[1],"/forecasts.csv", sep=""), sep=";", quote="\"", header=T, stringsAsFactors = FALSE)

# Adapt forecasts column name to merge tables correctly.
forecasts <- rename(forecasts, c("dc"="datacenter"))

# add time to the plots (instead of timeslots).
power <-
  ddply(
    forecasts, .(slot, datacenter), 
    function(x) {
      # Preserve some parameters.
      slot = x$slot
      datacenter = x$datacenter
      source = x$source
      at = x$at
      renPct = x$renPct
      
      # Easc subset, datacenter, slot.
      easc.subset = easc[(
        (easc$slot == slot) & 
          (easc$datacenter == datacenter)),
        ]

      time <- easc.subset$time[1]
      
      newdf <- data.frame(slot, datacenter, source, at, renPct, time)
      newdf
    }
  )

#Write the mean ren percent in a file
write(paste("ren percent in sources=", mean(forecasts$renPct), "%"), file = paste(args[1],"/sourceRenPct.txt",sep=""))

# Correctly parse date and time to R format
power$time <- strptime(power$time, "%Y-%m-%dT%H:%M:%S.000")

# Show one graph per datacenter of power usage, renewable, and total available 
p <- ggplot(data = power, aes(x = time, y = renPct, color = source)) +
  geom_line(aes(group = source)) +
  theme_bw() +
  theme(axis.text.x = element_text(angle = 45, hjust = 1)) +
  theme(legend.position="top", axis.title.x = element_blank())

# Generate a pdf file.
ggsave(paste(args[1],"/forecast.pdf",sep=""), width=6, height=3)
# Generate also a png file, more web friendly to show report on jenkings.
ggsave(paste(args[1],"/forecast.png",sep=""), width=6, height=3)

