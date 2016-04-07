#!/usr/bin/env Rscript

# graph.R directory renGoal renPenalty build#
args <- commandArgs(TRUE)

cat("Federated Graph\n")

sla <- read.csv("sla.csv", sep=";")
slaGoal=as.integer(args[3])
slaRenPenalty=as.integer(args[4])

setwd(args[2])
getwd()
png(paste("graph",args[5],"#",slaGoal,slaRenPenalty,".png", sep=""), width = 1000, height = 1000 )

split.screen(c(2,2))



renpct <- 0 
eascs <- read.csv("eascs.csv", sep=";")

eascA <- eascs[which(eascs$activity == "website") ,]
eascGA <- eascs[which(eascs$activity == "indexing_global") ,]
eascEA <- eascs[which(eascs$activity == "indexing_elearning") ,]


easc1 <- eascA[which(eascA$datacenter == "hp_milan") ,]
eascG1 <- eascGA[which(eascGA$datacenter == "hp_milan") ,]

easc2 <- eascA[which(eascA$datacenter == "hp_boston") ,]
eascE2 <- eascEA[which(eascEA$datacenter == "hp_milan") ,]

easc3 <- eascA[which(eascA$datacenter == "hp_phoenix") ,]

#easc <- easc1 + easc2 + easc3
#eascG <- eascG1 + eascG2 + eascG3
#eascE <- eascG1 + eascG2 + eascG3


forecast <- read.csv("forecasts.csv", sep=";")


pv1 <- forecast[which(forecast$source == "hp_milan_pv") ,]
pv2 <- forecast[which(forecast$source == "hp_boston_pv") ,]
pv3 <- forecast[which(forecast$source == "hp_phoenix_pv") ,]

grid <- forecast[which(forecast$source == "hp_milan_grid") ,]
grid1 <- forecast[which(forecast$source == "hp_milan_grid") ,]
grid2 <- forecast[which(forecast$source == "hp_boston_grid") ,]
grid3 <- forecast[which(forecast$source == "hp_phoenix_grid") ,]

#dev.off()

renWatt <- function (w0, w1, w2, pv, gridpc)
{
  watt <- w0 + w1 + w2;
	ifelse ((watt < pv), watt, pv + (watt-pv)*gridpc/100)
}                 
eascIncome <- function (perf, sla, price, penalty)
{
  ifelse ((perf > sla), price, price + (sla-perf) * penalty)
}

f1 <- function(x)
{
  sprintf("%.1f", x)
}

fill_area <- function(xx, yy, color, alp=255)
{
  colrgb <- col2rgb(color, alpha=T)
  px <- c(xx, rev(xx))
  py <- c(rep(0, NROW(xx)), rev(yy))
# cat("colrgb = ", colrgb )
  polygon(px, py, col=rgb(colrgb[1], colrgb[2], colrgb[3], alp, max=255))
}

screen(1)

plot (easc1$slot, easc1$performance, type="n", xlim=c(0,100), ylim=c(0,4000))
lines (easc1$slot,easc1$performance, col="blue") 

ren <- renWatt(easc1$watts, eascG1$watts , 0, pv1$capacity, grid1$renPct)
renpct <- sum(ren) / (sum (easc1$watts) + sum(eascG1$watts))* 100
lines (easc1$slot, sla$Sla, col="black", lty = 2)
totWatts <- (mean(easc1$watts) + mean(eascG1$watts ))*24
totRen <- mean(ren)*24

cat("Total watts = ", totWatts, " Total ren = ", totRen, "\n")

ren1 = totRen
tot1 = totWatts

energyCost1 <- (totWatts - totRen)* grid$price[1]/1000
cat("Energy cost = ", energyCost1, "\n")
title <- paste("Total DC1 RenPct = ", f1(renpct), "\n(Build", args[5], "Goal", slaGoal,"Penalty",slaRenPenalty,")")
###income <- eascIncome(easc1$performance + easc2$performance + easc3$performance, sla$Sla, sla$Price, sla$SlaPenalty)
###totIncome <- sum(income)
###cat("Total Easc Income = ", totIncome, "\n")
renPenalty1 = ifelse ((renpct > slaGoal),0,(slaGoal - renpct)*slaRenPenalty)
cat("Ren Penalty 1 = ", renPenalty1, "\n")

