#!/usr/bin/env Rscript
library(ggplot2)
require(reshape2)

args = commandArgs(trailingOnly=TRUE)

input = paste(args[1],".csv",sep="")

cbPalette <- c("#0072B2", "#000000", "#E69F00", "#56B4E9", "#009E73", "#F0E442", "#D55E00", "#CC79A7")


#SMA
df = read.csv(input,header=T,sep=';')
d <- df[df$expense=="SMA_pct" & df$pp==df$with,]
p <- ggplot(d, aes(x=factor(pp, levels=c("perf","Carver","green")), weight=amount, fill=expense)) 
p <- p + geom_bar() + coord_cartesian(ylim=c(40,70))
p <- p + geom_text(aes(label=amount, y=amount), position=position_dodge(width=0.9), vjust=1.5, color="white")
p <- p + theme_bw() + xlab("") + ylab("renewable %") + facet_grid( ~ day) +  scale_fill_manual(values=cbPalette)
p <- p + theme(legend.position="none", axis.text = element_text(size=10), axis.title = element_text(size=12), strip.text.x = element_text(size = 14))
p <- p + theme(axis.text.x = element_text(size=16, angle=45, vjust = 1, hjust=1))
output = paste(args[1],"-sma.pdf",sep="")
ggsave(output, width=8, height=3.5)

cat("*** SMA percentage ***\n")
long <- dcast(d[,c("day","amount","pp")], day ~ pp, value.var="amount")
long$dPerf = (long$perf - long$Carver) / long$perf * 100
long$dGreen = (long$green - long$Carver) / long$green * 100
print(long)
cat("mean diff wrt. perf: ",mean(long$dPerf),"\n")
cat("mean diff wrt. green:" , mean(long$dGreen),"\n")


#energy
d <- df[df$expense=="energy_w" & df$pp==df$with,]
d$amount <- d$amount / 1000 * 24
p <- ggplot(d) 
p <- p + geom_bar(aes(x=factor(pp, levels=c("perf","green", "Carver")), weight=amount, fill=expense)) + coord_cartesian(ylim=c(0,300))
p <- p + geom_text(aes(x=factor(pp), label=floor(amount), y=amount), position=position_dodge(width=0.9), vjust=1.5, color="white")
p <- p + theme_bw() + xlab("") + ylab("kWatts/h") + facet_grid( ~ day)
p <- p + theme(legend.position="none", axis.text = element_text(size=10), axis.title = element_text(size=12), strip.text.x = element_text(size = 14)) + scale_fill_manual(values=cbPalette)
p <- p + theme(axis.text.x = element_text(size=16, angle=45, vjust = 1, hjust=1))
output = paste(args[1],"-energy.pdf",sep="")
cat("\n\n*** kWatts ***\n")
long <- dcast(d[,c("day","amount","pp")], day ~ pp, value.var="amount")
long$dPerf = (long$perf - long$Carver) / long$perf * 100
long$dGreen = (long$green - long$Carver) / long$green * 100
print(long)
cat("mean diff wrt. perf: ",mean(long$dPerf),"\n")
cat("mean diff wrt. green:" , mean(long$dGreen),"\n")

#Energy dispatch

ggsave(output, width=8, height=3.5)


#SLO
d <- df[df$expense=="SLO" & df$pp==df$with,]
d$amount <- 1999.96 - d$amount
p <- ggplot(d) + geom_bar(aes(x=factor(pp, levels=c("perf","green","Carver")), weight=amount, fill=expense)) + coord_cartesian(ylim=c(0,2200))
p <- p + geom_text(aes(x=factor(pp), label=floor(amount), y=amount), position=position_dodge(width=0.9), vjust=1.5, color="white")
p <- p + theme_bw() + xlab("") + ylab("SLO penalty (euros)") + facet_grid( ~ day)
p <- p + theme(legend.position="none", axis.text = element_text(size=10), axis.title = element_text(size=12), strip.text.x = element_text(size = 14)) +  scale_fill_manual(values=cbPalette)
p <- p + theme(axis.text.x = element_text(size=16, angle=45, vjust = 1, hjust=1))
output = paste(args[1],"-slo.pdf",sep="")

cat("\n\n*** SLO ***\n")
ggsave(output, width=8, height=3.5)

long <- dcast(d[,c("day","amount","pp")], day ~ pp, value.var="amount")
long$dPerf = (long$perf - long$Carver) / long$perf * 100
long$dGreen = (long$green - long$Carver) / long$green * 100
print(long)
cat("mean diff wrt. perf: ",mean(long$dPerf),"\n")
cat("mean diff wrt. green:" , mean(long$dGreen),"\n")


#Only economical stuff
cat("\n\n*** Running costs ***\n")
d <- df[df$label=="Carver" | df$label=="perf" | df$label=="green",]
d <- d[d$expense=="SLO" | d$expense=="energy" | d$expense=="SMA",]
p <- ggplot(d) + geom_bar(aes(x=factor(label, levels=c("perf","green","Carver")), weight=amount, fill=expense)) 
#p <- p + geom_text(aes(x=factor(pp), label=amount, y=amount), position=position_dodge(width=0.9), vjust=-0.25)
p <- p + theme_bw() + xlab("") + ylab("Running cost (euros)") + facet_grid( ~ day) + theme(axis.text = element_text(size=10), axis.title = element_text(size=12), strip.text.x = element_text(size = 14))
p <- p + scale_colour_brewer()#scale_fill_manual(values=cbPalette)
p <- p + theme(axis.text.x = element_text(size=16, angle=45, vjust = 1, hjust=1))
output = paste(args[1],"-runningCosts.pdf",sep="")
#print(d);
long <- dcast(d[,c("day","amount","pp")], day ~ pp, value.var="amount", fun.aggregate = sum)
long$dPerf = (long$perf - long$Carver) / long$perf * 100
long$dGreen = (long$green - long$Carver) / long$green * 100

print(long)
cat("mean diff wrt. perf: ",mean(long$dPerf),"\n")
cat("mean diff wrt. green:" , mean(long$dGreen),"\n")

ggsave(output, width=8, height=4)