par(new=T)
plot (easc1$slot, grid1$renPct, type="n", xlim=c(0,100), ylim=c(0,1000), ylab="", main=title, axes=F)
fill_area (easc1$slot,pv1$capacity, "yellow", 100)
lines (easc1$slot,ren, col="green", ylab="pv,ren")
lines (easc1$slot,easc1$watts, col="red")
lines (easc1$slot,eascG1$watts , col="orange")
#lines (easc1$slot,eascE2$watts, col="brown")
fill_area (easc1$slot,grid1$renPct, "gray", 100)
lines (easc1$slot,pv1$capacity, col="yellow")

axis (side=4)

leg.txt <- c("biz perf", "pv", "gridPct", "power", "renPower", "sla")
leg.col <- c("blue", "yellow", "gray", "red", "green", "black")
legend (list(x=0, y=1000), legend=leg.txt, col=leg.col, lty=1)
cat("RenPct = ", renpct, " Goal = ", slaGoal, "\n")

leg2.txt <- c(paste("Ren Penalty ", f1(renPenalty1)))
legend (list(x=70, y=1000), legend=leg2.txt)

screen(2)


plot (easc1$slot, easc2$performance, type="n", xlim=c(0,100), ylim=c(0,4000))
lines (easc1$slot,easc2$performance, col="blue") 


ren <- renWatt(easc2$watts, 0 , eascE2$watts, pv2$capacity, grid2$renPct)
renpct <- sum(ren) / (sum (easc2$watts) + sum(eascE2$watts))* 100
lines (easc1$slot, sla$Sla, col="black", lty = 2)
totWatts <- (mean(easc2$watts) + mean(eascE2$watts ))*24
totRen <- mean(ren)*24
cat("Total watts = ", totWatts, " Total ren = ", totRen, "\n")

ren2 = totRen
tot2 = totWatts

energyCost2 <- (totWatts - totRen)* grid$price[1]/1000
cat("Energy cost = ", energyCost2, "\n")
title <- paste("Total DC2 RenPct = ", f1(renpct), "\n(Build", args[5], "Goal", slaGoal,"Penalty",slaRenPenalty,")")
###income <- eascIncome(easc1$performance + easc2$performance + easc3$performance, sla$Sla, sla$Price, sla$SlaPenalty)
###totIncome <- sum(income)
###cat("Total Easc Income = ", totIncome, "\n")
renPenalty2 = ifelse ((renpct > slaGoal),0,(slaGoal - renpct)*slaRenPenalty)
cat("Ren Penalty 2 = ", renPenalty2, "\n")

par(new=T)
plot (easc1$slot, grid2$renPct, type="n", xlim=c(0,100), ylim=c(0,1000), ylab="", main=title, axes=F)
fill_area (easc1$slot,pv2$capacity, "yellow", 100)
lines (easc1$slot,ren, col="green", ylab="pv,ren")
lines (easc1$slot,easc2$watts, col="red")
#lines (easc1$slot,eascG1$watts , col="orange")
lines (easc1$slot,eascE2$watts, col="brown")
fill_area (easc1$slot,grid2$renPct, "gray", 100)
lines (easc1$slot,pv2$capacity, col="yellow")

axis (side=4)

leg.txt <- c("biz perf", "pv", "gridPct", "power", "renPower", "sla")
leg.col <- c("blue", "yellow", "gray", "red", "green", "black")
legend (list(x=0, y=1000), legend=leg.txt, col=leg.col, lty=1)
cat("RenPct = ", renpct, " Goal = ", slaGoal, "\n")

leg2.txt <- c(paste("Ren Penalty ", f1(renPenalty2)))
legend (list(x=70, y=1000), legend=leg2.txt)

screen(3)


plot (easc1$slot, easc3$performance, type="n", xlim=c(0,100), ylim=c(0,4000))
lines (easc1$slot,easc3$performance, col="blue") 


ren <- renWatt(easc3$watts, 0 , 0, pv3$capacity, grid3$renPct)
renpct <- sum(ren) / (sum (easc3$watts))* 100
lines (easc1$slot, sla$Sla, col="black", lty = 2)
totWatts <- (mean(easc3$watts))*24
totRen <- mean(ren)*24
cat("Total watts = ", totWatts, " Total ren = ", totRen, "\n")

ren3 = totRen
tot3 = totWatts

energyCost3 <- (totWatts - totRen)* grid$price[1]/1000
cat("Energy cost = ", energyCost3, "\n")
title <- paste("Total DC3 RenPct = ", f1(renpct), "\n(Build", args[5], "Goal", slaGoal,"Penalty",slaRenPenalty,")")
###income <- eascIncome(easc1$performance + easc2$performance + easc3$performance, sla$Sla, sla$Price, sla$SlaPenalty)
###totIncome <- sum(income)
###cat("Total Easc Income = ", totIncome, "\n")
renPenalty3 = ifelse ((renpct > slaGoal),0,(slaGoal - renpct)*slaRenPenalty)
cat("Ren Penalty 3 = ", renPenalty3, "\n")

par(new=T)
plot (easc1$slot, grid3$renPct, type="n", xlim=c(0,100), ylim=c(0,1000), ylab="", main=title, axes=F)
fill_area (easc1$slot,pv3$capacity, "yellow", 100)
lines (easc1$slot,ren, col="green", ylab="pv,ren")
lines (easc1$slot,easc3$watts, col="red")
#lines (easc1$slot,eascG1$watts , col="orange")
#lines (easc1$slot,eascE2$watts, col="brown")
fill_area (easc1$slot,grid3$renPct, "gray", 100)
lines (easc1$slot,pv3$capacity, col="yellow")

axis (side=4)

leg.txt <- c("biz perf", "pv", "gridPct", "power", "renPower", "sla")
leg.col <- c("blue", "yellow", "gray", "red", "green", "black")
legend (list(x=0, y=1000), legend=leg.txt, col=leg.col, lty=1)
cat("RenPct = ", renpct, " Goal = ", slaGoal, "\n")

leg2.txt <- c(paste("Ren Penalty ", f1(renPenalty3)))
legend (list(x=70, y=1000), legend=leg2.txt)

screen(4)

renpct = (ren1 + ren2 + ren3) /(tot1 + tot2 + tot3)  * 100
title <- paste("Federation Total RenPct = ", f1(renpct), "\n(Build", args[5], "Goal", slaGoal,"Penalty",slaRenPenalty,")")
plot (easc1$slot, pv1$capacity, type="n", xlim=c(0,100), ylim=c(0,1000), col="yellow", ylab="Watt", main=title)




tot_fill <- easc1$watts+easc2$watts+easc3$watts+eascG1$watts+eascE2$watts;

fill_area(easc1$slot, tot_fill, "brown")
tot_fill <- tot_fill - eascG1$watts;
fill_area(easc1$slot, tot_fill, "orange")
 tot_fill <- tot_fill - eascE2$watts;
fill_area(easc1$slot, tot_fill, "blue");
tot_fill <- tot_fill - easc3$watts;
fill_area(easc1$slot, tot_fill, "green");
tot_fill <- tot_fill - easc2$watts;
fill_area(easc1$slot, tot_fill, "red");
tot_fill <- tot_fill - easc1$watts;


lines (easc1$slot,pv1$capacity, col="yellow", lwd=3)
lines (easc1$slot,pv2$capacity, col="yellow", lwd=3)
lines (easc1$slot,pv3$capacity, col="yellow", lwd=3)
lines (easc1$slot, sla$Sla/4, col="black", lwd=2)
#lines (easc1$slot,grid$renPct, col="gray", lwd=2) 
lines (easc1$slot,(easc1$performance + easc2$performance + easc3$performance)/4, col="blue") 


leg.txt <- c("biz perf", "pv", "sla", "Web1", "Web2", "Web3", "G1", "E2")
leg.col <- c("blue", "yellow", "black", "red", "green", "blue", "orange", "brown")
legend (list(x=0, y=1000), legend=leg.txt, col=leg.col, lty=1)


income <- eascIncome(easc1$performance + easc2$performance + easc3$performance, sla$Sla, sla$Price, sla$SlaPenalty)
totIncome <- sum(income)
cat("Total Easc Income = ", totIncome, "\n")

leg2.txt <- c(paste("Income ", f1(totIncome)), paste("Energy ", f1(-energyCost1 - energyCost2 - energyCost3)),
  paste("Ren Penalty ", f1(renPenalty1 + renPenalty2 + renPenalty3)),
  paste("Total ", f1(totIncome - energyCost1 - energyCost2 - energyCost3 + renPenalty1 + renPenalty2 + renPenalty3)))
legend (list(x=70, y=1000), legend=leg2.txt)

close.screen(all=TRUE)
  
dev.off()

